package local.primordial.mixin;

import local.primordial.effect.Protection;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The world's entities by chunk section: the Ascension Charm's wearer is not taken out of its section without leave. */
@Mixin(EntitySection.class)
public abstract class EntitySectionGuardMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void angelKeep(EntityAccess entity, CallbackInfoReturnable<Boolean> cir) { if (Protection.keepsInWorld(entity)) cir.setReturnValue(false); }
}
