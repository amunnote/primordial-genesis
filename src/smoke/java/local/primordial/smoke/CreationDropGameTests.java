package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.altar.AltarRecipe;
import local.primordial.entity.alchemical.AlchemicalCreations;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Each alchemical creation's own material: in its loot table, and wanted by some altar recipe. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class CreationDropGameTests {
    @GameTest(template="empty") public static void creationMaterials(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        String[] creatures={"alchemical_zombie","alchemical_skeleton","alchemical_spider","alchemical_creeper","alchemical_enderman",
            "alchemical_blaze","alchemical_ghast","alchemical_wither_skeleton","alchemical_magma_cube"};
        var drops=AlchemicalCreations.drops();
        helper.assertTrue(drops.size()==creatures.length,"one material per creation");
        var recipes=server.getRecipeManager().getAllRecipesFor(Primordial.ALTAR_RECIPE_TYPE.get());
        for(int i=0;i<creatures.length;i++){
            var item=drops.get(i).get();var id=ForgeRegistries.ITEMS.getKey(item);
            var table=server.getLootData().getLootTable(new ResourceLocation(Primordial.ID,"entities/"+creatures[i]));
            // the loot table names the item somewhere in its pools
            var json=net.minecraft.world.level.storage.loot.LootDataType.TABLE.parser().toJsonTree(table).toString();
            helper.assertTrue(json.contains(id.toString()),creatures[i]+" drops "+id);
            var stack=new ItemStack(item);
            helper.assertTrue(recipes.stream().anyMatch(r->r.getIngredients().stream().anyMatch(ing->ing.test(stack))),id+" is used in an altar recipe");
        }
        helper.succeed();
    }
}
