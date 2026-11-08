package local.primordial.client;

import local.primordial.Primordial;
import local.primordial.fauna.FaunaContent;
import local.primordial.fauna.WhistlingArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The guardians' creatures drawn with their glowing parts lit, and the things they leave thrown and shot. */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class FaunaClient {
    private FaunaClient() {}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FaunaContent.BIFANG.get(),c->new Creature<>(c,"bifang",0.6F));
        event.registerEntityRenderer(FaunaContent.FIREFLY.get(),c->new Creature<>(c,"thunder_firefly",0.1F));
        event.registerEntityRenderer(FaunaContent.SPARROW.get(),c->new Creature<>(c,"golden_bell_sparrow",0.2F));
        event.registerEntityRenderer(FaunaContent.LAMPFISH.get(),c->new Creature<>(c,"netherlamp_fish",0.25F));
        event.registerEntityRenderer(FaunaContent.THROWN_CRYSTAL.get(),ThrownItemRenderer::new);
        event.registerEntityRenderer(FaunaContent.THROWN_BOTTLE.get(),ThrownItemRenderer::new);
        event.registerEntityRenderer(FaunaContent.WHISTLING_ARROW.get(),WhistlingArrowRenderer::new);
    }
    private static final class Creature<T extends Mob&GeoEntity> extends GeoEntityRenderer<T> {
        Creature(EntityRendererProvider.Context context,String id,float shadow) {
            super(context,new DefaultedEntityGeoModel<>(new ResourceLocation(Primordial.ID,id)));
            shadowRadius=shadow;addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }
    private static final class WhistlingArrowRenderer extends ArrowRenderer<WhistlingArrow> {
        // in flight it is drawn as an arrow; its own look is in hand and inventory
        private static final ResourceLocation TEXTURE=new ResourceLocation("textures/entity/projectiles/arrow.png");
        WhistlingArrowRenderer(EntityRendererProvider.Context context) {super(context);}
        @Override public ResourceLocation getTextureLocation(WhistlingArrow arrow) {return TEXTURE;}
    }
}
