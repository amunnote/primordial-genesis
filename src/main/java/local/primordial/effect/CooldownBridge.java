package local.primordial.effect;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import top.theillusivec4.curios.api.CuriosApi;

public final class CooldownBridge {
    private static final Map<ItemCooldowns, WeakReference<Player>> OWNERS = new WeakHashMap<>();
    private CooldownBridge() {}
    public static synchronized void bind(ItemCooldowns cooldowns, Player player) {
        OWNERS.put(cooldowns, new WeakReference<>(player));
    }
    public static synchronized boolean bypass(ItemCooldowns cooldowns, Item item) {
        var ref = OWNERS.get(cooldowns);
        Player player = ref == null ? null : ref.get();
        return Protection.active(player) && CuriosApi.getCurio(item.getDefaultInstance()).isPresent();
    }
}
