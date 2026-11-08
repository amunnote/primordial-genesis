package local.primordial.mixin;

import local.primordial.effect.Protection;
import local.primordial.effect.HarmfulEffectProtection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingGuardMixin {
    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void angelUntargetable(CallbackInfoReturnable<Boolean> cir) {
        if (Protection.active((Entity)(Object)this)) cir.setReturnValue(false);
    }
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void angelHealth(float health, CallbackInfo ci) {
        if (Protection.blocksHealth((Entity)(Object)this, health)) ci.cancel();
    }
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void angelHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Protection.active((Entity)(Object)this)) cir.setReturnValue(false);
    }
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void angelDie(DamageSource source, CallbackInfo ci) { if (Protection.active((Entity)(Object)this)) ci.cancel(); }
    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void angelKill(CallbackInfo ci) { if (Protection.active((Entity)(Object)this)) ci.cancel(); }
    /** the dying animation that ends in removal: it never starts for the wearer, whatever wrote its death */
    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void angelTickDeath(CallbackInfo ci) {
        var self = (LivingEntity)(Object)this;
        if (Protection.active(self)) { self.deathTime = 0; ci.cancel(); }
    }
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void angelRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (Protection.lethalRemoval(reason) && Protection.active((Entity)(Object)this)) ci.cancel();
    }
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void angelEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && HarmfulEffectProtection.active((Entity)(Object)this))
            cir.setReturnValue(false);
    }
    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true)
    private void angelForceEffect(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && HarmfulEffectProtection.active((Entity)(Object)this)) ci.cancel();
    }
}
