package local.primordial.mixin;

import java.io.DataInput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every compound read, from disk or the wire, has the mod's old name turned to its new as it is read (LegacyIds). */
@Mixin(NbtIo.class)
public abstract class NbtLegacyMixin {
    @Inject(method="read(Ljava/io/DataInput;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;",at=@At("RETURN"))
    private static void angelCharm$legacyIds(DataInput input,NbtAccounter accounter,CallbackInfoReturnable<CompoundTag> info) {
        local.primordial.compat.LegacyIds.migrate(info.getReturnValue());
    }
}
