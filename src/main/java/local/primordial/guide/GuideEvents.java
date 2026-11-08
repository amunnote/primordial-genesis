package local.primordial.guide;
import local.primordial.Primordial;
import local.primordial.spirit.SpiritRealm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class GuideEvents {
    private static boolean once(Player p,String key){var data=p.getPersistentData();var kept=data.getCompound(Player.PERSISTED_NBT_TAG);if(kept.getBoolean(key))return false;kept.putBoolean(key,true);data.put(Player.PERSISTED_NBT_TAG,kept);return true;}
    public static void gift(Player p){if(once(p,"primordial:guide_gift_hint"))p.displayClientMessage(NextStep.t("open"),true);}
    public static void opened(ServerPlayer p){if(once(p,"primordial:guide_ctrl_hint"))p.sendSystemMessage(NextStep.t("ctrl"));}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        boolean compass=p.getMainHandItem().is(Primordial.SPIRIT_COMPASS.get())||p.getOffhandItem().is(Primordial.SPIRIT_COMPASS.get());
        for(var stack:p.getInventory().items)if(stack.is(Primordial.ALCHEMIST_GIFT.get()))updateGift(p,stack);
        if(p.getOffhandItem().is(Primordial.ALCHEMIST_GIFT.get()))updateGift(p,p.getOffhandItem());
        if(compass)refresh(p,false);
    }
    public static void updateGift(ServerPlayer p,ItemStack stack){
        gift(p);String json=Component.Serializer.toJson(NextStep.advice(p));
        if(!json.equals(stack.getOrCreateTag().getString("GuideNext")))stack.getOrCreateTag().putString("GuideNext",json);
    }
    public static void refresh(ServerPlayer p,boolean manual){
        var targets=NextStep.candidates(p);String selected=p.getPersistentData().getString("GuideTarget");
        var target=NextStep.byId(selected);
        if(target==null||(!NextStep.sideQuest(selected)&&targets.stream().noneMatch(t->t.id().equals(selected))))target=targets.get(0);
        p.getPersistentData().putString("GuideTarget",target.id());
        var pos=target.pos();boolean found=true;
        var guardian=local.primordial.spirit.Guardian.byId(target.id());
        if(guardian!=null){
            var data=p.getPersistentData();String key="CourtCache_"+guardian.id;
            var cache=data.getCompound(key);long now=p.server.overworld().getGameTime();
            // Cache both successes and failures, per player and symbol; searches run only in the overworld.
            boolean stale=!cache.contains("Time")||now-cache.getLong("Time")>1200||now<cache.getLong("Time");
            if(p.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&(manual||stale)&&now-data.getLong("CourtLastSearch")>=200){
                data.putLong("CourtLastSearch",now);
                var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE,guardian.shrine());
                var nearest=p.serverLevel().findNearestMapStructure(tag,p.blockPosition(),64,false);
                if(nearest==null)nearest=p.serverLevel().findNearestMapStructure(tag,p.blockPosition(),128,false);
                cache.putLong("Time",now);cache.putBoolean("Found",nearest!=null);
                if(nearest!=null)cache.putLong("Pos",nearest.asLong());data.put(key,cache);
            }
            found=cache.getBoolean("Found");if(found)pos=net.minecraft.core.BlockPos.of(cache.getLong("Pos"));
        }
        for(var stack:java.util.List.of(p.getMainHandItem(),p.getOffhandItem()))if(stack.is(Primordial.SPIRIT_COMPASS.get())){
            var tag=stack.getOrCreateTag();if(found)tag.putLong("GuideTargetPos",pos.asLong());else tag.remove("GuideTargetPos");
            tag.putString("GuideTargetDimension",target.dimension().location().toString());tag.putString("GuideTargetName",Component.Serializer.toJson(target.name()));
            tag.putString("GuideDirection",guardian==null?"":guardian.direction());
        }
        var distance=p.level().dimension()!=target.dimension()?NextStep.t("other_dimension"):!found?NextStep.t("not_found"):NextStep.t("distance",(int)Math.sqrt(pos.distToCenterSqr(p.getX(),p.getY(),p.getZ())));
        p.displayClientMessage(target.name().copy().append(" · ").append(distance).append(" · ").append(Component.literal(target.dimension().location().toString())).append(guardian==null?Component.empty():NextStep.t(guardian.direction())),true);
    }

    private static boolean isMaterial(ItemStack stack){return local.primordial.affix.AffixMaterials.BOSS_ITEMS.values().stream().anyMatch(i->stack.is(i.get()));}
    @SubscribeEvent public static void exchange(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!e.getLevel().getBlockState(e.getPos()).is(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE))return;
        // Only the first hand handles the exchange, avoiding a second offhand transaction.
        if(e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        var stack=isMaterial(p.getMainHandItem())?p.getMainHandItem():p.getOffhandItem();boolean material=isMaterial(stack);
        if(!material){p.displayClientMessage(NextStep.t("exchange.material"),true);return;}
        e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if(p.experienceLevel<30){p.displayClientMessage(NextStep.t("exchange.level"),true);return;}
        stack.shrink(1);p.giveExperienceLevels(-30);
        net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(p,new ItemStack(Primordial.SPIRIT_COMPASS.get()));
        p.displayClientMessage(NextStep.t("exchange.success"),true);
    }
}
