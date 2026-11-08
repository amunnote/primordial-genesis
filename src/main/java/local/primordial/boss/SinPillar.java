package local.primordial.boss;

import java.util.UUID;
import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 罪之柱 A Sin Pillar, raised by the Primordial Lord when its sins become one: a column of its sin's stone pouring a beam
 * of that sin into the lord. While any stands, the lord takes half harm. Each now and then looses its sin on whoever is
 * near (its ring of glyphs gives warning). Broken, it takes its sin away from the lord for the rest of the fight.
 */
public class SinPillar extends Mob implements SinEntity {
    public static final double HEALTH=60,REACH=7;
    public static final int PULSE=200,WARNING=30;
    static final EntityDataAccessor<Integer> SIN=SynchedEntityData.defineId(SinPillar.class,EntityDataSerializers.INT);
    private UUID owner;private int pulse;

    public SinPillar(EntityType<? extends SinPillar> type,Level level) {super(type,level);setNoGravity(true);setNoAi(true);setPersistenceRequired();xpReward=0;}
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,HEALTH).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.ARMOR,4);
    }
    public static SinPillar raise(ServerLevel level,PrimordialLord lord,BossAffix sin,Vec3 at) {
        var pillar=new SinPillar(SinBosses.SIN_PILLAR.get(),level);
        pillar.entityData.set(SIN,sin.ordinal());pillar.owner=lord.getUUID();
        pillar.moveTo(at.x,at.y,at.z,0,0);
        pillar.pulse=PULSE/2+sin.ordinal()*25;   // staggered, so they do not all loose at once
        level.addFreshEntity(pillar);
        SinParticles.send(level,SinParticles.FLARE.get(),sin.color(),at.add(0,1.5,0),12,0.6,0,0.1,0);
        return pillar;
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(SIN,0);}
    public BossAffix sin() {return BossAffix.values()[Math.floorMod(entityData.get(SIN),BossAffix.values().length)];}
    @Override public int colour() {return sin().color();}
    PrimordialLord lord() {
        return owner!=null&&level() instanceof ServerLevel level&&level.getEntity(owner) instanceof PrimordialLord lord&&lord.isAlive()?lord:null;
    }

    @Override public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if(level().isClientSide){
            if(random.nextInt(2)==0)level().addParticle(SinParticles.FLARE.get().of(colour()),getX()+(random.nextDouble()-0.5)*0.8,getY()+4+random.nextDouble()*0.5,getZ()+(random.nextDouble()-0.5)*0.8,0,0.02,0);
            return;
        }
        var level=(ServerLevel)level();
        var lord=lord();
        if(lord==null){collapse();return;}
        // its beam, poured into the lord
        if(tickCount%4==0)SinParticles.lineFar(level,SinParticles.GLINT.get(),colour(),position().add(0,4.2,0),lord.position().add(0,lord.getBbHeight()*0.6,0),1.6,0);
        if(--pulse<=WARNING&&pulse>0&&pulse%4==0)SinParticles.ring(level,SinParticles.GLINT.get(),colour(),position(),REACH,24,0.04);
        if(pulse<=0){pulse=PULSE;loose(level,lord);}
    }
    /** its sin, loosed on whoever is near */
    void loose(ServerLevel level,PrimordialLord lord) {
        var near=ArcaneSkill.foesAround(this,position(),REACH,4);
        var at=position();
        switch(sin()){
            case PRIDE->{for(var e:near){hit(e,6);ArcaneSkill.knock(e,at,1.4,0.4);}
                SinShockwave.spread(level,at,(float)REACH,12,colour(),false,0);}
            case GREED->{for(var e:near){hit(e,6);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1),this);}
                SinParticles.send(level,SinParticles.COIN.get(),colour(),at.add(0,4,0),30,REACH*0.6,0,-0.2,0);}
            case SLOTH->{for(var e:near){e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,2),this);e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,100,2),this);}
                SinParticles.send(level,SinParticles.DREAM.get(),colour(),at.add(0,1,0),20,REACH*0.5,0,0.03,0);}
            case ENVY->{for(var e:near)for(var effect:new java.util.ArrayList<>(e.getActiveEffects()))
                    if(effect.getEffect().getCategory()==MobEffectCategory.BENEFICIAL){e.removeEffect(effect.getEffect());lord.addEffect(new MobEffectInstance(effect));break;}
                SinParticles.send(level,SinParticles.SHARD.get(),colour(),at.add(0,1.5,0),24,REACH*0.5,0,0.05,0);}
            case WRATH->{for(var e:near){hit(e,8);e.setSecondsOnFire(4);}
                SinParticles.send(level,SinParticles.EMBER.get(),colour(),at.add(0,0.5,0),40,REACH*0.5,0,0.2,0);
                SinShockwave.spread(level,at,(float)REACH,10,0xFF5A2A,false,0);}
            case GLUTTONY->{for(var e:near){var pull=at.subtract(e.position()).normalize().scale(0.9);e.setDeltaMovement(e.getDeltaMovement().add(pull.x,0.2,pull.z));e.hurtMarked=true;
                    e.addEffect(new MobEffectInstance(SinBosses.DEVOURED.get(),60),this);e.addEffect(new MobEffectInstance(MobEffects.HUNGER,100,2),this);}
                SinParticles.send(level,SinParticles.BUBBLE.get(),colour(),at.add(0,1,0),30,REACH*0.5,0,0.02,0);}
            case LUST->{var target=lord.getTarget();
                if(target!=null&&target.isAlive()&&target.distanceTo(this)<32)
                    for(int i=0;i<2;i++)SinOrb.loose(level,lord,target,at.add(0,3,0),new Vec3((random.nextDouble()-0.5)*0.4,0.35,(random.nextDouble()-0.5)*0.4),colour());}
        }
        level.playSound(null,getX(),getY(),getZ(),SoundEvents.BEACON_POWER_SELECT,SoundSource.HOSTILE,1.2F,0.7F);
    }
    private void hit(LivingEntity e,float amount) {e.hurt(damageSources().indirectMagic(this,this),amount);}
    /** fallen with its lord gone, or the fight over */
    public void collapse() {
        if(level() instanceof ServerLevel level){
            SinParticles.send(level,SinParticles.FLARE.get(),colour(),position().add(0,1.5,0),16,0.6,0,0.06,0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,getX(),getY()+1.5,getZ(),12,0.4,1,0.4,0.02);
        }
        discard();
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        if(level() instanceof ServerLevel level){
            var lord=lord();
            if(lord!=null)lord.pillarBroken(sin());
            SinParticles.send(level,SinParticles.FLARE.get(),colour(),position().add(0,1.5,0),30,0.8,0,0.12,0);
            level.playSound(null,getX(),getY(),getZ(),SoundEvents.GLASS_BREAK,SoundSource.HOSTILE,2F,0.6F);
        }
    }
    @Override public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL)||source.is(DamageTypes.IN_WALL)||source.is(DamageTypes.DROWN)||source.is(DamageTypeTags.IS_FIRE)
            ||source.is(DamageTypeTags.IS_EXPLOSION)&&!(source.getEntity() instanceof net.minecraft.world.entity.player.Player)
            ||source.getEntity() instanceof SinEntity||super.isInvulnerableTo(source);
    }
    @Override public boolean isPushable() {return false;}
    @Override public void knockback(double strength,double x,double z) {}
    @Override public boolean removeWhenFarAway(double distance) {return false;}
    @Override protected boolean shouldDropLoot() {return false;}
    @Override public boolean canChangeDimensions() {return false;}
    @Override public boolean fireImmune() {return true;}
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);if(owner!=null)tag.putUUID("Owner",owner);tag.putInt("Pulse",pulse);}
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;pulse=tag.getInt("Pulse");}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return local.primordial.sound.ModSounds.voice("sin_pillar","ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("sin_pillar","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("sin_pillar","death");}
}
