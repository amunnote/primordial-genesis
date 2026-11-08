package local.primordial.smoke;

import java.util.EnumSet;
import local.primordial.affix.AffixState;
import local.primordial.affix.MobAffix;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class AffixBonusGameTests {
    private static void near(GameTestHelper helper,double actual,double expected,String what) {
        helper.assertTrue(Math.abs(actual-expected)<1e-4,what+": expected "+expected+", got "+actual);
    }
    @GameTest(template="empty") public static void eachAffixAddsFlatAndTenPercent(GameTestHelper helper) {
        var zombie=ArcaneCharmGameTests.plainZombie(helper);
        double health=zombie.getAttributeBaseValue(Attributes.MAX_HEALTH),attack=zombie.getAttributeBaseValue(Attributes.ATTACK_DAMAGE),
            armor=zombie.getAttributeBaseValue(Attributes.ARMOR);
        AffixState.assign(zombie,40,EnumSet.of(MobAffix.REGENERATION,MobAffix.VOID,MobAffix.SPLIT,MobAffix.REFLECTION),null);
        // flat bonuses first, then +10% per affix on top: (base + 4 x flat) x 1.4
        near(helper,zombie.getMaxHealth(),(health+20)*1.4,"4 affixes: health");
        near(helper,zombie.getAttributeValue(Attributes.ATTACK_DAMAGE),(attack+12)*1.4,"4 affixes: attack");
        near(helper,zombie.getAttributeValue(Attributes.ARMOR),(armor+8)*1.4,"4 affixes: armor");
        helper.assertTrue(zombie.getHealth()==zombie.getMaxHealth(),"spawns at full health");
        // dropping to one affix: +10%, and health never exceeds the new maximum
        AffixState.assign(zombie,40,EnumSet.of(MobAffix.VOID),null);
        near(helper,zombie.getMaxHealth(),(health+5)*1.1,"1 affix: health");
        helper.assertTrue(zombie.getHealth()<=zombie.getMaxHealth(),"health clamped");
        zombie.discard();
        helper.succeed();
    }
}
