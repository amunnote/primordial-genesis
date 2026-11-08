package local.primordial.effect;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import local.primordial.Primordial;
import local.primordial.mixin.EntityRemovalAccessor;
import local.primordial.mixin.HealthAccessor;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Ascension Charm's general defences, against any mod rather than one in particular:
 * <ul>
 * <li>No one can attack its wearer: Forge's attack event is refused, and with it every weapon's left-click power
 *     (Forge calls Item.onLeftClickEntity only once that event has passed).</li>
 * <li>Nothing drops from it, loot or experience, while it lives.</li>
 * <li>Every server tick, whatever got past the guards is put right for each wearer, whether or not the wearer still
 *     ticks: a removal mark or death written straight into its fields is cleared, health forced below what it had is
 *     restored, a modifier that lowers its maximum health is stripped, the in-world flag cleared on it is set again,
 *     and should it be put nowhere (a position that is not a number) or under the world, it is brought back to where
 *     it last stood safely.</li>
 * </ul>
 * Together with the guards at the game's own choke points (health, damage, death, removal, the world's entity
 * storage, the inventory) this holds against any mod, without naming one: whatever a mod does to the wearer must
 * go through these in the end.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class ProtectionRepair {
    /** the health each wearer last held: what it must not fall below */
    private static final Map<Player,Float> HELD=new WeakHashMap<>();
    /** where each wearer last stood safely: inside the world, at a real position */
    private static final Map<Player,net.minecraft.world.phys.Vec3> SAFE=new WeakHashMap<>();
    private ProtectionRepair() {}

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent event) {
        if(Protection.active(event.getTarget()))event.setCanceled(true);
    }
    /** and again last, should anything have let the attack through meanwhile */
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void attackLast(AttackEntityEvent event) {
        if(Protection.active(event.getTarget()))event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void drops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if(Protection.active(event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void experience(net.minecraftforge.event.entity.living.LivingExperienceDropEvent event) {
        if(Protection.active(event.getEntity()))event.setCanceled(true);
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        for(var player:event.getServer().getPlayerList().getPlayers())repair(player);
    }
    public static void repair(Player player) {
        if(!Protection.active(player)){HELD.remove(player);SAFE.remove(player);return;}
        // a removal or a death written past the guards
        var removal=(EntityRemovalAccessor)player;
        if(removal.angelRemovalReason()!=null&&Protection.lethalRemoval(removal.angelRemovalReason()))removal.angelSetRemovalReason(null);
        var health=(HealthAccessor)player;
        if(health.angelIsDead())health.angelSetDead(false);
        player.deathTime=0;
        if(player.getPose()==Pose.DYING)player.setPose(Pose.STANDING);
        // whatever lowers its maximum health
        var max=player.getAttribute(Attributes.MAX_HEALTH);
        if(max!=null)for(AttributeModifier modifier:java.util.List.copyOf(max.getModifiers()))if(modifier.getAmount()<0)max.removeModifier(modifier.getId());
        // health it was robbed of, by any means
        float now=player.getHealth(),held=Math.min(HELD.getOrDefault(player,now),player.getMaxHealth());
        if(!Float.isFinite(now)||now<=0)player.setHealth(player.getMaxHealth());
        else if(now<held)player.setHealth(held);
        HELD.put(player,player.getHealth());
        // marked as gone from the world while still in it
        if(!player.isAddedToWorld()&&player.level().getEntity(player.getId())==player)player.onAddedToWorld();
        // put nowhere or under the world: back to where it last stood safely
        var at=player.position();
        boolean lost=!Double.isFinite(at.x)||!Double.isFinite(at.y)||!Double.isFinite(at.z)||at.y<player.level().getMinBuildHeight()-64;
        if(!lost&&(player.onGround()||player.getAbilities().flying))SAFE.put(player,at);
        else{var safe=SAFE.getOrDefault(player,net.minecraft.world.phys.Vec3.atBottomCenterOf(player.level().getSharedSpawnPos()));
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.fallDistance=0;player.teleportTo(safe.x,safe.y,safe.z);}
    }
    /** for tests: forget what a wearer held */
    static void forget(UUID player) {HELD.keySet().removeIf(p->p.getUUID().equals(player));}
}
