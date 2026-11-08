package local.primordial.mixin;

import local.primordial.effect.CooldownBridge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCooldowns.class)
public abstract class CooldownMixin {
    @Inject(method = "addCooldown", at = @At("HEAD"), cancellable = true)
    private void angelCooldown(Item item, int ticks, CallbackInfo ci) {
        if (CooldownBridge.bypass((ItemCooldowns)(Object)this, item)) ci.cancel();
    }
    @Inject(method = "getCooldownPercent", at = @At("HEAD"), cancellable = true)
    private void angelRemaining(Item item, float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (CooldownBridge.bypass((ItemCooldowns)(Object)this, item)) cir.setReturnValue(0F);
    }
}
