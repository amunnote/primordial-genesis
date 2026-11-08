package local.primordial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import local.primordial.Primordial;
import local.primordial.altar.AlchemyAltarBlock;
import local.primordial.altar.AltarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Items on an altar orbit the table, spin and bob. The primordial altar also spins its star frame and floats its corner stars. */
public final class AltarRenderer implements BlockEntityRenderer<AltarBlockEntity> {
    static final ResourceLocation PRIMORDIAL_FRAME=new ResourceLocation(Primordial.ID,"block/primordial_altar_frame");
    static final ResourceLocation PRIMORDIAL_STAR=new ResourceLocation(Primordial.ID,"block/primordial_altar_star");
    private static final double[][] STAR_CORNERS={{3,3},{13,3},{3,13},{13,13}};
    private final ItemRenderer items;
    public AltarRenderer(BlockEntityRendererProvider.Context context) {items=context.getItemRenderer();}

    @Override public void render(AltarBlockEntity altar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(altar.getLevel()==null)return;
        float time=altar.getLevel().getGameTime()+partial;
        var block=altar.getBlockState().getBlock() instanceof AlchemyAltarBlock b?b:null;
        if(block!=null&&block.tier()>=4)renderPrimordial(time,pose,buffers,light);

        var stacks=altar.items();
        if(stacks.isEmpty())return;
        float tableTop=block!=null?block.tableTop():13/16F;
        int lightAbove=LevelRenderer.getLightColor(altar.getLevel(),altar.getBlockPos().above());
        int seed=(int)altar.getBlockPos().asLong();
        int main=altar.mainIndex();
        if(main>=0){
            // the main material floats in the middle; everything else circles it, rising and falling in a wave
            float centreY=tableTop+0.45F+(float)Math.sin(time*0.05)*0.04F;
            drawItem(stacks.get(main),0.5,centreY,0.5,time*1.5F,1.1F,lightAbove,seed+main,altar,pose,buffers);
            int m=stacks.size()-1;
            double radius=Math.min(0.42,0.26+0.012*m);
            for(int i=0,k=0;i<stacks.size();i++){
                if(i==main)continue;
                double angle=Math.toRadians(time*2.0+k*360.0/m);
                double y=centreY+Math.sin(time*0.1+k*2*Math.PI/m)*0.14;
                drawItem(stacks.get(i),0.5+Math.cos(angle)*radius,y-0.1,0.5+Math.sin(angle)*radius,time*3F+k*45F,0.65F,lightAbove,seed+i,altar,pose,buffers);
                k++;
            }
            return;
        }
        // nothing recognised as a main material yet: everything circles the table
        int n=stacks.size();
        double radius=n==1?0:Math.min(0.42,0.18+0.02*n);
        for(int i=0;i<n;i++){
            double angle=Math.toRadians(time*2.0+i*360.0/n);
            float bob=(float)Math.sin(time*0.08+i*1.3)*0.04F;
            drawItem(stacks.get(i),0.5+Math.cos(angle)*radius,tableTop+0.25+bob,0.5+Math.sin(angle)*radius,time*3F+i*45F,0.8F,lightAbove,seed+i,altar,pose,buffers);
        }
    }
    private void drawItem(ItemStack stack,double x,double y,double z,float spin,float scale,int light,int seed,
                          AltarBlockEntity altar,PoseStack pose,MultiBufferSource buffers) {
        pose.pushPose();
        pose.translate(x,y,z);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(scale,scale,scale);
        items.renderStatic(stack,ItemDisplayContext.GROUND,light,OverlayTexture.NO_OVERLAY,pose,buffers,altar.getLevel(),seed);
        pose.popPose();
    }

    /** star frame turns slowly about the core; each corner star bobs and spins on its own phase, at full brightness */
    private static void renderPrimordial(float time,PoseStack pose,MultiBufferSource buffers,int light) {
        var models=Minecraft.getInstance().getModelManager();
        BakedModel frame=models.getModel(PRIMORDIAL_FRAME),star=models.getModel(PRIMORDIAL_STAR);
        pose.pushPose();
        pose.translate(0.5,0,0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time*0.8F));
        pose.translate(-0.5,0,-0.5);
        draw(frame,pose,buffers,light);
        pose.popPose();
        for(int i=0;i<STAR_CORNERS.length;i++){
            double[] c=STAR_CORNERS[i];
            float bob=(float)Math.sin(time*0.06+i*Math.PI/2)*0.06F;
            pose.pushPose();
            // star model is centred on (8,8,8); place its centre at the corner, 13.5 px up
            pose.translate(c[0]/16.0,13.5/16.0-0.5+bob,c[1]/16.0);
            pose.mulPose(Axis.YP.rotationDegrees(time*2.5F+i*30F));
            pose.translate(-0.5,0,-0.5);
            draw(star,pose,buffers,LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
    private static void draw(BakedModel model,PoseStack pose,MultiBufferSource buffers,int light) {
        var consumer=buffers.getBuffer(Sheets.cutoutBlockSheet());
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),consumer,null,model,1F,1F,1F,
            light,OverlayTexture.NO_OVERLAY,ModelData.EMPTY,Sheets.cutoutBlockSheet());
    }

    /**
     * Class and method names here are deliberately unique in the mod: the event bus caches its generated listener
     * classes by simple name, so two nested "Registration" classes with a same-named handler for the same event
     * (as GemSwordClient once had) end up calling one handler twice and the other never.
     */
    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class AltarClientRegistration {
        @SubscribeEvent public static void registerAltarRenderer(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Primordial.BASIC_ALCHEMY_ALTAR_ENTITY.get(),AltarRenderer::new);
        }
        @SubscribeEvent public static void registerAltarModels(ModelEvent.RegisterAdditional event) {
            event.register(PRIMORDIAL_FRAME);
            event.register(PRIMORDIAL_STAR);
        }
    }
}
