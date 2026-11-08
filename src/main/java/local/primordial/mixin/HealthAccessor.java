package local.primordial.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface HealthAccessor {
    @Accessor("dead") void angelSetDead(boolean dead);
    @Accessor("dead") boolean angelIsDead();
    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> angelHealthKey() { throw new AssertionError("Mixin accessor not applied"); }
}
