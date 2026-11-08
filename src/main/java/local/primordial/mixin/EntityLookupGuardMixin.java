package local.primordial.mixin;

import local.primordial.effect.Protection;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The world's index of its entities by id and UUID: the Ascension Charm's wearer is not struck from it without leave. */
@Mixin(EntityLookup.class)
public abstract class EntityLookupGuardMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void angelKeep(EntityAccess entity, CallbackInfo ci) { if (Protection.keepsInWorld(entity)) ci.cancel(); }
}
