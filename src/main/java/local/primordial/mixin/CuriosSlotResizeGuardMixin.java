package local.primordial.mixin;

import com.google.common.collect.Multimap;
import local.primordial.effect.InventoryProtection;
import local.primordial.sage.SageHeart;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@Mixin(targets="top.theillusivec4.curios.common.inventory.CurioStacksHandler",remap=false)
public abstract class CuriosSlotResizeGuardMixin {
    @Shadow private ICuriosItemHandler itemHandler;
    @Shadow @Final private String identifier;
    @Shadow private IDynamicStackHandler stackHandler;
    @Shadow private IDynamicStackHandler cosmeticStackHandler;
    @Shadow private int baseSize;
    @Shadow private boolean update;
    @Shadow private Multimap<AttributeModifier.Operation,AttributeModifier> modifiersByOperation;
    @Unique private boolean sageCheckingResize;
    @Inject(method="update",at=@At("HEAD"),cancellable=true)
    private void sageAtomicResize(CallbackInfo ci) {
        // Capability lookups during the guard can re-enter this getter. Read the existing
        // lists in that case; never perform part of a resize during the permission check.
        if(sageCheckingResize){ci.cancel();return;}
        if(!update||itemHandler==null||stackHandler==null)return;
        double size=baseSize;
        for(var modifier:modifiersByOperation.get(AttributeModifier.Operation.ADDITION))size+=modifier.getAmount();
        for(var modifier:modifiersByOperation.get(AttributeModifier.Operation.MULTIPLY_BASE))size+=baseSize*modifier.getAmount();
        for(var modifier:modifiersByOperation.get(AttributeModifier.Operation.MULTIPLY_TOTAL))size*=modifier.getAmount();
        int wanted=(int)Math.max(0,size);if(wanted>=stackHandler.getSlots())return;
        sageCheckingResize=true;
        try {
            var wearer=itemHandler.getWearer();
            // The client recomputes its own slot count from the synced modifiers. It must reach the same
            // verdict as the server, otherwise it shrinks alone and the Curios screen indexes missing slots.
            boolean client=wearer!=null&&wearer.level().isClientSide;
            boolean protectedInventory=client?local.primordial.effect.Protection.active(wearer):InventoryProtection.locked(wearer);
            // While Sage's Gift is still worn, the slots it gives must not shrink under what they hold: Curios can recount
            // them before the gift's modifiers are back (logging in, respawning, changing dimension), and the
            // accessories pushed out would fall on the ground. They only go when the gift itself is taken off.
            boolean sageSlots=local.primordial.Primordial.ALCHEMICAL_SLOT.equals(identifier)||local.primordial.Primordial.ALCHEMICAL_ACCESSORY_SLOT.equals(identifier);
            boolean giftWorn=sageSlots&&wearer!=null&&SageHeart.active(wearer);
            for(int i=wanted;i<stackHandler.getSlots();i++){
                var stack=stackHandler.getStackInSlot(i);
                boolean cosmeticItem=cosmeticStackHandler!=null&&i<cosmeticStackHandler.getSlots()&&!cosmeticStackHandler.getStackInSlot(i).isEmpty();
                if((client?clientBound(wearer,stack):SageHeart.bound(wearer,stack))||protectedInventory&&(!stack.isEmpty()||cosmeticItem)
                    ||giftWorn&&(!stack.isEmpty()||cosmeticItem)){ci.cancel();return;}
            }
        }finally{sageCheckingResize=false;}
    }
    @Unique private static boolean clientBound(net.minecraft.world.entity.LivingEntity wearer,net.minecraft.world.item.ItemStack stack) {
        return wearer!=null&&stack.is(local.primordial.Primordial.SAGE_HEART.get())
            &&!(wearer instanceof net.minecraft.world.entity.player.Player player&&player.isCreative())
            &&!local.primordial.effect.Protection.active(wearer)&&wearer.isAlive();
    }
}
