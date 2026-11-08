package local.primordial.mixin;

import local.primordial.effect.Protection;
import local.primordial.network.MenuDropNetwork;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerDropMixin {
    @Inject(method="drop(Z)Z",at=@At("HEAD"),cancellable=true)
    private void angelWorldDrop(boolean all,CallbackInfoReturnable<Boolean> ci) {
        var player=(LocalPlayer)(Object)this;
        if(!Protection.active(player))return;
        boolean deliberate=Screen.hasShiftDown()&&!player.getMainHandItem().isEmpty();
        if(deliberate)MenuDropNetwork.CHANNEL.sendToServer(new MenuDropNetwork.Request(
            player.inventoryMenu.containerId,36+player.getInventory().selected,all));
        ci.setReturnValue(deliberate);
    }
}
