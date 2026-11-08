package local.primordial.client;

import local.primordial.Primordial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A small tab on the left edge of the inventory (survival and creative) that opens the status page.
 * Creative tabs run along the top and bottom, so the side is free; when the survival recipe book
 * takes the left side, the tab moves above the top-right corner instead.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class StatusTab {
    static final ResourceLocation ICON=new ResourceLocation(Primordial.ID,"textures/gui/status/tab.png");
    private static final int W=22,H=24;
    private StatusTab(){}
    @SubscribeEvent public static void addStatusTab(ScreenEvent.Init.Post event){
        var screen=event.getScreen();
        if(!(screen instanceof InventoryScreen||screen instanceof CreativeModeInventoryScreen))return;
        var tab=new Tab(screen);tab.place();event.addListener(tab);
    }
    /** the inventory moves when the recipe book opens or closes, so follow it every frame */
    @SubscribeEvent public static void followStatusTab(ScreenEvent.Render.Pre event){
        for(var child:event.getScreen().children())if(child instanceof Tab tab)tab.place();
    }

    static final class Tab extends AbstractButton {
        private final Screen screen;
        Tab(Screen screen){
            super(0,0,W,H,Component.translatable("screen.primordial.status"));this.screen=screen;
            setTooltip(Tooltip.create(Component.translatable("screen.primordial.status")));
        }
        void place(){
            var inventory=(AbstractContainerScreen<?>)screen;
            boolean bookOpen=screen instanceof InventoryScreen inv&&inv.getRecipeBookComponent().isVisible();
            int left=inventory.getGuiLeft()-W+2;
            if(bookOpen||left<0){setX(Math.min(screen.width-W,inventory.getGuiLeft()+inventory.getXSize()-W-2));setY(Math.max(0,inventory.getGuiTop()-H+2));}
            else{setX(left);setY(inventory.getGuiTop()+8);}
        }
        @Override public void onPress(){Minecraft.getInstance().setScreen(new StatusScreen(screen));}
        @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
            int x=getX(),y=getY();boolean hot=isHoveredOrFocused();
            // a tab in the inventory's own palette: light rim, grey body, darker shadow edge
            g.fill(x,y,x+W,y+H,0xFF000000);
            g.fill(x+1,y+1,x+W-1,y+H-1,hot?0xFFE6E6E6:0xFFC6C6C6);
            g.fill(x+1,y+1,x+W-1,y+2,0xFFFFFFFF);g.fill(x+1,y+1,x+2,y+H-1,0xFFFFFFFF);
            g.fill(x+1,y+H-2,x+W-1,y+H-1,0xFF555555);g.fill(x+W-2,y+1,x+W-1,y+H-1,0xFF555555);
            g.blit(ICON,x+3,y+4,0,0,16,16,16,16);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
    }
}
