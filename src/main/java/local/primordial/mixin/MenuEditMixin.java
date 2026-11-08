package local.primordial.mixin;

import local.primordial.effect.InventoryProtection;
import local.primordial.effect.Protection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class MenuEditMixin {
    @Invoker("doClick")
    protected abstract void angelInvokeDoClick(int slot, int button, ClickType type, Player player);
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void angelNoThrow(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        // -999 with PICKUP throws the carried stack out of the window. Drag-distribution (QUICK_CRAFT) also starts and
        // ends with -999 but drops nothing; blocking it broke any click where the mouse slid over a second slot,
        // which is why creative inventory placement sometimes took several clicks.
        if (Protection.active(player) && (type == ClickType.THROW || slot == -999 && type != ClickType.QUICK_CRAFT)) ci.cancel();
    }
    @Redirect(method = "clicked", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;doClick(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"))
    private void angelManualEdit(AbstractContainerMenu menu, int slot, int button, ClickType type, Player player) {
        InventoryProtection.userEdit(() -> angelInvokeDoClick(slot, button, type, player));
    }
}
