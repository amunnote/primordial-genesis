package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class AlchemyKnowledgeGameTests {
    @GameTest(template="empty") public static void recipeItemTeachesOnlySages(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        var id=AlchemyKnowledge.TRANSMUTATION_ALTAR;
        ItemStack recipe=AlchemyRecipeItem.create(id);recipe.setCount(3);
        player.setItemInHand(InteractionHand.MAIN_HAND,recipe);
        recipe.use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(!AlchemyKnowledge.knows(player,id),"Non-sage cannot learn");
        helper.assertTrue(recipe.getCount()==3,"Non-sage keeps the recipe");
        CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new)
            .setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));
        recipe.use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(AlchemyKnowledge.knows(player,id),"Sage learns the recipe");
        helper.assertTrue(recipe.getCount()==2,"Learning consumes one recipe, count="+recipe.getCount());
        helper.assertTrue(player.getInventory().countItem(Items.PAPER)==1,"The spent recipe turns into paper");
        var last=AlchemyRecipeItem.create(AlchemyKnowledge.TRUTH_ALTAR);
        player.setItemInHand(InteractionHand.MAIN_HAND,last);
        var result=last.use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getObject().is(Items.PAPER),"A single recipe becomes paper in hand");
        recipe.use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(recipe.getCount()==2,"Already-known recipe is not consumed");
        var respawned=helper.makeMockPlayer();
        var event=new net.minecraftforge.event.entity.player.PlayerEvent.Clone(respawned,player,true);
        AlchemyKnowledge.clone(event);
        helper.assertTrue(AlchemyKnowledge.knows(respawned,id),"Knowledge survives death");
        helper.succeed();
    }

    @GameTest(template="empty") public static void transmutationAltarNeedsKnowledge(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        var recipe=(CraftingRecipe)helper.getLevel().getRecipeManager()
            .byKey(new ResourceLocation(Primordial.ID,"transmutation_alchemy_altar")).orElseThrow();
        var menu=new CraftingMenu(1,player.getInventory(),ContainerLevelAccess.NULL);
        var grid=(CraftingContainer)menu.getSlot(1).container;
        helper.assertTrue(grid instanceof TransientCraftingContainer,"Crafting table grid is transient");
        ItemStack a=new ItemStack(Items.AMETHYST_SHARD),i=new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()),
            t=new ItemStack(Primordial.TRANSMUTATION_SOURCE.get()),b=new ItemStack(Primordial.BASIC_ALCHEMY_ALTAR_ITEM.get());
        ItemStack[] layout={a,i,a, ItemStack.EMPTY,t,ItemStack.EMPTY, t,b,t};
        for(int k=0;k<9;k++)grid.setItem(k,layout[k].copy());
        helper.assertTrue(!recipe.matches(grid,helper.getLevel()),"Unlearned player cannot craft");
        AlchemyKnowledge.learn(player,AlchemyKnowledge.TRANSMUTATION_ALTAR);
        helper.assertTrue(recipe.matches(grid,helper.getLevel()),"Learned player can craft");
        helper.assertTrue(recipe.assemble(grid,helper.getLevel().registryAccess()).is(Primordial.TRANSMUTATION_ALCHEMY_ALTAR_ITEM.get()),"Result is the altar");
        // a grid that isn't backed by a crafting table / inventory (e.g. an autocrafter) never matches
        var foreign=new TransientCraftingContainer(net.minecraft.world.inventory.ChestMenu.threeRows(2,player.getInventory()),3,3);
        for(int k=0;k<9;k++)foreign.setItem(k,layout[k].copy());
        helper.assertTrue(!recipe.matches(foreign,helper.getLevel()),"Grid without a known crafter cannot craft");
        // tier 3 needs its own recipe
        var truth=(CraftingRecipe)helper.getLevel().getRecipeManager()
            .byKey(new ResourceLocation(Primordial.ID,"truth_alchemy_altar")).orElseThrow();
        ItemStack p=new ItemStack(Primordial.PRIMORDIAL_GEM_POWDER.get()),e=new ItemStack(Primordial.TRUTH_ESSENCE.get()),
            c=new ItemStack(Items.CRYING_OBSIDIAN),ta=new ItemStack(Primordial.TRANSMUTATION_ALCHEMY_ALTAR_ITEM.get());
        ItemStack[] truthLayout={p,e,p, ItemStack.EMPTY,c,ItemStack.EMPTY, c,ta,c};
        for(int k=0;k<9;k++)grid.setItem(k,truthLayout[k].copy());
        helper.assertTrue(!truth.matches(grid,helper.getLevel()),"Truth altar needs its own recipe");
        AlchemyKnowledge.learn(player,AlchemyKnowledge.TRUTH_ALTAR);
        helper.assertTrue(truth.matches(grid,helper.getLevel()),"Truth altar craftable once learned");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=200) public static void chestRecipesStopOnceEveryoneKnows(GameTestHelper helper) {
        var level=helper.getLevel();
        var table=level.getServer().getLootData().getLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO))
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        // one member of this world who knows nothing yet
        var member=java.util.UUID.randomUUID();
        var world=local.primordial.alchemy.AlchemyWorldKnowledge.get(level.getServer());
        world.record(member,java.util.Set.of());world.markPresent(member);
        int transmutation=0,truth=0;
        for(int k=0;k<400;k++)for(ItemStack s:table.getRandomItems(params)){
            var id=AlchemyRecipeItem.recipe(s);
            if(AlchemyKnowledge.TRANSMUTATION_ALTAR.equals(id))transmutation++;
            if(AlchemyKnowledge.TRUTH_ALTAR.equals(id))truth++;
        }
        helper.assertTrue(transmutation>0,"Overworld chests can hold the transmutation recipe");
        helper.assertTrue(truth==0,"The truth recipe only appears in the Nether");
        world.record(member,java.util.Set.of(AlchemyKnowledge.TRANSMUTATION_ALTAR));
        int after=0;
        for(int k=0;k<400;k++)for(ItemStack s:table.getRandomItems(params))
            if(AlchemyKnowledge.TRANSMUTATION_ALTAR.equals(AlchemyRecipeItem.recipe(s)))after++;
        helper.assertTrue(after==0,"Once every player knows it the recipe stops appearing, got "+after);
        // a member who has not learned it but is not in the game does not bring it back; once they are, it returns
        var away=java.util.UUID.randomUUID();world.record(away,java.util.Set.of());
        helper.assertTrue(world.everyoneKnows(AlchemyKnowledge.TRANSMUTATION_ALTAR),"only the players in the game count");
        world.markPresent(away);
        helper.assertTrue(!world.everyoneKnows(AlchemyKnowledge.TRANSMUTATION_ALTAR),"someone in the game who has not learned it brings it back");
        world.markAbsent(away);
        helper.assertTrue(count(level,net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON,params,AlchemyKnowledge.PRIMORDIAL_ALTAR)==0,
            "Primordial altar recipe only in the End");
        // table-specific recipes: buried treasure -> origin orb, fortress -> origin ingot (no dimension restriction)
        helper.assertTrue(count(level,net.minecraft.world.level.storage.loot.BuiltInLootTables.BURIED_TREASURE,params,AlchemyKnowledge.ORIGIN_ORB)>0,
            "Buried treasure can hold the origin orb recipe");
        helper.assertTrue(count(level,net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON,params,AlchemyKnowledge.ORIGIN_ORB)==0,
            "Origin orb recipe only in buried treasure");
        helper.assertTrue(count(level,net.minecraft.world.level.storage.loot.BuiltInLootTables.NETHER_BRIDGE,params,AlchemyKnowledge.ORIGIN_INGOT)>0,
            "Fortress chests can hold the origin ingot recipe");
        world.markAbsent(member);
        helper.succeed();
    }
    private static int count(net.minecraft.server.level.ServerLevel level,ResourceLocation tableId,
                             net.minecraft.world.level.storage.loot.LootParams params,ResourceLocation recipe) {
        var table=level.getServer().getLootData().getLootTable(tableId);
        int n=0;
        for(int k=0;k<300;k++)for(ItemStack s:table.getRandomItems(params))if(recipe.equals(AlchemyRecipeItem.recipe(s)))n++;
        return n;
    }
}
