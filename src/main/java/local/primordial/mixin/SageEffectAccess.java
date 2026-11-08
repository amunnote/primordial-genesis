package local.primordial.mixin;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MobEffectInstance.class)
public interface SageEffectAccess {
    @Accessor("duration") void sageDuration(int value);
    @Accessor("hiddenEffect") MobEffectInstance sageHidden();
    @Accessor("hiddenEffect") void sageHidden(MobEffectInstance hidden);
}
