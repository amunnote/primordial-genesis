package local.primordial.effect;

import java.util.UUID;
import local.primordial.Primordial;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * The Ascension Charm slot exists only while the player has an Ascension Charm: in the inventory, on the
 * cursor, or already worn in that slot. It is a permanent slot modifier, so it survives relogging with the charm inside.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AscensionSlot {
    public static final String SLOT="ascension_charm";
    private static final UUID MODIFIER=UUID.fromString("5a7c1e2d-3b4f-4a6e-8d9c-0e1f2a3b4c51");
    private AscensionSlot() {}

    public static boolean holdsCharm(Player player) {
        var charm=Primordial.CHARM.get();
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(charm))return true;
        if(player.containerMenu.getCarried().is(charm))return true;
        return CuriosApi.getCuriosInventory(player).map(inv->inv.getStacksHandler(SLOT).map(handler->{
            for(int i=0;i<handler.getStacks().getSlots();i++)if(handler.getStacks().getStackInSlot(i).is(charm))return true;
            return false;
        }).orElse(false)).orElse(false);
    }
    /**
     * Runs on both sides: the client knows its own inventory and cursor, so it reaches the same slot count as the
     * server instead of waiting for Curios to sync a modifier it does not reliably carry over.
     */
    public static void update(Player player) {
        if(player.level().isClientSide&&player!=net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->AscensionSlotClient::localPlayer))return;
        boolean wanted=holdsCharm(player);
        CuriosApi.getCuriosInventory(player).ifPresent(inv->inv.getStacksHandler(SLOT).ifPresent(handler->{
            boolean present=handler.getModifiers().containsKey(MODIFIER);
            if(wanted&&!present)handler.addPermanentModifier(new AttributeModifier(MODIFIER,"primordial.ascension_slot",1,AttributeModifier.Operation.ADDITION));
            else if(!wanted&&present)handler.removeModifier(MODIFIER);
        }));
    }
    @SubscribeEvent public static void ascensionSlotTick(TickEvent.PlayerTickEvent event) {
        // every tick on both sides, so neither runs ahead of the other for long
        if(event.phase==TickEvent.Phase.END)update(event.player);
    }
    @SubscribeEvent public static void ascensionSlotLogin(PlayerEvent.PlayerLoggedInEvent event) {update(event.getEntity());}
}
