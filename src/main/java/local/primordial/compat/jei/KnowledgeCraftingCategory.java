package local.primordial.compat.jei;

import local.primordial.Primordial;
import local.primordial.alchemy.KnowledgeShapedRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** Crafting-table recipes that need a learned alchemy recipe (taken out of JEI's vanilla crafting category). */
public final class KnowledgeCraftingCategory implements IRecipeCategory<KnowledgeShapedRecipe> {
    public static final RecipeType<KnowledgeShapedRecipe> TYPE=RecipeType.create(Primordial.ID,"knowledge_crafting",KnowledgeShapedRecipe.class);
    private static final int W=116,H=54;
    private final IDrawable icon,arrow;
    public KnowledgeCraftingCategory(IGuiHelper gui) {
        icon=gui.createDrawableItemLike(Items.CRAFTING_TABLE);
        arrow=gui.getRecipeArrow();
    }
    @Override public RecipeType<KnowledgeShapedRecipe> getRecipeType() {return TYPE;}
    @Override public Component getTitle() {return Component.translatable("jei.primordial.knowledge_crafting");}
    @Override public IDrawable getIcon() {return icon;}
    @Override public int getWidth() {return W;}
    @Override public int getHeight() {return H;}

    @Override public void setRecipe(IRecipeLayoutBuilder builder,KnowledgeShapedRecipe recipe,IFocusGroup focuses) {
        var result=recipe.getResultItem(null);
        if(KnowledgeGate.hidden(recipe.knowledge())){KnowledgeGate.addHidden(builder,recipe.getIngredients(),result,95,19);return;}
        var ingredients=recipe.getIngredients();
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            var slot=builder.addSlot(RecipeIngredientRole.INPUT,1+col*18,1+row*18).setStandardSlotBackground();
            if(col<recipe.getWidth()&&row<recipe.getHeight())slot.addIngredients(ingredients.get(row*recipe.getWidth()+col));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT,95,19).setOutputSlotBackground().addItemStack(result);
    }
    @Override public void draw(KnowledgeShapedRecipe recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        if(KnowledgeGate.hidden(recipe.knowledge())){KnowledgeGate.drawLocked(graphics,W,H,recipe.getResultItem(null),recipe.knowledge(),95);return;}
        arrow.draw(graphics,61,19);
    }
}
