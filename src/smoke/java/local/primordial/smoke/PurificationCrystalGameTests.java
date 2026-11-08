package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.affix.AffixStatuses;
import local.primordial.effect.HarmfulEffectProtection;
import local.primordial.item.PurificationCrystalMigration;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

/** The Purification Crystal: an alchemical accessory against every harmful effect and every negative status. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class PurificationCrystalGameTests {
    /** the alchemical accessory slots come with Sage's Gift */
    static void sageGift(Player player) {
        CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new).setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));
        TestSlots.tick(player);
    }

    @GameTest(template="empty") public static void nothingHarmfulTakesHold(GameTestHelper helper) {
        var wearer=helper.makeMockSurvivalPlayer();var plain=helper.makeMockSurvivalPlayer();
        sageGift(wearer);
        // what is on the wearer before the crystal goes on
        wearer.addEffect(new MobEffectInstance(MobEffects.POISON,200));
        AffixStatuses.applyMalice(wearer);AffixStatuses.applyFeeble(wearer);
        helper.assertTrue(AffixStatuses.malice(wearer)>0&&AffixStatuses.feeble(wearer)>0,"statuses taken before the crystal");
        var inv=CuriosApi.getCuriosInventory(wearer).orElseThrow(IllegalStateException::new);
        inv.setEquippedCurio(Primordial.ALCHEMICAL_ACCESSORY_SLOT,0,new ItemStack(Primordial.PURIFICATION_CRYSTAL.get()));
        TestSlots.tick(wearer);TestSlots.tick(wearer);
        helper.assertTrue(HarmfulEffectProtection.crystalActive(wearer),"worn in an alchemical accessory slot, it works");
        HarmfulEffectProtection.cleanse(wearer);
        helper.assertTrue(!wearer.hasEffect(MobEffects.POISON),"the poison on it is gone");
        helper.assertTrue(AffixStatuses.malice(wearer)==0&&AffixStatuses.feeble(wearer)==0,"the statuses on it are gone");
        // and nothing new takes hold
        AffixStatuses.applyDesaturation(wearer);AffixStatuses.applyBefuddled(wearer);AffixStatuses.applyBloodPact(wearer);
        helper.assertTrue(AffixStatuses.desaturation(wearer)==0&&AffixStatuses.befuddled(wearer)==0&&AffixStatuses.bloodPact(wearer)==0,"no new status takes hold");
        helper.assertTrue(!wearer.addEffect(new MobEffectInstance(MobEffects.WITHER,200))||!wearer.hasEffect(MobEffects.WITHER),"no new harmful effect takes hold");
        helper.assertTrue(wearer.getMaxHealth()==20,"it gives no health any more");
        // someone without it is left as they are
        AffixStatuses.applyDesaturation(plain);
        helper.assertTrue(AffixStatuses.desaturation(plain)>0,"without it, statuses take hold");
        helper.succeed();
    }

    @GameTest(template="empty") public static void oldSlotMovesToAccessory(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();sageGift(player);
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        helper.assertTrue(!HarmfulEffectProtection.crystalActive(player),"nothing worn yet");
        inv.getStacksHandler("curio").orElseThrow().getStacks().setStackInSlot(0,new ItemStack(Primordial.PURIFICATION_CRYSTAL.get()));
        PurificationCrystalMigration.move(player);
        helper.assertTrue(inv.getStacksHandler("curio").orElseThrow().getStacks().getStackInSlot(0).isEmpty(),"out of the old slot");
        helper.assertTrue(HarmfulEffectProtection.crystalActive(player),"into an alchemical accessory slot");
        helper.succeed();
    }
}
