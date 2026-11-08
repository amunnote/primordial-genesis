package local.primordial.sage;

import local.primordial.Primordial;
import local.primordial.effect.Protection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.*;

@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class SageEvents {
    public static final String PROJECTILE_WEAPON="primordial:alchemical_projectile",PROJECTILE_SHARE="primordial:alchemical_projectile_share";
    private SageEvents() {}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        var player=event.getEntity();var data=player.getPersistentData();
        SageHeart.settle(player);
        if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)SageProgress.importCompleted(serverPlayer);
        CompoundTag persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        if(!persisted.getBoolean(SageHeart.GIVEN)){
            var stack=Primordial.SAGE_HEART.get().getDefaultInstance();
            if(player.getInventory().add(stack)){
                persisted.putBoolean(SageHeart.GIVEN,true);data.put(Player.PERSISTED_NBT_TAG,persisted);
            }else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.primordial.sage.full"),false);
        }
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        SageProgress.copy(event.getOriginal(),event.getEntity());
        SageHeart.copyUnlocks(event.getOriginal(),event.getEntity());
        if(event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(SageHeart.GIVEN)){
            var persisted=event.getEntity().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            persisted.putBoolean(SageHeart.GIVEN,true);event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);
        }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {SageHeart.settle(event.getEntity());}
    @SubscribeEvent public static void travel(PlayerEvent.PlayerChangedDimensionEvent event) {SageHeart.settle(event.getEntity());}
    @SubscribeEvent public static void advancement(net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent event) {
        if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)SageProgress.record(serverPlayer,event.getAdvancement());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase==TickEvent.Phase.END){
            if(SageHeart.discover(event.player)&&event.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                SageProgress.sync(serverPlayer);
            SageHeart.processSlotRelease(event.player);
            SageHeart.maintain(event.player);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void unequip(CurioUnequipEvent event) {
        if(SageHeart.bound(event.getEntity(),event.getStack()))event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hurt(LivingHurtEvent event) {
        if(event.getSource().getEntity() instanceof Player player&&SageHeart.active(player)
            &&SageHeart.endUnlocked(player)&&!Protection.active(player)) {
            // what share of its harm the weapon keeps: all for a true alchemical treasure, 75% for a pseudo one, half for anything else
            float share;
            if(event.getSource().getDirectEntity() instanceof Projectile projectile&&projectile.getPersistentData().contains(PROJECTILE_SHARE))
                share=projectile.getPersistentData().getFloat(PROJECTILE_SHARE);
            else if(event.getSource().getDirectEntity() instanceof Projectile projectile&&projectile.getPersistentData().contains(PROJECTILE_WEAPON))
                share=projectile.getPersistentData().getBoolean(PROJECTILE_WEAPON)?1F:SageHeart.ORDINARY_SHARE;   // fired before the shares
            else share=SageHeart.weaponShare(player.getMainHandItem());
            if(share<1)event.setAmount(event.getAmount()*share);
        }
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide)return;
        if(event.getEntity() instanceof Projectile projectile&&projectile.getOwner() instanceof Player player
            &&!projectile.getPersistentData().contains(PROJECTILE_SHARE))
            projectile.getPersistentData().putFloat(PROJECTILE_SHARE,Math.max(SageHeart.weaponShare(player.getMainHandItem()),SageHeart.weaponShare(player.getOffhandItem())));
        if(event.getEntity() instanceof Mob mob&&SageHeart.attackCapable(mob))mob.targetSelector.addGoal(0,new SageTargetGoal(mob));
    }
}
