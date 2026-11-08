package local.primordial.smoke;

import java.util.EnumSet;
import local.primordial.Primordial;
import local.primordial.affix.AffixDistribution;
import local.primordial.affix.AffixMobActions;
import local.primordial.affix.AffixState;
import local.primordial.affix.AffixSynergy;
import local.primordial.affix.BossAffix;
import local.primordial.affix.MobAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.alchemy.AlchemyWorldKnowledge;
import local.primordial.alchemy.RecipeTiers;
import local.primordial.altar.AltarBlockEntity;
import local.primordial.altar.AltarCrafting;
import local.primordial.item.AlchemyRecipeItem;
import local.primordial.item.MysteryRecipeItem;
import local.primordial.alchemy.MysteryEnchanting;
import local.primordial.item.RecipeFragmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Recipe fragments and mystery recipes, the realm's recipes, Ruin's blast, and affixes rolled to work together. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class RecipeFragmentGameTests {
    private static AltarBlockEntity altar(GameTestHelper helper,BlockPos rel,Block block) {
        helper.setBlock(rel,block);return (AltarBlockEntity)helper.getBlockEntity(rel);
    }
    private static ResourceLocation ofTier(GameTestHelper helper,int tier) {
        for(var id:AlchemyKnowledge.recipes())if(AlchemyKnowledge.findable(id)&&RecipeTiers.tier(helper.getLevel(),id)==tier)return id;
        throw new IllegalStateException("no recipe of tier "+tier);
    }

    @GameTest(template="empty") public static void learnedRecipesBreakIntoFragments(GameTestHelper helper) {
        var level=helper.getLevel();var player=helper.makeMockPlayer();
        var basic=ofTier(helper,1);var truth=ofTier(helper,3);
        var rel=new BlockPos(1,1,1);var altar=altar(helper,rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        altar.add(AlchemyRecipeItem.create(basic));altar.add(AlchemyRecipeItem.create(basic));
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel),player)==AltarCrafting.Result.UNKNOWN,"only learned recipes break down");
        AlchemyKnowledge.learn(player,basic);
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel),player)==AltarCrafting.Result.CRAFTED,"a learned basic recipe breaks down");
        helper.assertTrue(altar.items().size()==2&&altar.items().stream().allMatch(s->s.is(Primordial.RECIPE_FRAGMENT.get())&&RecipeFragmentItem.tier(s)==1),
            "two basic fragments, got "+altar.items());
        altar.clear();
        AlchemyKnowledge.learn(player,truth);altar.add(AlchemyRecipeItem.create(truth));
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel),player)==AltarCrafting.Result.NO_RECIPE,"a truth recipe does not break down");
        helper.assertTrue(altar.items().size()==1&&altar.items().get(0).is(Primordial.ALCHEMY_RECIPE.get()),"and stays a recipe");
        helper.succeed();
    }

    @GameTest(template="empty") public static void fragmentsMakeAMysteryRecipe(GameTestHelper helper) {
        var level=helper.getLevel();var player=helper.makeMockPlayer();
        var rel=new BlockPos(1,1,1);var altar=altar(helper,rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        altar.add(new ItemStack(Primordial.TRANSMUTATION_SOURCE.get()));
        for(int i=0;i<6;i++)altar.add(RecipeFragmentItem.create(2));
        altar.add(RecipeFragmentItem.create(1));
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel),player)==AltarCrafting.Result.NO_RECIPE,"fragments of mixed tiers make nothing");
        altar.clear();
        altar.add(new ItemStack(Primordial.TRANSMUTATION_SOURCE.get()));
        for(int i=0;i<7;i++)altar.add(RecipeFragmentItem.create(2));
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel),player)==AltarCrafting.Result.CRAFTED,"seven of one tier and a source");
        var product=altar.items().get(0);
        helper.assertTrue(altar.items().size()==1&&product.is(Primordial.MYSTERY_RECIPE.get())&&MysteryRecipeItem.tier(product)==2,"a transmutation mystery recipe, got "+product+" "+product.getTag());
        helper.succeed();
    }

    @GameTest(template="empty") public static void mysteryRecipeAtTheEnchantingTable(GameTestHelper helper) {
        var level=helper.getLevel();var player=helper.makeMockSurvivalPlayer();
        var rel=new BlockPos(1,1,1);helper.setBlock(rel,Blocks.ENCHANTING_TABLE);var pos=helper.absolutePos(rel);
        var menu=new net.minecraft.world.inventory.EnchantmentMenu(0,player.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(level,pos));
        // the mystery alone offers nothing; an amethyst block where the lapis goes deals three unlearned basic recipes
        menu.getSlot(0).set(MysteryRecipeItem.create(1));menu.slotsChanged(menu.getSlot(0).container);
        helper.assertTrue(MysteryEnchanting.offered(menu.enchantClue,menu.levelClue,0)==null,"without amethyst nothing is offered");
        helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(Items.AMETHYST_BLOCK)),"an amethyst block goes where the lapis goes");
        menu.getSlot(1).set(new ItemStack(Items.AMETHYST_BLOCK,2));menu.slotsChanged(menu.getSlot(0).container);
        var offered=new java.util.ArrayList<ResourceLocation>();
        for(int i=0;i<3;i++){var id=MysteryEnchanting.offered(menu.enchantClue,menu.levelClue,i);if(id!=null)offered.add(id);}
        helper.assertTrue(offered.size()==3&&new java.util.HashSet<>(offered).size()==3,"three different recipes offered, got "+offered);
        for(var id:offered)helper.assertTrue(RecipeTiers.tier(level,id)==1&&!AlchemyKnowledge.knows(player,id)&&AlchemyKnowledge.findable(id),"each a basic recipe not yet learned: "+id);
        // the same hand again: reopening deals nothing new
        menu.slotsChanged(menu.getSlot(0).container);
        helper.assertTrue(offered.get(1).equals(MysteryEnchanting.offered(menu.enchantClue,menu.levelClue,1)),"the same offer until one is chosen");
        helper.assertTrue(menu.clickMenuButton(player,1),"choosing the second");
        var got=menu.getSlot(0).getItem();
        helper.assertTrue(got.is(Primordial.ALCHEMY_RECIPE.get())&&offered.get(1).equals(AlchemyRecipeItem.recipe(got)),"the mystery became the chosen recipe, got "+got);
        helper.assertTrue(menu.getSlot(1).getItem().getCount()==1,"one amethyst block spent");
        // an amethyst block enchants nothing else
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));menu.slotsChanged(menu.getSlot(0).container);
        helper.assertTrue(menu.costs[0]==0&&menu.costs[1]==0&&menu.costs[2]==0&&!menu.clickMenuButton(player,0),"amethyst is no lapis");
        // with every basic recipe learned the table says so
        for(var id:MysteryRecipeItem.choices(level,player,1))AlchemyKnowledge.learn(player,id);
        menu.getSlot(0).set(MysteryRecipeItem.create(1));menu.slotsChanged(menu.getSlot(0).container);
        helper.assertTrue(menu.levelClue[0]==MysteryEnchanting.ALL_LEARNED&&!menu.clickMenuButton(player,0),"nothing left to learn: all learned, and it stays");
        helper.assertTrue(menu.getSlot(0).getItem().is(Primordial.MYSTERY_RECIPE.get()),"the mystery kept");
        helper.succeed();
    }

    @GameTest(template="empty") public static void realmRecipesIgnoreKnowledge(GameTestHelper helper) {
        var server=helper.getLevel().getServer();var world=AlchemyWorldKnowledge.get(server);
        var everyone=java.util.UUID.randomUUID();
        world.record(everyone,AlchemyKnowledge.recipes());world.markPresent(everyone);
        helper.assertTrue(world.everyoneKnows(AlchemyKnowledge.TRANSMUTATION_ALTAR),"everyone in the game knows them all");
        boolean found=false;
        for(int i=0;i<20&&!found;i++)found=local.primordial.spirit.SpiritRealm.realmRecipe(server,helper.getLevel().random)!=null;
        helper.assertTrue(found,"the realm still gives recipes");
        world.markAbsent(everyone);
        helper.assertTrue(local.primordial.spirit.SpiritLootModifier.MAX_PER_CHEST==3,"up to three to a chest");
        helper.succeed();
    }

    @GameTest(template="empty") public static void onlyABossBlastBreaksBlocks(GameTestHelper helper) {
        var level=helper.getLevel();
        boolean griefing=level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
        var ground=new BlockPos(2,1,2);
        helper.setBlock(ground,Blocks.DIRT);
        var mob=helper.spawn(EntityType.ZOMBIE,2,2,2);
        AffixMobActions.ruin(AffixState.assign(mob,10,EnumSet.of(MobAffix.RUIN),null));
        helper.assertTrue(level.getBlockState(helper.absolutePos(ground)).is(Blocks.DIRT),"an ordinary mob's blast leaves the ground");
        mob.discard();
        var boss=helper.spawn(EntityType.ZOMBIE,2,2,2);
        AffixMobActions.ruin(AffixState.assign(boss,10,EnumSet.of(MobAffix.RUIN),BossAffix.WRATH));
        helper.assertTrue(!level.getBlockState(helper.absolutePos(ground)).is(Blocks.DIRT),"a boss's blast breaks it");
        boss.discard();
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(griefing,level.getServer());
        helper.succeed();
    }

    @GameTest(template="empty") public static void affixesRollTogether(GameTestHelper helper) {
        // how often a rolled pair works together, against how often it would by chance
        int pairs=0,working=0;
        for(long seed=0;seed<3000;seed++){
            var rolled=new java.util.ArrayList<>(AffixDistribution.choose(3,seed,a->true));
            for(int i=0;i<rolled.size();i++)for(int j=i+1;j<rolled.size();j++){pairs++;if(AffixSynergy.works(rolled.get(i),rolled.get(j)))working++;}
            helper.assertTrue(!(rolled.contains(MobAffix.MAGIC)&&rolled.contains(MobAffix.ARMOR_PIERCE)),"magic and armor pierce still never together");
        }
        var all=MobAffix.values();int total=0,partners=0;
        for(int i=0;i<all.length;i++)for(int j=i+1;j<all.length;j++){total++;if(AffixSynergy.works(all[i],all[j]))partners++;}
        double rolledShare=working/(double)pairs,chance=partners/(double)total;
        helper.assertTrue(rolledShare>chance*2,"rolled pairs work together far more often than by chance: "+rolledShare+" vs "+chance);
        helper.succeed();
    }
}
