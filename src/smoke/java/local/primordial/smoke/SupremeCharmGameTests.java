package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.boss.SinBosses;
import local.primordial.item.AlchemyCharmItem;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The supreme charms: their recipes, the guardians who carry them, and their powers. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class SupremeCharmGameTests {
    @GameTest(template="empty") public static void supremeRecipes(GameTestHelper helper) {
        var recipes=helper.getLevel().getRecipeManager();
        for(var id:new net.minecraft.resources.ResourceLocation[]{SinBosses.CLEAR_WORLD_RECIPE,SinBosses.INVIOLABLE_RECIPE}){
            var recipe=recipes.byKey(id);
            helper.assertTrue(recipe.isPresent()&&recipe.get() instanceof local.primordial.altar.AltarRecipe altar&&altar.tier()==4,"primordial altar recipe for "+id);
            helper.assertTrue(AlchemyKnowledge.bossOnly(id),id+" is carried only by a guardian");
        }
        // the Sin Lords keep the recipes they carried: the supreme ones come after the Seven Sins
        var order=AlchemyKnowledge.bossOnlyRecipes();
        helper.assertTrue(order.indexOf(SinBosses.SEVEN_SINS_RECIPE)==5&&order.indexOf(SinBosses.CLEAR_WORLD_RECIPE)==6&&order.indexOf(SinBosses.INVIOLABLE_RECIPE)==7,"boss-only order: "+order);
        var both=recipes.byKey(SinBosses.CLEAR_WORLD_RECIPE).orElseThrow();
        var eternity=new ItemStack(Primordial.FLEETING_ETERNITY_CHARM.get());var metal=new ItemStack(SinBosses.METAL_ESSENCE.get());
        helper.assertTrue(both.getIngredients().stream().anyMatch(i->i.test(eternity))&&both.getIngredients().stream().anyMatch(i->i.test(metal)),"Transparent World joins Fleeting Eternity, with the Essence of Metal");
        helper.succeed();
    }

    @GameTest(template="empty") public static void guardiansCarryThem(GameTestHelper helper) {
        var level=helper.getLevel();
        var tiger=helper.spawn(SinBosses.BAIHU.get(),2,2,2);var tortoise=helper.spawn(SinBosses.XUANWU.get(),6,2,6);
        tiger.kill();tortoise.kill();
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(helper.absolutePos(net.minecraft.core.BlockPos.ZERO)).inflate(16));
        helper.assertTrue(drops.stream().anyMatch(d->SinBosses.CLEAR_WORLD_RECIPE.equals(AlchemyRecipeItem.recipe(d.getItem()))),"the White Tiger drops Transparent World's recipe");
        helper.assertTrue(drops.stream().anyMatch(d->SinBosses.INVIOLABLE_RECIPE.equals(AlchemyRecipeItem.recipe(d.getItem()))),"the Black Tortoise drops Untouched by Any Law's");
        helper.assertTrue(drops.stream().anyMatch(d->d.getItem().is(SinBosses.METAL_ESSENCE.get()))&&drops.stream().anyMatch(d->d.getItem().is(SinBosses.WATER_ESSENCE.get())),"and their essences");
        drops.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    @GameTest(template="empty") public static void supremePowers(GameTestHelper helper) {
        var clear=PrimordialCharmGameTests.wearing(helper,Primordial.CLEAR_WORLD_CHARM.get());
        helper.assertTrue(AlchemyCharmItem.insight(clear)==0.2&&AlchemyCharmItem.echo(clear)==0.75&&AlchemyCharmItem.stasis(clear)&&AlchemyCharmItem.homing(clear),"Transparent World holds both its forebears and Insight");
        var ward=PrimordialCharmGameTests.wearing(helper,Primordial.INVIOLABLE_CHARM.get());
        helper.assertTrue(AlchemyCharmItem.ward(ward)==1200&&AlchemyCharmItem.cleanse(ward)&&AlchemyCharmItem.allReduction(ward)==0.9,"Untouched by Any Law holds both its forebears, one reduction for all damage, and Inviolate");
        // a killing blow is refused once
        ward.hurt(ward.damageSources().magic(),100000);
        helper.assertTrue(ward.isAlive()&&ward.getHealth()>=ward.getMaxHealth()*0.29F,"the first killing blow is refused: "+ward.getHealth());
        helper.assertTrue(ward.getPersistentData().getLong(local.primordial.effect.AlchemistEvents.WARD_KEY)>helper.getLevel().getGameTime(),"and the ward is spent for a while");
        helper.succeed();
    }

    @GameTest(template="empty") public static void ascensionCarriesEverything(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        TestSlots.wearAscensionCharm(player);
        helper.assertTrue(local.primordial.effect.Protection.active(player),"wearing the Ascension Charm");
        helper.assertTrue(AlchemyCharmItem.insight(player)==0.2&&AlchemyCharmItem.echo(player)==0.75&&AlchemyCharmItem.stasis(player)&&AlchemyCharmItem.homing(player)
            &&AlchemyCharmItem.infiniteAmmo(player)&&AlchemyCharmItem.ignoresInvulnerability(player)&&AlchemyCharmItem.armorPierce(player)==1.0,"every power of Transparent World");
        helper.assertTrue(AlchemyCharmItem.allReduction(player)==0.9&&AlchemyCharmItem.ward(player)==1200&&AlchemyCharmItem.cleanse(player)&&AlchemyCharmItem.reflect(player)==0.4
            &&AlchemyCharmItem.hitCap(player)==0.2,"and every power of Untouched by Any Law");
        helper.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)>=1,"and their bonuses: no knockback");
        // its recipe: the Primordial Lord carries it
        var recipe=helper.getLevel().getRecipeManager().byKey(SinBosses.ASCENSION_RECIPE);
        helper.assertTrue(recipe.isPresent()&&recipe.get() instanceof local.primordial.altar.AltarRecipe altar&&altar.tier()==4,"a primordial altar recipe");
        helper.assertTrue(AlchemyKnowledge.bossOnlyRecipes().indexOf(SinBosses.ASCENSION_RECIPE)==8,"registered after the supreme charms");
        var lord=helper.spawn(SinBosses.PRIMORDIAL.get(),2,2,2);
        lord.kill();
        var drops=helper.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(lord.blockPosition()).inflate(6));
        helper.assertTrue(drops.stream().anyMatch(d->SinBosses.ASCENSION_RECIPE.equals(AlchemyRecipeItem.recipe(d.getItem())))
            &&drops.stream().anyMatch(d->d.getItem().is(SinBosses.DIVINITY_SHARD.get())&&d.getItem().getCount()==2),"the Primordial Lord drops its recipe and two Divinity Shards");
        drops.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }
}
