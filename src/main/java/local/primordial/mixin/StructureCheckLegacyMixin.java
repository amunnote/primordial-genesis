package local.primordial.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The structure check reads which structures a stored chunk holds by scanning it, not by the read NbtLegacyMixin sees:
 * the old names are made new here too, so /locate and the compass find courts generated when the mod was angel_charm.
 */
@Mixin(StructureCheck.class)
public abstract class StructureCheckLegacyMixin {
    @Inject(method="loadStructures",at=@At("HEAD"))
    private void angelCharm$legacyStarts(CompoundTag starts,CallbackInfoReturnable<?> info) {
        local.primordial.compat.LegacyIds.migrate(starts);
    }
}
