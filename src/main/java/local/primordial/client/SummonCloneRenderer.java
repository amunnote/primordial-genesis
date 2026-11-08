package local.primordial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import local.primordial.Primordial;
import local.primordial.entity.SummonClone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Summon's clones: the vanilla player model wearing the owner's own skin (from the client's player list, or the default
 * skin for that UUID), drawn see-through and a little cold in colour. No texture of its own.
 */
public final class SummonCloneRenderer extends HumanoidMobRenderer<SummonClone,SummonCloneRenderer.Model> {
    public SummonCloneRenderer(EntityRendererProvider.Context context) {super(context,new Model(context.bakeLayer(ModelLayers.PLAYER)),0.3F);}
    @Override public ResourceLocation getTextureLocation(SummonClone clone) {
        var owner=clone.getOwnerUUID();
        if(owner==null)return DefaultPlayerSkin.getDefaultSkin();
        var connection=Minecraft.getInstance().getConnection();
        var info=connection!=null?connection.getPlayerInfo(owner):null;
        return info!=null?info.getSkinLocation():DefaultPlayerSkin.getDefaultSkin(owner);
    }
    @Override protected RenderType getRenderType(SummonClone clone,boolean bodyVisible,boolean translucent,boolean glowing) {
        return RenderType.entityTranslucent(getTextureLocation(clone));
    }
    /** the player model, drawn at less than half its opacity */
    public static final class Model extends PlayerModel<SummonClone> {
        Model(ModelPart root) {super(root,false);}
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,float red,float green,float blue,float alpha) {
            super.renderToBuffer(pose,consumer,light,overlay,red*0.75F,green*0.85F,blue,alpha*0.45F);
        }
    }
    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class SummonCloneClientRegistration { // unique names: see AltarRenderer.AltarClientRegistration
        @SubscribeEvent public static void registerSummonCloneRenderer(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(Primordial.SUMMON_CLONE.get(),SummonCloneRenderer::new);
        }
    }
}
