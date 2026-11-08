package local.primordial.xuanwu;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

public class ColdjadeTurtle extends TideMob implements GeoEntity {
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    
    public ColdjadeTurtle(EntityType<? extends ColdjadeTurtle> t,Level l){super(t,l);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,44).add(Attributes.MOVEMENT_SPEED,0.16).add(Attributes.ATTACK_DAMAGE,4);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1.2,false));
        goalSelector.addGoal(4,new RandomStrollGoal(this,0.8));goalSelector.addGoal(5,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
    }
    @Override protected void customServerAiStep(){super.customServerAiStep();if(getTarget()!=null&&(!getTarget().isAlive()||distanceToSqr(getTarget())>32*32||getTarget() instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator())))setTarget(null);}
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> SHELL_TICKS=net.minecraft.network.syncher.SynchedEntityData.defineId(ColdjadeTurtle.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(SHELL_TICKS,0);}
    private boolean shelled(){return entityData.get(SHELL_TICKS)>0;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){boolean hit=super.hurt(source,shelled()?amount*0.4F:amount);if(hit&&!level().isClientSide){entityData.set(SHELL_TICKS,80);getNavigation().stop();}return hit;}
    @Override public void aiStep(){super.aiStep();if(shelled()){getNavigation().stop();setDeltaMovement(Vec3Zero());if(!level().isClientSide)entityData.set(SHELL_TICKS,entityData.get(SHELL_TICKS)-1);}}
    private net.minecraft.world.phys.Vec3 Vec3Zero(){return new net.minecraft.world.phys.Vec3(0,getDeltaMovement().y,0);}
    @Override public void travel(net.minecraft.world.phys.Vec3 input){if(shelled())super.travel(net.minecraft.world.phys.Vec3.ZERO);else super.travel(input);}
    @Override public boolean doHurtTarget(Entity e){if(shelled())return false;swing(net.minecraft.world.InteractionHand.MAIN_HAND);return super.doHurtTarget(e);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"body",3,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.coldjade_turtle."+(shelled()?"idle":swinging?"attack":s.isMoving()?(isInWater()?"swim":"walk"):"idle")))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
