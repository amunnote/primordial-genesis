package local.primordial.client;
import local.primordial.Primordial;
import local.primordial.zhuque.*;
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
public final class ZhuqueMobClient {
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ZhuqueContent.LEAVES.get(),net.minecraft.client.renderer.RenderType.cutoutMipped()));}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(ZhuqueContent.CROW.get(),c->new Renderer<EmberFeatherCrow>(c,"ember_feather_crow",true));e.registerEntityRenderer(ZhuqueContent.RAT.get(),c->new Renderer<EmberTailRat>(c,"ember_tail_rat",true));}
    private static final class Renderer<T extends Mob & GeoEntity> extends GeoEntityRenderer<T>{
        Renderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context c,String id,boolean glow){super(c,new DefaultedEntityGeoModel<>(new ResourceLocation(Primordial.ID,id)));shadowRadius=0.7F;if(glow)addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));}
    }
}
