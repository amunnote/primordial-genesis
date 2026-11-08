package local.primordial.smoke;

import java.util.EnumSet;
import local.primordial.Primordial;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.AffixCombat;
import local.primordial.affix.AffixState;
import local.primordial.affix.MobAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.altar.AltarBlockEntity;
import local.primordial.altar.AltarCrafting;
import local.primordial.client.RecipeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class AffixAccessoryGameTests {
    /** puts the accessory in the next free alchemical accessory slot (Sage's Gift opens them) */
    static void wear(net.minecraft.world.entity.player.Player player,MobAffix affix) {
        player.getAttribute(Attributes.LUCK).setBaseValue(30);
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        if(!inv.isEquipped(Primordial.SAGE_HEART.get())){inv.setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));TestSlots.tick(player);}
        int index=AffixAccessories.worn(player).size();
        inv.setEquippedCurio(Primordial.ALCHEMICAL_ACCESSORY_SLOT,index,new ItemStack(AffixAccessories.ITEMS.get(affix).get()));
        TestSlots.tick(player);
    }

    @GameTest(template="empty") public static void everyAffixHasAnAccessoryRecipe(GameTestHelper helper) {
        var recipes=helper.getLevel().getRecipeManager();
        helper.assertTrue(AlchemyKnowledge.affixAccessoryRecipes().size()==MobAffix.values().length,"one accessory recipe per affix");
        for(var affix:MobAffix.values()){
            var id=AffixAccessories.recipe(affix);
            var recipe=recipes.byKey(id).orElse(null);
            helper.assertTrue(recipe instanceof local.primordial.altar.AltarRecipe altar&&altar.tier()==2&&altar.knowledge().equals(id),"transmutation altar recipe for "+affix.id());
        }
        // crafting one on a transmutation altar once learned
        var player=helper.makeMockPlayer();
        AlchemyKnowledge.learn(player,AffixAccessories.recipe(MobAffix.VOID));
        var rel=new BlockPos(1,1,1);helper.setBlock(rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());
        var altar=(AltarBlockEntity)helper.getBlockEntity(rel);
        var material=new ItemStack(local.primordial.affix.AffixMaterials.ITEMS.get(MobAffix.VOID).get());
        altar.add(material.copy());altar.add(material.copy());
        altar.add(new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()));altar.add(new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()));
        altar.add(new ItemStack(Items.PHANTOM_MEMBRANE));
        helper.assertTrue(AltarCrafting.tryCraft(helper.getLevel(),helper.absolutePos(rel),player)==AltarCrafting.Result.CRAFTED,"crafts the void accessory");
        helper.assertTrue(altar.items().get(0).is(AffixAccessories.ITEMS.get(MobAffix.VOID).get()),"void accessory made");
        helper.succeed();
    }

    @GameTest(template="empty") public static void accessoriesWardOffTheirAffix(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        var zombie=helper.spawn(EntityType.ZOMBIE,1,2,1);
        var state=AffixState.assign(zombie,30,EnumSet.of(MobAffix.CURSE,MobAffix.ARMOR_PIERCE),null);
        helper.assertTrue(AffixAccessories.acts(state,MobAffix.CURSE,player),"curse acts on an unwarded player");
        wear(player,MobAffix.CURSE);
        helper.assertTrue(AffixAccessories.wears(player,MobAffix.CURSE),"the accessory is worn");
        helper.assertTrue(!AffixAccessories.acts(state,MobAffix.CURSE,player),"the curse accessory wards off Curse");
        helper.assertTrue(AffixAccessories.acts(state,MobAffix.ARMOR_PIERCE,player),"but not Armor Pierce");
        // a lent affix is warded off by Amplify's accessory
        state.lend(MobAffix.SEAL,200);
        helper.assertTrue(AffixAccessories.acts(state,MobAffix.SEAL,player),"a lent seal acts");
        wear(player,MobAffix.AMPLIFY);
        helper.assertTrue(!AffixAccessories.acts(state,MobAffix.SEAL,player),"Amplify's accessory wards off lent affixes");
        // sealing a mob stops its affixes
        state.seal(100);
        helper.assertTrue(!state.has(MobAffix.CURSE)&&state.affixes.contains(MobAffix.CURSE),"a sealed mob keeps its affixes but they do nothing");
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void recipeMapShowsBothWays(GameTestHelper helper) {
        var level=helper.getLevel();
        var map=RecipeMap.of(new ItemStack(Items.IRON_INGOT),level.getRecipeManager(),level.registryAccess(),id->true);
        helper.assertTrue(map.madeFrom().stream().anyMatch(l->l.inputs().stream().anyMatch(i->i.choices().stream().anyMatch(s->s.is(Items.IRON_BLOCK)))),"iron ingots come from iron blocks");
        helper.assertTrue(map.usedIn().stream().anyMatch(l->l.outputs().stream().anyMatch(s->s.is(Items.IRON_PICKAXE))),"iron ingots make iron pickaxes");
        var block=map.madeFrom().stream().filter(l->l.inputs().stream().anyMatch(i->i.choices().stream().anyMatch(s->s.is(Items.IRON_BLOCK)))).findFirst().get();
        helper.assertTrue(block.station().is(Items.CRAFTING_TABLE),"made at a crafting table");
        var smelt=map.madeFrom().stream().filter(l->l.station().is(Items.FURNACE)).findFirst();
        helper.assertTrue(smelt.isPresent(),"and smelted in a furnace");
        // the netherite upgrade lists its three smithing inputs
        var netherite=RecipeMap.of(new ItemStack(Items.NETHERITE_INGOT),level.getRecipeManager(),level.registryAccess(),id->true);
        helper.assertTrue(netherite.usedIn().stream().anyMatch(l->l.station().is(Items.SMITHING_TABLE)&&l.inputs().size()==3),"smithing upgrades have three inputs");
        // alchemy: the void material is used in its accessory, locked until learned
        var material=new ItemStack(local.primordial.affix.AffixMaterials.ITEMS.get(MobAffix.VOID).get());
        var locked=RecipeMap.of(material,level.getRecipeManager(),level.registryAccess(),id->false);
        var accessory=locked.usedIn().stream().filter(l->l.id().equals(AffixAccessories.recipe(MobAffix.VOID))).findFirst();
        helper.assertTrue(accessory.isPresent()&&accessory.get().locked()&&accessory.get().station().is(Primordial.TRANSMUTATION_ALCHEMY_ALTAR_ITEM.get()),"accessory recipe shown locked at the transmutation altar");
        helper.assertTrue(accessory.get().inputs().get(0).count()==2,"two of the material");
        helper.assertTrue(locked.usedIn().stream().anyMatch(l->l.outputs().size()>10),"the random transmutation shows every material it can give");
        helper.succeed();
    }

    @GameTest(template="empty") public static void accessoryEffectsApply(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        wear(player,MobAffix.DAMAGE_CAP);
        player.setHealth(player.getMaxHealth());
        player.hurt(player.damageSources().generic(),15);
        helper.assertTrue(player.getHealth()>=player.getMaxHealth()*0.6F-0.01F,"a single hit takes at most 40%: health "+player.getHealth());
        // armor pierce from the accessory
        var zombie=helper.spawn(EntityType.ZOMBIE,1,2,1);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        float plain=AffixCombat.piercedArmor(zombie,player.damageSources().playerAttack(player),10)==null?-1:0;
        helper.assertTrue(plain<0,"no pierce without the accessory");
        wear(player,MobAffix.ARMOR_PIERCE);
        helper.assertTrue(AffixCombat.piercedArmor(zombie,player.damageSources().playerAttack(player),10)!=null,"the accessory pierces armor");
        zombie.discard();
        helper.succeed();
    }
}
