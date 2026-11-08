package local.primordial.mixin;
import local.primordial.effect.InventoryProtection;
import local.primordial.effect.Protection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerDropPacketMixin {
    @Accessor("player") public abstract ServerPlayer angelPacketPlayer();

    // Run after vanilla's thread check. Negative slots are drops, not inventory edits.
    @Inject(method="handleSetCreativeModeSlot",at=@At(value="INVOKE",
        target="Lnet/minecraft/server/level/ServerPlayerGameMode;isCreative()Z"),cancellable=true)
    private void angelCreativeDrop(ServerboundSetCreativeModeSlotPacket packet,CallbackInfo ci) {
        if(packet.getSlotNum()<0&&Protection.active(angelPacketPlayer())) {
            angelPacketPlayer().inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
    // Vanilla has already validated game mode, slot, features and stack size here.
    // Scope the exemption to this one user-requested write; never to the whole tick.
    @Redirect(method="handleSetCreativeModeSlot",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/inventory/Slot;setByPlayer(Lnet/minecraft/world/item/ItemStack;)V"))
    private void angelCreativeEdit(Slot slot,ItemStack stack) {
        InventoryProtection.userEdit(()->slot.setByPlayer(stack));
    }
    @Redirect(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
    private void angelManualSwap(ServerPlayer player, net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack stack) {
        InventoryProtection.userEdit(() -> player.setItemInHand(hand,stack));
    }
    @Redirect(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;drop(Z)Z"))
    private boolean angelShiftDrop(ServerPlayer player, boolean all) {
        if (!Protection.active(player)) return player.drop(all);
        return player.isShiftKeyDown() && InventoryProtection.userDrop(() -> player.drop(all));
    }
}
