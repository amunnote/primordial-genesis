package local.primordial.smoke;

import java.util.HashMap;
import local.primordial.Primordial;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class RecipeLootGameTests {
    /** the laboratory chest draws on five recipe rules in the overworld: its own four and the overworld-wide one */
    @GameTest(template="empty",timeoutTicks=400) public static void atMostOneRecipePerChest(GameTestHelper helper) {
        var level=helper.getLevel();
        var table=level.getServer().getLootData().getLootTable(new ResourceLocation(Primordial.ID,"chests/alchemy_laboratory"));
        var counts=new HashMap<String,Integer>();int rolls=3000,withRecipe=0;
        for(int i=0;i<rolls;i++){
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,helper.absoluteVec(new net.minecraft.world.phys.Vec3(1,2,1))).create(LootContextParamSets.CHEST);
            int recipes=0;
            for(var stack:table.getRandomItems(params))if(stack.getItem() instanceof AlchemyRecipeItem){
                recipes+=stack.getCount();counts.merge(String.valueOf(AlchemyRecipeItem.recipe(stack)),1,Integer::sum);}
            helper.assertTrue(recipes<=1,"a chest held "+recipes+" recipes");
            if(recipes==1)withRecipe++;
        }
        // with four 50% rules, one 20% and the 30% transcription rule, about 1-0.5^4*0.8*0.7 = 96.5% of chests get a recipe
        helper.assertTrue(withRecipe>rolls*0.92,"share of chests with a recipe: "+withRecipe+"/"+rolls);
        // the four laboratory recipes have the same chance, so they stay about equally common
        String[] lab={"breaking_edge_charm","spellbreaker_charm","hundred_forged_charm","crystal_vestment_charm"};
        int min=Integer.MAX_VALUE,max=0;
        for(String id:lab){int n=counts.getOrDefault(Primordial.ID+":"+id,0);min=Math.min(min,n);max=Math.max(max,n);}
        helper.assertTrue(min>0&&max<min*1.35,"laboratory recipes roughly even: "+counts);
        helper.succeed();
    }
}
