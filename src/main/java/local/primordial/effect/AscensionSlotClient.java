package local.primordial.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Client-only lookup, kept apart so the server never loads client classes. */
final class AscensionSlotClient {
    private AscensionSlotClient() {}
    static Player localPlayer() {return Minecraft.getInstance().player;}
}
