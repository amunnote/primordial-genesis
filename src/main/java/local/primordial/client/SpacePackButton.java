package local.primordial.client;

import local.primordial.Primordial;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.MobAffix;
import local.primordial.network.MenuDropNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.type.ICuriosMenu;

/**
 * On the Curios screen, while Space's accessory is worn: a button above the top-right corner that opens its pack
 * (local.primordial.effect.SpacePack). Shown and hidden every frame, since the accessory can be taken off right there.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class SpacePackButton {
    private static final int W=20,H=20;
    private SpacePackButton() {}
    @SubscribeEvent public static void addSpacePackButton(ScreenEvent.Init.Post event) {
        if(!(event.getScreen() instanceof AbstractContainerScreen<?> screen)||!(screen.getMenu() instanceof ICuriosMenu))return;
        var button=new Button(screen);button.place();event.addListener(button);
    }
    @SubscribeEvent public static void followSpacePackButton(ScreenEvent.Render.Pre event) {
        for(var child:event.getScreen().children())if(child instanceof Button button)button.place();
    }
    static final class Button extends AbstractButton {
        private final AbstractContainerScreen<?> screen;
        private final ItemStack icon=new ItemStack(AffixAccessories.ITEMS.get(MobAffix.SPACE).get());
        Button(AbstractContainerScreen<?> screen) {
            super(0,0,W,H,Component.translatable("container.primordial.space_pack"));this.screen=screen;
            setTooltip(Tooltip.create(Component.translatable("container.primordial.space_pack.open")));
        }
        void place() {
            var player=Minecraft.getInstance().player;
            visible=player!=null&&AffixAccessories.wears(player,MobAffix.SPACE);
            setX(Math.min(screen.width-W,screen.getGuiLeft()+screen.getXSize()-W));setY(Math.max(0,screen.getGuiTop()-H-2));
        }
        @Override public void onPress() {MenuDropNetwork.openSpacePack();}
        @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial) {
            int x=getX(),y=getY();boolean hot=isHoveredOrFocused();
            g.fill(x,y,x+W,y+H,0xFF000000);
            g.fill(x+1,y+1,x+W-1,y+H-1,hot?0xFFE6E6E6:0xFFC6C6C6);
            g.fill(x+1,y+1,x+W-1,y+2,0xFFFFFFFF);g.fill(x+1,y+1,x+2,y+H-1,0xFFFFFFFF);
            g.fill(x+1,y+H-2,x+W-1,y+H-1,0xFF555555);g.fill(x+W-2,y+1,x+W-1,y+H-1,0xFF555555);
            g.renderItem(icon,x+2,y+2);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) {defaultButtonNarrationText(output);}
    }
}
