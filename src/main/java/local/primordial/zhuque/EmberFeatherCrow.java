package local.primordial.zhuque;

import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

/** Circle above prey, fold wings and dive; a single hit per pass. */
public class EmberFeatherCrow extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(EmberFeatherCrow.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    private int timer,cooldown=40;private Vec3 dash=Vec3.ZERO;private boolean struck;
    public EmberFeatherCrow(EntityType<? extends EmberFeatherCrow> t,Level l){super(t,l);setNoGravity(true);xpReward=6;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PHASE,0);}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,22).add(Attributes.ATTACK_DAMAGE,7).add(Attributes.MOVEMENT_SPEED,0.3).add(Attributes.FOLLOW_RANGE,28);}
    @Override protected void registerGoals(){targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    @Override public boolean causeFallDamage(float d,float m,net.minecraft.world.damagesource.DamageSource s){return false;}
    @Override protected void customServerAiStep(){
        super.customServerAiStep();setNoGravity(true);var target=getTarget();int phase=entityData.get(PHASE);
        if(phase==1){setDeltaMovement(Vec3.ZERO);if(--timer<=0){entityData.set(PHASE,2);timer=16;struck=false;}return;}
        if(phase==2){setDeltaMovement(dash);if(!struck&&target!=null&&target.isAlive()&&getBoundingBox().expandTowards(dash).inflate(0.4).intersects(target.getBoundingBox())){if(target.hurt(damageSources().mobAttack(this),7)&&random.nextBoolean())target.setSecondsOnFire(3);struck=true;}if(--timer<=0||horizontalCollision){entityData.set(PHASE,3);timer=20;}return;}
        if(phase==3){setDeltaMovement(getDeltaMovement().scale(0.2));if(--timer<=0){entityData.set(PHASE,0);cooldown=40;}return;}
        if(target!=null&&target.isAlive()){
            getLookControl().setLookAt(target,30,30);setYRot((float)(Math.atan2(target.getZ()-getZ(),target.getX()-getX())*180/Math.PI)-90);yBodyRot=getYRot();
            if(--cooldown<=0&&distanceToSqr(target)<18*18&&hasLineOfSight(target)){dash=target.getBoundingBox().getCenter().subtract(position()).normalize().scale(1.1);entityData.set(PHASE,1);timer=10;return;}
        }
        var ground=level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,blockPosition());double a=tickCount*0.035+getId();
        Vec3 centre=target==null?new Vec3(getX(),ground.getY()+4,getZ()):target.position().add(0,6,0);
        Vec3 delta=centre.add(Math.cos(a)*6,0,Math.sin(a)*6).subtract(position()).normalize().scale(0.18);
        setDeltaMovement(getDeltaMovement().scale(0.8).add(delta.scale(0.2)).add(0,horizontalCollision?0.15:0,0));
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"body",0,s->{String a=hurtTime>0?"hurt":entityData.get(PHASE)==1?"fly":entityData.get(PHASE)==2?"dive":onGround()?"idle":"fly";return s.setAndContinue((a.equals("idle")||a.equals("fly"))?RawAnimation.begin().thenLoop("animation.ember_feather_crow."+a):RawAnimation.begin().thenPlayAndHold("animation.ember_feather_crow."+a));}));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
