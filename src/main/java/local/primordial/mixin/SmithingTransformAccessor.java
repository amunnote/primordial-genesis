package local.primordial.mixin;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** a smithing upgrade's three inputs, which it does not list as ingredients; read by the recipe map */
@Mixin(SmithingTransformRecipe.class)
public interface SmithingTransformAccessor {
    @Accessor("template") Ingredient angel$template();
    @Accessor("base") Ingredient angel$base();
    @Accessor("addition") Ingredient angel$addition();
}
