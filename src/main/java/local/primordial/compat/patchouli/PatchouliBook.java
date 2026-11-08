package local.primordial.compat.patchouli;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import vazkii.patchouli.api.PatchouliAPI;

/** Only loaded when Patchouli is installed. */
public final class PatchouliBook {
    private PatchouliBook() {}
    public static void open(ServerPlayer player,ResourceLocation book) {PatchouliAPI.get().openBookGUI(player,book);}
}
