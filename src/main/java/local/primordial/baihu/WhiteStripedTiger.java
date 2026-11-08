package local.primordial.baihu;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

public class WhiteStripedTiger extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    
    public WhiteStripedTiger(EntityType<? extends WhiteStripedTiger> t,Level l){super(t,l);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.MOVEMENT_SPEED,0.3).add(Attributes.ATTACK_DAMAGE,7).add(Attributes.ATTACK_KNOCKBACK,1.5);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new MeleeAttackGoal(this,1.65,false));
        
        goalSelector.addGoal(4,new WaterAvoidingRandomStrollGoal(this,0.8));goalSelector.addGoal(5,new LookAtPlayerGoal(this,net.minecraft.world.entity.player.Player.class,8));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());
    }
    @Override public boolean doHurtTarget(Entity e){swing(net.minecraft.world.InteractionHand.MAIN_HAND);setDeltaMovement(getDeltaMovement().add(e.position().subtract(position()).normalize().scale(0.45)).add(0,0.18,0));boolean hit=super.doHurtTarget(e);if(hit&&e instanceof LivingEntity living)living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,40,0));return hit;}
    @Override protected void customServerAiStep(){super.customServerAiStep();if(getTarget()!=null&&(!getTarget().isAlive()||distanceToSqr(getTarget())>32*32||getTarget() instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator())))setTarget(null);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"body",3,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.white_striped_tiger."+(swinging?"attack":s.isMoving()?(getTarget()!=null?"run":"walk"):"idle")))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
