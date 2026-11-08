package local.primordial.compat.jei;

import java.util.List;
import local.primordial.Primordial;
import local.primordial.affix.AffixMaterials;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 寻灵罗盘's exchange (GuideEvents.exchange): holding any Sin Lord's affix material, right-click an enchanting table to
 * give the material and 30 levels for the compass. Not a crafting recipe, so a category of its own.
 */
public final class SpiritCompassCategory implements IRecipeCategory<SpiritCompassCategory.Exchange> {
    public record Exchange() {}
    public static final RecipeType<Exchange> TYPE=RecipeType.create(Primordial.ID,"spirit_compass",Exchange.class);
    public static final int LEVELS=30;
    private final IDrawable icon,arrow;
    public SpiritCompassCategory(IGuiHelper gui) {
        icon=gui.createDrawableItemLike(Primordial.SPIRIT_COMPASS.get());
        arrow=gui.getRecipeArrow();
    }
    @Override public RecipeType<Exchange> getRecipeType() {return TYPE;}
    @Override public Component getTitle() {return Component.translatable("jei.primordial.spirit_compass");}
    @Override public IDrawable getIcon() {return icon;}
    @Override public int getWidth() {return 140;}
    @Override public int getHeight() {return 54;}
    @Override public void setRecipe(IRecipeLayoutBuilder builder,Exchange exchange,IFocusGroup focuses) {
        // any of the seven materials will do: they turn through in the slot
        List<ItemStack> materials=AffixMaterials.BOSS_ITEMS.values().stream().map(item->new ItemStack(item.get())).toList();
        builder.addSlot(RecipeIngredientRole.INPUT,1,19).setStandardSlotBackground().addItemStacks(materials)
            .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.primordial.spirit_compass.any")));
        builder.addSlot(RecipeIngredientRole.CATALYST,1,1).addItemStack(new ItemStack(Items.ENCHANTING_TABLE))
            .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.primordial.spirit_compass.table")));
        builder.addSlot(RecipeIngredientRole.OUTPUT,61,19).setOutputSlotBackground().addItemStack(new ItemStack(Primordial.SPIRIT_COMPASS.get()));
    }
    @Override public void draw(Exchange exchange,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        arrow.draw(graphics,26,19);
        var font=Minecraft.getInstance().font;
        graphics.drawString(font,Component.translatable("jei.primordial.spirit_compass.levels",LEVELS),88,18,0xFF80FF20,true);
        graphics.drawString(font,Component.translatable("jei.primordial.spirit_compass.how"),88,30,0xFF808080,false);
    }
}
