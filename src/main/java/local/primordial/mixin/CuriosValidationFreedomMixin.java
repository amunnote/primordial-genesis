package local.primordial.mixin;

import local.primordial.effect.EquipFreedom;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;

@Mixin(targets="top.theillusivec4.curios.mixin.CuriosImplMixinHooks",remap=false)
public abstract class CuriosValidationFreedomMixin {
    @Inject(method="isStackValid",at=@At("HEAD"),cancellable=true)
    private static void ascensionValidate(SlotContext context,ItemStack stack,CallbackInfoReturnable<Boolean> callback) {
        if(EquipFreedom.allows(context,stack))callback.setReturnValue(true);
    }
    @Inject(method="getItemStackSlots(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Map;",at=@At("HEAD"),cancellable=true)
    private static void ascensionSlots(ItemStack stack,net.minecraft.world.entity.LivingEntity entity,CallbackInfoReturnable<java.util.Map<String,top.theillusivec4.curios.api.type.ISlotType>> callback) {
        if(local.primordial.effect.Protection.active(entity)&&!stack.isEmpty()&&top.theillusivec4.curios.api.CuriosApi.getCurio(stack).isPresent())
            callback.setReturnValue(top.theillusivec4.curios.api.CuriosApi.getEntitySlots(entity));
    }
}
