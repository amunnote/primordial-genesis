package local.primordial.alchemy;

import local.primordial.Primordial;
import local.primordial.altar.AltarRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * The tier of an alchemy recipe: the altar its craft needs (1 basic, 2 transmutation, 3 truth, 4 primordial). A recipe
 * whose knowledge unlocks a crafting-table recipe counts as basic; one that unlocks nothing that can be found is 0.
 */
public final class RecipeTiers {
    /** recipes up to this tier may be broken into fragments */
    public static final int FRAGMENT_MAX=2;
    private RecipeTiers() {}
    public static int tier(Level level,ResourceLocation recipe) {
        if(recipe==null)return 0;
        int tier=0;
        for(AltarRecipe altar:level.getRecipeManager().getAllRecipesFor(Primordial.ALTAR_RECIPE_TYPE.get()))
            if(recipe.equals(altar.knowledge()))tier=tier==0?altar.tier():Math.min(tier,altar.tier());
        if(tier>0)return tier;
        for(var crafting:level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING))
            if(crafting instanceof KnowledgeShapedRecipe shaped&&recipe.equals(shaped.knowledge()))return 1;
        return 0;
    }
    public static net.minecraft.network.chat.Component name(int tier) {
        return net.minecraft.network.chat.Component.translatable("recipe_tier.primordial."+Math.max(1,Math.min(4,tier)));
    }
}
