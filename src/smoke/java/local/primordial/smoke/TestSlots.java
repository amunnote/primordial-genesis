package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.effect.AscensionSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Equipping helpers: the alchemical charm slot comes with Sage's Gift, the Ascension Charm slot with carrying the charm. */
final class TestSlots {
    private TestSlots() {}
    /** Curios applies slot and attribute changes on the wearer's next tick */
    static void tick(Player player) {MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(player));}
    static int slots(Player player,String id) {
        return CuriosApi.getCuriosInventory(player).map(inv->inv.getStacksHandler(id).map(h->h.getSlots()).orElse(0)).orElse(0);
    }
    static void wearCharm(Player player,ItemStack charm) {
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        if(!inv.isEquipped(Primordial.SAGE_HEART.get())){inv.setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));tick(player);}
        inv.setEquippedCurio(Primordial.ALCHEMICAL_SLOT,0,charm);
        tick(player);
    }
    static void wearAscensionCharm(Player player) {
        player.getInventory().add(new ItemStack(Primordial.CHARM.get()));
        AscensionSlot.update(player);tick(player);
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(Primordial.CHARM.get())){
            var charm=player.getInventory().removeItemNoUpdate(i);
            CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new).setEquippedCurio(AscensionSlot.SLOT,0,charm);
            break;
        }
        tick(player);
    }
}
