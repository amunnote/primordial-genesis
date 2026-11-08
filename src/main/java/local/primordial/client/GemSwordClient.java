package local.primordial.client;

import local.primordial.Primordial;
import local.primordial.effect.GemSwordPower;
import local.primordial.network.MenuDropNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class GemSwordClient {
    public static final KeyMapping SPECTATOR=new KeyMapping("key.primordial.gem_spectator",GLFW.GLFW_KEY_V,"key.categories.primordial");
    private GemSwordClient() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var player=Minecraft.getInstance().player;
        while(SPECTATOR.consumeClick())
            if(player!=null&&GemSwordPower.carries(player))MenuDropNetwork.CHANNEL.sendToServer(new MenuDropNetwork.GemSpectator());
    }
    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class GemSwordClientRegistration { // unique names: see AltarRenderer.AltarClientRegistration
        @SubscribeEvent public static void registerGemSwordKeys(RegisterKeyMappingsEvent event) {event.register(SPECTATOR);}
        @SubscribeEvent public static void registerGemSwordRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(Primordial.GEM_LIGHTNING.get(),GemLightningRenderer::new);
        }
    }
}
