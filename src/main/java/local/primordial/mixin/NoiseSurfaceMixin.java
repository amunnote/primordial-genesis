package local.primordial.mixin;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The four lands' ground before the overworld's own surface rules (GuardianSurface); only the overworld's settings. */
@Mixin(NoiseGeneratorSettings.class)
public abstract class NoiseSurfaceMixin {
    @Inject(method="surfaceRule",at=@At("RETURN"),cancellable=true)
    private void angelCharm$guardianGround(CallbackInfoReturnable<SurfaceRules.RuleSource> info) {
        if(local.primordial.qinglong.GuardianAnchors.overworldSettings((NoiseGeneratorSettings)(Object)this))
            info.setReturnValue(SurfaceRules.sequence(local.primordial.qinglong.GuardianSurface.rules(),info.getReturnValue()));
    }
}
