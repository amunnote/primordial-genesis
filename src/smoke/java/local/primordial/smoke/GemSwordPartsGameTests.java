package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.boss.SinBosses;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The Gem Sword's parts: the irregular crystal's recipe and the Dragon that carries it, the blade cut from it, the Bird's hilt. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class GemSwordPartsGameTests {
    @GameTest(template="empty") public static void crystalBladeAndSword(GameTestHelper helper) {
        var level=helper.getLevel();
        var crystal=level.getRecipeManager().byKey(SinBosses.IRREGULAR_CRYSTAL_RECIPE).orElseThrow();
        helper.assertTrue(crystal.getResultItem(level.registryAccess()).is(Primordial.IRREGULAR_CRYSTAL.get()),"it makes the irregular crystal");
        for(var item:java.util.List.of(new ItemStack(local.primordial.effect.WorldCores.WORLD_CORE.get()),new ItemStack(SinBosses.SEVEN_SINS.get())))
            helper.assertTrue(crystal.getIngredients().stream().anyMatch(i->i.test(item)),"the irregular crystal wants "+item);
        var bossOnly=AlchemyKnowledge.bossOnlyRecipes();
        helper.assertTrue(AlchemyKnowledge.bossOnly(SinBosses.IRREGULAR_CRYSTAL_RECIPE)&&bossOnly.indexOf(SinBosses.IRREGULAR_CRYSTAL_RECIPE)>bossOnly.indexOf(SinBosses.ASCENSION_RECIPE),
            "carried by a boss, registered after the Sin Lords' share");
        var cut=level.getRecipeManager().getRecipeFor(RecipeType.STONECUTTING,new SimpleContainer(new ItemStack(Primordial.IRREGULAR_CRYSTAL.get())),level);
        helper.assertTrue(cut.isPresent()&&cut.get().getResultItem(level.registryAccess()).is(Primordial.GEM_SWORD_BLADE.get()),"a stonecutter cuts the crystal into the blade");
        var sword=level.getRecipeManager().byKey(SinBosses.GEM_SWORD_RECIPE).orElseThrow();
        var blade=new ItemStack(Primordial.GEM_SWORD_BLADE.get());var raw=new ItemStack(Primordial.IRREGULAR_CRYSTAL.get());
        helper.assertTrue(sword.getIngredients().stream().anyMatch(i->i.test(blade))&&sword.getIngredients().stream().noneMatch(i->i.test(raw)),"the Gem Sword wants the blade, not the raw crystal");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=60,batch="gem_sword_parts") public static void dragonAndBirdDrops(GameTestHelper helper) {
        var level=helper.getLevel();var difficulty=level.getDifficulty();level.getServer().setDifficulty(Difficulty.NORMAL,true);
        level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true,level.getServer());
        var dragon=helper.spawn(SinBosses.QINGLONG.get(),2,2,2);dragon.wake();var at=dragon.position();
        dragon.kill();
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(6));
        helper.assertTrue(drops.stream().anyMatch(i->SinBosses.IRREGULAR_CRYSTAL_RECIPE.equals(AlchemyRecipeItem.recipe(i.getItem()))),"the Azure Dragon drops the irregular crystal's recipe");
        for(var item:drops)item.discard();
        var bird=helper.spawn(SinBosses.ZHUQUE.get(),2,2,2);bird.wake();at=bird.position();
        bird.kill();
        drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(6));
        helper.assertTrue(drops.stream().anyMatch(i->i.getItem().is(Primordial.PRIMORDIAL_HILT.get())),"the Vermilion Bird drops the primordial hilt");
        for(var item:drops)item.discard();
        level.getServer().setDifficulty(difficulty,true);
        helper.succeed();
    }
}