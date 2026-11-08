package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.altar.AltarBlockEntity;
import local.primordial.altar.AltarCrafting;
import local.primordial.altar.AltarCrafting.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class AltarCraftingGameTests {
    private static AltarBlockEntity altar(GameTestHelper helper,BlockPos rel,Block block) {
        helper.setBlock(rel,block);
        return (AltarBlockEntity)helper.getBlockEntity(rel);
    }
    private static void put(AltarBlockEntity altar,ItemStack stack,int times) {for(int i=0;i<times;i++)altar.add(stack.copyWithCount(1));}

    @GameTest(template="empty") public static void altarAlchemyNeedsKnowledgeAndTier(GameTestHelper helper) {
        var player=helper.makeMockPlayer();
        var level=helper.getLevel();
        // origin orb: heart of the sea + 8 source ingots, transmutation altar or better
        var rel=new BlockPos(1,1,1);
        var transmutation=altar(helper,rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        helper.assertTrue(transmutation.capacity()==12,"Transmutation altar holds 12");
        put(transmutation,new ItemStack(Items.HEART_OF_THE_SEA),1);
        put(transmutation,new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()),8);
        put(transmutation,new ItemStack(local.primordial.entity.alchemical.AlchemicalCreations.MOLTEN_GEL.get()),2);
        var abs=helper.absolutePos(rel);
        helper.assertTrue(AltarCrafting.tryCraft(level,abs,player)==Result.UNKNOWN,"Needs the origin orb knowledge");
        AlchemyKnowledge.learn(player,AlchemyKnowledge.ORIGIN_ORB);
        helper.assertTrue(AltarCrafting.tryCraft(level,abs,player)==Result.CRAFTED,"Crafts once learned");
        helper.assertTrue(transmutation.items().size()==1&&transmutation.items().get(0).is(Primordial.ORIGIN_ORB.get()),"Orb sits on the altar");

        // the same recipe on a basic altar (capacity 8 would not even fit it, so use the ingot recipe for the tier check)
        var rel2=new BlockPos(3,1,1);
        var basic=altar(helper,rel2,Primordial.BASIC_ALCHEMY_ALTAR.get());
        helper.assertTrue(basic.capacity()==8,"Basic altar holds 8");
        put(basic,new ItemStack(Primordial.PRIMORDIAL_GEM_POWDER.get()),1);
        put(basic,new ItemStack(Primordial.ORIGIN_ORB.get()),1);
        put(basic,new ItemStack(Items.NETHERITE_INGOT),1);
        AlchemyKnowledge.learn(player,AlchemyKnowledge.ORIGIN_INGOT);
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel2),player)==Result.TIER_TOO_LOW,"Origin ingot needs a truth altar");

        var rel3=new BlockPos(5,1,1);
        var truth=altar(helper,rel3,Primordial.TRUTH_ALCHEMY_ALTAR.get());
        put(truth,new ItemStack(Items.NETHERITE_INGOT),1);
        put(truth,new ItemStack(Primordial.ORIGIN_ORB.get()),1);
        put(truth,new ItemStack(Primordial.PRIMORDIAL_GEM_POWDER.get()),1);
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel3),player)==Result.CRAFTED,"Order on the altar does not matter");
        helper.assertTrue(truth.items().get(0).is(Primordial.ORIGIN_INGOT.get()),"Origin ingot made");

        // extra or missing items never match
        put(truth,new ItemStack(Items.DIRT),1);
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel3),player)==Result.NO_RECIPE,"Unrelated items form no recipe");
        helper.succeed();
    }

    @GameTest(template="empty") public static void affixMaterialsTransmuteAtRandom(GameTestHelper helper) {
        var player=helper.makeMockPlayer();
        var level=helper.getLevel();
        var from=local.primordial.affix.AffixMaterials.ITEMS.get(local.primordial.affix.MobAffix.VOID).get();
        var seen=new java.util.HashSet<net.minecraft.world.item.Item>();
        var rel=new BlockPos(1,1,1);
        var altar=altar(helper,rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        for(int i=0;i<60;i++){
            altar.clear();
            put(altar,new ItemStack(from),1);
            put(altar,new ItemStack(Primordial.TRANSMUTATION_SOURCE.get()),1);
            put(altar,new ItemStack(Items.COPPER_INGOT),1);
            put(altar,new ItemStack(Items.GOLD_INGOT),1);
            put(altar,new ItemStack(Items.AMETHYST_SHARD),1);
            helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel),player)==Result.CRAFTED,"Transmutes without any knowledge");
            helper.assertTrue(altar.items().size()==1,"One material comes out");
            var out=altar.items().get(0);
            helper.assertTrue(out.is(net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation(Primordial.ID,"affix_materials"))),"An ordinary affix material: "+out);
            helper.assertTrue(!out.is(from),"Never the same material back");
            seen.add(out.getItem());
        }
        helper.assertTrue(seen.size()>10,"Spread over many materials, saw "+seen.size());
        var basicRel=new BlockPos(3,1,1);
        var basic=altar(helper,basicRel,Primordial.BASIC_ALCHEMY_ALTAR.get());
        put(basic,new ItemStack(from),1);
        put(basic,new ItemStack(Primordial.TRANSMUTATION_SOURCE.get()),1);
        put(basic,new ItemStack(Items.COPPER_INGOT),1);
        put(basic,new ItemStack(Items.GOLD_INGOT),1);
        put(basic,new ItemStack(Items.AMETHYST_SHARD),1);
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(basicRel),player)==Result.TIER_TOO_LOW,"Needs a transmutation altar");
        helper.succeed();
    }

    @GameTest(template="empty") public static void mainMaterialIsFoundWhileFilling(GameTestHelper helper) {
        var altar=altar(helper,new BlockPos(1,1,1),Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        put(altar,new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()),2);
        helper.assertTrue(altar.mainIndex()==-1,"No main material on the table yet");
        put(altar,new ItemStack(Items.HEART_OF_THE_SEA),1);
        helper.assertTrue(altar.mainIndex()==2,"Heart of the sea, placed third, is the main material: "+altar.mainIndex());
        altar.removeLast();
        helper.assertTrue(altar.mainIndex()==-1,"Main material gone after taking it off");
        var other=altar(helper,new BlockPos(3,1,1),Primordial.TRUTH_ALCHEMY_ALTAR.get());
        put(other,new ItemStack(Items.DIRT),1);
        put(other,new ItemStack(Items.HEART_OF_THE_SEA),1);
        helper.assertTrue(other.mainIndex()==-1,"Unrelated items mean no recipe, so no main material");
        helper.succeed();
    }

    @GameTest(template="empty") public static void creationBookTeachesEverything(GameTestHelper helper) {
        var player=helper.makeMockPlayer();
        var book=new ItemStack(Primordial.CREATION_BOOK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,book);
        book.use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        for(var id:AlchemyKnowledge.recipes())helper.assertTrue(AlchemyKnowledge.knows(player,id),"Book teaches "+id);
        helper.assertTrue(book.isEmpty(),"Book is used up in survival");
        helper.succeed();
    }
}
