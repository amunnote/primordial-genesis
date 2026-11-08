package local.primordial.smoke;

import java.util.List;
import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.alchemy.ItemLootModifier;
import local.primordial.item.MysteryRecipeItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

/** The overworld's chest finds (ItemLootModifier), and the Alchemist's Gift's mending. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class ChestFindsGameTests {
    @GameTest(template="empty") public static void plainCharms(GameTestHelper helper) {
        var traveler=helper.makeMockSurvivalPlayer();TestSlots.wearCharm(traveler,new ItemStack(Primordial.WORN_TRAVELER_CHARM.get()));
        // Sage's Gift, needed for the charm slot, brings +10 luck of its own
        helper.assertTrue(traveler.getAttributeValue(Attributes.LUCK)-10==8,"the traveler's charm: +8 luck, got "+(traveler.getAttributeValue(Attributes.LUCK)-10));
        helper.assertTrue(traveler.getAttributeValue(Attributes.ATTACK_DAMAGE)==1&&traveler.getArmorValue()==0,"and nothing else");
        var iron=helper.makeMockSurvivalPlayer();double before=iron.getMaxHealth();
        TestSlots.wearCharm(iron,new ItemStack(Primordial.IRON_VITALITY_CHARM.get()));
        helper.assertTrue(iron.getAttributeValue(Attributes.LUCK)-10==2,"the iron clasp: +2 luck");
        helper.assertTrue(iron.getMaxHealth()-before==4,"the iron clasp: +4 maximum health, got "+(iron.getMaxHealth()-before));
        helper.succeed();
    }

    @GameTest(template="empty") public static void earringWantsNoLuck(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();PurificationCrystalGameTests.sageGift(player);
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        inv.setEquippedCurio(Primordial.ALCHEMICAL_ACCESSORY_SLOT,0,new ItemStack(Primordial.GLOWSTONE_EARRING.get()));
        TestSlots.tick(player);TestSlots.tick(player);
        helper.assertTrue(player.getAttributeValue(Attributes.LUCK)<20,"well short of the 20 luck an affix accessory wants");
        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION),"yet the earring gives night vision");
        helper.succeed();
    }

    @GameTest(template="empty") public static void dollTakesTheBlow(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"doll_test"));
        player.getInventory().add(new ItemStack(Primordial.SUBSTITUTE_DOLL.get(),2));
        // a fresh player is spared harm at first, so the death itself is put to it, as a lethal blow brings it
        player.setHealth(0);
        boolean saved=net.minecraftforge.common.ForgeHooks.onLivingDeath(player,player.damageSources().generic());
        helper.assertTrue(saved&&Math.abs(player.getHealth()-player.getMaxHealth()*0.3F)<0.01,"saved at 30% health, got "+player.getHealth());
        helper.assertTrue(player.getInventory().countItem(Primordial.SUBSTITUTE_DOLL.get())==1,"one doll used up");
        // the void passes every protection: no doll turns it aside
        player.setHealth(0);
        helper.assertTrue(!net.minecraftforge.common.ForgeHooks.onLivingDeath(player,player.damageSources().fellOutOfWorld()),"the void kills all the same");
        helper.assertTrue(player.getInventory().countItem(Primordial.SUBSTITUTE_DOLL.get())==1,"the void is not turned aside");
        helper.succeed();
    }

    @GameTest(template="empty") public static void tornPageTeachesTierOne(GameTestHelper helper) {
        var level=helper.getLevel();var player=helper.makeMockSurvivalPlayer();
        // not a sage: nothing learned, nothing spent
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Primordial.TORN_RECIPE_PAGE.get(),3));
        player.getMainHandItem().use(level,player,InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount()==3,"only a sage can learn from it");
        PurificationCrystalGameTests.sageGift(player);
        int before=MysteryRecipeItem.choices(level,player,1).size();
        helper.assertTrue(before>0,"tier 1 recipes left to learn");
        player.getMainHandItem().use(level,player,InteractionHand.MAIN_HAND);
        helper.assertTrue(MysteryRecipeItem.choices(level,player,1).size()==before-1,"one tier 1 recipe learned");
        helper.assertTrue(player.getMainHandItem().getCount()==2,"one page spent");
        // all of them known: a page becomes a tier 1 fragment
        for(ResourceLocation id:MysteryRecipeItem.choices(level,player,1))AlchemyKnowledge.learn(player,id);
        player.getMainHandItem().use(level,player,InteractionHand.MAIN_HAND);
        boolean fragment=false;
        for(int i=0;i<player.getInventory().getContainerSize();i++){var s=player.getInventory().getItem(i);if(s.is(Primordial.RECIPE_FRAGMENT.get()))fragment=true;}
        helper.assertTrue(fragment,"a tier 1 recipe fragment instead");
        helper.succeed();
    }

    @GameTest(template="empty") public static void parcelOpens(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Primordial.APPRENTICE_PARCEL.get()));
        player.getMainHandItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        var inv=player.getInventory();
        helper.assertTrue(inv.countItem(Primordial.RECIPE_FRAGMENT.get())==2,"two recipe fragments");
        helper.assertTrue(inv.countItem(Primordial.ALCHEMICAL_ESSENCE.get())==3&&inv.countItem(Primordial.ALCHEMICAL_SOURCE_INGOT.get())==2
            &&inv.countItem(Primordial.ALCHEMICAL_SOURCE_STRING.get())==2,"and basic alchemical materials");
        helper.assertTrue(inv.countItem(Primordial.APPRENTICE_PARCEL.get())==0,"the parcel used up");
        helper.succeed();
    }

    @GameTest(template="empty") public static void giftMends(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();PurificationCrystalGameTests.sageGift(player);
        CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new)
            .setEquippedCurio(Primordial.ALCHEMICAL_ACCESSORY_SLOT,0,new ItemStack(Primordial.ALCHEMIST_GIFT.get()));
        player.setHealth(10);
        var stack=new ItemStack(Primordial.ALCHEMIST_GIFT.get());var gift=(local.primordial.item.AlchemistGiftItem)stack.getItem();
        var context=new top.theillusivec4.curios.api.SlotContext(Primordial.ALCHEMICAL_ACCESSORY_SLOT,player,0,false,true);
        for(int i=0;i<40;i++){player.tickCount++;gift.curioTick(context,stack);gift.curioTick(context,stack);}
        // two seconds pass (the game time does not move in a test, so each check is its own moment): two points, not four
        helper.assertTrue(player.getHealth()>10,"it mends, got "+player.getHealth());
        helper.succeed();
    }

    @GameTest(template="empty") public static void findsInOverworldChests(GameTestHelper helper) {
        var village=List.of("minecraft:chests/village/*","minecraft:chests/simple_dungeon");
        helper.assertTrue(ItemLootModifier.matches(village,new ResourceLocation("minecraft","chests/village/village_plains_house")),"a village house is a village chest");
        helper.assertTrue(ItemLootModifier.matches(village,new ResourceLocation("minecraft","chests/simple_dungeon")),"a named table");
        helper.assertTrue(!ItemLootModifier.matches(village,new ResourceLocation("minecraft","chests/nether_bridge")),"not the nether's");
        // the dungeon's chest, rolled many times over: every one of the finds turns up in it (6% to 15% a chest)
        var level=helper.getLevel();
        var table=level.getServer().getLootData().getLootTable(new ResourceLocation("minecraft","chests/simple_dungeon"));
        var found=new java.util.HashSet<net.minecraft.world.item.Item>();
        for(int i=0;i<300;i++){
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            for(var stack:table.getRandomItems(params))found.add(stack.getItem());
        }
        for(var item:List.of(Primordial.TORN_RECIPE_PAGE,Primordial.APPRENTICE_PARCEL,Primordial.WORN_TRAVELER_CHARM,Primordial.IRON_VITALITY_CHARM,
                Primordial.GLOWSTONE_EARRING,Primordial.SUBSTITUTE_DOLL))
            helper.assertTrue(found.contains(item.get()),"found in a dungeon chest: "+item.getId());
        helper.succeed();
    }
}
