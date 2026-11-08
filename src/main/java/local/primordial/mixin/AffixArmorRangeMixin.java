package local.primordial.mixin;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla's thirty-armor attribute limit would silently truncate the specified affix bonuses. */
@Mixin(RangedAttribute.class)
public abstract class AffixArmorRangeMixin {
    @Inject(method="sanitizeValue",at=@At("HEAD"),cancellable=true)
    private void affixArmorRange(double value,CallbackInfoReturnable<Double> callback) {
        if((Object)this==Attributes.ARMOR)callback.setReturnValue(net.minecraft.util.Mth.clamp(value,0,1024));
    }
}
