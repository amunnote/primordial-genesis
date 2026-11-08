package local.primordial.qinglong;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

public class VerdantAntlerDeer extends Animal implements GeoEntity {
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    private int shed=24000;
    public VerdantAntlerDeer(EntityType<? extends VerdantAntlerDeer> t,Level l){super(t,l);}
    // Design names a diamond-equipment band but no numeric deer HP: choose 32.
    public static AttributeSupplier.Builder attributes(){return Animal.createMobAttributes().add(Attributes.MAX_HEALTH,32).add(Attributes.MOVEMENT_SPEED,0.3).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.ATTACK_KNOCKBACK,1.5);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new MeleeAttackGoal(this,1.65,false));
        goalSelector.addGoal(2,new BreedGoal(this,1));goalSelector.addGoal(3,new FollowParentGoal(this,1.1));
        goalSelector.addGoal(4,new WaterAvoidingRandomStrollGoal(this,0.8));goalSelector.addGoal(5,new LookAtPlayerGoal(this,net.minecraft.world.entity.player.Player.class,8));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());
    }
    @Override public boolean doHurtTarget(Entity e){swing(net.minecraft.world.InteractionHand.MAIN_HAND);boolean hit=super.doHurtTarget(e);if(hit&&e instanceof LivingEntity living)living.knockback(1.2,getX()-e.getX(),getZ()-e.getZ());return hit;}
    @Override protected void customServerAiStep(){super.customServerAiStep();if(!isBaby()&&--shed<=0){spawnAtLocation(QinglongContent.ANTLER.get());shed=24000;}if(getTarget()!=null&&(!getTarget().isAlive()||distanceToSqr(getTarget())>32*32||getTarget() instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator())))setTarget(null);}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t){super.addAdditionalSaveData(t);t.putInt("AntlerShed",shed);}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t){super.readAdditionalSaveData(t);shed=t.contains("AntlerShed")?Math.max(1,t.getInt("AntlerShed")):24000;}
    @Override public boolean isFood(net.minecraft.world.item.ItemStack s){return s.is(net.minecraft.world.item.Items.WHEAT);}
    @Override public AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel l,AgeableMob m){return QinglongContent.DEER.get().create(l);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"body",3,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.verdant_antler_deer."+(swinging?"attack":s.isMoving()?(getTarget()!=null?"run":"walk"):"idle")))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
