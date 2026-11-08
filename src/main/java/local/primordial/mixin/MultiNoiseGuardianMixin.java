package local.primordial.mixin;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The four guardians' lands, laid over the overworld's climate map (GuardianAnchors). */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseGuardianMixin {
    @Inject(method="getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",at=@At("HEAD"),cancellable=true)
    private void angelCharm$guardianLand(int x,int y,int z,Climate.Sampler sampler,CallbackInfoReturnable<Holder<Biome>> info) {
        var forced=local.primordial.qinglong.GuardianAnchors.forced((BiomeSource)(Object)this,x,y,z,sampler);
        if(forced!=null)info.setReturnValue(forced);
    }
    /** so their features and courts are known to the world before any of them is laid down */
    @Inject(method="collectPossibleBiomes",at=@At("RETURN"),cancellable=true)
    private void angelCharm$guardianBiomes(CallbackInfoReturnable<Stream<Holder<Biome>>> info) {
        info.setReturnValue(local.primordial.qinglong.GuardianAnchors.withGuardianLands(info.getReturnValue().toList()));
    }
}
