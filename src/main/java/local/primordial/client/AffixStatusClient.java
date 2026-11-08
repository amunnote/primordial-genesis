package local.primordial.client;

import local.primordial.Primordial;
import local.primordial.affix.AffixStatuses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Use Minecraft's built-in desaturation post effect for the independent Lost Color status. */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class AffixStatusClient {
    private static final ResourceLocation SHADER=new ResourceLocation("minecraft","shaders/post/desaturate.json");
    private static PostChain ourEffect;
    private AffixStatusClient() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var minecraft=Minecraft.getInstance();
        if(minecraft.player==null)AffixStatuses.clientSync(0,0,0,0,0,0,0,0,0,0,0,0);
        else AffixStatuses.clientTick();
        boolean active=minecraft.player!=null&&AffixStatuses.desaturation(minecraft.player)>0;
        var renderer=minecraft.gameRenderer;
        if(active){
            if(ourEffect==null){renderer.loadEffect(SHADER);ourEffect=renderer.currentEffect();}
        }else if(ourEffect!=null){
            if(renderer.currentEffect()==ourEffect)renderer.shutdownEffect();
            ourEffect=null;
        }
    }
}
