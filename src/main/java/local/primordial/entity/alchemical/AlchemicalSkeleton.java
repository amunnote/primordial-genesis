package local.primordial.entity.alchemical;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 炼金射手. A skeleton that does not burn in daylight, never freezes into a stray, is not afraid of wolves, and leads
 * its shots: it aims where the target will be when the arrow arrives, and far more precisely.
 */
public class AlchemicalSkeleton extends Skeleton implements AlchemicalCreation {
    static final float ARROW_SPEED=1.6F,INACCURACY=1.0F;
    private final AnimatableInstanceCache cache=AlchemicalCreation.cache(this);
    public AlchemicalSkeleton(EntityType<? extends Skeleton> type,Level level) {super(type,level);}

    public static AttributeSupplier.Builder attributes() {
        return AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH,26).add(Attributes.MOVEMENT_SPEED,0.28);
    }
    @Override public String creationId() {return "alchemical_skeleton";}
    @Override protected boolean isSunBurnTick() {return false;}
    @Override public boolean canFreeze() {return false;}
    @Override protected void doFreezeConversion() {}
    @Override public MobType getMobType() {return MobType.UNDEFINED;}

    /** the skeleton's goals without hiding from the sun or running from wolves; the bow or sword goal comes from reassessWeaponGoal */
    @Override protected void registerGoals() {
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,1.0));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8.0F));
        goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,IronGolem.class,true));
    }

    @Override public void performRangedAttack(LivingEntity target,float power) {
        var weapon=getItemInHand(ProjectileUtil.getWeaponHoldingHand(this,item->item instanceof BowItem));
        AbstractArrow arrow=getArrow(getProjectile(weapon),power);
        if(getMainHandItem().getItem() instanceof BowItem bow)arrow=bow.customArrow(arrow);
        // lead the target by its motion over the flight time
        double ticks=distanceTo(target)/ARROW_SPEED;
        double dx=target.getX()+(target.getX()-target.xo)*ticks-getX();
        double dz=target.getZ()+(target.getZ()-target.zo)*ticks-getZ();
        double dy=target.getY(1.0/3.0)-arrow.getY();
        arrow.shoot(dx,dy+Math.sqrt(dx*dx+dz*dz)*0.2,dz,ARROW_SPEED,INACCURACY);
        playSound(SoundEvents.SKELETON_SHOOT,1.0F,1.0F/(getRandom().nextFloat()*0.4F+0.8F));
        level().addFreshEntity(arrow);
    }

    /**
     * 星陨箭雨 Starfall: seeing its foe 4 to 24 blocks off, it marks the ground under it with a turning circle of glyphs
     * and, as the circle closes, rains six arrows straight down onto the mark from twelve blocks up. Keep moving.
     */
    public static final int STARFALL_ARROWS=6;
    private final ArcaneSkill skill=new ArcaneSkill(140,24,4,24,true,
        (mob,target)->target.position(),
        (level,mob,target,mark,left)->{
            if(left%2==0)ArcaneSkill.ring(level,mob,mark,1.8,10,0.04);
            if(left%4==0)ArcaneSkill.spark(level,mob,mark.x,mark.y+12,mark.z,0,-0.3,0);
        },
        (level,mob,target,mark)->{
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            var random=mob.getRandom();
            for(int i=0;i<STARFALL_ARROWS;i++){
                var arrow=new net.minecraft.world.entity.projectile.Arrow(level,mob);
                arrow.setPos(mark.x+(random.nextDouble()-0.5)*3.2,mark.y+12,mark.z+(random.nextDouble()-0.5)*3.2);
                arrow.shoot(0,-1,0,1.8F,1.5F);
                arrow.setBaseDamage(3);arrow.setCritArrow(true);
                arrow.pickup=net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(arrow);
            }
            ArcaneSkill.burst(level,mob,new net.minecraft.world.phys.Vec3(mark.x,mark.y+12,mark.z),20,0.25);
            level.playSound(null,mob.getX(),mob.getY(),mob.getZ(),net.minecraft.sounds.SoundEvents.ILLUSIONER_CAST_SPELL,net.minecraft.sounds.SoundSource.HOSTILE,1F,1.3F);
        });
    public ArcaneSkill skill() {return skill;}
    @Override protected void customServerAiStep() {super.customServerAiStep();skill.tick(this);}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AlchemicalCreation.movementAndAttack(this,controllers);
        RawAnimation aim=RawAnimation.begin().thenLoop("animation.alchemical_skeleton.aim");
        controllers.add(new AnimationController<>(this,"aim",4,state->{
            if(isAggressive()&&getMainHandItem().getItem() instanceof BowItem)return state.setAndContinue(aim);
            return PlayState.STOP;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return local.primordial.sound.ModSounds.voice("alchemical_skeleton","ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("alchemical_skeleton","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("alchemical_skeleton","death");}
}
