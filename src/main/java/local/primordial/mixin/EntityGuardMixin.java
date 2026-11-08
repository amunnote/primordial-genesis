package local.primordial.mixin;

import local.primordial.effect.Protection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGuardMixin {
    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void angelUntargetable(CallbackInfoReturnable<Boolean> cir) {
        if (Protection.active((Entity)(Object)this)) cir.setReturnValue(false);
    }
    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void angelKill(CallbackInfo ci) { if (Protection.active((Entity)(Object)this)) ci.cancel(); }
    /** the wearer is never set at a position that is not a number (an infinite or NaN one leaves it nowhere) */
    @Inject(method = "setPosRaw", at = @At("HEAD"), cancellable = true)
    private void angelNowhere(double x, double y, double z, CallbackInfo ci) {
        if ((!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) && Protection.active((Entity)(Object)this)) ci.cancel();
    }
    @Inject(method = {"remove", "setRemoved"}, at = @At("HEAD"), cancellable = true)
    private void angelRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (Protection.lethalRemoval(reason) && Protection.active((Entity)(Object)this)) ci.cancel();
    }
}
