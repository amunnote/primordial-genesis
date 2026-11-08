package local.primordial.smoke;

import local.primordial.Primordial;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The test world starts on day 0, a full moon, when the moon's tide makes every monster stronger. The tests are
 * written for monsters as they are, so the world is set to a quarter moon (no tide) before they run; the moon's own
 * test sets the phases it needs, in a batch of its own, and puts this back.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class TestWorldSetup {
    /** day 2 (a first-quarter moon) at noon */
    public static final long NEUTRAL_TIME=2*24000L+6000;
    private TestWorldSetup() {}
    @SubscribeEvent public static void neutralMoon(ServerStartedEvent event) {event.getServer().overworld().setDayTime(NEUTRAL_TIME);}
}
