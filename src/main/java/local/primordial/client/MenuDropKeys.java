package local.primordial.client;
import local.primordial.Primordial;
import local.primordial.effect.Protection;
import local.primordial.network.MenuDropNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class MenuDropKeys {
    @SubscribeEvent public static void key(ScreenEvent.KeyPressed.Pre event) {
        if(event.getKeyCode()!=GLFW.GLFW_KEY_Q
            ||!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
            ||!Protection.active(Minecraft.getInstance().player))return;
        event.setCanceled(true);
        if((event.getModifiers()&GLFW.GLFW_MOD_SHIFT)!=0)
            drop(screen,screen.getSlotUnderMouse(),(event.getModifiers()&GLFW.GLFW_MOD_CONTROL)!=0);
    }
    public static void drop(AbstractContainerScreen<?> screen,Slot slot,boolean all) {
        if(slot==null||!slot.hasItem()||!Protection.active(Minecraft.getInstance().player))return;
        var player=Minecraft.getInstance().player;
        if(screen instanceof CreativeModeInventoryScreen) {
            if(slot.container==player.getInventory()) {
                // Creative inventory wrappers use screen indices, not server menu indices.
                for(var actual:player.inventoryMenu.slots) {
                    if(actual.container==slot.container&&actual.getSlotIndex()==slot.getSlotIndex()) {
                        MenuDropNetwork.CHANNEL.sendToServer(new MenuDropNetwork.Request(
                            player.inventoryMenu.containerId,actual.index,all));
                        return;
                    }
                }
            } else {
                // The creative catalogue contains templates, not owned inventory stacks.
                MenuDropNetwork.CHANNEL.sendToServer(new MenuDropNetwork.TemplateRequest(
                    slot.getItem().copyWithCount(all?slot.getItem().getMaxStackSize():1)));
            }
        } else MenuDropNetwork.CHANNEL.sendToServer(new MenuDropNetwork.Request(screen.getMenu().containerId,slot.index,all));
    }
}
