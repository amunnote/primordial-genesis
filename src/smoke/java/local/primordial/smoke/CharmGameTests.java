package local.primordial.smoke;

import local.primordial.Primordial;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class CharmGameTests {
    private static void wear(Player player,ItemStack stack) {
        TestSlots.wearCharm(player,stack);
    }
    private static boolean near(double a,double b) {return Math.abs(a-b)<1e-6;}
    /** health lost by a fresh zombie with 20 armor when the player deals 10 */
    private static float hit(GameTestHelper helper,Player player) {
        Zombie zombie=ArcaneCharmGameTests.plainZombie(helper);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(20);zombie.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        float before=zombie.getHealth();
        zombie.hurt(player.damageSources().playerAttack(player),10);
        float lost=before-zombie.getHealth();zombie.discard();
        return lost;
    }
    @GameTest(template="empty") public static void charmStatsAndArmorPierce(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        double baseAttack=player.getAttributeValue(Attributes.ATTACK_DAMAGE),baseArmor=player.getAttributeValue(Attributes.ARMOR);

        wear(player,new ItemStack(Primordial.BASIC_CHARM.get()));
        helper.assertTrue(near(player.getAttributeValue(Attributes.ATTACK_DAMAGE),baseAttack+5),"basic charm: +5 attack");
        helper.assertTrue(near(player.getAttributeValue(Attributes.ARMOR),baseArmor+5),"basic charm: +5 armor");

        wear(player,new ItemStack(Primordial.BREAKING_EDGE_CHARM.get()));
        helper.assertTrue(near(player.getAttributeValue(Attributes.ATTACK_DAMAGE),baseAttack*1.3),"realmbreaker edge: +30% attack");
        helper.assertTrue(near(player.getAttributeValue(Attributes.ARMOR),baseArmor),"realmbreaker edge: no armor");
        float plain=hit(helper,player);

        wear(player,new ItemStack(Primordial.CONQUEST_CHARM.get()));
        helper.assertTrue(near(player.getAttributeValue(Attributes.ATTACK_DAMAGE),baseAttack*1.6),"pact of conquest: +60% attack");
        float pierced=hit(helper,player);
        // 20 armor vs 10 damage leaves 4; with half the armor ignored, 8
        helper.assertTrue(Math.abs(plain-4)<0.01&&Math.abs(pierced-8)<0.01,"armor pierce: expected 4 then 8, got "+plain+" then "+pierced);

        var recipes=helper.getLevel().getRecipeManager();
        for(String id:new String[]{"basic_charm","breaking_edge_charm","conquest_charm"})
            helper.assertTrue(recipes.byKey(new ResourceLocation(Primordial.ID,id)).isPresent(),"recipe "+id+" loaded");
        helper.succeed();
    }
}
