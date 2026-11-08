package local.primordial.mixin;

import local.primordial.effect.Protection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeDropMixin {
    @Inject(method="slotClicked",at=@At("HEAD"),cancellable=true)
    private void angelBlockPredictedDrop(Slot slot,int slotId,int button,ClickType click,CallbackInfo ci) {
        if(Protection.active(Minecraft.getInstance().player)
            &&(click==ClickType.THROW||(slotId==-999&&click==ClickType.PICKUP)))ci.cancel();
    }
}
