package local.primordial.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

/**
 * 冥灯鱼 a netherlamp fish of the Black Tortoise's deep sea: dark, with a lamp hung before its mouth on a stalk. A fish
 * like any other (it flees, it can be taken in a water bucket); its 冥灯囊 makes 冥灯.
 */
public class NetherlampFish extends AbstractFish implements GeoEntity {
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public NetherlampFish(EntityType<? extends NetherlampFish> type,Level level) {super(type,level);}
    public static AttributeSupplier.Builder attributes() {return AbstractFish.createAttributes();}
    /** the deep: six or more blocks below the sea's face */
    public static boolean canSpawn(EntityType<NetherlampFish> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return level.getFluidState(pos).is(FluidTags.WATER)&&(reason!=MobSpawnType.NATURAL||pos.getY()<=level.getLevel().getSeaLevel()-6);
    }
    @Override public ItemStack getBucketItemStack() {return new ItemStack(FaunaContent.LAMPFISH_BUCKET.get());}
    @Override protected SoundEvent getFlopSound() {return SoundEvents.COD_FLOP;}
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return SoundEvents.COD_HURT;}
    @Override protected SoundEvent getDeathSound() {return SoundEvents.COD_DEATH;}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this,"body",3,state->state.setAndContinue(RawAnimation.begin().thenLoop("animation.netherlamp_fish."+(isInWater()?"swim":"flop")))));
        controllers.add(new AnimationController<>(this,"lamp",0,state->state.setAndContinue(RawAnimation.begin().thenLoop("animation.netherlamp_fish.glow"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
}
