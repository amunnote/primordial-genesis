package local.primordial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import local.primordial.entity.GemLightning;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/** The vanilla lightning shape, tinted with a hue that runs up the bolt. */
public final class GemLightningRenderer extends EntityRenderer<GemLightning> {
    public GemLightningRenderer(EntityRendererProvider.Context context) {super(context);}
    @Override public void render(GemLightning bolt,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        float[] offsetX=new float[8],offsetZ=new float[8];
        float x=0,z=0;
        RandomSource shape=RandomSource.create(bolt.seed);
        for(int i=7;i>=0;i--) {
            offsetX[i]=x;offsetZ[i]=z;
            x+=shape.nextInt(11)-5;z+=shape.nextInt(11)-5;
        }
        float baseHue=(bolt.seed&1023L)/1024.0F;
        VertexConsumer consumer=buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix=pose.last().pose();
        for(int layer=0;layer<4;layer++) {
            RandomSource random=RandomSource.create(bolt.seed);
            for(int branch=0;branch<3;branch++) {
                int top=7,bottom=0;
                if(branch>0) {top=7-branch;bottom=top-2;}
                float x1=offsetX[top]-x,z1=offsetZ[top]-z;
                for(int segment=top;segment>=bottom;segment--) {
                    float x2=x1,z2=z1;
                    if(branch==0) {x1+=random.nextInt(11)-5;z1+=random.nextInt(11)-5;}
                    else {x1+=random.nextInt(31)-15;z1+=random.nextInt(31)-15;}
                    float outer=0.1F+layer*0.2F,inner=0.1F+layer*0.2F;
                    if(branch==0) {outer*=segment*0.1F+1.0F;inner*=(segment-1.0F)*0.1F+1.0F;}
                    int rgb=Mth.hsvToRgb((baseHue+segment*0.12F+branch*0.07F)%1.0F,0.85F,1.0F);
                    float r=(rgb>>16&255)/255.0F,g=(rgb>>8&255)/255.0F,b=(rgb&255)/255.0F;
                    quad(matrix,consumer,x1,z1,segment,x2,z2,r,g,b,outer,inner,false,false,true,false);
                    quad(matrix,consumer,x1,z1,segment,x2,z2,r,g,b,outer,inner,true,false,true,true);
                    quad(matrix,consumer,x1,z1,segment,x2,z2,r,g,b,outer,inner,true,true,false,true);
                    quad(matrix,consumer,x1,z1,segment,x2,z2,r,g,b,outer,inner,false,true,false,false);
                }
            }
        }
    }
    private static void quad(Matrix4f matrix,VertexConsumer consumer,float x1,float z1,int segment,float x2,float z2,
            float r,float g,float b,float outer,float inner,boolean a,boolean c,boolean d,boolean e) {
        consumer.vertex(matrix,x1+(a?inner:-inner),segment*16,z1+(c?inner:-inner)).color(r,g,b,0.4F).endVertex();
        consumer.vertex(matrix,x2+(a?outer:-outer),(segment+1)*16,z2+(c?outer:-outer)).color(r,g,b,0.4F).endVertex();
        consumer.vertex(matrix,x2+(d?outer:-outer),(segment+1)*16,z2+(e?outer:-outer)).color(r,g,b,0.4F).endVertex();
        consumer.vertex(matrix,x1+(d?inner:-inner),segment*16,z1+(e?inner:-inner)).color(r,g,b,0.4F).endVertex();
    }
    @Override public ResourceLocation getTextureLocation(GemLightning bolt) {return TextureAtlas.LOCATION_BLOCKS;}
}
