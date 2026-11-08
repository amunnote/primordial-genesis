package local.primordial.client;

import java.util.HashMap;
import java.util.Map;
import local.primordial.Primordial;
import local.primordial.alchemy.RecipeTiers;
import local.primordial.item.AlchemyRecipeItem;
import local.primordial.item.MysteryRecipeItem;
import local.primordial.item.RecipeFragmentItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * The tier a recipe, a recipe fragment or a mystery recipe is drawn by: the item property primordial:tier (tier / 10),
 * which their item models turn into one picture a tier. A recipe's tier comes from the altar recipes the client knows,
 * looked up once a recipe and kept until the recipes change.
 */
public final class RecipeTierClient {
    private static final Map<ResourceLocation,Integer> TIERS=new HashMap<>();
    private RecipeTierClient() {}

    static int recipeTier(ResourceLocation recipe) {
        var level=Minecraft.getInstance().level;
        if(recipe==null||level==null)return 0;
        return TIERS.computeIfAbsent(recipe,id->RecipeTiers.tier(level,id));
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent public static void setup(FMLClientSetupEvent event) {event.enqueueWork(()->{
            var tier=new ResourceLocation(Primordial.ID,"tier");
            ItemProperties.register(Primordial.ALCHEMY_RECIPE.get(),tier,(stack,level,entity,seed)->recipeTier(AlchemyRecipeItem.recipe(stack))/10F);
            ItemProperties.register(Primordial.RECIPE_FRAGMENT.get(),tier,(stack,level,entity,seed)->RecipeFragmentItem.tier(stack)/10F);
            ItemProperties.register(Primordial.MYSTERY_RECIPE.get(),tier,(stack,level,entity,seed)->MysteryRecipeItem.tier(stack)/10F);
        });}
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
    public static final class Recipes {
        @SubscribeEvent public static void changed(RecipesUpdatedEvent event) {TIERS.clear();}
    }
}
