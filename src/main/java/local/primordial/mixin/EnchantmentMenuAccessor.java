package local.primordial.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The enchanting table's two private fields EnchantmentMenuMysteryMixin needs (accessors, so the refmap maps them). */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {
    @Accessor("enchantSlots") Container primordial$enchantSlots();
    @Accessor("enchantmentSeed") DataSlot primordial$enchantmentSeed();
}
