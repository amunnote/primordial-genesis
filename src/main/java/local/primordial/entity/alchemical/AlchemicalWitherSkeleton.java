package local.primordial.entity.alchemical;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 炼金凋骸. A wither skeleton rebuilt smaller: it fits through the two-block gaps that keep its kin out, is no undead (Smite
 * and healing potions do nothing special), shrugs off half of all knockback, does not run from wolves, and its blows
 * wither twice as hard. One in four carries a bow and shoots burning arrows.
 */
public class AlchemicalWitherSkeleton extends WitherSkeleton implements AlchemicalCreation {
    public static final float BOW_CHANCE=0.25F;
    private final AnimatableInstanceCache cache=AlchemicalCreation.cache(this);
    public AlchemicalWitherSkeleton(EntityType<? extends WitherSkeleton> type,Level level) {super(type,level);}

    public static AttributeSupplier.Builder attributes() {
        return AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH,32).add(Attributes.MOVEMENT_SPEED,0.3)
            .add(Attributes.ATTACK_DAMAGE,5).add(Attributes.KNOCKBACK_RESISTANCE,0.5).add(Attributes.ARMOR,4);
    }
    @Override public String creationId() {return "alchemical_wither_skeleton";}
    @Override public MobType getMobType() {return MobType.UNDEFINED;}
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size) {return 1.74F;}

    @Override protected void registerGoals() {
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,1.0));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8.0F));
        goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,IronGolem.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,AbstractPiglin.class,true));
    }
    @Override protected void populateDefaultEquipmentSlots(RandomSource random,DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(random.nextFloat()<BOW_CHANCE?Items.BOW:Items.STONE_SWORD));
    }
    @Override public boolean doHurtTarget(Entity target) {
        if(!super.doHurtTarget(target))return false;
        if(target instanceof LivingEntity living)living.addEffect(new MobEffectInstance(MobEffects.WITHER,200,1),this);
        return true;
    }

    /**
     * 幽魂斩波 Spectral Cleave: with its foe within 5 blocks, glyphs gather along its blade in an arc before it; then it
     * sweeps: everything in a 110 degree arc up to 5 blocks ahead takes 8 damage, withers (II, four seconds) and is thrown back.
     */
    public static final float CLEAVE_DAMAGE=8;public static final double CLEAVE_RANGE=5,CLEAVE_HALF_ANGLE=55;
    private final ArcaneSkill skill=new ArcaneSkill(120,10,0,CLEAVE_RANGE,false,
        (mob,target)->mob.position(),
        (level,mob,target,mark,left)->{
            mob.getLookControl().setLookAt(target);
            if(left%2==0)arc(level,mob,2.5,7);
        },
        (level,mob,target,mark)->{
            mob.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            for(var e:inArc(mob)){
                e.hurt(ArcaneSkill.melee(mob),CLEAVE_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.WITHER,80,1),mob);
                ArcaneSkill.knock(e,mob.position(),0.8,0.25);
            }
            for(double r=1.5;r<=CLEAVE_RANGE;r+=1.2)arc(level,mob,r,(int)(r*4));
            level.playSound(null,mob.getX(),mob.getY(),mob.getZ(),net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,net.minecraft.sounds.SoundSource.HOSTILE,1.2F,0.6F);
            level.playSound(null,mob.getX(),mob.getY(),mob.getZ(),net.minecraft.sounds.SoundEvents.WITHER_SHOOT,net.minecraft.sounds.SoundSource.HOSTILE,0.4F,1.4F);
        });
    /** its foes within the cleave's arc */
    static java.util.List<LivingEntity> inArc(net.minecraft.world.entity.Mob mob) {
        var facing=net.minecraft.world.phys.Vec3.directionFromRotation(0,mob.getYRot());
        var out=new java.util.ArrayList<LivingEntity>();
        for(var e:ArcaneSkill.foesAround(mob,mob.position(),CLEAVE_RANGE,2)){
            var to=new net.minecraft.world.phys.Vec3(e.getX()-mob.getX(),0,e.getZ()-mob.getZ());
            if(to.lengthSqr()<0.25||Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,to.normalize().dot(facing)))))<=CLEAVE_HALF_ANGLE)out.add(e);
        }
        return out;
    }
    static void arc(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Mob mob,double radius,int count) {
        double yaw=Math.toRadians(mob.getYRot()+90);
        for(int i=0;i<count;i++){
            double a=yaw+Math.toRadians(-CLEAVE_HALF_ANGLE+2*CLEAVE_HALF_ANGLE*i/(count-1.0));
            ArcaneSkill.spark(level,mob,mob.getX()+Math.cos(a)*radius,mob.getY(0.5),mob.getZ()+Math.sin(a)*radius,0,0.02,0);
        }
    }
    public ArcaneSkill skill() {return skill;}
    @Override protected void customServerAiStep() {super.customServerAiStep();skill.tick(this);}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AlchemicalCreation.movementAndAttack(this,controllers);
        RawAnimation aim=RawAnimation.begin().thenLoop("animation.alchemical_wither_skeleton.aim");
        controllers.add(new AnimationController<>(this,"aim",4,state->
            isAggressive()&&getMainHandItem().getItem() instanceof BowItem?state.setAndContinue(aim):PlayState.STOP));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return local.primordial.sound.ModSounds.voice("alchemical_wither_skeleton","ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("alchemical_wither_skeleton","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("alchemical_wither_skeleton","death");}
}
