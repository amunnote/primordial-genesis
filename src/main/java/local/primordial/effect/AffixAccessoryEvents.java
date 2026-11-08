package local.primordial.effect;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.AffixBonds;
import local.primordial.affix.AffixBonds.Bond;
import local.primordial.affix.AffixCombat;
import local.primordial.affix.AffixDamage;
import local.primordial.affix.AffixGrowth;
import local.primordial.affix.AffixState;
import local.primordial.affix.AffixStatuses;
import local.primordial.affix.ItemSeal;
import local.primordial.affix.MobAffix;
import local.primordial.entity.SummonClone;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What the affix accessories do for their wearer (see the tooltips, tooltip.primordial.affix_accessory.*). Each effect amplifies one dimension while Amplify is equipped ({@link AffixAccessories#power}).
 * Their wards against monster affixes are checked in the affix code itself. Where a bond changes an accessory's own
 * effect it is applied here, at that effect ({@link AffixBonds}); the bonds' own effects are in {@link AffixBondEvents}.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AffixAccessoryEvents {
    private static final String COMBAT="primordial:accessory_combat";
    private static final String MELEE_MARK="primordial:mastery_melee",SHOT_MARK="primordial:mastery_shot";
    private static final String THUNDER_READY="primordial:thunder_ready",SPLIT_READY="primordial:split_ready",SUMMON_READY="primordial:summon_ready";
    /** on a pet or player near an Undying wearer: until when it is guarded, and when it can be saved again */
    public static final String GUARDED_UNTIL="primordial:ward_undying_until",GUARD_READY="primordial:ward_undying_ready";
    /** on a spirit wolf Summon's accessory called before its clones (old saves): when it fades, and whose it is */
    public static final String SPIRIT_UNTIL="primordial:spirit_until",SPIRIT_OWNER="primordial:spirit_owner";
    private static final UUID GROWTH_ATTACK=UUID.fromString("5e2b7c1a-9f34-4d8e-a1b6-0c7d2e9f4a11"),GROWTH_HEALTH=UUID.fromString("5e2b7c1a-9f34-4d8e-a1b6-0c7d2e9f4a12");
    public static final int COMBAT_TICKS=200;
    private static final String GUARD_POWER="primordial:accessory_guard_power";
    private static final String SEAL_READY="primordial:accessory_seal_ready",SELF_READY="primordial:accessory_self_ready";
    private static final String SHATTER_READY="primordial:accessory_shatter_ready",SHATTER_UNTIL="primordial:accessory_shatter_until";
    private static final UUID SHATTER_MODIFIER=UUID.fromString("5e2b7c1a-9f34-4d8e-a1b6-0c7d2e9f4a13");
    /** Space: the hit before armor, kept for its true-damage share; Desaturation: worn (read by every monster's look); Time: worn last second */
    private static final String SPACE_BASE="primordial:accessory_space_base",SPACE_BASE_AT="primordial:accessory_space_base_at";
    private static final String UNSEEN="primordial:accessory_unseen",TIME_WORN="primordial:accessory_time_worn";
    private static final String EMPTY_GATE_READY="primordial:bond_empty_gate_ready",SUMMON_CHECK="primordial:accessory_summon_check";
    /** Desaturation: monsters seek the wearer from this much nearer (-30%, tunable) */
    public static final double UNSEEN_SIGHT=0.30;
    /** Summon: two clones, called again only once both are dead and 300 s have passed */
    public static final int CLONES=2,SUMMON_COOLDOWN=6000;
    /** the wearer's own cooldowns, which Time's accessory shortens */
    private static final String[] WEARER_COOLDOWNS={THUNDER_READY,SPLIT_READY,SELF_READY,SUMMON_READY};
    private AffixAccessoryEvents() {}
    @SubscribeEvent public static void shatterExpires(LivingEvent.LivingTickEvent event) {
        var entity=event.getEntity();
        if(entity.level().isClientSide||!entity.getPersistentData().contains(SHATTER_UNTIL)||!ready(entity,SHATTER_UNTIL))return;
        for(var attribute:new Attribute[]{Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS}){
            var instance=entity.getAttribute(attribute);if(instance!=null)instance.removeModifier(SHATTER_MODIFIER);
        }
        entity.getPersistentData().remove(SHATTER_UNTIL);
    }

    private static long now(LivingEntity entity) {return entity.level().getGameTime();}
    private static boolean ready(LivingEntity entity,String key) {return now(entity)>=entity.getPersistentData().getLong(key);}
    private static void cooldown(LivingEntity entity,String key,double ticks) {entity.getPersistentData().putLong(key,now(entity)+Math.round(ticks));}
    /** a cooldown of the wearer's accessory (kept on the wearer or on a target): Time's accessory shortens it by 30% */
    private static void wearerCooldown(Player wearer,LivingEntity on,String key,double ticks) {cooldown(on,key,ticks*AffixBonds.cooldownFactor(wearer));}
    private static void fight(Player player) {player.getPersistentData().putLong(COMBAT,now(player));}
    public static boolean inCombat(Player player) {
        return player.getPersistentData().contains(COMBAT)&&now(player)-player.getPersistentData().getLong(COMBAT)<COMBAT_TICKS;
    }
    private static boolean roll(LivingEntity entity,double chance) {return entity.getRandom().nextDouble()<chance;}
    private static boolean hostile(LivingEntity entity) {return entity instanceof Enemy&&entity.isAlive();}
    /** players, pets and the wearer's clones near the wearer, the wearer included */
    private static java.util.List<LivingEntity> allies(Player player,double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(radius),e->e.isAlive()
            &&player.distanceToSqr(e)<=radius*radius&&(e instanceof Player other&&!other.isSpectator()||e instanceof TamableAnimal pet&&pet.isTame()
                ||SummonClone.ownedBy(e,player)));
    }
    /** one more hit on a creature that was just hit: its damage immunity would swallow it otherwise */
    private static void extraHit(LivingEntity victim,DamageSource source,float amount) {
        if(amount<=0||!victim.isAlive())return;
        int frames=victim.invulnerableTime;victim.invulnerableTime=0;
        AffixCombat.secondary(()->victim.hurt(source,amount));
        victim.invulnerableTime=Math.max(victim.invulnerableTime,frames);
    }

    /** Called after an actual health decrease inside hurt(), after all damage events and absorption. */
    public static void bloodPact(Player player,LivingEntity victim,float lost) {
        if(lost>0&&victim!=player&&AffixAccessories.wears(player,MobAffix.BLOOD_PACT)){
            double power=AffixAccessories.power(player);
            // 唤名: Amplify's share counts once more (still only from a real accessory slot: power())
            if(AffixBonds.active(player,Bond.NAMING))power*=AffixAccessories.power(player);
            player.heal(lost*0.12F*(float)power);
        }
    }
    // ------------------------------------------------------------------ damage taken and dealt
    @SubscribeEvent public static void accessoryHurt(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide||AffixCombat.secondary())return;
        var source=event.getSource();float amount=event.getAmount();
        if(event.getEntity() instanceof Player victim&&AffixDamage.mitigable(source)){
            var worn=AffixAccessories.worn(victim);double power=AffixAccessories.power(victim);
            boolean physical=AffixCombat.physical(source);
            if(worn.contains(MobAffix.RESISTANCE))amount*=1-0.12*power;
            if(physical&&worn.contains(MobAffix.PHYSICAL_RESISTANCE))amount*=1-0.22*power;
            if(!physical&&worn.contains(MobAffix.MAGIC_RESISTANCE))amount*=1-0.20*power;
            // wards: the attacker's Strength and Growth no longer add to its blows
            if(source.getEntity() instanceof Mob mob&&source.getDirectEntity()==mob){
                var state=AffixState.get(mob);
                if(state!=null&&state.has(MobAffix.STRENGTH)&&AffixAccessories.blocks(victim,state,MobAffix.STRENGTH)){
                    var boost=mob.getEffect(MobEffects.DAMAGE_BOOST);
                    if(boost!=null)amount=Math.max(0,amount-3*(boost.getAmplifier()+1));
                }
                if(state!=null&&state.has(MobAffix.GROWTH)&&AffixAccessories.blocks(victim,state,MobAffix.GROWTH))amount/=AffixGrowth.growth(mob);
            }
        }
        if(source.getEntity() instanceof Player attacker&&event.getEntity()!=attacker&&AffixDamage.scalable(source)){
            var worn=AffixAccessories.worn(attacker);double power=AffixAccessories.power(attacker);
            boolean shot=source.is(DamageTypeTags.IS_PROJECTILE),melee=source.is(DamageTypes.PLAYER_ATTACK);
            if(worn.contains(MobAffix.MASTERY)&&(shot||melee)){
                var data=attacker.getPersistentData();String other=shot?MELEE_MARK:SHOT_MARK;
                double bonus=0.15;
                if(now(attacker)<data.getLong(other)){bonus+=0.25;data.remove(other);}
                amount*=1+bonus*power;
                data.putLong(shot?SHOT_MARK:MELEE_MARK,now(attacker)+80);
            }
            // ward: the target's Resistance affix no longer softens the wearer's blows
            if(event.getEntity() instanceof Mob mob){
                var state=AffixState.get(mob);
                if(state!=null&&state.has(MobAffix.RESISTANCE)&&AffixAccessories.blocks(attacker,state,MobAffix.RESISTANCE)){
                    var resistance=mob.getEffect(MobEffects.DAMAGE_RESISTANCE);
                    if(resistance!=null&&resistance.getAmplifier()<4)amount/=1-0.2F*(resistance.getAmplifier()+1);
                }
            }
        }
        event.setAmount(amount);
    }
    /** Space: the blow as it stands after every bonus, before armor: its true-damage share is taken from this */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void accessorySpaceBase(LivingHurtEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||AffixCombat.secondary()||!(event.getSource().getEntity() instanceof Player player)||victim==player
            ||!AffixAccessories.wears(player,MobAffix.SPACE))return;
        victim.getPersistentData().putFloat(SPACE_BASE,event.getAmount());victim.getPersistentData().putLong(SPACE_BASE_AT,now(victim));
    }

    @SubscribeEvent(priority=EventPriority.LOW) public static void accessoryDamage(LivingDamageEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||AffixCombat.secondary()||event.getAmount()<=0)return;
        var source=event.getSource();
        if(victim instanceof Player player)wearerHit(event,player,source);
        if(event.isCanceled()||event.getAmount()<=0)return;
        if(source.getEntity() instanceof Player player&&victim!=player)wearerStrikes(player,victim,source,event.getAmount());
    }
    private static void wearerHit(LivingDamageEvent event,Player player,DamageSource source) {
        var worn=AffixAccessories.worn(player);
        if(source.getEntity() instanceof LivingEntity)fight(player);
        if(worn.isEmpty())return;
        double power=AffixAccessories.power(player);float amount=event.getAmount();
        // fixed damage knows no cap; a full Breach of the attacker's passes it
        boolean capped=AffixDamage.cappable(source)&&!AffixBonds.uncapped(player,source)&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        boolean settled=AffixBondEvents.settled(player);
        // 不动: settled, the cap draws in by a quarter
        if(worn.contains(MobAffix.DAMAGE_CAP)&&capped)amount=Math.min(amount,player.getMaxHealth()*(float)(0.4/power*(settled?0.75:1)));
        // 空门: what passes a fifth of max health may be swallowed whole (a cap-like effect: true damage yes, fixed never)
        if(capped&&AffixBonds.active(player,Bond.EMPTY_GATE)&&amount>player.getMaxHealth()*0.2F&&ready(player,EMPTY_GATE_READY)&&roll(player,0.25)){
            cooldown(player,EMPTY_GATE_READY,200);event.setAmount(0);
            ((ServerLevel)player.level()).sendParticles(ParticleTypes.REVERSE_PORTAL,player.getX(),player.getY(0.5),player.getZ(),30,0.4,0.6,0.4,0.05);
            return;
        }
        if(worn.contains(MobAffix.SHARED)&&source.getEntity() instanceof LivingEntity){
            Mob nearest=null;
            for(var mob:player.level().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(8),m->hostile(m)&&m.distanceToSqr(player)<=64))
                if(nearest==null||mob.distanceToSqr(player)<nearest.distanceToSqr(player))nearest=mob;
            if(nearest!=null){
                float share=amount*(float)(0.24*power);amount-=share;
                var bearer=nearest;
                AffixCombat.secondary(()->bearer.hurt(player.damageSources().magic(),share));
            }
        }
        event.setAmount(amount);
        float taken=amount;
        // Growth: every blow taken, +4% max health, up to five
        if(worn.contains(MobAffix.GROWTH)&&taken>0&&source.getEntity()!=null){AffixStatuses.growthStep(player,false);growthModifiers(player);}
        if(source.getEntity() instanceof LivingEntity attacker&&attacker!=player){
            if(worn.contains(MobAffix.REFLECTION)&&source.getDirectEntity()==attacker){
                // 不动: settled, half as much again; 以血还血: 30% of it comes back as health
                float back=taken*0.35F*(float)power*(settled?1.5F:1);
                AffixCombat.secondary(()->attacker.hurt(player.damageSources().thorns(player),back));
                if(AffixBonds.active(player,Bond.BLOOD_FOR_BLOOD))player.heal(back*0.3F);
            }
            // Summon: hurt by a creature, the clones come
            if(worn.contains(MobAffix.SUMMON))summonClones(player,attacker);
        }
        if(worn.contains(MobAffix.SPLIT)&&ready(player,SPLIT_READY)&&player.getHealth()-taken>0&&player.getHealth()-taken<player.getMaxHealth()*0.3F){
            wearerCooldown(player,player,SPLIT_READY,1200/power);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,160,0,false,true));
            for(var mob:player.level().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(24),m->m.getTarget()==player))mob.setTarget(null);
            ((ServerLevel)player.level()).sendParticles(ParticleTypes.CLOUD,player.getX(),player.getY(0.5),player.getZ(),30,0.6,0.6,0.6,0.05);
        }
    }
    private static void wearerStrikes(Player player,LivingEntity victim,DamageSource source,float dealt) {
        var worn=AffixAccessories.worn(player);
        fight(player);
        if(worn.isEmpty())return;
        double power=AffixAccessories.power(player);
        boolean melee=source.getDirectEntity()==player;
        // an attack (a blow or a shot), not something lingering such as fire or poison
        boolean blow=melee||source.is(DamageTypeTags.IS_PROJECTILE);
        var level=(ServerLevel)player.level();
        // Space: the attack carries 10% more as true damage (passes armor and every resistance, still capped)
        if(worn.contains(MobAffix.SPACE)&&blow&&AffixDamage.mitigable(source)){
            var data=victim.getPersistentData();
            float base=data.getLong(SPACE_BASE_AT)==now(victim)?data.getFloat(SPACE_BASE):dealt;
            AffixDamage.dealTrue(victim,player,base*0.10F*(float)power);
        }
        // Growth: every blow given, +4% attack, up to five
        if(worn.contains(MobAffix.GROWTH)){AffixStatuses.growthStep(player,true);growthModifiers(player);}
        boolean binding=AffixBonds.active(player,Bond.BINDING);
        if(worn.contains(MobAffix.CURSE)&&roll(player,0.25*power)){
            // 缚咒: the wearer's curses last half again as long, and mark the target
            int ticks=binding?150:100;
            victim.addEffect(new MobEffectInstance(ArmorImbueEvents.randomHarm(player),ticks,1),player);
            if(binding)AffixBonds.markCursed(victim,ticks);
        }
        if(worn.contains(MobAffix.SEAL)&&ready(victim,SEAL_READY)&&roll(player,0.20*power)){
            wearerCooldown(player,victim,SEAL_READY,200);
            int ticks=binding?150:100;
            if(victim instanceof Mob mob&&AffixState.get(mob)!=null)AffixState.get(mob).seal(ticks);
            ItemSeal.apply(victim,ticks);
        }
        if(worn.contains(MobAffix.SLOWNESS)){
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1),player);
            if(AffixBonds.active(player,Bond.HEAVENBIND))AffixBonds.markSlowed(victim,player,60);
        }
        // 天缚: hitting a target in 失色 (the status, or the affix) renews its 降智
        if(AffixBonds.active(player,Bond.HEAVENBIND)&&(AffixStatuses.desaturated(victim)
            ||victim instanceof Mob mob&&AffixState.get(mob)!=null&&AffixState.get(mob).has(MobAffix.DESATURATION)))
            AffixStatuses.applyBefuddledMob(victim,AffixStatuses.MOB_BEFUDDLED_TICKS);
        if(worn.contains(MobAffix.BEFUDDLEMENT)&&victim instanceof Mob mob&&!ArmorImbueEvents.boss(mob)&&roll(player,0.2*power)){
            var others=level.getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(10),m->m!=mob&&hostile(m));
            mob.setTarget(others.isEmpty()?null:others.get(player.getRandom().nextInt(others.size())));
            mob.getNavigation().stop();
            level.sendParticles(ParticleTypes.WITCH,mob.getX(),mob.getY()+mob.getBbHeight()+0.3,mob.getZ(),8,0.3,0.2,0.3,0.05);
        }
        // Malice: 20% to put malice on the target for 5 s (唤名: twice the chance, twice as long)
        if(worn.contains(MobAffix.MALICE)){
            int twice=AffixBonds.active(player,Bond.NAMING)?2:1;
            if(roll(player,0.20*power*twice))AffixStatuses.applyMalice(victim,AffixStatuses.MOB_MALICE_TICKS*twice);
        }
        // Thunder: any hit (melee, arrow, spell...) calls the bolt down: 8 + 2% of the target's max health (capped at 60), as magic
        if(worn.contains(MobAffix.THUNDER)&&ready(player,THUNDER_READY)){
            wearerCooldown(player,player,THUNDER_READY,200);
            thunderbolt(player,victim,power);
        }
        if(worn.contains(MobAffix.MAGIC)&&melee)extraHit(victim,player.damageSources().indirectMagic(player,player),dealt*0.25F*(float)power);
        if(worn.contains(MobAffix.SHATTER)){
            if(victim instanceof Player target&&target.isBlocking()){target.getCooldowns().addCooldown(Items.SHIELD,100);target.stopUsingItem();
                level.broadcastEntityEvent(target,(byte)30);}
            else if(victim instanceof Mob mob&&ready(mob,SHATTER_READY)&&roll(player,0.20*power)){
                wearerCooldown(player,mob,SHATTER_READY,200);cooldown(mob,SHATTER_UNTIL,100);
                for(var attribute:new Attribute[]{Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS}){
                    var instance=mob.getAttribute(attribute);if(instance==null)continue;
                    instance.removeModifier(SHATTER_MODIFIER);
                    instance.addTransientModifier(new AttributeModifier(SHATTER_MODIFIER,"primordial.shatter",-0.30,AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }
        // 破阵: one more Breach stack on this target
        if(blow&&AffixBonds.active(player,Bond.BREACH))AffixBonds.breachHit(player,victim);
        // Summon: attacking, the clones come
        if(worn.contains(MobAffix.SUMMON))summonClones(player,victim);
    }
    /** Thunder's bolt (also called down by 天缚); 霹雳 lays two layers of 破甲 with it */
    static void thunderbolt(Player player,LivingEntity victim,double power) {
        var level=(ServerLevel)player.level();
        var bolt=EntityType.LIGHTNING_BOLT.create(level);
        if(bolt!=null){bolt.moveTo(victim.position());bolt.setVisualOnly(true);level.addFreshEntity(bolt);}
        extraHit(victim,player.damageSources().indirectMagic(player,player),Math.min(60,(8+victim.getMaxHealth()*0.02F)*(float)power));
        if(AffixBonds.active(player,Bond.THUNDERCLAP)&&victim.isAlive())AffixBonds.shred(victim,2);
    }

    // ------------------------------------------------------------------ death
    @SubscribeEvent(priority=EventPriority.HIGH) public static void accessoryRescue(LivingDeathEvent event) {
        var entity=event.getEntity();
        if(entity.level().isClientSide||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        var data=entity.getPersistentData();
        boolean self=entity instanceof Player player&&AffixAccessories.wears(player,MobAffix.UNDYING)&&ready(entity,SELF_READY);
        if(self||now(entity)<data.getLong(GUARDED_UNTIL)&&ready(entity,GUARD_READY)){
            event.setCanceled(true);
            if(self)wearerCooldown((Player)entity,entity,SELF_READY,12000/AffixAccessories.power(entity));else cooldown(entity,GUARD_READY,6000/Math.max(1,data.getDouble(GUARD_POWER)));data.remove(GUARDED_UNTIL);
            revive(entity,1);
            entity.removeAllEffects();
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,1));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,800,0));
            entity.level().broadcastEntityEvent(entity,(byte)35);
        }
    }
    static void revive(LivingEntity entity,float health) {
        ((local.primordial.mixin.HealthAccessor)entity).angelSetDead(false);
        entity.deathTime=0;entity.setHealth(health);entity.invulnerableTime=40;
    }
    /** Ruin: what the wearer kills bursts, hurting the monsters around it */
    @SubscribeEvent public static void accessoryRuin(LivingDeathEvent event) {
        var dead=event.getEntity();
        if(AffixCombat.secondary()||!(dead.level() instanceof ServerLevel level)||!(event.getSource().getEntity() instanceof Player player)||dead==player)return;
        if(!AffixAccessories.wears(player,MobAffix.RUIN))return;
        float damage=Math.min(80,dead.getMaxHealth()*0.40F*(float)AffixAccessories.power(player));
        var around=level.getEntitiesOfClass(Mob.class,dead.getBoundingBox().inflate(4),m->m!=dead&&hostile(m)&&m.distanceToSqr(dead)<=16);
        level.sendParticles(ParticleTypes.EXPLOSION,dead.getX(),dead.getY(0.5),dead.getZ(),1,0,0,0,0);
        level.playSound(null,dead.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,0.6F,1.3F);
        AffixCombat.secondary(()->{for(var mob:around)mob.hurt(player.damageSources().explosion(player,player),damage);});
    }

    // ------------------------------------------------------------------ every second (Growth's countdown every tick)
    @SubscribeEvent public static void accessoryTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player.level() instanceof ServerLevel level))return;
        var player=event.player;
        // Growth: five seconds without a blow given or taken, every stack is gone
        if(AffixStatuses.growthAttack(player)+AffixStatuses.growthHealth(player)>0&&AffixStatuses.growthLeft(player)<=0){
            AffixStatuses.clearGrowth(player);growthModifiers(player);
        }
        if(player.tickCount%20!=0||!player.isAlive())return;
        var worn=AffixAccessories.worn(player);
        if(!worn.contains(MobAffix.GROWTH))AffixStatuses.clearGrowth(player);
        growthModifiers(player);
        timeCooldowns(player,worn);
        player.getPersistentData().putBoolean(UNSEEN,worn.contains(MobAffix.DESATURATION));
        if(worn.isEmpty())return;
        double power=AffixAccessories.power(player),aura=AffixBonds.auraBonus(player);
        if(worn.contains(MobAffix.REGENERATION)){
            float heal=(0.5F+player.getMaxHealth()*0.005F)*(float)power,room=Math.max(0,player.getMaxHealth()-player.getHealth());
            player.heal(heal);
            // 长生: what overflows becomes golden blood, under golden blood's own capacity
            if(heal>room&&AffixBonds.active(player,Bond.LONGEVITY))ArmorImbueEvents.addGoldenBlood(player,heal-room);
        }
        if(worn.contains(MobAffix.HEALING)&&player.tickCount%100==0)
            for(var ally:allies(player,10+aura)){ally.heal((3+ally.getMaxHealth()*0.04F)*(float)power);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,ally.getX(),ally.getY(0.8),ally.getZ(),3,0.3,0.3,0.3,0);}
        if(worn.contains(MobAffix.SLOWNESS)){
            boolean heavenbind=AffixBonds.active(player,Bond.HEAVENBIND);
            for(var mob:level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(6*power+aura),AffixAccessoryEvents::hostile)){
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,0,false,true),player);
                if(heavenbind)AffixBonds.markSlowed(mob,player,40);
            }
        }
        // Desaturation: every hostile creature within 24 blocks glows (neutral and passive ones do not)
        if(worn.contains(MobAffix.DESATURATION))
            for(var mob:level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(24*power+aura),AffixAccessoryEvents::hostile))
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING,40,0,false,false));
        if(worn.contains(MobAffix.UNDYING))
            for(var ally:allies(player,16+aura))if(ally!=player){
                ally.getPersistentData().putLong(GUARDED_UNTIL,now(player)+100);
                ally.getPersistentData().putDouble(GUARD_POWER,power);
            }
    }
    /** Desaturation: a monster looking for a target sees the wearer only from 70% as far; one already on it keeps it */
    @SubscribeEvent public static void accessoryUnseen(LivingEvent.LivingVisibilityEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||!(event.getLookingEntity() instanceof Mob mob)
            ||!(mob instanceof Enemy)||mob.getTarget()==player||!player.getPersistentData().getBoolean(UNSEEN))return;
        event.modifyVisibility(1-UNSEEN_SIGHT);
    }
    /** Growth: +4% attack a stack on one track, +4% max health a stack on the other; never heals, and Amplify does not touch it */
    private static void growthModifiers(Player player) {
        modify(player,Attributes.ATTACK_DAMAGE,GROWTH_ATTACK,AffixStatuses.growthAttack(player)*0.04);
        modify(player,Attributes.MAX_HEALTH,GROWTH_HEALTH,AffixStatuses.growthHealth(player)*0.04);
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
    }
    private static void modify(Player player,Attribute attribute,UUID id,double amount) {
        var instance=player.getAttribute(attribute);if(instance==null)return;
        var current=instance.getModifier(id);
        if(current!=null&&current.getAmount()==amount)return;
        instance.removeModifier(id);
        if(amount>0)instance.addTransientModifier(new AttributeModifier(id,"primordial.growth_accessory",amount,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    /**
     * Time: cooldowns already running when it is put on keep 70% of what was left; taken off, what is left grows back
     * by as much (so putting it on and off again gains nothing). New cooldowns start at x0.7 ({@link #wearerCooldown}).
     */
    private static void timeCooldowns(Player player,EnumSet<MobAffix> worn) {
        var data=player.getPersistentData();
        boolean wearing=worn.contains(MobAffix.TIME);
        if(wearing==data.getBoolean(TIME_WORN))return;
        data.putBoolean(TIME_WORN,wearing);
        double factor=wearing?AffixBonds.TIME_COOLDOWN:1/AffixBonds.TIME_COOLDOWN;
        for(var key:WEARER_COOLDOWNS){
            long left=data.getLong(key)-now(player);
            if(left>0)data.putLong(key,now(player)+Math.round(left*factor));
        }
    }

    // ------------------------------------------------------------------ Summon: the clones
    /** the wearer's clones alive around it (they keep within 16 blocks of it, so 128 finds every loaded one) */
    public static List<SummonClone> clones(Player player) {
        if(player.level().isClientSide)return List.of();
        return player.level().getEntitiesOfClass(SummonClone.class,player.getBoundingBox().inflate(128),clone->clone.isAlive()&&SummonClone.ownedBy(clone,player));
    }
    /** two clones, if none is about and the cooldown since the last pair fell is over */
    private static void summonClones(Player player,LivingEntity foe) {
        if(!(player.level() instanceof ServerLevel level)||!ready(player,SUMMON_READY)||!ready(player,SUMMON_CHECK))return;
        cooldown(player,SUMMON_CHECK,20);
        if(!clones(player).isEmpty())return;
        String[] blessings={"amplify","regeneration","strength"};
        for(int i=0;i<CLONES;i++){
            var clone=Primordial.SUMMON_CLONE.get().create(level);if(clone==null)return;
            clone.moveTo(player.getX()+player.getRandom().nextGaussian(),player.getY(),player.getZ()+player.getRandom().nextGaussian(),player.getYRot(),0);
            if(!level.noCollision(clone))clone.moveTo(player.getX(),player.getY(),player.getZ(),player.getYRot(),0);
            clone.copyFrom(player);
            // 点化: each clone is given one weak blessing (a bond's gift, not one of the owner's affixes)
            if(AffixBonds.active(player,Bond.AWAKENING))clone.bless(blessings[player.getRandom().nextInt(blessings.length)]);
            if(foe!=null&&foe.isAlive()&&!(foe instanceof Player))clone.setTarget(foe);
            level.addFreshEntity(clone);
            level.sendParticles(ParticleTypes.SOUL,clone.getX(),clone.getY(0.5),clone.getZ(),12,0.3,0.4,0.3,0.03);
        }
        level.playSound(null,player.blockPosition(),SoundEvents.BELL_BLOCK,SoundSource.PLAYERS,0.8F,1.4F);
    }
    /** a clone died: the bonds that care hear of it; the last of them starts the 300 s cooldown */
    public static void cloneDied(SummonClone clone,Player owner) {
        AffixBondEvents.cloneDied(clone,owner);
        boolean others=false;
        for(var other:clones(owner))if(other!=clone){others=true;break;}
        if(!others)wearerCooldown(owner,owner,SUMMON_READY,SUMMON_COOLDOWN/AffixAccessories.power(owner));
    }
    /** spirit wolves Summon's accessory called before it called clones (old saves) fade as they always did */
    @SubscribeEvent public static void spiritFades(LivingEvent.LivingTickEvent event) {
        var entity=event.getEntity();
        if(entity.level().isClientSide||entity.tickCount%20!=0||!(entity instanceof Wolf)||!entity.getPersistentData().contains(SPIRIT_UNTIL))return;
        if(now(entity)>=entity.getPersistentData().getLong(SPIRIT_UNTIL)){
            ((ServerLevel)entity.level()).sendParticles(ParticleTypes.SOUL,entity.getX(),entity.getY(0.5),entity.getZ(),12,0.3,0.4,0.3,0.03);
            entity.discard();
        }
    }

    // ------------------------------------------------------------------ effects and experience
    /** Magic Resistance: three in ten harmful effects never take hold (rolled apart from Time's halving, before it counts) */
    @SubscribeEvent public static void accessoryShrugs(MobEffectEvent.Applicable event) {
        if(event.getEntity().level().isClientSide||event.getEffectInstance().getEffect().getCategory()!=MobEffectCategory.HARMFUL)return;
        if(!(event.getEntity() instanceof Player player))return;
        var worn=AffixAccessories.worn(player);
        if(worn.contains(MobAffix.MAGIC_RESISTANCE)&&roll(player,0.3*AffixAccessories.power(player)))event.setResult(Event.Result.DENY);
    }
    /** Enlightenment: +60% experience */
    @SubscribeEvent public static void accessoryInsight(PlayerXpEvent.XpChange event) {
        if(event.getAmount()<=0||event.getEntity().level().isClientSide)return;
        var worn=AffixAccessories.worn(event.getEntity());
        if(worn.contains(MobAffix.ENLIGHTENMENT))event.setAmount((int)Math.round(event.getAmount()*(1+0.6*AffixAccessories.power(event.getEntity()))));
    }
}
