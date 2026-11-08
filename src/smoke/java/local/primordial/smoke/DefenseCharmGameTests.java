package local.primordial.smoke;

import local.primordial.Primordial;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class DefenseCharmGameTests {
    private static void wear(Player player,ItemStack stack) {
        TestSlots.wearCharm(player,stack);
    }
    /** health the wearer loses from 10 damage */
    private static float taken(Player victim,DamageSource source) {
        victim.setHealth(20);victim.invulnerableTime=0;
        victim.hurt(source,10);
        return 20-victim.getHealth();
    }
    private static void near(GameTestHelper helper,float actual,double expected,String what) {
        helper.assertTrue(Math.abs(actual-expected)<0.01,what+": expected "+expected+", got "+actual);
    }
    @GameTest(template="empty") public static void defensiveCharmsReduceDamage(GameTestHelper helper) {
        Player victim=helper.makeMockSurvivalPlayer(),attacker=helper.makeMockPlayer();
        var physical=attacker.damageSources().playerAttack(attacker);
        var magic=victim.damageSources().magic();
        near(helper,taken(victim,physical),10,"no charm, physical");
        near(helper,taken(victim,magic),10,"no charm, magic");

        wear(victim,new ItemStack(Primordial.HUNDRED_FORGED_CHARM.get()));
        near(helper,taken(victim,physical),7,"hundred-forged mail: 30% less physical");
        near(helper,taken(victim,magic),10,"hundred-forged mail: magic unchanged");
        wear(victim,new ItemStack(Primordial.BULWARK_PACT_CHARM.get()));
        near(helper,taken(victim,physical),5,"bulwark pact: 50% less physical");

        wear(victim,new ItemStack(Primordial.CRYSTAL_VESTMENT_CHARM.get()));
        near(helper,taken(victim,magic),7,"crystalline vestment: 30% less non-physical");
        near(helper,taken(victim,physical),10,"crystalline vestment: physical unchanged");
        wear(victim,new ItemStack(Primordial.ASTRAL_PACT_CHARM.get()));
        near(helper,taken(victim,magic),5,"astral pact: 50% less non-physical");

        var recipes=helper.getLevel().getRecipeManager();
        for(String id:new String[]{"hundred_forged_charm","bulwark_pact_charm","crystal_vestment_charm"})
            helper.assertTrue(recipes.byKey(new ResourceLocation(Primordial.ID,id)).isPresent(),"recipe "+id+" loaded");
        var astral=recipes.byKey(new ResourceLocation(Primordial.ID,"astral_pact_charm")).orElse(null);
        helper.assertTrue(astral!=null,"recipe astral_pact_charm loaded");
        // any strength or duration of each potion counts, a different potion does not
        for(var potion:new net.minecraft.world.item.alchemy.Potion[]{Potions.WEAKNESS,Potions.LONG_SLOWNESS,Potions.STRONG_HARMING,Potions.STRONG_POISON}){
            var stack=PotionUtils.setPotion(new ItemStack(Items.POTION),potion);
            helper.assertTrue(astral.getIngredients().stream().anyMatch(i->i.test(stack)),"astral pact accepts "+potion.getName(""));
        }
        var healing=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.HEALING);
        helper.assertTrue(astral.getIngredients().stream().noneMatch(i->i.test(healing)),"astral pact rejects healing");
        helper.succeed();
    }
}
