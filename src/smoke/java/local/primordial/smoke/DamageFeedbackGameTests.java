package local.primordial.smoke;

import local.primordial.affix.AffixBonds;
import local.primordial.affix.AffixDamage;
import local.primordial.affix.DamageFeedback;
import local.primordial.affix.DamageFeedback.Look;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Each kind of hit has its own look (DamageFeedback), and the book has a page for every bond. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class DamageFeedbackGameTests {
    @GameTest(template="empty") public static void eachKindOfHitHasItsLook(GameTestHelper helper) {
        var zombie=helper.spawn(EntityType.ZOMBIE,2,2,2);var victim=helper.spawn(EntityType.ZOMBIE,2,2,3);
        var sources=victim.damageSources();
        helper.assertTrue(DamageFeedback.look(sources.mobAttack(zombie))==Look.PHYSICAL,"a blow is physical");
        helper.assertTrue(DamageFeedback.look(sources.magic())==Look.MAGIC,"magic is magic");
        helper.assertTrue(DamageFeedback.look(AffixDamage.source(victim,AffixDamage.TRUE_DAMAGE,zombie))==Look.TRUE,"true damage");
        helper.assertTrue(DamageFeedback.look(AffixDamage.source(victim,AffixDamage.FIXED_DAMAGE,zombie))==Look.FIXED,"fixed damage");
        helper.assertTrue(DamageFeedback.look(sources.fall())==null,"a fall shows nothing of its own");
        // a hit that lands goes through the feedback without trouble
        victim.hurt(sources.magic(),2);AffixDamage.dealFixed(victim,zombie,2);
        helper.assertTrue(victim.getHealth()<victim.getMaxHealth(),"the hits landed");
        zombie.discard();victim.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void everyBondIsInTheBook(GameTestHelper helper) {
        for(var lang:new String[]{"zh_cn","en_us"}){
            var path="/assets/primordial/patchouli_books/alchemist_gift/"+lang+"/entries/accessories/bonds.json";
            try(var in=DamageFeedbackGameTests.class.getResourceAsStream(path)){
                helper.assertTrue(in!=null,"the bonds entry: "+path);
                var pages=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("pages");
                helper.assertTrue(pages.size()==AffixBonds.Bond.values().length+1,lang+": a page for each bond and one to open, got "+pages.size());
            }catch(java.io.IOException e){throw new RuntimeException(e);}
        }
        helper.succeed();
    }
}
