package local.primordial.effect;

import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

public final class EquipFreedom {
    private EquipFreedom() {}
    public static boolean allows(SlotContext context,ItemStack stack) {
        return !context.cosmetic()&&!stack.isEmpty()&&Protection.active(context.entity())&&CuriosApi.getCurio(stack).isPresent();
    }
}
