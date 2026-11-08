package local.primordial.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

/**
 * 鸣金雀 a golden-bell sparrow of the White Tiger's wastes: white and gold, a little bell at its throat. It hops about
 * pecking; hurt, or come upon by a player who is not creeping, it bursts up and away with a ring of its bell, gliding
 * down a few blocks off. Its 金铃羽 feathers fletch 鸣镝, the whistling arrows.
 */
public class GoldenBellSparrow extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    private int flight;

    public GoldenBellSparrow(EntityType<? extends GoldenBellSparrow> type,Level level) {super(type,level);}
    public static AttributeSupplier.Builder attributes() {return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,4).add(Attributes.MOVEMENT_SPEED,0.22);}
    public static boolean canSpawn(EntityType<GoldenBellSparrow> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return level.getBlockState(pos.below()).isSolidRender(level,pos.below())&&level.getRawBrightness(pos,0)>8;
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,1.0));
        goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,6));
        goalSelector.addGoal(5,new RandomLookAroundGoal(this));
    }
    public boolean flying() {return !onGround();}
    /** up and away from what frightened it */
    void startle(Vec3 from) {
        if(flight>0)return;
        var away=position().subtract(from).multiply(1,0,1);
        away=away.lengthSqr()<1e-4?new Vec3(random.nextDouble()-0.5,0,random.nextDouble()-0.5):away;
        away=away.normalize().scale(0.5);
        setDeltaMovement(away.x,0.65,away.z);flight=40;hasImpulse=true;
        playSound(SoundEvents.NOTE_BLOCK_BELL.value(),0.8F,1.8F+random.nextFloat()*0.2F);
    }
    @Override public void aiStep() {
        super.aiStep();
        if(level().isClientSide)return;
        if(flight>0){
            flight--;
            // a glide: it falls slowly, still drawing away
            var v=getDeltaMovement();if(v.y<0)setDeltaMovement(v.x*1.02,v.y*0.6,v.z*1.02);
            if(flight%8==0)playSound(SoundEvents.PARROT_FLY,0.4F,1.6F);
        }else if(tickCount%10==0){
            var player=level().getNearestPlayer(this,4);
            if(player!=null&&!player.isShiftKeyDown()&&!player.isSpectator()&&!player.isCreative())startle(player.position());
        }
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        boolean hurt=super.hurt(source,amount);
        if(hurt&&isAlive())startle(source.getSourcePosition()!=null?source.getSourcePosition():position().add(random.nextDouble()-0.5,0,random.nextDouble()-0.5));
        return hurt;
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) {return false;}
    @Override protected SoundEvent getAmbientSound() {return random.nextInt(3)==0?SoundEvents.NOTE_BLOCK_CHIME.value():SoundEvents.PARROT_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source) {return SoundEvents.PARROT_HURT;}
    @Override protected SoundEvent getDeathSound() {return SoundEvents.PARROT_DEATH;}
    @Override public float getVoicePitch() {return 1.6F+random.nextFloat()*0.2F;}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"body",2,state->state.setAndContinue(RawAnimation.begin().thenLoop("animation.golden_bell_sparrow."
            +(flying()?"fly":state.isMoving()?"walk":"idle")))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
}
