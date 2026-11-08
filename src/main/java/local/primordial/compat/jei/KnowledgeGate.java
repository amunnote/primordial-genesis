package local.primordial.compat.jei;

import java.util.List;
import local.primordial.alchemy.AlchemyKnowledge;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Shared presentation for recipes whose alchemy knowledge has not been learned. */
final class KnowledgeGate {
    private KnowledgeGate() {}
    static boolean hidden(ResourceLocation knowledge) {return knowledge!=null&&!AlchemyKnowledge.clientKnows(knowledge);}

    static Component learnRoute(ResourceLocation knowledge) {
        String key="findable";
        if(AlchemyKnowledge.bossOnly(knowledge))key="boss";
        else if(AlchemyKnowledge.epiphanyOnly(knowledge)) {
            if(knowledge.equals(local.primordial.boss.SinBosses.PRIMORDIAL_SIGIL_RECIPE))
                return Component.translatable("jei.primordial.learn.sigil",(int)local.primordial.spirit.PrimordialTemple.WITNESS);
            return Component.translatable("jei.primordial.learn.core",local.primordial.effect.WorldCores.AFFIXES_REQUIRED);
        } else if(AlchemyKnowledge.TRANSMUTATION_SOURCE.equals(knowledge))key="transmutation";
        else if(AlchemyKnowledge.TRUTH_ESSENCE.equals(knowledge))
            return Component.translatable("jei.primordial.learn.truth",AlchemyKnowledge.TRUTH_THRESHOLD);
        else if(AlchemyKnowledge.ALCHEMY_RECIPE_COPY.equals(knowledge))key="copy";
        else if(!AlchemyKnowledge.findable(knowledge))return Component.empty();
        return Component.translatable("jei.primordial.learn."+key);
    }

    static void addHidden(IRecipeLayoutBuilder builder,List<Ingredient> inputs,ItemStack output,int outputX,int outputY) {
        var in=builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        for(Ingredient ingredient:inputs)if(!ingredient.isEmpty())in.addIngredients(ingredient);
        builder.addSlot(RecipeIngredientRole.OUTPUT,outputX,outputY).setOutputSlotBackground().addItemStack(output);
    }
    static void drawLocked(GuiGraphics graphics,int width,int height,ItemStack output,ResourceLocation knowledge,int outputX) {
        var font=Minecraft.getInstance().font;
        // Reserve the entire right-hand output column, including its background.
        int textWidth=Math.min(width-4,outputX-8),x=2,y=2;
        drawFitted(graphics,output.getHoverName(),x,y,textWidth,0xFFFFFFFF);
        y+=font.lineHeight+2;
        drawFitted(graphics,Component.translatable("jei.primordial.knowledge_locked"),x,y,textWidth,0xFF7A3AD0);
        y+=font.lineHeight+2;
        Component route=Component.translatable("jei.primordial.learn.label").append(" ").append(learnRoute(knowledge));
        var lines=font.split(route,textWidth);
        int count=Math.min(3,Math.min(lines.size(),Math.max(0,(height-y)/font.lineHeight)));
        for(int i=0;i<count;i++){
            var line=lines.get(i);
            if(i==count-1&&lines.size()>count){
                // Re-split the final line to reserve the ellipsis without splitting surrogate pairs.
                StringBuilder plain=new StringBuilder();
                line.accept((index,style,codepoint)->{plain.appendCodePoint(codepoint);return true;});
                String tail=font.plainSubstrByWidth(plain.toString(),Math.max(0,textWidth-font.width("…")))+"…";
                graphics.drawString(font,tail,x,y,0xFFB0B0B0,false);
            } else graphics.drawString(font,line,x,y,0xFFB0B0B0,false);
            y+=font.lineHeight;
        }
    }
    private static void drawFitted(GuiGraphics graphics,Component text,int x,int y,int width,int color) {
        var font=Minecraft.getInstance().font;
        float scale=Math.min(1F,(float)width/Math.max(1,font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(x,y,0);
        graphics.pose().scale(scale,scale,1F);
        graphics.drawString(font,text,0,0,color,false);
        graphics.pose().popPose();
    }
}
