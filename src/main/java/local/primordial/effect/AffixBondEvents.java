package local.primordial.effect;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;
import local.primordial.Primordial;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.AffixBonds;
import local.primordial.affix.AffixBonds.Bond;
import local.primordial.affix.AffixCombat;
import local.primordial.affix.AffixDamage;
import local.primordial.affix.AffixStatuses;
import local.primordial.entity.SummonClone;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What the bonds ({@link AffixBonds}) do, beyond the accessory effects they change in place (those are at the effect, in
 * {@link AffixAccessoryEvents}: 不动's cap and reflection, 破阵's stacks, 缚咒's curse and seal, 天缚's marks, 唤名's malice,
 * blood pact and auras, 以血还血, 长生, 空门, 霹雳, 点化). Every number is the draft's, to be tuned.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AffixBondEvents {
    private static final String SETTLED_SINCE="primordial:bond_settled_since";
    /** 不动: where the wearer stood last tick */
    private static final Map<Player,Vec3> SETTLED_AT=new WeakHashMap<>();
    private static final String SPACETIME_READY="primordial:bond_spacetime_ready",UNQUENCHED_READY="primordial:bond_unquenched_ready",
        WHEEL_READY="primordial:bond_wheel_ready";
    /** 不动: still this long, the wearer settles */
    public static final int SETTLE_TICKS=40;
    private AffixBondEvents() {}
    private static long now(LivingEntity entity) {return entity.level().getGameTime();}
    private static boolean ready(LivingEntity entity,String key) {return now(entity)>=entity.getPersistentData().getLong(key);}
    private static void cooldown(LivingEntity entity,String key,int ticks) {entity.getPersistentData().putLong(key,now(entity)+ticks);}
    /** ticks until 不熄 can save the wearer again (0 = ready); read by the status page through the status sync */
    public static int unquenchedLeft(Player player) {return (int)Math.max(0,player.getPersistentData().getLong(UNQUENCHED_READY)-now(player));}
    /** ticks until 命轮 can pull the wearer back again (0 = ready) */
    public static int wheelLeft(Player player) {return (int)Math.max(0,player.getPersistentData().getLong(WHEEL_READY)-now(player));}
    private static boolean roll(LivingEntity entity,double chance) {return entity.getRandom().nextDouble()<chance;}
    private static boolean hostile(LivingEntity entity) {return entity instanceof Enemy&&entity.isAlive();}

    /** 不动: the wearer has stood still for two seconds (any move ends it at once) */
    public static boolean settled(Player player) {
        return AffixBonds.active(player,Bond.UNMOVING)&&player.getPersistentData().contains(SETTLED_SINCE)
            &&now(player)-player.getPersistentData().getLong(SETTLED_SINCE)>=SETTLE_TICKS;
    }

    // ------------------------------------------------------------------ 不熄 and 命轮: where the wearer was, second by second
    private record Moment(ResourceKey<Level> dimension,Vec3 position,float health) {}
    private static final Map<Player,ArrayDeque<Moment>> HISTORY=new WeakHashMap<>();
    private static final Map<Player,Vec3> GROUND=new WeakHashMap<>();
    private static final int HISTORY_SECONDS=6;
    private static void remember(Player player) {
        var moments=HISTORY.computeIfAbsent(player,p->new ArrayDeque<>());
        // the place kept is the last one it stood on, so 命轮 never puts it back in mid-air over the void
        moments.addLast(new Moment(player.level().dimension(),GROUND.getOrDefault(player,player.position()),player.getHealth()));
        while(moments.size()>HISTORY_SECONDS)moments.removeFirst();
    }
    /** about this many seconds ago (the oldest kept, if not that many yet), if it was in this world */
    private static Moment past(Player player,int seconds) {
        var moments=HISTORY.get(player);
        if(moments==null||moments.isEmpty())return null;
        var list=new java.util.ArrayList<>(moments);
        var moment=list.get(Math.max(0,list.size()-1-seconds));
        return moment.dimension.equals(player.level().dimension())?moment:null;
    }

    // ------------------------------------------------------------------ every tick, every second
    @SubscribeEvent public static void bondTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player.level() instanceof ServerLevel level))return;
        var player=event.player;
        if(!player.isAlive())return;
        var data=player.getPersistentData();
        // 不动: any move (or a jump) starts the count again
        if(AffixBonds.active(player,Bond.UNMOVING)){
            Vec3 at=player.position(),last=SETTLED_AT.get(player);
            boolean moved=last==null||!data.contains(SETTLED_SINCE)||!player.onGround()||last.distanceToSqr(at)>1e-6;
            SETTLED_AT.put(player,at);
            if(moved)data.putLong(SETTLED_SINCE,now(player));
            if(settled(player)&&player.tickCount%10==0)level.sendParticles(ParticleTypes.ENCHANT,player.getX(),player.getY()+0.2,player.getZ(),4,0.4,0.1,0.4,0.2);
        }else data.remove(SETTLED_SINCE);
        if(player.onGround())GROUND.put(player,player.position());
        if(player.tickCount%20!=0)return;
        if(AffixBonds.active(player,Bond.UNQUENCHED)||AffixBonds.active(player,Bond.WHEEL))remember(player);else HISTORY.remove(player);
        double aura=AffixBonds.auraBonus(player),power=AffixAccessories.power(player);
        // 向死而生: below a quarter of its health the wearer regrows quickly (1% of max health a second)
        if(AffixBonds.active(player,Bond.DEFIANCE)&&player.getHealth()<player.getMaxHealth()*0.25F)player.heal(player.getMaxHealth()*0.01F);
        // 褪色世界: every four seconds, one more stack of 失色 on each enemy within 8 blocks
        if(AffixBonds.active(player,Bond.FADED_WORLD)&&player.tickCount%80==0)
            for(var mob:level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(8+aura),m->hostile(m)&&m.distanceToSqr(player)<=(8+aura)*(8+aura)))
                AffixStatuses.addDesaturationStack(mob);
        // 天缚: every three seconds, each target the wearer has slowed has a one-in-five chance of a bolt
        if(AffixBonds.active(player,Bond.HEAVENBIND)&&player.tickCount%60==0)
            for(var mob:level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(24),m->m.isAlive()&&AffixBonds.slowedBy(m,player)
                &&m.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)))
                if(roll(player,0.20))AffixAccessoryEvents.thunderbolt(player,mob,power);
    }
    /** 霹雳's 破甲 runs out */
    @SubscribeEvent public static void shredExpires(LivingEvent.LivingTickEvent event) {
        var entity=event.getEntity();
        if(!entity.level().isClientSide&&entity.tickCount%10==0)AffixBonds.shredExpires(entity);
    }

    // ------------------------------------------------------------------ blows
    @SubscribeEvent public static void bondAttack(LivingAttackEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide)return;
        var source=event.getSource();
        if(victim instanceof Player player){
            // 命轮: falling out of the world, back to where it stood five seconds ago (health stays as it is)
            if(source.is(DamageTypes.FELL_OUT_OF_WORLD)&&player.getY()<player.level().getMinBuildHeight()&&wheel(player)){event.setCanceled(true);return;}
            // 时空: one blow in ten slips past (a dodge: fixed damage too), five seconds apart
            if(!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)&&source.getEntity()!=null&&source.getEntity()!=player
                &&AffixBonds.active(player,Bond.SPACETIME)&&ready(player,SPACETIME_READY)&&roll(player,0.10)){
                cooldown(player,SPACETIME_READY,100);event.setCanceled(true);
                ((ServerLevel)player.level()).sendParticles(ParticleTypes.PORTAL,player.getX(),player.getY()+1,player.getZ(),20,0.4,0.6,0.4,0.3);
                return;
            }
        }
        // 破阵: full stacks, this blow passes the target's damage caps
        if(!AffixCombat.secondary()&&source.getEntity() instanceof Player attacker&&attacker!=victim&&AffixBonds.active(attacker,Bond.BREACH))
            AffixBonds.armBreach(attacker,victim);
    }
    /** 命轮: back to where the wearer stood about five seconds ago, if that was in this world; three minutes apart */
    private static boolean wheel(Player player) {
        if(!AffixBonds.active(player,Bond.WHEEL)||!ready(player,WHEEL_READY))return false;
        var past=past(player,5);
        if(past==null||past.position.y<player.level().getMinBuildHeight())return false;
        cooldown(player,WHEEL_READY,3600);
        if(player instanceof ServerPlayer server)AffixStatuses.sync(server);
        player.teleportTo(past.position.x,past.position.y,past.position.z);
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;player.hurtMarked=true;
        player.level().playSound(null,player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1F,0.8F);
        ((ServerLevel)player.level()).sendParticles(ParticleTypes.REVERSE_PORTAL,player.getX(),player.getY(1),player.getZ(),40,0.4,0.8,0.4,0.05);
        return true;
    }
    @SubscribeEvent public static void bondHurt(LivingHurtEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||event.getAmount()<=0)return;
        var source=event.getSource();float amount=event.getAmount();
        if(victim instanceof Player player&&AffixDamage.mitigable(source)){
            // 万御: 8% less of everything, a multiplier of its own
            if(AffixBonds.active(player,Bond.AEGIS))amount*=0.92F;
            // 众生: while a clone stands, 10% less
            if(AffixBonds.active(player,Bond.THRONG)&&!AffixAccessoryEvents.clones(player).isEmpty())amount*=0.90F;
        }
        if(source.getEntity() instanceof Player attacker&&attacker!=victim&&AffixDamage.scalable(source)){
            boolean magic=source.is(AffixCombat.MAGIC);
            if(!AffixCombat.secondary()&&AffixBonds.active(attacker,Bond.DEFIANCE)){
                float health=attacker.getHealth()/Math.max(1,attacker.getMaxHealth());
                // 向死而生: +15% below half health, +30% below a quarter
                if(health<0.25F)amount*=1.30F;else if(health<0.5F)amount*=1.15F;
            }
            // 法躯: a quarter of the wearer's non-physical reduction is added to its magic damage
            if(magic&&AffixBonds.active(attacker,Bond.SPELL_BODY))amount*=1+0.25F*(float)(1-AffixDamage.nonPhysicalTaken(attacker,attacker));
            // 缚咒: a target carrying the wearer's curse takes 20% more magic damage
            if(magic&&AffixBonds.active(attacker,Bond.BINDING)&&AffixBonds.cursed(victim))amount*=1.20F;
            // 褪色世界: +15% crit chance on targets in 失色 (shots here; melee through CriticalHitEvent)
            if(!AffixCombat.secondary()&&source.is(DamageTypeTags.IS_PROJECTILE)&&AffixBonds.active(attacker,Bond.FADED_WORLD)
                &&AffixStatuses.desaturated(victim)&&roll(attacker,0.15)){
                amount*=1.5F;
                ((ServerLevel)victim.level()).sendParticles(ParticleTypes.CRIT,victim.getX(),victim.getY(0.6),victim.getZ(),10,0.3,0.4,0.3,0.3);
            }
        }
        event.setAmount(amount);
    }
    @SubscribeEvent public static void bondCrit(CriticalHitEvent event) {
        var player=event.getEntity();
        if(player.level().isClientSide||event.isVanillaCritical()||!(event.getTarget() instanceof LivingEntity target))return;
        if(AffixBonds.active(player,Bond.FADED_WORLD)&&AffixStatuses.desaturated(target)&&roll(player,0.15))event.setResult(Event.Result.ALLOW);
    }
    /** 命轮: a fall that would kill is taken back instead, to where the wearer stood five seconds ago */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void bondFall(LivingDamageEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||!event.getSource().is(DamageTypeTags.IS_FALL))return;
        if(event.getAmount()>=player.getHealth()+player.getAbsorptionAmount()&&wheel(player))event.setCanceled(true);
    }

    // ------------------------------------------------------------------ 不熄
    /** a killing blow does not kill: health goes back to what it was three seconds ago, Ruin bursts around; five minutes apart */
    @SubscribeEvent(priority=EventPriority.HIGH) public static void bondUnquenched(LivingDeathEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        if(!AffixBonds.active(player,Bond.UNQUENCHED)||!ready(player,UNQUENCHED_READY))return;
        var past=past(player,3);if(past==null||past.health<=0)return;
        event.setCanceled(true);
        cooldown(player,UNQUENCHED_READY,6000);
        if(player instanceof ServerPlayer server)AffixStatuses.sync(server);
        // Growth keeps the stacks it has at this moment (its stacks are not part of what is rolled back)
        AffixAccessoryEvents.revive(player,Math.min(player.getMaxHealth(),past.health));
        player.clearFire();
        var level=(ServerLevel)player.level();
        float damage=Math.min(80,player.getMaxHealth()*0.40F*(float)AffixAccessories.power(player));
        var around=level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(4),m->hostile(m)&&m.distanceToSqr(player)<=16);
        AffixCombat.secondary(()->{for(var mob:around)mob.hurt(player.damageSources().explosion(player,player),damage);});
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,player.getX(),player.getY(0.5),player.getZ(),1,0,0,0,0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,player.getX(),player.getY(1),player.getZ(),60,0.4,0.8,0.4,0.05);
        level.playSound(null,player.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,1F,1.6F);
    }

    // ------------------------------------------------------------------ the clones
    /** 众生: 30% of every heal the wearer gets is given to each of its clones as well */
    @SubscribeEvent public static void bondHeal(LivingHealEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||event.getAmount()<=0||!AffixBonds.active(player,Bond.THRONG))return;
        float share=event.getAmount()*0.30F;
        for(var clone:AffixAccessoryEvents.clones(player))clone.heal(share);
    }
    /** 众生: a clone falling gives its owner 2 health; 余烬: it bursts for half its owner's attack */
    static void cloneDied(SummonClone clone,Player owner) {
        if(AffixBonds.active(owner,Bond.THRONG))owner.heal(2);
        if(AffixBonds.active(owner,Bond.EMBERS)&&clone.level() instanceof ServerLevel level){
            float damage=(float)(owner.getAttributeValue(Attributes.ATTACK_DAMAGE)*0.5);
            var around=level.getEntitiesOfClass(Mob.class,clone.getBoundingBox().inflate(3),m->hostile(m)&&m.distanceToSqr(clone)<=9);
            level.sendParticles(ParticleTypes.EXPLOSION,clone.getX(),clone.getY(0.5),clone.getZ(),1,0,0,0,0);
            level.playSound(null,clone.blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,0.5F,1.4F);
            AffixCombat.secondary(()->{for(var mob:around)mob.hurt(owner.damageSources().explosion(clone,owner),damage);});
        }
    }
}
