package local.primordial.mixin;
import java.util.function.Function;
import local.primordial.effect.CuriosOwner;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.theillusivec4.curios.api.SlotContext;

@Mixin(targets = "top.theillusivec4.curios.common.inventory.DynamicStackHandler", remap = false)
public abstract class CuriosOwnerMixin implements CuriosOwner {
    @Shadow protected Function<Integer, SlotContext> ctxBuilder;
    @Override public LivingEntity angelWearer() { return ctxBuilder == null ? null : ctxBuilder.apply(0).entity(); }
    @org.spongepowered.asm.mixin.injection.Inject(method = "shrink", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void angelShrink(int amount, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        var stacks=(top.theillusivec4.curios.common.inventory.DynamicStackHandler)(Object)this;
        for(int i=Math.max(0,stacks.getSlots()-amount);i<stacks.getSlots();i++)
            if(local.primordial.sage.SageHeart.bound(angelWearer(),stacks.getStackInSlot(i))
                ||local.primordial.effect.InventoryProtection.locked(angelWearer())&&!stacks.getStackInSlot(i).isEmpty()){ci.cancel();break;}
    }
}
