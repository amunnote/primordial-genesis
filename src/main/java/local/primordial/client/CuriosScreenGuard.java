package local.primordial.client;

import local.primordial.Primordial;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.SlotItemHandler;
import top.theillusivec4.curios.api.type.ICuriosMenu;

/** Rebuilds the open Curios menu when a slot points past the end of a resized slot handler. */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class CuriosScreenGuard {
    private CuriosScreenGuard() {}
    @SubscribeEvent public static void beforeRender(ScreenEvent.Render.Pre event) {
        if(!(event.getScreen() instanceof AbstractContainerScreen<?> screen)||!(screen.getMenu() instanceof ICuriosMenu menu))return;
        for(var slot:screen.getMenu().slots) {
            if(slot instanceof SlotItemHandler handled&&handled.getSlotIndex()>=handled.getItemHandler().getSlots()) {
                menu.resetSlots();
                return;
            }
        }
    }
}
