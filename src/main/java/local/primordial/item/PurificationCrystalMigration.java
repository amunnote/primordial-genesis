package local.primordial.item;

import local.primordial.Primordial;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * The Purification Crystal was worn in the general "curio" slot before 1.0.75; now it is an alchemical accessory. A
 * crystal still in the old slot is moved, as its wearer logs in, to an empty alchemical accessory slot, or else into their
 * inventory. Taken out first, and put back only once it is out, so a slot that will not let it go never doubles it.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class PurificationCrystalMigration {
    private PurificationCrystalMigration() {}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {move(event.getEntity());}
    public static void move(Player player) {
        if(player.level().isClientSide)return;
        CuriosApi.getCuriosInventory(player).ifPresent(inventory->inventory.getStacksHandler("curio").ifPresent(old->{
            var stacks=old.getStacks();
            for(int i=0;i<stacks.getSlots();i++){
                var crystal=stacks.getStackInSlot(i);
                if(!crystal.is(Primordial.PURIFICATION_CRYSTAL.get()))continue;
                var moving=crystal.copy();
                stacks.setStackInSlot(i,ItemStack.EMPTY);
                if(!stacks.getStackInSlot(i).isEmpty())continue;   // held where it is (the Ascension Charm's lock): left alone
                boolean placed=inventory.getStacksHandler(Primordial.ALCHEMICAL_ACCESSORY_SLOT).map(slots->{
                    var into=slots.getStacks();
                    for(int j=0;j<into.getSlots();j++)if(into.getStackInSlot(j).isEmpty()){into.setStackInSlot(j,moving);return true;}
                    return false;
                }).orElse(false);
                if(!placed)net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player,moving);
            }
        }));
    }
}
