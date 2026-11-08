package local.primordial.client;

import local.primordial.Primordial;
import local.primordial.affix.ItemSeal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** A sealed item shows how long the seal has left the way an ender pearl shows its cooldown: a pale veil draining away. */
public final class ItemSealDecorator implements IItemDecorator {
    private static final ItemSealDecorator INSTANCE=new ItemSealDecorator();
    @Override public boolean render(GuiGraphics graphics,Font font,ItemStack stack,int x,int y) {
        if(!stack.hasTag()||!stack.getTag().contains(ItemSeal.KEY))return false;
        var level=Minecraft.getInstance().level;
        if(level==null)return false;
        long left=stack.getTag().getLong(ItemSeal.KEY)-level.getGameTime();
        if(left<=0)return false;
        float share=Math.min(1F,left/(float)ItemSeal.DURATION);
        int top=y+(int)Math.floor(16*(1-share));
        graphics.pose().pushPose();graphics.pose().translate(0,0,200);
        graphics.fill(net.minecraft.client.renderer.RenderType.guiOverlay(),x,top,x+16,y+16,0x7FFFFFFF);
        graphics.pose().popPose();
        return false;
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class ItemSealDecoratorRegistration {
        @SubscribeEvent public static void registerItemSealDecorator(RegisterItemDecorationsEvent event) {
            for(var item:ForgeRegistries.ITEMS)event.register(item,INSTANCE);
        }
    }
}
