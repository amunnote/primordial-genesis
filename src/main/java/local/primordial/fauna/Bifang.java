package local.primordial.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

/**
 * 毕方 Bifang: like a crane with one foot, blue-green with red bars and a white beak, its crest and tail aflame. The
 * Vermilion Bird's rare elite: never more than one within 64 blocks, a quarter of the chances to spawn taken (bifangSpawnChance in primordial-world.toml). It hops on
 * its one leg, its peck sets alight, and every five seconds or so it breathes three fireballs at a foe 4 to 14 blocks
 * off. Fire does not touch it.
 */
public class Bifang extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> BREATH=SynchedEntityData.defineId(Bifang.class,EntityDataSerializers.INT);
    public static final int BREATH_TICKS=20,BREATH_COOLDOWN=100;
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    private int breathCooldown=40;

    public Bifang(EntityType<? extends Bifang> type,Level level) {super(type,level);xpReward=40;}
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,80).add(Attributes.ARMOR,6).add(Attributes.ATTACK_DAMAGE,9)
            .add(Attributes.MOVEMENT_SPEED,0.28).add(Attributes.FOLLOW_RANGE,32).add(Attributes.KNOCKBACK_RESISTANCE,0.4);
    }
    public static boolean canSpawn(EntityType<Bifang> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        if(level.getDifficulty()==Difficulty.PEACEFUL||!level.getBlockState(pos.below()).isSolidRender(level,pos.below()))return false;
        if(reason==MobSpawnType.NATURAL&&random.nextDouble()>=local.primordial.qinglong.WorldConfig.bifangSpawnChance())return false;
        return level.getEntitiesOfClass(Bifang.class,new AABB(pos).inflate(64)).isEmpty();
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(BREATH,0);}
    public boolean breathing() {return entityData.get(BREATH)>0;}

    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.2,false));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,0.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,12));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean doHurtTarget(Entity target) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        boolean hit=super.doHurtTarget(target);
        if(hit)target.setSecondsOnFire(4);
        return hit;
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        int breath=entityData.get(BREATH);
        var target=getTarget();
        if(breath>0){
            entityData.set(BREATH,breath-1);getNavigation().stop();
            if(target!=null){getLookControl().setLookAt(target,30,30);if(breath==BREATH_TICKS/2)fireballs(target);}
            return;
        }
        if(breathCooldown>0){breathCooldown--;return;}
        if(target!=null&&target.isAlive()&&hasLineOfSight(target)){
            double d=distanceTo(target);
            if(d>=4&&d<=14){entityData.set(BREATH,BREATH_TICKS);breathCooldown=BREATH_COOLDOWN;playSound(SoundEvents.BLAZE_SHOOT,1.4F,0.8F);}
        }
    }
    private void fireballs(LivingEntity target) {
        var eye=getEyePosition();
        for(int i=-1;i<=1;i++){
            double dx=target.getX()-eye.x,dy=target.getY(0.5)-eye.y,dz=target.getZ()-eye.z;
            double side=i*0.18;double sx=dx*Math.cos(side)-dz*Math.sin(side),sz=dx*Math.sin(side)+dz*Math.cos(side);
            var ball=new SmallFireball(level(),this,sx,dy,sz);
            ball.setPos(eye.x,eye.y-0.2,eye.z);level().addFreshEntity(ball);
        }
    }
    @Override public void aiStep() {
        super.aiStep();
        // the crest and tail burn
        if(level().isClientSide&&random.nextInt(3)==0){
            var back=getViewVector(1).scale(-0.6);
            level().addParticle(ParticleTypes.FLAME,getX()+back.x+(random.nextDouble()-0.5)*0.3,getY()+1.0+random.nextDouble()*0.3,getZ()+back.z+(random.nextDouble()-0.5)*0.3,0,0.02,0);
            if(breathing())level().addParticle(ParticleTypes.LAVA,getX(),getEyeY(),getZ(),0,0,0);
        }
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) {return false;}
    @Override protected SoundEvent getAmbientSound() {return SoundEvents.BLAZE_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source) {return SoundEvents.BLAZE_HURT;}
    @Override protected SoundEvent getDeathSound() {return SoundEvents.BLAZE_DEATH;}
    @Override public float getVoicePitch() {return 1.5F+random.nextFloat()*0.2F;}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"body",3,state->state.setAndContinue(RawAnimation.begin().thenLoop("animation.bifang."
            +(breathing()?"breathe":swinging?"attack":state.isMoving()?"walk":"idle")))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
}
