package local.primordial.mixin;
import local.primordial.effect.CuriosOwner;
import local.primordial.effect.InventoryProtection;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStackHandler.class, remap = false)
public abstract class CuriosStackGuardMixin {
    @Shadow public abstract ItemStack getStackInSlot(int slot);
    @Shadow public abstract int getSlots();
    private boolean angelLocked() { return this instanceof CuriosOwner owner && InventoryProtection.locked(owner.angelWearer()); }
    @Inject(method = "setStackInSlot", at = @At("HEAD"), cancellable = true)
    private void angelSet(int slot, ItemStack stack, CallbackInfo ci) {
        if ((angelLocked() || sageBound(slot)) && InventoryProtection.removes(getStackInSlot(slot), stack)) ci.cancel();
    }
    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void angelExtract(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (angelLocked() || sageBound(slot)) cir.setReturnValue(ItemStack.EMPTY);
    }
    @Inject(method = "setSize", at = @At("HEAD"), cancellable = true)
    private void angelResize(int size, CallbackInfo ci) {
        for(int i=Math.max(0,size);i<getSlots();i++)
            if(sageBound(i)||angelLocked()&&!getStackInSlot(i).isEmpty()){ci.cancel();break;}
    }
    @Inject(method = "deserializeNBT", at = @At("HEAD"), cancellable = true)
    private void angelDeserialize(net.minecraft.nbt.CompoundTag nbt, CallbackInfo ci) {
        if (angelLocked()) ci.cancel();
        for(int i=0;i<getSlots();i++)if(sageBound(i)){ci.cancel();break;}
    }
    private boolean sageBound(int slot) {
        return slot>=0&&slot<getSlots()&&this instanceof CuriosOwner owner
            &&local.primordial.sage.SageHeart.bound(owner.angelWearer(),getStackInSlot(slot));
    }
}
