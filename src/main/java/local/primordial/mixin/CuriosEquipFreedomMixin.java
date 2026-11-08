package local.primordial.mixin;

import java.util.function.Function;
import local.primordial.effect.EquipFreedom;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;

@Mixin(targets="top.theillusivec4.curios.common.inventory.DynamicStackHandler",remap=false)
public abstract class CuriosEquipFreedomMixin {
    @Shadow private Function<Integer,SlotContext> ctxBuilder;
    @Inject(method="isItemValid",at=@At("HEAD"),cancellable=true)
    private void ascensionEquip(int slot,ItemStack stack,CallbackInfoReturnable<Boolean> callback) {
        var handler=(top.theillusivec4.curios.common.inventory.DynamicStackHandler)(Object)this;
        if(slot>=0&&slot<handler.getSlots()&&EquipFreedom.allows(ctxBuilder.apply(slot),stack))callback.setReturnValue(true);
    }
}
