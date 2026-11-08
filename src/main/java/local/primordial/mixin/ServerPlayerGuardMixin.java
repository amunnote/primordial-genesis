package local.primordial.mixin;

import local.primordial.effect.Protection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerGuardMixin {
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void angelDie(DamageSource source, CallbackInfo ci) { if (Protection.active((Entity)(Object)this)) ci.cancel(); }
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void angelHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Protection.active((Entity)(Object)this)) cir.setReturnValue(false);
    }
}
