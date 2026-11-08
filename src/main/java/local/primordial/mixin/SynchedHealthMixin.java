package local.primordial.mixin;

import local.primordial.effect.Protection;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SynchedEntityData.class)
public abstract class SynchedHealthMixin {
    @Accessor("entity") protected abstract Entity angelEntity();
    @Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V",
            at = @At("HEAD"), cancellable = true)
    private <T> void angelRawHealth(EntityDataAccessor<T> key, T value, boolean force, CallbackInfo ci) {
        if (key == HealthAccessor.angelHealthKey() && value instanceof Float health
                && Protection.blocksHealth(angelEntity(), health)) ci.cancel();
    }
}
