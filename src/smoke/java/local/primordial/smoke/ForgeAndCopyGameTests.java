package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.AffixDistribution;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.BossAffix;
import local.primordial.affix.Imbuing;
import local.primordial.affix.MobAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.altar.AltarBlockEntity;
import local.primordial.altar.AltarCrafting;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class ForgeAndCopyGameTests {
    private static AltarBlockEntity altar(GameTestHelper helper,BlockPos rel,Block block) {
        helper.setBlock(rel,block);return (AltarBlockEntity)helper.getBlockEntity(rel);
    }
    private static void put(AltarBlockEntity altar,ItemStack stack,int times) {for(int i=0;i<times;i++)altar.add(stack.copyWithCount(1));}

    @GameTest(template="empty") public static void recipesCanBeCopied(GameTestHelper helper) {
        var player=helper.makeMockPlayer();
        var rel=new BlockPos(1,1,1);var altar=altar(helper,rel,Primordial.BASIC_ALCHEMY_ALTAR.get());
        var recipe=AlchemyRecipeItem.create(AlchemyKnowledge.ORIGIN_ORB);
        put(altar,recipe,1);put(altar,new ItemStack(Items.PAPER),1);put(altar,new ItemStack(Items.INK_SAC),1);
        put(altar,new ItemStack(Items.FEATHER),1);put(altar,new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()),2);
        helper.assertTrue(AltarCrafting.tryCraft(helper.getLevel(),helper.absolutePos(rel),player)==AltarCrafting.Result.UNKNOWN,"copying needs its recipe");
        AlchemyKnowledge.learn(player,AlchemyKnowledge.ALCHEMY_RECIPE_COPY);
        helper.assertTrue(AltarCrafting.tryCraft(helper.getLevel(),helper.absolutePos(rel),player)==AltarCrafting.Result.CRAFTED,"copies once learned");
        helper.assertTrue(altar.items().size()==2&&altar.items().stream().allMatch(s->AlchemyKnowledge.ORIGIN_ORB.equals(AlchemyRecipeItem.recipe(s))),
            "two copies of the same recipe, got "+altar.items());
        helper.succeed();
    }

    @GameTest(template="empty") public static void affixMaterialsForgeIntoWeapons(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        var rel=new BlockPos(1,1,1);var altar=altar(helper,rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        var voidMaterial=new ItemStack(AffixMaterials.ITEMS.get(MobAffix.VOID).get());
        put(altar,new ItemStack(Items.IRON_SWORD),1);put(altar,voidMaterial,1);
        put(altar,new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()),2);put(altar,new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()),4);
        helper.assertTrue(AltarCrafting.tryCraft(helper.getLevel(),helper.absolutePos(rel),player)==AltarCrafting.Result.CRAFTED,"forging needs no recipe");
        var sword=altar.items().get(0);
        helper.assertTrue(sword.is(Items.IRON_SWORD)&&Imbuing.imbues(sword).equals(java.util.List.of("void"))&&Imbuing.pseudoTreasure(sword),"a void sword, got "+sword.getTag());
        helper.assertTrue(local.primordial.sage.SageHeart.alchemicalTreasure(sword),"counts as an alchemical treasure");
        helper.assertTrue(!Imbuing.canImbue(sword,"void")&&!Imbuing.canImbue(sword,"curse"),"one affix per weapon");
        player.setItemInHand(InteractionHand.MAIN_HAND,sword);
        helper.assertTrue(AffixAccessories.worn(player).contains(MobAffix.VOID)&&AffixAccessories.wears(player,MobAffix.VOID),"held, it works like the accessory");
        // the same affix again does not forge
        altar.clear();put(altar,sword,1);put(altar,voidMaterial,1);
        put(altar,new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()),2);put(altar,new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()),4);
        helper.assertTrue(AltarCrafting.tryCraft(helper.getLevel(),helper.absolutePos(rel),player)==AltarCrafting.Result.NO_RECIPE,"a forged weapon takes no second affix");
        // boss materials need a truth altar
        var trel=new BlockPos(3,1,1);var truth=altar(helper,trel,Primordial.TRUTH_ALCHEMY_ALTAR.get());
        put(truth,new ItemStack(Items.BOW),1);put(truth,new ItemStack(AffixMaterials.BOSS_ITEMS.get(BossAffix.WRATH).get()),1);
        put(truth,new ItemStack(Primordial.TRUTH_ESSENCE.get()),1);put(truth,new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()),4);
        put(truth,new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()),8);
        helper.assertTrue(AltarCrafting.tryCraft(helper.getLevel(),helper.absolutePos(trel),player)==AltarCrafting.Result.CRAFTED,"boss forging on a truth altar");
        helper.assertTrue(Imbuing.boss(truth.items().get(0)).contains(BossAffix.WRATH),"a bow of wrath");
        helper.succeed();
    }

    @GameTest(template="empty") public static void anyWeaponTakesAffixes(GameTestHelper helper) {
        for(var weapon:new net.minecraft.world.item.Item[]{Items.DIAMOND_SWORD,Items.IRON_AXE,Items.BOW,Items.CROSSBOW,Items.TRIDENT})
            helper.assertTrue(Imbuing.isWeapon(new ItemStack(weapon)),"vanilla weapon "+weapon);
        for(var other:new net.minecraft.world.item.Item[]{Items.IRON_PICKAXE,Items.DIAMOND_SHOVEL,Items.STICK,Items.DIAMOND_CHESTPLATE,Items.CARROT_ON_A_STICK})
            helper.assertTrue(!Imbuing.isWeapon(new ItemStack(other)),"not a weapon: "+other);
        // a weapon from another mod: no weapon class or tag, but it adds attack damage in the main hand
        var modded=new ItemStack(Items.CARROT_ON_A_STICK);
        modded.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
            new net.minecraft.world.entity.ai.attributes.AttributeModifier("modded blade",9,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION),
            net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        helper.assertTrue(Imbuing.isWeapon(modded),"an attack item from another mod counts");
        helper.assertTrue(local.primordial.affix.WeaponIngredient.INSTANCE.test(modded)&&!local.primordial.affix.WeaponIngredient.INSTANCE.isEmpty(),"and the recipe ingredient takes it");
        // it forges on the altar like any sword
        var player=helper.makeMockPlayer();
        var rel=new BlockPos(1,1,1);var altar=altar(helper,rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        put(altar,modded,1);put(altar,new ItemStack(AffixMaterials.ITEMS.get(MobAffix.CURSE).get()),1);
        put(altar,new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()),2);put(altar,new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()),4);
        helper.assertTrue(AltarCrafting.tryCraft(helper.getLevel(),helper.absolutePos(rel),player)==AltarCrafting.Result.CRAFTED,"a modded weapon forges");
        helper.assertTrue(Imbuing.imbues(altar.items().get(0)).equals(java.util.List.of("curse")),"with the curse affix");
        helper.assertTrue(java.util.Arrays.stream(local.primordial.affix.WeaponIngredient.INSTANCE.getItems()).anyMatch(s->s.is(Items.NETHERITE_SWORD)),"JEI lists the weapons");
        helper.succeed();
    }

    @GameTest(template="empty") public static void blockedHitsStayBlocked(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        var skeleton=helper.spawn(EntityType.SKELETON,2,2,2);
        local.primordial.affix.AffixState.assign(skeleton,40,java.util.EnumSet.of(MobAffix.STRENGTH,MobAffix.CURSE,MobAffix.SEAL),null);
        var arrow=EntityType.ARROW.create(helper.getLevel());
        var event=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,player.damageSources().arrow(arrow,skeleton),0);
        local.primordial.affix.AffixEvents.hurt(event);
        helper.assertTrue(event.getAmount()==0,"an arrow the shield stopped stays at 0, got "+event.getAmount());
        var hit=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,player.damageSources().arrow(arrow,skeleton),4);
        local.primordial.affix.AffixEvents.hurt(hit);
        helper.assertTrue(hit.getAmount()>4,"an arrow that lands still gets the affix bonus");
        skeleton.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void accessoriesStayWhileTheGiftIsWorn(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        AffixAccessoryGameTests.wear(player,MobAffix.VOID);
        var inv=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        var handler=inv.getStacksHandler(Primordial.ALCHEMICAL_ACCESSORY_SLOT).orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.getSlots()>=1&&!handler.getStacks().getStackInSlot(0).isEmpty(),"worn");
        // Curios recounting before the gift's slot modifiers are back
        for(var id:java.util.List.copyOf(handler.getModifiers().keySet()))handler.removeModifier(id);
        handler.update();
        helper.assertTrue(handler.getSlots()>=1&&AffixAccessories.wears(player,MobAffix.VOID),"the accessory does not fall out while the gift is worn");
        helper.succeed();
    }

    @GameTest(template="empty") public static void transmutationNeverRepeats(GameTestHelper helper) {
        var player=helper.makeMockPlayer();
        var rel=new BlockPos(1,1,1);var altar=altar(helper,rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        var input=AffixMaterials.ITEMS.get(MobAffix.SPACE).get();
        net.minecraft.world.item.Item last=null;
        var round=new java.util.HashSet<net.minecraft.world.item.Item>();
        int others=MobAffix.values().length-1;
        for(int i=0;i<others*2+3;i++){
            altar.clear();
            put(altar,new ItemStack(input),1);put(altar,new ItemStack(Primordial.TRANSMUTATION_SOURCE.get()),1);
            put(altar,new ItemStack(Items.COPPER_INGOT),1);put(altar,new ItemStack(Items.GOLD_INGOT),1);put(altar,new ItemStack(Items.AMETHYST_SHARD),1);
            helper.assertTrue(AltarCrafting.tryCraft(helper.getLevel(),helper.absolutePos(rel),player)==AltarCrafting.Result.CRAFTED,"transmutes");
            var out=altar.items().get(0).getItem();
            helper.assertTrue(out!=input&&out!=last,"never the input nor the last result, got "+out+" after "+last);
            // within a round every material comes out once; a new round starts when all have
            if(round.size()==others)round.clear();
            helper.assertTrue(round.add(out),"conversion "+(i+1)+" repeated "+out+" within the round");
            last=out;
        }
        helper.succeed();
    }

    @GameTest(template="empty") public static void thunderHitsAnyAttack(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        AffixAccessoryGameTests.wear(player,MobAffix.THUNDER);
        var golem=helper.spawn(EntityType.IRON_GOLEM,2,2,2);
        float before=golem.getHealth();
        golem.hurt(player.damageSources().thrown(player,player),1);
        float lost=before-golem.getHealth();
        float expected=1+Math.min(60,8+golem.getMaxHealth()*0.02F);
        helper.assertTrue(Math.abs(lost-expected)<0.6F,"a thrown hit calls 8 + 2% max health in lightning, lost "+lost+" expected "+expected);
        golem.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void bossesHaveAtLeastFiveAffixes(GameTestHelper helper) {
        helper.assertTrue(AffixDistribution.count(0,0.99,true)==5,"a low-level boss gets five");
        helper.assertTrue(AffixDistribution.count(0,0.99,false)>=1,"ordinary mobs at least one");
        helper.succeed();
    }

    @GameTest(template="empty") public static void knownRecipesAreAdvancements(GameTestHelper helper) {
        var advancements=helper.getLevel().getServer().getAdvancements();
        for(var id:AlchemyKnowledge.recipes()){
            var known=new ResourceLocation(id.getNamespace(),"known/"+id.getPath());
            helper.assertTrue(advancements.getAdvancement(known)!=null,"known advancement for "+id);
            helper.assertTrue(AlchemyKnowledge.isKnownRecipeAdvancement(known),"marked as a known-recipe advancement: "+known);
        }
        helper.assertTrue(advancements.getAdvancement(new ResourceLocation(Primordial.ID,"known/root"))!=null,"and their root");
        helper.succeed();
    }
}
