package local.primordial.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** lets an alchemical creeper put its fuse out after a blast it survives */
@Mixin(Creeper.class)
public interface CreeperAccessor {
    @Accessor("swell") void angel$setSwell(int swell);
    @Accessor("oldSwell") void angel$setOldSwell(int swell);
    @Accessor("DATA_IS_IGNITED")
    static EntityDataAccessor<Boolean> angel$ignited() { throw new AssertionError("Mixin accessor not applied"); }
}
