package local.primordial.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TransientCraftingContainer.class)
public interface TransientCraftingMenuAccessor {
    @Accessor("menu") AbstractContainerMenu angel_charm$menu();
}
