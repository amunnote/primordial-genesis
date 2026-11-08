package local.primordial.effect;

import local.primordial.Primordial;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

public final class Protection {
    private Protection() {}
    public static boolean active(Entity entity) {
        if (!(entity instanceof Player player) || !Primordial.CHARM.isPresent()) return false;
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.getCurios().values().stream().anyMatch(handler -> {
                    for (int i = 0; i < handler.getStacks().getSlots(); i++)
                        if (handler.getStacks().getStackInSlot(i).is(Primordial.CHARM.get())) return true;
                    return false;
                })).orElse(false);
    }
    public static boolean lethalRemoval(Entity.RemovalReason reason) {
        // Logout, unloading and dimension transfers must work normally.
        return reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED;
    }
    /**
     * Whether this may not be taken out of the world's entity storage now: the wearer may leave it only with a
     * reason that is not a death (logging out, being unloaded, changing dimension), set before it goes.
     */
    public static boolean keepsInWorld(Object object) {
        if (!(object instanceof Entity entity) || !active(entity)) return false;
        var reason = entity.getRemovalReason();
        return reason == null || lethalRemoval(reason);
    }
    public static boolean blocksHealth(Entity entity, float newHealth) {
        return active(entity) && (!Float.isFinite(newHealth)
                || newHealth < ((Player) entity).getHealth());
    }
}
