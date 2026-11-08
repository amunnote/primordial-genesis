package local.primordial.mixin;

import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * When a chunk takes its biomes, the four lands come first even through another mod's wrapper round the overworld's
 * source (one that lays its own biomes over the multi-noise result would cover them): see GuardianAnchors.resolver.
 */
@Mixin(ChunkAccess.class)
public abstract class ChunkGuardianBiomeMixin {
    @ModifyVariable(method="fillBiomesFromNoise",at=@At("HEAD"),argsOnly=true)
    private BiomeResolver angelCharm$guardianLands(BiomeResolver resolver) {
        return local.primordial.qinglong.GuardianAnchors.resolver(resolver);
    }
}
