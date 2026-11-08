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

public class XuanSerpent extends TideMob implements GeoEntity {
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    
    public XuanSerpent(EntityType<? extends XuanSerpent> t,Level l){super(t,l);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,36).add(Attributes.MOVEMENT_SPEED,0.24).add(Attributes.ATTACK_DAMAGE,8);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1.2,false){@Override public boolean canUse(){return (!needsWater())&&super.canUse();}});
        goalSelector.addGoal(4,new RandomStrollGoal(this,0.8));goalSelector.addGoal(5,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,net.minecraft.world.entity.player.Player.class,10,true,false,p->!needsWater()));
    }
    @Override public boolean doHurtTarget(Entity e){swing(net.minecraft.world.InteractionHand.MAIN_HAND);return super.doHurtTarget(e);}
    @Override protected void customServerAiStep(){super.customServerAiStep();if(getTarget()!=null&&(!getTarget().isAlive()||distanceToSqr(getTarget())>32*32||getTarget() instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator())))setTarget(null);}
    private int dryTicks;
    private boolean needsWater(){return !isInWater()&&dryTicks>200&&level().dimension()!=local.primordial.spirit.SpiritRealm.LEVEL;}
    @Override public void aiStep(){super.aiStep();if(level().isClientSide)return;if(isInWater()){dryTicks=0;if(getTarget()!=null){var delta=getTarget().position().subtract(position()).normalize();setDeltaMovement(getDeltaMovement().add(delta.scale(0.035)).add(0,Math.sin(tickCount*0.25)*0.012,0));}}else if(++dryTicks>200&&needsWater()&&tickCount%40==0){
        setTarget(null);net.minecraft.core.BlockPos best=null;double distance=Double.MAX_VALUE;
        for(var q:net.minecraft.core.BlockPos.betweenClosed(blockPosition().offset(-12,-5,-12),blockPosition().offset(12,3,12)))if(level().getFluidState(q).is(net.minecraft.tags.FluidTags.WATER)&&q.distSqr(blockPosition())<distance){best=q.immutable();distance=q.distSqr(blockPosition());}
        if(best!=null)getNavigation().moveTo(best.getX()+0.5,best.getY()+0.5,best.getZ()+0.5,1.1);
    }}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"body",3,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.xuan_serpent."+(hurtTime>0?"hurt":swinging?"attack":s.isMoving()?"swim":"idle")))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
