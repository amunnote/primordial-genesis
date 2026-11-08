package local.primordial.mixin;

import local.primordial.entity.alchemical.AlchemicalCreeper;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An alchemical creeper's fuse ends in a blast it survives, instead of the creeper's own fatal explosion. */
@Mixin(Creeper.class)
public abstract class CreeperDetonationMixin {
    @Inject(method="explodeCreeper",at=@At("HEAD"),cancellable=true)
    private void angel$alchemicalDetonation(CallbackInfo ci) {
        if((Object)this instanceof AlchemicalCreeper creeper){creeper.detonate();ci.cancel();}
    }
}
