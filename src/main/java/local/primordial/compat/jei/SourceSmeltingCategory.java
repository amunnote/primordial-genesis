package local.primordial.compat.jei;

import local.primordial.Primordial;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 金锭烧成炼金源锭：只认紫水晶碎片作燃料（见 AlchemyFurnaceMixin），所以单独成类，替代原版熔炉/高炉分类里的那两条。 */
public final class SourceSmeltingCategory implements IRecipeCategory<SourceSmeltingCategory.Recipe> {
    public record Recipe(ItemStack station,int ticks,float experience) {}
    public static final RecipeType<Recipe> TYPE=RecipeType.create(Primordial.ID,"source_smelting",Recipe.class);
    public static List<Recipe> recipes() {
        return List.of(new Recipe(new ItemStack(Items.FURNACE),200,0.7F),new Recipe(new ItemStack(Items.BLAST_FURNACE),100,0.7F));
    }
    private final IGuiHelper gui;
    private final IDrawable icon;
    private final IDrawableAnimated flame;
    private final Map<Integer,IDrawableAnimated> arrows=new HashMap<>();
    public SourceSmeltingCategory(IGuiHelper gui) {
        this.gui=gui;
        icon=gui.createDrawableItemLike(Primordial.ALCHEMICAL_SOURCE_INGOT.get());
        flame=gui.createAnimatedRecipeFlame(300);
    }
    @Override public RecipeType<Recipe> getRecipeType() {return TYPE;}
    @Override public Component getTitle() {return Component.translatable("jei.primordial.source_smelting");}
    @Override public IDrawable getIcon() {return icon;}
    @Override public int getWidth() {return 140;}
    @Override public int getHeight() {return 54;}
    @Override public void setRecipe(IRecipeLayoutBuilder builder,Recipe recipe,IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT,1,1).setStandardSlotBackground().addItemStack(new ItemStack(Items.GOLD_INGOT));
        builder.addSlot(RecipeIngredientRole.CATALYST,1,37).setStandardSlotBackground()
            .addItemStack(new ItemStack(Items.AMETHYST_SHARD))
            .addRichTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.primordial.amethyst_fuel")));
        builder.addSlot(RecipeIngredientRole.OUTPUT,61,19).setOutputSlotBackground()
            .addItemStack(new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()));
        builder.addSlot(RecipeIngredientRole.CATALYST,122,1).addItemStack(recipe.station());
    }
    @Override public void draw(Recipe recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        flame.draw(graphics,2,20);
        arrows.computeIfAbsent(recipe.ticks(),gui::createAnimatedRecipeArrow).draw(graphics,26,19);
        var font=Minecraft.getInstance().font;
        graphics.drawString(font,Component.translatable("jei.primordial.source_smelting.time",recipe.ticks()/20),88,24,0xFF808080,false);
        graphics.drawString(font,Component.translatable("jei.primordial.source_smelting.xp",recipe.experience()),88,36,0xFF808080,false);
    }
}
