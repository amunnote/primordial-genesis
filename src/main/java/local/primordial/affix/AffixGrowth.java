package local.primordial.affix;

import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.network.MenuDropNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Growth: size, attack and max health grow together from 1x to 2x over five minutes. The factor is saved on the
 * mob and sent to the clients watching it; LivingEntity.getScale() and its renderer read it (see the growth mixins).
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AffixGrowth {
    public static final String KEY="primordial:growth";
    public static final float MAX=2,PRIMORDIAL_MAX=1.25F;
    /** one step every 5 s, 60 steps to double */
    public static final int STEP_TICKS=100;
    public static final float STEP=1F/60;
    private static final UUID HEALTH=UUID.fromString("3c1d8a52-71e4-4b0f-9a2d-6e5f4c3b2a01"),ATTACK=UUID.fromString("3c1d8a52-71e4-4b0f-9a2d-6e5f4c3b2a02");
    private AffixGrowth() {}

    public static float growth(Entity entity) {
        if(!(entity instanceof LivingEntity))return 1;
        var data=entity.getPersistentData();
        return data.contains(KEY)?Math.max(1,data.getFloat(KEY)):1;
    }
    /** grow one step while the mob has Growth; shrink back at once when it loses it (an Amplify loan running out) */
    public static void step(AffixState state) {
        var mob=state.mob;float growth=growth(mob);
        // the Primordial Lord grows only while it fights, and only to a quarter more
        if(state.primordial()){
            boolean fighting=mob.level().getGameTime()-state.tag.getLong("lastCombat")<200;
            if(fighting&&growth<PRIMORDIAL_MAX)set(mob,Math.min(PRIMORDIAL_MAX,growth+STEP));
            else if(!fighting&&mob.getPersistentData().contains(KEY))set(mob,1);
            return;
        }
        if(state.affixes.contains(MobAffix.GROWTH)){if(growth<MAX)set(mob,Math.min(MAX,growth+STEP));}
        else if(mob.getPersistentData().contains(KEY))set(mob,1);
    }
    public static void set(Mob mob,float growth) {
        float before=mob.getMaxHealth();
        if(growth<=1)mob.getPersistentData().remove(KEY);else mob.getPersistentData().putFloat(KEY,growth);
        modify(mob,Attributes.MAX_HEALTH,HEALTH,growth-1);modify(mob,Attributes.ATTACK_DAMAGE,ATTACK,growth-1);
        float after=mob.getMaxHealth();
        // growing keeps the share of health it had; shrinking only clamps
        if(after>before&&before>0)mob.setHealth(mob.getHealth()*after/before);
        else if(mob.getHealth()>after)mob.setHealth(after);
        mob.refreshDimensions();
        if(!mob.level().isClientSide)MenuDropNetwork.syncGrowth(mob,growth);
    }
    private static void modify(Mob mob,Attribute attribute,UUID uuid,double amount) {
        var instance=mob.getAttribute(attribute);if(instance==null)return;instance.removeModifier(uuid);
        if(amount>0)instance.addPermanentModifier(new AttributeModifier(uuid,"primordial.growth",amount,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    /** a player starting to see a grown mob learns its size */
    @SubscribeEvent public static void startTracking(PlayerEvent.StartTracking event) {
        if(event.getEntity() instanceof ServerPlayer player&&event.getTarget().getPersistentData().contains(KEY))
            MenuDropNetwork.syncGrowth(player,event.getTarget(),growth(event.getTarget()));
    }
    /** client side of the sync packet */
    public static void clientSync(int entityId,float growth) {
        local.primordial.client.AffixClient.growth(entityId,growth);
    }
}
