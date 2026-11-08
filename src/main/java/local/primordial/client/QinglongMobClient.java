package local.primordial.client;
import local.primordial.Primordial;
import local.primordial.qinglong.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class QinglongMobClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(QinglongContent.JIAO.get(),c->new Renderer<ThunderJiao>(c,"thunder_jiao",true));e.registerEntityRenderer(QinglongContent.DEER.get(),c->new Renderer<VerdantAntlerDeer>(c,"verdant_antler_deer",false));}
    private static final class Renderer<T extends Mob & GeoEntity> extends GeoEntityRenderer<T>{
        Renderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context c,String id,boolean glow){super(c,new DefaultedEntityGeoModel<>(new ResourceLocation(Primordial.ID,id)));shadowRadius=0.7F;if(glow)addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));}
    }
}
