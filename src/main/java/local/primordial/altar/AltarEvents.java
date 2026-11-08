package local.primordial.altar;

import local.primordial.Primordial;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AltarEvents {
    private AltarEvents() {}
    /** Vanilla skips the block when sneaking with an item in hand; let Shift+right-click take items off anyway. */
    @SubscribeEvent public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if(!event.getEntity().isSecondaryUseActive())return;
        if(!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof AlchemyAltarBlock))return;
        if(!(event.getLevel().getBlockEntity(event.getPos()) instanceof AltarBlockEntity altar)||altar.items().isEmpty())return;
        event.setUseBlock(Event.Result.ALLOW);
        event.setUseItem(Event.Result.DENY);
    }
}
