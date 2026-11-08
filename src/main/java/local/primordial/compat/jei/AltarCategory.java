package local.primordial.compat.jei;

import local.primordial.Primordial;
import local.primordial.altar.AltarCrafting;
import local.primordial.altar.AltarRecipe;
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

/** 炼金仪炼成：主材料居中，其他材料围成一圈（多于 10 个时内外两圈），右侧为产物，底部注明所需炼金仪。 */
public final class AltarCategory implements IRecipeCategory<AltarRecipe> {
    public static final RecipeType<AltarRecipe> TYPE=RecipeType.create(Primordial.ID,"altar",AltarRecipe.class);
    private static final int W=150,H=96,CX=44,CY=44;
    private final IDrawable icon,arrow;
    public AltarCategory(IGuiHelper gui) {
        icon=gui.createDrawableItemLike(Primordial.BASIC_ALCHEMY_ALTAR_ITEM.get());
        arrow=gui.getRecipeArrow();
    }
    @Override public RecipeType<AltarRecipe> getRecipeType() {return TYPE;}
    @Override public Component getTitle() {return Component.translatable("jei.primordial.altar");}
    @Override public IDrawable getIcon() {return icon;}
    @Override public int getWidth() {return W;}
    @Override public int getHeight() {return H;}

    @Override public void setRecipe(IRecipeLayoutBuilder builder,AltarRecipe recipe,IFocusGroup focuses) {
        if(KnowledgeGate.hidden(recipe.knowledge())){KnowledgeGate.addHidden(builder,recipe.getIngredients(),recipe.result(),124,CY-8);return;}
        // main material in the middle, as on the altar; the others in a ring around it (two rings past 10)
        builder.addSlot(RecipeIngredientRole.INPUT,CX-8,CY-8).setStandardSlotBackground().addIngredients(recipe.main());
        var others=recipe.others();
        int n=others.size(),outer=n>10?10:n;
        for(int i=0;i<n;i++){
            boolean ring=i<outer;
            int count=ring?outer:n-outer,k=ring?i:i-outer;
            // inner ring sits half an outer step around so its slots fall between the outer ones
            double radius=ring?34:18,angle=-Math.PI/2+2*Math.PI*k/count+(ring?0:Math.PI/outer);
            int x=(int)Math.round(CX+Math.cos(angle)*radius)-8,y=(int)Math.round(CY+Math.sin(angle)*radius)-8;
            builder.addSlot(RecipeIngredientRole.INPUT,x,y).setStandardSlotBackground().addIngredients(others.get(i));
        }
        var output=builder.addSlot(RecipeIngredientRole.OUTPUT,124,CY-8).setOutputSlotBackground();
        output.addItemStacks(recipe.shownResults());
    }
    @Override public void draw(AltarRecipe recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        if(KnowledgeGate.hidden(recipe.knowledge())){KnowledgeGate.drawLocked(graphics,W,H,recipe.result(),recipe.knowledge(),124);return;}
        arrow.draw(graphics,92,CY-8);
        var font=Minecraft.getInstance().font;
        Component need=Component.translatable("jei.primordial.altar.tier",AltarCrafting.tierName(recipe.tier()));
        graphics.drawString(font,need,(W-font.width(need))/2,H-font.lineHeight,0xFF606060,false);
        if(recipe.distinct()){
            Component note=Component.translatable("jei.primordial.altar.distinct");
            graphics.drawString(font,note,W-font.width(note)-2,CY+14,0xFF7A3AD0,false);
        }
        if(recipe.randomizes()){
            Component note=Component.translatable("jei.primordial.altar.random");
            graphics.drawString(font,note,W-font.width(note)-2,CY+14,0xFF7A3AD0,false);
        }
    }
}
