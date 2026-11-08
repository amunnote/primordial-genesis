package local.primordial.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Just before the levels are made, after every server-start handler has had its turn (another mod may have wrapped
 * or replaced the overworld's biome source and noise settings by then): see GuardianAnchors.
 */
@Mixin(MinecraftServer.class)
public abstract class WrappedBiomeSourceMixin {
    @Inject(method="createLevels",at=@At("HEAD"))
    private void angelCharm$guardianSeats(ChunkProgressListener listener,CallbackInfo info) {
        local.primordial.compat.LegacyIds.migrateWorld((MinecraftServer)(Object)this);
        local.primordial.qinglong.GuardianAnchors.prepare((MinecraftServer)(Object)this);
    }
}
