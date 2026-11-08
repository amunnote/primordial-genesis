package local.primordial.mixin;

import local.primordial.alchemy.MysteryEnchanting;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The enchanting table's three rows naming the recipes a Mystery Recipe offers, or that all of its tier are learned. */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMysteryMixin {
    @Unique private static final ResourceLocation ANGEL_CHARM$TABLE=new ResourceLocation("textures/gui/container/enchanting_table.png");

    @Unique private int angelCharm$row(int mouseX,int mouseY) {
        var screen=(EnchantmentScreen)(Object)this;int x=screen.getGuiLeft()+60,y=screen.getGuiTop()+14;
        for(int i=0;i<3;i++)if(mouseX>=x&&mouseX<x+108&&mouseY>=y+19*i&&mouseY<y+19*i+19)return i;
        return -1;
    }

    @Inject(method="renderBg",at=@At("TAIL"))
    private void angelCharm$rows(GuiGraphics graphics,float partial,int mouseX,int mouseY,CallbackInfo info) {
        var screen=(EnchantmentScreen)(Object)this;var menu=screen.getMenu();
        if(!MysteryEnchanting.active(menu.getSlot(0).container))return;
        var font=Minecraft.getInstance().font;int x=screen.getGuiLeft()+60,y=screen.getGuiTop()+14;
        if(menu.levelClue[0]==MysteryEnchanting.ALL_LEARNED){
            var text=Component.translatable("screen.primordial.mystery_recipe.all_learned");
            graphics.drawString(font,text,x+54-font.width(text)/2,y+24,0x8A7AA8,false);
            return;
        }
        int hovered=angelCharm$row(mouseX,mouseY);
        for(int i=0;i<3;i++){
            var recipe=MysteryEnchanting.offered(menu.enchantClue,menu.levelClue,i);if(recipe==null)continue;
            graphics.blit(ANGEL_CHARM$TABLE,x,y+19*i,0,hovered==i?204:166,108,19);
            var stack=AlchemyRecipeItem.create(recipe);
            graphics.renderItem(stack,x+1,y+19*i+1);
            // the name of what it makes: the "alchemy recipe:" before it is the item's, not the row's
            String name=font.plainSubstrByWidth(AlchemyRecipeItem.recipeName(recipe).getString().replaceFirst("^[^：:]*[：:]\\s*",""),86);
            graphics.drawString(font,name,x+20,y+19*i+6,hovered==i?0xFFFF80:0x685E4A,false);
        }
    }

    @Inject(method="render",at=@At("TAIL"))
    private void angelCharm$tooltip(GuiGraphics graphics,int mouseX,int mouseY,float partial,CallbackInfo info) {
        var menu=((EnchantmentScreen)(Object)this).getMenu();
        if(!MysteryEnchanting.active(menu.getSlot(0).container))return;
        var recipe=MysteryEnchanting.offered(menu.enchantClue,menu.levelClue,angelCharm$row(mouseX,mouseY));
        if(recipe!=null)graphics.renderTooltip(Minecraft.getInstance().font,AlchemyRecipeItem.create(recipe),mouseX,mouseY);
    }
}
