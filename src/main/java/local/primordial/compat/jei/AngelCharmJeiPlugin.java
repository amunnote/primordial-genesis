package local.primordial.compat.jei;

import local.primordial.Primordial;
import local.primordial.alchemy.KnowledgeShapedRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import java.util.List;

@JeiPlugin
public final class AngelCharmJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID=new ResourceLocation(Primordial.ID,"jei");

    @Override public ResourceLocation getPluginUid() {return UID;}

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui=registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new SourceSmeltingCategory(gui),new AltarCategory(gui),new KnowledgeCraftingCategory(gui),new SpiritCompassCategory(gui));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(SourceSmeltingCategory.TYPE,SourceSmeltingCategory.recipes());
        registration.addRecipes(SpiritCompassCategory.TYPE,List.of(new SpiritCompassCategory.Exchange()));
        registration.addItemStackInfo(new ItemStack(Primordial.SPIRIT_COMPASS.get()),Component.translatable("jei.primordial.info.spirit_compass"));
        registration.addItemStackInfo(new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()),
            Component.translatable("jei.primordial.info.essence"));
        var recipes=recipeManager();
        if(recipes!=null){
            var altarRecipes=recipes.getAllRecipesFor(Primordial.ALTAR_RECIPE_TYPE.get());
            var craftingRecipes=knowledgeRecipes(recipes);
            registration.addRecipes(AltarCategory.TYPE,altarRecipes);
            registration.addRecipes(KnowledgeCraftingCategory.TYPE,craftingRecipes);
            var informed=new java.util.HashSet<net.minecraft.world.item.Item>();
            for(var recipe:altarRecipes)if(recipe.knowledge()!=null)
                for(var output:recipe.shownResults())addLearnInfo(registration,informed,output,recipe.knowledge());
            for(var recipe:craftingRecipes)if(recipe.knowledge()!=null)
                addLearnInfo(registration,informed,recipe.getResultItem(null),recipe.knowledge());
        }
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.FURNACE),SourceSmeltingCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.BLAST_FURNACE),SourceSmeltingCategory.TYPE);
        for(var altar:List.of(Primordial.BASIC_ALCHEMY_ALTAR_ITEM,Primordial.TRANSMUTATION_ALCHEMY_ALTAR_ITEM,
                Primordial.TRUTH_ALCHEMY_ALTAR_ITEM,Primordial.PRIMORDIAL_ALCHEMY_ALTAR_ITEM))
            registration.addRecipeCatalyst(new ItemStack(altar.get()),AltarCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE),KnowledgeCraftingCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.ENCHANTING_TABLE),SpiritCompassCategory.TYPE);
    }

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        var recipes=recipeManager();
        if(recipes==null)return;
        // 原版熔炉/高炉分类会显示任意燃料，改由 SourceSmeltingCategory 展示
        recipes.byKey(new ResourceLocation(Primordial.ID,"alchemical_source_ingot_from_smelting"))
            .filter(SmeltingRecipe.class::isInstance).map(SmeltingRecipe.class::cast)
            .ifPresent(r->runtime.getRecipeManager().hideRecipes(RecipeTypes.SMELTING,List.of(r)));
        recipes.byKey(new ResourceLocation(Primordial.ID,"alchemical_source_ingot_from_blasting"))
            .filter(BlastingRecipe.class::isInstance).map(BlastingRecipe.class::cast)
            .ifPresent(r->runtime.getRecipeManager().hideRecipes(RecipeTypes.BLASTING,List.of(r)));
        // knowledge-gated crafting recipes live in KnowledgeCraftingCategory, which hides them until learned
        List<CraftingRecipe> gated=List.copyOf(knowledgeRecipes(recipes));
        if(!gated.isEmpty())runtime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING,gated);
    }

    private static void addLearnInfo(IRecipeRegistration registration,java.util.Set<net.minecraft.world.item.Item> informed,
                                     ItemStack output,ResourceLocation knowledge) {
        if(!output.isEmpty()&&informed.add(output.getItem()))registration.addItemStackInfo(output,KnowledgeGate.learnRoute(knowledge));
    }

    private static RecipeManager recipeManager() {
        var level=Minecraft.getInstance().level;
        return level==null?null:level.getRecipeManager();
    }
    private static List<KnowledgeShapedRecipe> knowledgeRecipes(RecipeManager recipes) {
        return recipes.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
            .filter(KnowledgeShapedRecipe.class::isInstance).map(KnowledgeShapedRecipe.class::cast).toList();
    }
}
