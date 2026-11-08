package local.primordial.affix;

import local.primordial.Primordial;
import local.primordial.effect.Protection;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AffixEvents {
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> BOSSES=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation(Primordial.ID,"bosses"));
    /** classes known to carry a boss bar, or not (filled as they are met) */
    private static final java.util.Map<Class<?>,Boolean> BOSS_BAR=new java.util.concurrent.ConcurrentHashMap<>();
    /**
     * Whether this creature is a boss: tagged primordial:bosses (with forge:bosses and c:bosses in it), or of a class that
     * keeps a boss bar of its own, as every boss does, from whichever mod (so no mod has to be named or tag its bosses).
     */
    public static boolean isBoss(net.minecraft.world.entity.Entity entity) {
        if(entity.getType().is(BOSSES))return true;
        // this mod's own (the Sin Lords and the rest) are told apart where it matters, as before: only the others' bars count
        return !(entity instanceof local.primordial.boss.SinEntity)&&BOSS_BAR.computeIfAbsent(entity.getClass(),AffixEvents::keepsBossBar);
    }
    private static boolean keepsBossBar(Class<?> type) {
        for(var c=type;c!=null&&c!=net.minecraft.world.entity.LivingEntity.class;c=c.getSuperclass())
            for(var field:c.getDeclaredFields())
                if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())&&net.minecraft.world.BossEvent.class.isAssignableFrom(field.getType()))return true;
        return false;
    }
    /**
     * 【原初】's own measure of some affixes, for a boss with a great pool of health: Damage Cap holds each hit to 5% of
     * its health, Time brings it back at 20%, Regeneration heals 0.4% a second, Space blinks it 10% of the seconds.
     */
    public static final float PRIMORDIAL_CAP=0.05F,PRIMORDIAL_REVIVAL=0.2F,PRIMORDIAL_REGENERATION=0.004F;
    public static final int PRIMORDIAL_SPACE=10;
    private AffixEvents() {}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void join(EntityJoinLevelEvent event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        if(event.getEntity() instanceof Projectile projectile){AffixProjectiles.join(projectile,level);return;}
        if(!(event.getEntity() instanceof Mob mob))return;
        var ledger=AffixLedger.get(level);if(ledger.retiredChild(mob.getUUID())){ledger.forgetChild(mob.getUUID());event.setCanceled(true);mob.discard();return;}
        if(event.loadedFromDisk()) {var state=AffixState.get(mob);if(state!=null){state.applyAttributes();state.name();}return;}
        roll(mob,level);
    }
    /**
     * Gives a mob that has never rolled its affixes. A mob with no survival player in range yet (spawned far away,
     * placed by world generation, or from before the mod was added) stays unrolled and is retried every second
     * from tick(), so it still gets its affixes once a player comes near.
     */
    public static void roll(Mob mob,ServerLevel level) {
        if(mob.getPersistentData().contains(AffixState.KEY)||!AffixConfig.ENABLED.get())return;
        boolean boss=isBoss(mob);
        if(!eligible(mob,boss))return;
        int levelNumber=highestLevel(level,mob.getX(),mob.getY(),mob.getZ());if(levelNumber<0&&!boss)return;
        levelNumber=Math.max(0,levelNumber);int count=AffixDistribution.count(levelNumber,mob.getRandom().nextDouble(),boss)+local.primordial.effect.MoonTide.extraAffixes(level);
        BossAffix special=boss?BossAffix.values()[mob.getRandom().nextInt(BossAffix.values().length)]:null;
        AffixState.assign(mob,levelNumber,AffixDistribution.choose(count,mob.getRandom().nextLong(),affix->AffixEligibility.usable(mob,affix)),special);
    }
    /** vanilla conversions (husk to zombie, piglin to zombified piglin...) keep the affixes instead of re-rolling under the old name */
    @SubscribeEvent public static void affixConversion(LivingConversionEvent.Post event) {
        if(event.getEntity().level().isClientSide||!(event.getEntity() instanceof Mob from)||!(event.getOutcome() instanceof Mob to))return;
        if(AffixState.get(from)==null)return;
        if(eligible(to,isBoss(to)))AffixState.inherit(from,to);
        else{to.setCustomName(AffixState.givenName(from));to.setCustomNameVisible(AffixState.givenNameVisible(from));}
    }
    public static int highestLevel(ServerLevel level,double x,double y,double z) {
        int highest=-1;double radius=AffixConfig.RADIUS.get();
        for(var player:level.players())if(!player.isSpectator()&&!player.isCreative()&&player.distanceToSqr(x,y,z)<=radius*radius)
            highest=Math.max(highest,player.experienceLevel);return highest;
    }
    /** bosses, and hostile mobs: vanilla's Enemy marker, or the monster spawn category many modded mobs use instead */
    public static boolean eligible(Mob mob,boolean boss) {
        if(mob instanceof local.primordial.boss.SinEntity)return false;
        return boss||mob instanceof Enemy||mob.getType().getCategory()==net.minecraft.world.entity.MobCategory.MONSTER;
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void attack(LivingAttackEvent event) {
        if(local.primordial.effect.GemSwordPower.slaying())return;
        if(AffixCombat.ruinTerrain()){event.setCanceled(true);return;}
        if(AffixCombat.secondary()||event.getEntity().level().isClientSide||Protection.active(event.getEntity()))return;
        if(event.getEntity() instanceof Mob defender){
            var affixes=AffixState.get(defender);
            if(AffixAccessories.acts(affixes,MobAffix.VOID,event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity hitter?hitter:null)&&defender.getRandom().nextInt(100)<30){event.setCanceled(true);return;}
        }
        var state=AffixCombat.source(event.getSource());
        if(event.getSource().getDirectEntity() instanceof Mob attacker&&ItemSeal.active(attacker,attacker.getMainHandItem())
            &&!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)){event.setCanceled(true);return;}
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void hurt(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide||local.primordial.effect.GemSwordPower.slaying())return;
        // a hit the shield stopped arrives here at 0: the affix bonuses must not turn it back into damage
        if(event.getAmount()<=0||!AffixDamage.scalable(event.getSource()))return;
        var attacker=AffixCombat.secondary()?null:AffixCombat.source(event.getSource());var defender=event.getEntity() instanceof Mob mob?AffixState.get(mob):null;
        float amount=AffixCombat.outgoing(attacker,event.getEntity(),event.getSource(),event.getAmount());
        event.setAmount(AffixCombat.incoming(defender,event.getSource(),amount));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent event) {
        if(event.getEntity().level().isClientSide||local.primordial.effect.GemSwordPower.slaying())return;
        var defender=event.getEntity() instanceof Mob mob?AffixState.get(mob):null;
        // fixed damage knows no cap; a full Breach lets the next blow of the one who built it past the cap
        if(AffixDamage.cappable(event.getSource())&&!AffixBonds.uncapped(event.getEntity(),event.getSource())
            &&AffixAccessories.acts(defender,MobAffix.DAMAGE_CAP,event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity hitter?hitter:null))event.setAmount(Math.min(event.getAmount(),defender.mob.getMaxHealth()*(defender.primordial()?PRIMORDIAL_CAP:0.3F)));
        if(!AffixCombat.secondary())event.setAmount(AffixCombat.sharedDamage(defender,event.getSource(),event.getAmount()));
        if(AffixCombat.secondary())return;
        var attacker=AffixCombat.source(event.getSource());
        if(event.getEntity() instanceof Player player&&attacker!=null&&attacker.boss(BossAffix.LUST)
            &&!Protection.active(player)&&player.getHealth()<player.getMaxHealth()*0.2F) {
            event.setCanceled(true);AffixCombat.execute(attacker,player);return;
        }
        AffixCombat.landed(attacker,defender,event.getEntity(),event.getSource(),event.getAmount());
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void revive(LivingDeathEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||mob.level().isClientSide||local.primordial.effect.GemSwordPower.slaying())return;var state=AffixState.get(mob);if(state==null)return;
        // (Pride's second life is not the Primordial Lord's: it has Time and Undying, once each)
        if(state.boss(BossAffix.PRIDE)&&!state.primordial()) {
            state.tag.putBoolean("pridePhase",true);state.applyAttributes();revive(mob,mob.getMaxHealth());state.name();event.setCanceled(true);state.combat();
        } else if(AffixAccessories.acts(state,MobAffix.TIME,event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity killer?killer:null)&&!state.tag.getBoolean("timeUsed")) {
            float share=state.primordial()?PRIMORDIAL_REVIVAL:AffixScaling.revivalHealthPercent(state.level)/100F;
            state.tag.putBoolean("timeUsed",true);revive(mob,Math.max(1,mob.getMaxHealth()*share));event.setCanceled(true);state.combat();
        }
    }
    private static void revive(Mob mob,float health) {((local.primordial.mixin.HealthAccessor)mob).angelSetDead(false);mob.deathTime=0;mob.setHealth(health);mob.invulnerableTime=0;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if(event.isCanceled())return;
        if(event.getEntity().level() instanceof ServerLevel level) {
            if(event.getEntity() instanceof Mob mob){
                var state=AffixState.get(mob);
                if(state!=null&&state.has(MobAffix.RUIN)&&!local.primordial.effect.GemSwordPower.slaying())AffixMobActions.ruin(state);
            }
            AffixLedger.get(level).forgetChild(event.getEntity().getUUID());
            AffixLedger.get(level).release(level.getServer(),event.getEntity().getUUID());AffixMobActions.cleanup(level,event.getEntity().getUUID());
        }
    }
    /** the player credited with the kill: the damage source's player, else the player who last hurt the mob (died to fire, a fall...) */
    private static Player killer(LivingDropsEvent event) {
        if(event.getSource().getEntity() instanceof Player player)return player;
        return event.getEntity().getLastHurtByMob() instanceof Player player?player:null;
    }
    @SubscribeEvent public static void essenceDrop(LivingDropsEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||mob.level().isClientSide)return;
        var state=AffixState.get(mob);
        if(state==null)return;
        int looting=Math.max(0,event.getLootingLevel());
        AffixMaterials.drop(state,looting,event.isRecentlyHit()?killer(event):null,event.getDrops());
        if(state.tag.getBoolean("clone"))return;   // split copies drop nothing, or Split would be an essence farm
        if(state.originalBoss==null&&mob.getRandom().nextDouble()>=Math.min(1.0,0.5+0.1*looting))return;
        var drop=new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity().getRemovalReason()==net.minecraft.world.entity.Entity.RemovalReason.DISCARDED){
            AffixLedger.get(level).forgetChild(event.getEntity().getUUID());
            AffixLedger.get(level).release(level.getServer(),event.getEntity().getUUID());
        }
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel level)||!mob.isAlive())return;
        for(var stack:mob.getHandSlots())ItemSeal.expire(mob,stack);
        if(mob.tickCount%20==0&&!mob.getPersistentData().contains(AffixState.KEY))roll(mob,level);
        var state=AffixState.get(mob);if(state==null)return;state.rage();
        var target=mob.getTarget();boolean aggro=target!=null&&target.isAlive()&&mob.distanceToSqr(target)<=64*64
            &&!(target instanceof Player player&&(player.isCreative()||player.isSpectator()));
        if(aggro)state.combat();
        boolean idle=level.getGameTime()-state.tag.getLong("lastCombat")>=200;
        if(state.primordial()){}   // the Primordial Lord is reset by its own rest, with its phases
        else if(idle) {
            if(state.originalBoss!=null&&!state.tag.getBoolean("reset")) {
                AffixLedger.get(level).release(level.getServer(),mob.getUUID());AffixMobActions.cleanup(level,mob.getUUID());state.reset();state.tag.putBoolean("reset",true);
            } else if(state.originalBoss==null){state.tag.putBoolean("timeUsed",false);state.tag.putBoolean("splitUsed",false);}
        } else state.tag.putBoolean("reset",false);
        // An idle mob has its encounter budget rearmed, but cannot spend it until combat resumes.
        if(!idle&&state.has(MobAffix.SPLIT)&&!AffixAccessories.foeBlocks(mob,state,MobAffix.SPLIT)&&!state.tag.getBoolean("clone")&&!state.tag.getBoolean("splitUsed")&&mob.getHealth()<=mob.getMaxHealth()*0.3F) {
            state.tag.putBoolean("splitUsed",true);AffixMobActions.split(state);
        }
        if(mob.tickCount%20!=0)return;
        if(aggro&&AffixAccessories.acts(state,MobAffix.SPACE,target)&&target instanceof Player&&mob.distanceToSqr(target)<=24*24
            &&!local.primordial.boss.PrimordialLord.holdsStill(mob)
            &&mob.getRandom().nextInt(100)<(state.primordial()?PRIMORDIAL_SPACE:AffixScaling.spaceChance(state.level)))AffixMobActions.teleport(state,target);
        if(state.has(MobAffix.THUNDER))thunder(state,mob,target,aggro,level.getGameTime());
        if(state.has(MobAffix.HEALING)&&mob.tickCount%100==0)AffixMobActions.healNearby(state);
        if(state.has(MobAffix.REGENERATION)&&!AffixAccessories.foeBlocks(mob,state,MobAffix.REGENERATION)) {
            float amount=mob.getMaxHealth()*(state.primordial()?PRIMORDIAL_REGENERATION:AffixScaling.regenerationPercent(state.level)/100F);
            mob.heal(amount*(state.boss(BossAffix.GLUTTONY)?1.5F:1));
        }
        int tier=state.primordial()?0:AffixScaling.potionTier(state.level);
        if(tier>0&&state.has(MobAffix.STRENGTH))mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,60,tier-1,false,false));
        if(tier>0&&state.has(MobAffix.RESISTANCE))mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,60,tier-1,false,false));
        if(aggro&&AffixAccessories.acts(state,MobAffix.SUMMON,target)&&!state.tag.getBoolean("clone")&&mob.tickCount%200==0
            &&mob.getRandom().nextInt(100)<AffixScaling.summonChance(state.level))AffixMobActions.summon(state);
        if(aggro&&state.boss(BossAffix.GLUTTONY))AffixMobActions.consume(state);
    }
    /** Thunder: only at a player it is hunting (its current target), within 32 blocks and in sight */
    public static final int THUNDER_INTERVAL=300,THUNDER_WARMUP=100;
    public static void thunder(AffixState state,Mob mob,net.minecraft.world.entity.LivingEntity target,boolean aggro,long now) {
        if(!aggro||!(target instanceof Player player)||AffixAccessories.blocks(player,state,MobAffix.THUNDER)){state.tag.remove("thunderTarget");return;}
        // a new quarry gets 5 s before the first bolt; then one every 15 s
        if(!state.tag.hasUUID("thunderTarget")||!state.tag.getUUID("thunderTarget").equals(player.getUUID())){
            state.tag.putUUID("thunderTarget",player.getUUID());state.tag.putLong("thunderNext",now+THUNDER_WARMUP);return;}
        if(now<state.tag.getLong("thunderNext")||mob.distanceToSqr(player)>32*32||!mob.hasLineOfSight(player))return;
        state.tag.putLong("thunderNext",now+THUNDER_INTERVAL);
        AffixMobActions.thunder(state,player);
    }
    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent event) {
        if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level) {
            AffixProjectiles.tick(level);if(level==level.getServer().overworld()&&level.getGameTime()%20==0)AffixLedger.get(level).refresh(level.getServer());
        }
    }
    @SubscribeEvent public static void impact(ProjectileImpactEvent event) {
        if(!AffixProjectiles.causal(event.getProjectile()))return;
        if(!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)||!AffixProjectiles.intended(event.getProjectile(),hit.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void itemAttack(AttackEntityEvent event) {if(ItemSeal.active(event.getEntity(),event.getEntity().getMainHandItem()))event.setCanceled(true);}
    @SubscribeEvent public static void itemUse(PlayerInteractEvent.RightClickItem event) {if(ItemSeal.active(event.getEntity(),event.getItemStack()))event.setCanceled(true);}
    @SubscribeEvent public static void blockUse(PlayerInteractEvent.RightClickBlock event) {if(ItemSeal.active(event.getEntity(),event.getItemStack()))event.setCanceled(true);}
    @SubscribeEvent public static void blockAttack(PlayerInteractEvent.LeftClickBlock event) {if(ItemSeal.active(event.getEntity(),event.getItemStack()))event.setCanceled(true);}
    @SubscribeEvent public static void useStart(LivingEntityUseItemEvent.Start event) {if(ItemSeal.active(event.getEntity(),event.getItem()))event.setCanceled(true);}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if(event.getEntity()!=null&&ItemSeal.active(event.getEntity(),event.getItemStack()))event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.primordial.sealed").withStyle(net.minecraft.ChatFormatting.RED));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity().level() instanceof ServerLevel level)AffixLedger.get(level).refresh(level.getServer());
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||event.player.level().isClientSide)return;
        for(int slot=0;slot<event.player.getInventory().getContainerSize();slot++)ItemSeal.expire(event.player,event.player.getInventory().getItem(slot));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void itemAttributes(net.minecraftforge.event.ItemAttributeModifierEvent event) {
        if(event.getItemStack().hasTag()&&event.getItemStack().getTag().getBoolean("primordial:visual_equipment"))event.clearModifiers();
    }
}
