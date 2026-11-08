package local.primordial.mixin;

import local.primordial.affix.AffixGrowth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Growth mobs are bigger: getScale() feeds their hitbox (and eye height) on both sides. */
@Mixin(LivingEntity.class)
public abstract class AffixGrowthScaleMixin {
    @Inject(method="getScale",at=@At("RETURN"),cancellable=true)
    private void angelGrowthScale(CallbackInfoReturnable<Float> cir) {
        float growth=AffixGrowth.growth((LivingEntity)(Object)this);
        if(growth>1)cir.setReturnValue(cir.getReturnValueF()*growth);
    }
}
