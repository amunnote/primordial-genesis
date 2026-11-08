package local.primordial.smoke;

import local.primordial.Primordial;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class CharmLuckGameTests {
    @GameTest(template="empty") public static void charmsGiveLuckByTier(GameTestHelper helper) {
        var tiers=new Object[][]{
            {5.0,new RegistryObject[]{Primordial.BASIC_CHARM}},
            {10.0,new RegistryObject[]{Primordial.BREAKING_EDGE_CHARM,Primordial.SPELLBREAKER_CHARM,Primordial.THOUSAND_BLADE_CHARM,
                Primordial.QUICKSTRING_CHARM,Primordial.HUNDRED_FORGED_CHARM,Primordial.CRYSTAL_VESTMENT_CHARM}},
            {30.0,new RegistryObject[]{Primordial.CONQUEST_CHARM,Primordial.ARCANE_PACT_CHARM,Primordial.SWIFT_PACT_CHARM,
                Primordial.SWIFTSTRING_PACT_CHARM,Primordial.BULWARK_PACT_CHARM,Primordial.ASTRAL_PACT_CHARM}}};
        for(var tier:tiers)for(var charm:(RegistryObject<?>[])tier[1]){
            var player=helper.makeMockSurvivalPlayer();
            TestSlots.wearCharm(player,new ItemStack((Item)charm.get()));
            // Sage's Gift, needed for the alchemical charm slot, brings +10 luck of its own
            double luck=player.getAttributeValue(Attributes.LUCK)-10;
            helper.assertTrue(Math.abs(luck-(double)tier[0])<1e-6,charm.getId()+": expected +"+tier[0]+" luck, got +"+luck);
        }
        helper.succeed();
    }
}
