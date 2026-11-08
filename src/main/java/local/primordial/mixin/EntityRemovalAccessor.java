package local.primordial.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** an entity's removal mark, for {@link local.primordial.effect.ProtectionRepair} to clear one written past the guards */
@Mixin(Entity.class)
public interface EntityRemovalAccessor {
    @Accessor("removalReason") Entity.RemovalReason angelRemovalReason();
    @Accessor("removalReason") void angelSetRemovalReason(Entity.RemovalReason reason);
}
