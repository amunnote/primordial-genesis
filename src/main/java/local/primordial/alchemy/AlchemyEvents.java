package local.primordial.alchemy;

import local.primordial.Primordial;
import local.primordial.affix.AffixState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AlchemyEvents {
    private AlchemyEvents() {}
    /** putting the Sage's Gift on may complete the Essence of Truth epiphany reached while not a sage */
    @SubscribeEvent public static void giftOn(top.theillusivec4.curios.api.event.CurioChangeEvent event) {
        if(event.getEntity() instanceof Player player&&!player.level().isClientSide&&event.getTo().is(Primordial.SAGE_HEART.get()))
            AlchemyKnowledge.checkTruthEpiphany(player);
    }
    /** First kill of a mob carrying affixes by a sage: an epiphany teaches the Source of Transmutation. */
    @SubscribeEvent(priority=EventPriority.HIGH) public static void affixKill(LivingDeathEvent event) {
        if(event.getEntity().level().isClientSide||!(event.getEntity() instanceof Mob mob))return;
        if(event.getSource().getEntity() instanceof Player player&&AffixState.get(mob)!=null)
            AlchemyKnowledge.epiphany(player,AlchemyKnowledge.TRANSMUTATION_SOURCE);
    }
}
