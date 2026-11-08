package local.primordial.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Projectile.class)
public interface AffixProjectileAccess {
    @Invoker("onHitEntity") void angelAffixHit(EntityHitResult hit);
}
