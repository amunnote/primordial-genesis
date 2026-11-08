package local.primordial.mixin;

import local.primordial.effect.Protection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** On the client: the Ascension Charm's wearer is not removed from the world it sees for a death or a discard. */
@Mixin(ClientLevel.class)
public abstract class ClientRemoveGuardMixin {
    @Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
    private void angelKeep(int id, Entity.RemovalReason reason, CallbackInfo ci) {
        if (Protection.lethalRemoval(reason) && Protection.active(((ClientLevel)(Object)this).getEntity(id))) ci.cancel();
    }
}
