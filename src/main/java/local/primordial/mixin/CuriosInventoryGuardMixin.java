package local.primordial.mixin;
import java.util.Map;
import local.primordial.Primordial;
import local.primordial.effect.InventoryProtection;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

@Mixin(targets = "top.theillusivec4.curios.common.capability.CurioInventoryCapability$CurioInventoryWrapper", remap = false)
public abstract class CuriosInventoryGuardMixin {
    @Shadow LivingEntity wearer;
    @Shadow Map<String, ICurioStacksHandler> curios;
    private boolean angelLocked() {
        // reset is also called during capability construction: never recurse into an unfinished capability.
        if (curios == null || !Primordial.CHARM.isPresent()) return false;
        for (var charm:curios.values())for (int i=0;i<charm.getSlots();i++) {
            var stack=charm.getStacks().getStackInSlot(i);
            if(local.primordial.sage.SageHeart.bound(wearer,stack))return true;
            if(stack.is(Primordial.CHARM.get())&&InventoryProtection.locked(wearer))return true;
        }
        return false;
    }
    @Inject(method = "reset", at = @At("HEAD"), cancellable = true)
    private void angelReset(CallbackInfo ci) { if (angelLocked()) ci.cancel(); }
    @Inject(method = "setCurios", at = @At("HEAD"), cancellable = true)
    private void angelReplace(Map<String, ICurioStacksHandler> replacements, CallbackInfo ci) { if (angelLocked()) ci.cancel(); }
    @Inject(method = "readTag", at = @At("HEAD"), cancellable = true)
    private void angelRead(net.minecraft.nbt.Tag nbt, CallbackInfo ci) { if (angelLocked()) ci.cancel(); }
    @Inject(method = "loadInventory", at = @At("HEAD"), cancellable = true)
    private void angelLoad(net.minecraft.nbt.ListTag nbt, CallbackInfo ci) { if (angelLocked()) ci.cancel(); }
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = {
        "saveInventory(Z)Lnet/minecraft/nbt/ListTag;",
        "saveInventory(ZLjava/util/function/Predicate;)Lnet/minecraft/nbt/ListTag;",
        "saveInventory(ZLjava/util/function/BiPredicate;)Lnet/minecraft/nbt/ListTag;"},
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean angelDoNotClear(boolean clear) { return angelLocked() ? false : clear; }
}
