package local.primordial.effect;

import java.util.ArrayList;
import local.primordial.Primordial;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = Primordial.ID)
public final class CharmEvents {
    private static final String SNAPSHOT = "primordial:movement_snapshot";
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent e) { if (Protection.active(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent e) { if (Protection.active(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent e) {
        if (Protection.active(e.getEntity())) { e.setCanceled(true); return; }
        // true damage passes every reduction (and is a share of a hit already multiplied); fixed damage is its own number
        if (!local.primordial.affix.AffixDamage.mitigable(e.getSource())) return;
        // all-damage reduction (Sage's Gift growth): 0.2 = 20% less damage taken, 1 or more = none
        if (e.getEntity() instanceof Player victim) {
            double reduction = victim.getAttributeValue(Primordial.REDUCTION.get());
            if (reduction > 0) e.setAmount((float) (e.getAmount() * Math.max(0, 1 - reduction)));
        }
        if (e.getSource().getEntity() instanceof Player player) {
            // every all-damage source (charms, Sage's Gift, Alchemist's Gift) is an attribute modifier
            double multiplier = player.getAttributeValue(Primordial.DAMAGE.get());
            if (multiplier == 1) return;
            e.setAmount((float) Math.min(Float.MAX_VALUE, e.getAmount() * multiplier));
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void death(LivingDeathEvent e) { if (Protection.active(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void effect(MobEffectEvent.Applicable e) {
        if (e.getEffectInstance().getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL
                && HarmfulEffectProtection.active(e.getEntity())) e.setResult(Event.Result.DENY);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.PlayerTickEvent e) {
        // Both phases counter mods that change ability flags during their ticks.
        maintain(e.player);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) { maintain(e.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { maintain(e.getEntity()); }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void equip(top.theillusivec4.curios.api.event.CurioEquipEvent e) {
        if(EquipFreedom.allows(e.getSlotContext(),e.getStack()))e.setResult(Event.Result.ALLOW);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void unequip(top.theillusivec4.curios.api.event.CurioUnequipEvent e) {
        if(Protection.active(e.getEntity())&&InventoryProtection.allowsEdit())e.setResult(Event.Result.ALLOW);
    }
    public static void maintain(Player player) {
        var data = player.getPersistentData();
        boolean active = Protection.active(player);
        var abilities = player.getAbilities();
        boolean previousMayfly = abilities.mayfly;
        boolean previousFlying = abilities.flying;
        if (active) {
            if (!data.contains(SNAPSHOT)) {
                var snapshot = new net.minecraft.nbt.CompoundTag();
                snapshot.putBoolean("mayfly", abilities.mayfly);
                snapshot.putBoolean("flying", abilities.flying);
                snapshot.putBoolean("physics", player.noPhysics);
                snapshot.putBoolean("gravity", player.isNoGravity());
                snapshot.putBoolean("creativeFlight", true);
                data.put(SNAPSHOT, snapshot);
            }
            var snapshot = data.getCompound(SNAPSHOT);
            if (!snapshot.getBoolean("creativeFlight")) {
                // Older releases forced no-gravity while the charm was equipped.
                player.setNoGravity(snapshot.getBoolean("gravity"));
                snapshot.putBoolean("creativeFlight", true);
                data.put(SNAPSHOT, snapshot);
            }
            abilities.mayfly = true;
            player.noPhysics = player.isSpectator() || snapshot.getBoolean("physics");
            player.fallDistance = 0;
            player.clearFire();
            player.setAirSupply(player.getMaxAirSupply());
            ((local.primordial.mixin.HealthAccessor)player).angelSetDead(false);
            player.deathTime = 0;
            if (!Float.isFinite(player.getHealth()) || player.getHealth() <= 0) player.setHealth(player.getMaxHealth());
            if (player.getPose() == Pose.DYING) player.setPose(Pose.STANDING);
            for (var effect : new ArrayList<>(player.getActiveEffects()))
                if (effect.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                    player.removeEffect(effect.getEffect());
            var cooldowns = player.getCooldowns();
            CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
                var stacks = inventory.getEquippedCurios();
                for (int i = 0; i < stacks.getSlots(); i++)
                    if (!stacks.getStackInSlot(i).isEmpty() && ((local.primordial.mixin.CooldownEntriesAccessor)cooldowns)
                            .angelEntries().containsKey(stacks.getStackInSlot(i).getItem()))
                        cooldowns.removeCooldown(stacks.getStackInSlot(i).getItem());
            });
        } else if (data.contains(SNAPSHOT)) {
            var snapshot = data.getCompound(SNAPSHOT);
            boolean nativeFlight = abilities.instabuild || player.isSpectator();
            abilities.mayfly = nativeFlight || snapshot.getBoolean("mayfly");
            abilities.flying = player.isSpectator() || (abilities.mayfly && snapshot.getBoolean("flying"));
            player.noPhysics = player.isSpectator() || snapshot.getBoolean("physics");
            player.setNoGravity(snapshot.getBoolean("gravity"));
            player.fallDistance = 0;
            data.remove(SNAPSHOT);
        }
        if (!player.level().isClientSide && (previousMayfly != abilities.mayfly || previousFlying != abilities.flying))
            player.onUpdateAbilities();
    }
}
