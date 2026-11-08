package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.effect.AscensionSlot;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class SlotLayoutGameTests {
    @GameTest(template="empty") public static void startingSlotsAndAscensionSlot(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        TestSlots.tick(player);
        helper.assertTrue(TestSlots.slots(player,Primordial.ALCHEMICAL_SLOT)==0,"no alchemical charm slot at the start");
        helper.assertTrue(TestSlots.slots(player,Primordial.ALCHEMICAL_ACCESSORY_SLOT)==0,"no alchemical accessory slot at the start");
        helper.assertTrue(TestSlots.slots(player,"charm")==0,"the old charm slot is gone");
        helper.assertTrue(TestSlots.slots(player,AscensionSlot.SLOT)==0,"no ascension slot without the charm");

        // Sage's Gift opens the alchemical slots; every charm goes in the alchemical charm slot
        TestSlots.wearCharm(player,new ItemStack(Primordial.BASIC_CHARM.get()));
        helper.assertTrue(TestSlots.slots(player,Primordial.ALCHEMICAL_SLOT)==1&&TestSlots.slots(player,Primordial.ALCHEMICAL_ACCESSORY_SLOT)==3,"Sage's Gift: +1 charm, +3 accessory");
        var curios=CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        for(var charm:new net.minecraft.world.item.Item[]{Primordial.BASIC_CHARM.get(),Primordial.CONQUEST_CHARM.get(),Primordial.SWIFTSTRING_PACT_CHARM.get()}){
            var stack=new ItemStack(charm);
            var context=new SlotContext(Primordial.ALCHEMICAL_SLOT,player,0,false,true);
            helper.assertTrue(CuriosApi.isStackValid(context,stack)&&((top.theillusivec4.curios.api.type.capability.ICurioItem)charm).canEquip(context,stack),
                charm+" goes in the alchemical charm slot");
        }

        // carrying the Ascension Charm adds its slot; wearing it keeps the slot; letting go of it removes the slot
        var other=helper.makeMockSurvivalPlayer();TestSlots.tick(other);
        other.getInventory().add(new ItemStack(Primordial.CHARM.get()));
        AscensionSlot.update(other);TestSlots.tick(other);
        helper.assertTrue(TestSlots.slots(other,AscensionSlot.SLOT)==1,"carrying the charm adds the ascension slot");
        other.getInventory().clearContent();
        AscensionSlot.update(other);TestSlots.tick(other);
        helper.assertTrue(TestSlots.slots(other,AscensionSlot.SLOT)==0,"without the charm the slot goes away");
        TestSlots.wearAscensionCharm(other);
        AscensionSlot.update(other);TestSlots.tick(other);
        helper.assertTrue(TestSlots.slots(other,AscensionSlot.SLOT)==1&&local.primordial.effect.Protection.active(other),"worn in its slot, the slot stays and the charm works");
        helper.succeed();
    }
}
