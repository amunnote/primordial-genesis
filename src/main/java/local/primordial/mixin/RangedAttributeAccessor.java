package local.primordial.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** the bounds a ranged attribute clamps its values to (see {@link local.primordial.effect.AttributeCeiling}) */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {
    @Accessor("maxValue") double angelMaxValue();
    @Mutable @Accessor("maxValue") void angelSetMaxValue(double value);
}
