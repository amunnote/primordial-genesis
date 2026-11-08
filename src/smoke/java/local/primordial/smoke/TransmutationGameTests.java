package local.primordial.smoke;

import java.util.EnumSet;
import java.util.List;
import local.primordial.Primordial;
import local.primordial.affix.AffixState;
import local.primordial.affix.MobAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.alchemy.Transmutation;
import local.primordial.altar.AltarBlockEntity;
import local.primordial.altar.AltarCrafting;
import local.primordial.altar.AltarCrafting.Result;
import local.primordial.item.TransmutationSourceItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class TransmutationGameTests {
    private static void sage(Player player) {
        CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new)
            .setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));
    }
    private static AltarBlockEntity altar(GameTestHelper helper,BlockPos rel,net.minecraft.world.level.block.Block block) {
        helper.setBlock(rel,block);
        return (AltarBlockEntity)helper.getBlockEntity(rel);
    }

    @GameTest(template="empty") public static void affixKillGivesEpiphanyOnlyToSages(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        var zombie=helper.spawn(EntityType.ZOMBIE,new BlockPos(1,2,1));
        AffixState.assign(zombie,1,EnumSet.of(MobAffix.STRENGTH),null);
        zombie.hurt(player.damageSources().playerAttack(player),10000);
        helper.assertTrue(!AlchemyKnowledge.knows(player,AlchemyKnowledge.TRANSMUTATION_SOURCE),"Not a sage: no epiphany");
        sage(player);
        var second=helper.spawn(EntityType.ZOMBIE,new BlockPos(2,2,1));
        AffixState.assign(second,1,EnumSet.of(MobAffix.STRENGTH),null);
        second.hurt(player.damageSources().playerAttack(player),10000);
        helper.assertTrue(AlchemyKnowledge.knows(player,AlchemyKnowledge.TRANSMUTATION_SOURCE),"A sage killing an affix mob learns the recipe");
        var plain=helper.makeMockPlayer();sage(plain);
        var cow=helper.spawn(EntityType.COW,new BlockPos(3,2,1));
        cow.hurt(plain.damageSources().playerAttack(plain),10000);
        helper.assertTrue(!AlchemyKnowledge.knows(plain,AlchemyKnowledge.TRANSMUTATION_SOURCE),"Mobs without affixes teach nothing");
        helper.succeed();
    }

    @GameTest(template="empty") public static void truthEpiphanyAfterTwelveRecipes(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        for(int i=0;i<AlchemyKnowledge.TRUTH_THRESHOLD;i++)AlchemyKnowledge.learn(player,new ResourceLocation(Primordial.ID,"test_"+i));
        helper.assertTrue(!AlchemyKnowledge.knows(player,AlchemyKnowledge.TRUTH_ESSENCE),"Not a sage: no epiphany yet");
        sage(player);
        AlchemyKnowledge.checkTruthEpiphany(player); // as when the Gift is put on
        helper.assertTrue(AlchemyKnowledge.knows(player,AlchemyKnowledge.TRUTH_ESSENCE),"Twelve recipes + sage: learns the Essence of Truth");
        var fresh=helper.makeMockPlayer();sage(fresh);
        for(int i=0;i<AlchemyKnowledge.TRUTH_THRESHOLD-1;i++)AlchemyKnowledge.learn(fresh,new ResourceLocation(Primordial.ID,"test_"+i));
        helper.assertTrue(!AlchemyKnowledge.knows(fresh,AlchemyKnowledge.TRUTH_ESSENCE),"Eleven is not enough");
        AlchemyKnowledge.learn(fresh,new ResourceLocation(Primordial.ID,"test_last"));
        helper.assertTrue(AlchemyKnowledge.knows(fresh,AlchemyKnowledge.TRUTH_ESSENCE),"The twelfth recipe triggers it");
        helper.succeed();
    }

    @GameTest(template="empty") public static void altarRecipesForSourceAndTruth(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        var level=helper.getLevel();
        AlchemyKnowledge.learn(player,AlchemyKnowledge.TRANSMUTATION_SOURCE);
        AlchemyKnowledge.learn(player,AlchemyKnowledge.TRUTH_ESSENCE);
        var basic=altar(helper,new BlockPos(1,1,1),Primordial.BASIC_ALCHEMY_ALTAR.get());
        for(var item:List.of(Items.COAL,Items.COPPER_INGOT,Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.REDSTONE,Items.LAPIS_LAZULI))
            basic.add(new ItemStack(item));
        basic.add(new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()));
        helper.assertTrue(basic.mainIndex()==7,"Essence placed last is still the main material");
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(new BlockPos(1,1,1)),player)==Result.CRAFTED,"Source of transmutation on a basic altar");
        helper.assertTrue(basic.items().size()==3&&basic.items().stream().allMatch(s->s.is(Primordial.TRANSMUTATION_SOURCE.get())&&s.getCount()==1),"Makes three, one per place");

        List<Enchantment> enchants=List.of(Enchantments.SHARPNESS,Enchantments.ALL_DAMAGE_PROTECTION,Enchantments.BLOCK_EFFICIENCY,
            Enchantments.UNBREAKING,Enchantments.MENDING,Enchantments.BLOCK_FORTUNE,Enchantments.MOB_LOOTING);
        var rel=new BlockPos(3,1,1);
        var trans=altar(helper,rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        trans.add(new ItemStack(Primordial.TRANSMUTATION_SOURCE.get()));
        trans.add(new ItemStack(local.primordial.entity.alchemical.AlchemicalCreations.RUNE_EMBER.get()));
        for(int i=0;i<7;i++)trans.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchants.get(i==6?0:i),1)));
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel),player)==Result.NO_RECIPE,"Two identical books do not count");
        trans.removeLast();
        trans.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchants.get(6),1)));
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel),player)==Result.CRAFTED,"Seven different books make the essence");
        helper.assertTrue(trans.items().get(0).is(Primordial.TRUTH_ESSENCE.get()),"Essence of truth made");
        helper.succeed();
    }

    @GameTest(template="empty") public static void sourceSmeltsInTheInventory(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        var slot=player.inventoryMenu.getSlot(9);
        slot.set(new ItemStack(Items.RAW_IRON,5));
        ItemStack sources=new ItemStack(Primordial.TRANSMUTATION_SOURCE.get(),3);
        int smelted=TransmutationSourceItem.smelt(sources,slot,player);
        helper.assertTrue(smelted==3&&sources.isEmpty(),"Three sources smelt three ores");
        helper.assertTrue(slot.getItem().is(Items.RAW_IRON)&&slot.getItem().getCount()==2,"Two raw iron left in the slot");
        int ingots=player.getInventory().countItem(Items.IRON_INGOT);
        helper.assertTrue(ingots>=3&&ingots<=6,"Three to six ingots (30% doubles), got "+ingots);
        ItemStack more=new ItemStack(Primordial.TRANSMUTATION_SOURCE.get(),10);
        TransmutationSourceItem.smelt(more,slot,player);
        helper.assertTrue(more.getCount()==8,"Only as many sources as items are used");
        helper.assertTrue(slot.getItem().is(Items.IRON_INGOT),"An emptied slot receives the products");
        helper.succeed();
    }

    @GameTest(template="empty") public static void sourceTransmutesMobs(GameTestHelper helper) {
        var cow=helper.spawn(EntityType.COW,new BlockPos(1,2,1));
        var made=Transmutation.transmute(helper.getLevel(),cow);
        helper.assertTrue(made!=null&&made.getType()!=EntityType.COW,"Cow turned into something else");
        helper.assertTrue(cow.isRemoved(),"The original is gone");
        helper.assertTrue(made.getType()!=EntityType.WITHER&&made.getType()!=EntityType.ENDER_DRAGON,"Never a boss");
        made.discard();
        helper.succeed();
    }
}
