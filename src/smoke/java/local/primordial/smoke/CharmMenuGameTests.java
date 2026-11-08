package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.effect.Protection;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class CharmMenuGameTests {
    /** Wearing the Ascension Charm must not break drag-distribution in menus (it starts/ends with slot -999). */
    @GameTest(template="empty") public static void charmAllowsDragDistributionButNotThrowing(GameTestHelper helper) {
        var player=helper.makeMockPlayer();
        TestSlots.wearAscensionCharm(player);
        helper.assertTrue(Protection.active(player),"Charm is worn");
        var menu=player.inventoryMenu;
        int a=36,b=37; // first two hotbar slots
        menu.setCarried(new ItemStack(Items.DIAMOND,10));
        menu.clicked(-999,AbstractContainerMenu.getQuickcraftMask(0,0),ClickType.QUICK_CRAFT,player);
        menu.clicked(a,AbstractContainerMenu.getQuickcraftMask(1,0),ClickType.QUICK_CRAFT,player);
        menu.clicked(b,AbstractContainerMenu.getQuickcraftMask(1,0),ClickType.QUICK_CRAFT,player);
        menu.clicked(-999,AbstractContainerMenu.getQuickcraftMask(2,0),ClickType.QUICK_CRAFT,player);
        int placed=menu.getSlot(a).getItem().getCount()+menu.getSlot(b).getItem().getCount();
        helper.assertTrue(menu.getSlot(a).getItem().is(Items.DIAMOND)&&menu.getSlot(b).getItem().is(Items.DIAMOND),"Drag spread the stack over both slots");
        helper.assertTrue(placed+menu.getCarried().getCount()==10,"Nothing was lost, placed="+placed);
        // clicking outside the window with a carried stack would throw it: still blocked
        menu.setCarried(new ItemStack(Items.EMERALD,3));
        menu.clicked(-999,0,ClickType.PICKUP,player);
        helper.assertTrue(menu.getCarried().is(Items.EMERALD)&&menu.getCarried().getCount()==3,"Throwing out of the window stays blocked");
        helper.succeed();
    }
}
