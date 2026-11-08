package local.primordial.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

/**
 * 雷萤 a thunder firefly: drifts over the Azure Dragon's grove at night and in rain, its tail bright with stored
 * lightning. Harmless; a glass bottle used on it takes it (a 萤雷瓶).
 */
public class ThunderFirefly extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public ThunderFirefly(EntityType<? extends ThunderFirefly> type,Level level) {
        super(type,level);moveControl=new FlyingMoveControl(this,20,true);setNoGravity(true);
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,2).add(Attributes.FLYING_SPEED,0.35).add(Attributes.MOVEMENT_SPEED,0.18);
    }
    public static boolean canSpawn(EntityType<ThunderFirefly> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        var world=level.getLevel();
        return reason!=MobSpawnType.NATURAL||(world.isNight()||world.isRaining())&&level.getBlockState(pos).isAir();
    }
    @Override protected PathNavigation createNavigation(Level level) {
        var nav=new FlyingPathNavigation(this,level);nav.setCanOpenDoors(false);nav.setCanFloat(true);nav.setCanPassDoors(true);return nav;
    }
    @Override protected void registerGoals() {goalSelector.addGoal(1,new WaterAvoidingRandomFlyingGoal(this,1.0));}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        var held=player.getItemInHand(hand);
        if(!held.is(Items.GLASS_BOTTLE))return super.mobInteract(player,hand);
        playSound(SoundEvents.BOTTLE_FILL,1F,1.4F);
        if(!level().isClientSide){
            player.setItemInHand(hand,ItemUtils.createFilledResult(held,player,new ItemStack(FaunaContent.FIREFLY_BOTTLE.get())));
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public void aiStep() {
        super.aiStep();
        if(level().isClientSide&&random.nextInt(24)==0)level().addParticle(ParticleTypes.ELECTRIC_SPARK,getX(),getY()+0.1,getZ(),0,0,0);
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source) {return false;}
    @Override protected void checkFallDamage(double y,boolean onGround,net.minecraft.world.level.block.state.BlockState state,BlockPos pos) {}
    @Override public boolean isPushable() {return false;}
    @Override protected void playStepSound(BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return random.nextInt(4)==0?SoundEvents.AMETHYST_BLOCK_CHIME:null;}
    @Override protected float getSoundVolume() {return 0.3F;}
    @Override public SoundSource getSoundSource() {return SoundSource.AMBIENT;}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"wings",0,state->state.setAndContinue(RawAnimation.begin().thenLoop("animation.thunder_firefly.fly"))));
        controllers.add(new AnimationController<>(this,"light",0,state->state.setAndContinue(RawAnimation.begin().thenLoop("animation.thunder_firefly.glow"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
}
