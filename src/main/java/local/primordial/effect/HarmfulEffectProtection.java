package local.primordial.effect;

import java.util.ArrayList;
import local.primordial.Primordial;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

/** Effect immunity is separate from the Ascension Charm's damage and death protection. */
public final class HarmfulEffectProtection {
    private HarmfulEffectProtection() {}
    public static boolean crystalActive(Entity entity) {
        if (!(entity instanceof Player player) || !Primordial.PURIFICATION_CRYSTAL.isPresent()) return false;
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.getStacksHandler(Primordial.ALCHEMICAL_ACCESSORY_SLOT)
                .map(handler -> {
                    for (int i = 0; i < handler.getStacks().getSlots(); i++)
                        if (handler.getStacks().getStackInSlot(i).is(Primordial.PURIFICATION_CRYSTAL.get())) return true;
                    return false;
                }).orElse(false)).orElse(false);
    }
    public static boolean active(Entity entity) { return Protection.active(entity) || crystalActive(entity); }
    /** clears every harmful effect and every negative status (AffixStatuses) the entity carries */
    public static void cleanse(LivingEntity entity) {
        for (var effect : new ArrayList<>(entity.getActiveEffects()))
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) entity.removeEffect(effect.getEffect());
        local.primordial.affix.AffixStatuses.clearNegative(entity);
    }
}
