package local.primordial.mixin;

import local.primordial.effect.InventoryProtection;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryGuardMixin {
    @Accessor("player") protected abstract Player angelPlayer();
    @Invoker("getItem") protected abstract ItemStack angelGetItem(int slot);
    @Inject(method = {"dropAll", "clearContent"}, at = @At("HEAD"), cancellable = true)
    private void angelClear(CallbackInfo ci) { if (InventoryProtection.locked(angelPlayer())) ci.cancel(); }
    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void angelSet(int slot, ItemStack stack, CallbackInfo ci) {
        if (InventoryProtection.locked(angelPlayer()) && InventoryProtection.removes(angelGetItem(slot), stack)) ci.cancel();
    }
    @Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void angelExtract(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
        if (InventoryProtection.locked(angelPlayer())) cir.setReturnValue(ItemStack.EMPTY);
    }
    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
    private void angelExtractAll(int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (InventoryProtection.locked(angelPlayer())) cir.setReturnValue(ItemStack.EMPTY);
    }
    @Inject(method = "removeItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void angelDelete(ItemStack stack, CallbackInfo ci) { if (InventoryProtection.locked(angelPlayer())) ci.cancel(); }
}
