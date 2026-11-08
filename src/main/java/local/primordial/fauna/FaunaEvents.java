package local.primordial.fauna;

import local.primordial.Primordial;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 冥灯 held in either hand lights the deep: night vision under water, kept topped up while it is held. */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class FaunaEvents {
    private FaunaEvents() {}
    @SubscribeEvent public static void lampUnderWater(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        boolean held=player.getMainHandItem().is(FaunaContent.NETHERLAMP.get())||player.getOffhandItem().is(FaunaContent.NETHERLAMP.get());
        if(!held||!player.isEyeInFluid(FluidTags.WATER))return;
        var current=player.getEffect(MobEffects.NIGHT_VISION);
        // long enough that it does not flicker as vanilla's warning does under 10 seconds
        if(current==null||current.getDuration()<220)player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,260,0,true,false,true));
    }
}
