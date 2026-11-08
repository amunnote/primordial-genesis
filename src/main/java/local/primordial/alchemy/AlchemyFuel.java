package local.primordial.alchemy;

import local.primordial.Primordial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Primordial.ID)
public final class AlchemyFuel {
    private AlchemyFuel() {}

    @SubscribeEvent
    public static void fuel(FurnaceFuelBurnTimeEvent event) {
        if (event.getItemStack().is(Items.AMETHYST_SHARD)
                && (event.getRecipeType() == null || event.getRecipeType() == RecipeType.SMELTING
                        || event.getRecipeType() == RecipeType.BLASTING)) {
            // A blast furnace halves this value, matching its 100-tick recipe.
            event.setBurnTime(200);
        }
    }
}
