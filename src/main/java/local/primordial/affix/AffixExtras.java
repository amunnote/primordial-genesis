package local.primordial.affix;

import java.util.ArrayList;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.effect.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The second set of ordinary affixes: Shatter, Undying, Blood Pact, Growth, Enlightenment, Befuddlement, Amplify, Mastery.
 * The auras (Undying, Enlightenment, Amplify) are refreshed every second by their bearer and fade 5 s after leaving it.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AffixExtras {
    /** on a mob called by Blood Pact: the player it was called for */
    public static final String BLOOD_PACT_FOR="primordial:blood_pact_for";
    public static final int UNDYING_RADIUS=16,ENLIGHTENMENT_RADIUS=32,AMPLIFY_RADIUS=16,AMPLIFY_TICKS=200;
    public static final int BLOOD_PACT_INTERVAL=160,BLOOD_PACT_MAX=6;
    private AffixExtras() {}

    /** hostile mobs and bosses: the creatures these affixes treat as allies */
    static boolean hostile(Mob mob){return mob.isAlive()&&AffixEvents.eligible(mob,AffixEvents.isBoss(mob));}
    static boolean griefing(ServerLevel level,Mob mob){
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)&&ForgeEventFactory.getMobGriefingEvent(level,mob);
    }
    static boolean ignores(LivingEntity target){return target instanceof Player player&&(player.isCreative()||player.isSpectator());}

    // ------------------------------------------------------------------ hits
    /** called from AffixCombat.landed for every hit that did damage */
    static void landed(AffixState attacker,AffixState defender,LivingEntity victim,DamageSource source) {
        if(attacker!=null&&victim instanceof Player player){
            if(AffixAccessories.acts(attacker,MobAffix.BLOOD_PACT,player))AffixStatuses.applyBloodPact(player);
            if(AffixAccessories.acts(attacker,MobAffix.BEFUDDLEMENT,player))AffixStatuses.applyBefuddled(player);
        }
        if(defender!=null&&source.getEntity() instanceof Player player&&AffixAccessories.acts(defender,MobAffix.BEFUDDLEMENT,player))AffixStatuses.applyBefuddled(player);
    }
    /** Shatter breaks the shield in your off hand, blocked or not */
    @SubscribeEvent(priority=EventPriority.LOW) public static void shatter(LivingAttackEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||AffixCombat.secondary())return;
        var attacker=AffixCombat.source(event.getSource());
        if(!AffixAccessories.acts(attacker,MobAffix.SHATTER,player)||Protection.active(player))return;
        var shield=player.getOffhandItem();
        if(shield.isEmpty()||!shield.canPerformAction(ToolActions.SHIELD_BLOCK))return;
        if(attacker.mob.getRandom().nextInt(100)>=AffixScaling.shatterChance(attacker.level))return;
        breakShield(player);
    }
    public static void breakShield(Player player) {
        var shield=player.getOffhandItem();
        if(player.isUsingItem()&&player.getUsedItemHand()==InteractionHand.OFF_HAND)player.stopUsingItem();
        player.broadcastBreakEvent(InteractionHand.OFF_HAND);
        ForgeEventFactory.onPlayerDestroyItem(player,shield,InteractionHand.OFF_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        player.level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.SHIELD_BREAK,SoundSource.PLAYERS,1F,0.8F);
    }

    // ------------------------------------------------------------------ Undying
    /** like a Totem of Undying, once per mob; after the affix's own Time revival, before the death is processed */
    @SubscribeEvent(priority=EventPriority.HIGH) public static void undying(LivingDeathEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel level))return;
        if(AffixStatuses.undying(mob)<=0||AffixStatuses.undyingUsed(mob)||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        // the killer wards off Undying
        if(event.getSource().getEntity() instanceof Player killer&&AffixAccessories.wears(killer,MobAffix.UNDYING))return;
        event.setCanceled(true);
        AffixStatuses.useUndying(mob);
        mob.removeAllEffects();
        // undead cannot take Regeneration, so they come back with a third of their health instead
        mob.setHealth(mob.isInvertedHealAndHarm()?Math.max(1,mob.getMaxHealth()/3):1);
        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));
        mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,1));
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,800,0));
        level.broadcastEntityEvent(mob,(byte)35);   // the totem's particles and sound
    }

    // ------------------------------------------------------------------ per mob
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel level)||!mob.isAlive())return;
        var state=AffixState.get(mob);
        if(state!=null){
            if(mob.tickCount%20==0){state.expireLent(20);auras(state,level);}
            if(mob.tickCount%AffixGrowth.STEP_TICKS==0)AffixGrowth.step(state);
            if(state.has(MobAffix.MASTERY))mastery(state,level);
        }else if(mob.tickCount%AffixGrowth.STEP_TICKS==0&&mob.getPersistentData().contains(AffixGrowth.KEY))AffixGrowth.set(mob,1);
        if(mob.tickCount%10==0&&AffixStatuses.enlightened(mob)>0)enlightened(mob,level);
    }
    /** a grown mob loaded from disk gets its hitbox back */
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if(event.getEntity() instanceof Mob mob&&mob.getPersistentData().contains(AffixGrowth.KEY))mob.refreshDimensions();
    }

    public static void auras(AffixState state,ServerLevel level) {
        var mob=state.mob;
        if(state.has(MobAffix.UNDYING)){
            if(!state.primordial())mob.addEffect(new MobEffectInstance(MobEffects.GLOWING,40,0,false,false));
            for(var ally:nearby(level,mob,UNDYING_RADIUS,false))if(!AffixStatuses.undyingUsed(ally)){
                if(AffixStatuses.undying(ally)<=0)level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,ally.getX(),ally.getY()+ally.getBbHeight()*0.6,ally.getZ(),6,0.3,0.4,0.3,0.05);
                AffixStatuses.applyUndying(ally);
            }
        }
        if(state.has(MobAffix.ENLIGHTENMENT)){
            for(var ally:nearby(level,mob,ENLIGHTENMENT_RADIUS,true))AffixStatuses.applyEnlightened(ally);
            level.sendParticles(ParticleTypes.ENCHANT,mob.getX(),mob.getY()+mob.getBbHeight()+0.3,mob.getZ(),8,0.4,0.2,0.4,0.6);
        }
        if(state.has(MobAffix.AMPLIFY))for(var ally:nearby(level,mob,AMPLIFY_RADIUS,false))amplify(ally,level);
    }
    static java.util.List<Mob> nearby(ServerLevel level,Mob center,int radius,boolean self) {
        return level.getEntitiesOfClass(Mob.class,center.getBoundingBox().inflate(radius),
            other->(self||other!=center)&&hostile(other)&&center.distanceToSqr(other)<=radius*radius);
    }
    /** lend a random affix it can use, or keep its loan going */
    public static void amplify(Mob ally,ServerLevel level) {
        var state=AffixState.get(ally);if(state==null)return;
        if(!state.temporary.isEmpty()){for(var lent:state.temporary.toArray(MobAffix[]::new))state.lend(lent,AMPLIFY_TICKS);return;}
        var choices=new ArrayList<MobAffix>();
        for(var affix:MobAffix.values())if(affix!=MobAffix.AMPLIFY&&!state.affixes.contains(affix)
            &&AffixEligibility.usable(ally,affix)&&!AffixEligibility.conflicts(affix,state.affixes))choices.add(affix);
        if(choices.isEmpty())return;
        state.lend(choices.get(ally.getRandom().nextInt(choices.size())),AMPLIFY_TICKS);
        level.sendParticles(ParticleTypes.FLAME,ally.getX(),ally.getY()+ally.getBbHeight()*0.5,ally.getZ(),10,0.3,0.5,0.3,0.02);
    }

    // ------------------------------------------------------------------ Mastery
    /** shooters also strike up close; everything else also fires arrows from a distance */
    public static void mastery(AffixState state,ServerLevel level) {
        var mob=state.mob;var target=mob.getTarget();
        if(target==null||!target.isAlive()||ignores(target)||AffixAccessories.blocks(target,state,MobAffix.MASTERY))return;
        long now=level.getGameTime();if(now<state.tag.getLong("masteryNext"))return;
        double distance=mob.distanceToSqr(target);
        if(AffixEligibility.shoots(mob)){
            double reach=Math.max(4,mob.getBbWidth()*2F*mob.getBbWidth()*2F+target.getBbWidth());
            if(distance>reach||mob.getAttribute(Attributes.ATTACK_DAMAGE)==null||!mob.hasLineOfSight(target))return;
            mob.swing(InteractionHand.MAIN_HAND);mob.doHurtTarget(target);
            state.tag.putLong("masteryNext",now+20);
        }else{
            if(distance<16||distance>400||!mob.hasLineOfSight(target))return;
            shoot(mob,target,level);
            state.tag.putLong("masteryNext",now+60);
        }
    }
    public static void shoot(Mob mob,LivingEntity target,ServerLevel level) {
        AbstractArrow arrow=ProjectileUtil.getMobArrow(mob,new ItemStack(Items.ARROW),1F);
        double dx=target.getX()-mob.getX(),dy=target.getY(0.3333)-arrow.getY(),dz=target.getZ()-mob.getZ(),flat=Math.sqrt(dx*dx+dz*dz);
        arrow.shoot(dx,dy+flat*0.2,dz,1.6F,(float)(14-level.getDifficulty().getId()*4));
        double attack=mob.getAttribute(Attributes.ATTACK_DAMAGE)!=null?mob.getAttributeValue(Attributes.ATTACK_DAMAGE):2;
        arrow.setBaseDamage(Math.max(2,attack*0.5));
        arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
        mob.playSound(SoundEvents.SKELETON_SHOOT,1F,1F/(mob.getRandom().nextFloat()*0.4F+0.8F));
        level.addFreshEntity(arrow);
    }

    // ------------------------------------------------------------------ Enlightenment
    private static final String STALL_AT="primordial:enlight_check",STALL_DISTANCE="primordial:enlight_distance",STALLED="primordial:enlight_stalled";
    private static final String BREAKING="primordial:enlight_breaking",PROGRESS="primordial:enlight_progress";
    /** an enlightened mob that cannot reach its target digs through, pillars up, bridges over and digs down */
    public static void enlightened(Mob mob,ServerLevel level) {
        var target=mob.getTarget();
        if(target==null||!target.isAlive()||ignores(target)||AffixAccessories.wears(target,MobAffix.ENLIGHTENMENT)||!griefing(level,mob)||mob.isInFluidType())return;
        var data=mob.getPersistentData();double distance=mob.distanceTo(target);
        if(distance<1.5)return;
        long now=level.getGameTime();
        if(now-data.getLong(STALL_AT)>=40){
            data.putBoolean(STALLED,data.contains(STALL_DISTANCE)&&data.getDouble(STALL_DISTANCE)-distance<0.5);
            data.putDouble(STALL_DISTANCE,distance);data.putLong(STALL_AT,now);
        }
        var path=mob.getNavigation().getPath();
        if(path!=null&&path.canReach()&&!data.getBoolean(STALLED))return;
        BlockPos feet=mob.blockPosition();int height=Math.max(1,Mth.ceil(mob.getBbHeight()));
        double dy=target.getY()-mob.getY();
        if(dy>1.5&&mob.onGround()){
            // pillar up: clear the head room, step up a block and set one where the feet were
            var above=feet.above(height);
            if(solid(level,above)){dig(mob,level,above);return;}
            if(level.getBlockState(feet).canBeReplaced()&&level.noCollision(mob,mob.getBoundingBox().move(0,1.05,0))){
                mob.setPos(mob.getX(),feet.getY()+1.05,mob.getZ());place(level,feet);
            }
            return;
        }
        if(dy<-1.5&&mob.onGround()&&Math.abs(target.getX()-mob.getX())<3&&Math.abs(target.getZ()-mob.getZ())<3){dig(mob,level,feet.below());return;}
        var direction=Direction.getNearest(target.getX()-mob.getX(),0,target.getZ()-mob.getZ());
        var front=feet.relative(direction);
        for(int y=height-1;y>=0;y--)if(solid(level,front.above(y))){dig(mob,level,front.above(y));return;}
        // bridge a gap toward a target that is not below
        var under=front.below();
        if(dy>-1.5&&!solid(level,under)&&level.getBlockState(under).canBeReplaced()&&mob.onGround())place(level,under);
    }
    static boolean solid(ServerLevel level,BlockPos pos){return !level.getBlockState(pos).getCollisionShape(level,pos).isEmpty();}
    public static void place(ServerLevel level,BlockPos pos) {
        level.setBlockAndUpdate(pos,local.primordial.Primordial.ENLIGHTENED_STONE.get().defaultBlockState());
        level.playSound(null,pos,SoundEvents.STONE_PLACE,SoundSource.HOSTILE,1F,0.9F);
    }
    /** breaks a block over time, like a player would: 1 s plus 1.5 s per point of hardness, at most 10 s */
    public static void dig(Mob mob,ServerLevel level,BlockPos pos) {
        var block=level.getBlockState(pos);float hardness=block.getDestroySpeed(level,pos);
        if(block.isAir()||block.hasBlockEntity()||hardness<0||hardness>=50)return;
        var data=mob.getPersistentData();
        if(data.getLong(BREAKING)!=pos.asLong()){data.putLong(BREAKING,pos.asLong());data.putInt(PROGRESS,0);}
        int progress=data.getInt(PROGRESS)+10,needed=(int)Math.min(200,20+hardness*30);
        data.putInt(PROGRESS,progress);
        if(progress<needed){level.destroyBlockProgress(mob.getId(),pos,progress*10/needed);mob.swing(InteractionHand.MAIN_HAND);return;}
        level.destroyBlockProgress(mob.getId(),pos,-1);data.remove(BREAKING);data.remove(PROGRESS);
        if(ForgeEventFactory.onEntityDestroyBlock(mob,pos,block))level.destroyBlock(pos,true,mob);
    }

    // ------------------------------------------------------------------ Blood Pact
    @SubscribeEvent public static void bloodPact(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide)return;var player=event.player;
        if(AffixStatuses.bloodPact(player)>0&&player.tickCount%BLOOD_PACT_INTERVAL==0)callMonster(player);
    }
    /** calls one of the area's monsters 8 to 16 blocks away, up to six at a time */
    public static Mob callMonster(Player player) {
        if(!(player.level() instanceof ServerLevel level))return null;
        if(level.getDifficulty()==Difficulty.PEACEFUL||player.isCreative()||player.isSpectator())return null;
        UUID id=player.getUUID();
        int called=level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(48),
            mob->mob.isAlive()&&mob.getPersistentData().hasUUID(BLOOD_PACT_FOR)&&mob.getPersistentData().getUUID(BLOOD_PACT_FOR).equals(id)).size();
        if(called>=BLOOD_PACT_MAX)return null;
        var random=player.getRandom();
        var spawn=level.getBiome(player.blockPosition()).value().getMobSettings().getMobs(MobCategory.MONSTER).getRandom(random);
        EntityType<?> type=spawn.isPresent()?spawn.get().type:EntityType.ZOMBIE;
        for(int attempt=0;attempt<16;attempt++){
            double angle=random.nextDouble()*Math.PI*2,radius=8+random.nextDouble()*8;
            int x=Mth.floor(player.getX()+Math.cos(angle)*radius),z=Mth.floor(player.getZ()+Math.sin(angle)*radius);
            for(int dy=4;dy>=-4;dy--){
                var pos=new BlockPos(x,Mth.floor(player.getY())+dy,z);
                if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))continue;
                var box=type.getAABB(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);
                if(!level.noCollision(box)||level.containsAnyLiquid(box))continue;
                if(!(type.create(level) instanceof Mob mob))return null;
                mob.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,random.nextFloat()*360,0);
                mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.EVENT,null,null);
                mob.getPersistentData().putUUID(BLOOD_PACT_FOR,id);mob.setTarget(player);
                if(!level.addFreshEntity(mob))return null;
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,mob.getX(),mob.getY()+0.5,mob.getZ(),12,0.4,0.6,0.4,0.1);
                level.playSound(null,pos,SoundEvents.WARDEN_HEARTBEAT,SoundSource.HOSTILE,1F,0.8F);
                return mob;
            }
        }
        return null;
    }
}
