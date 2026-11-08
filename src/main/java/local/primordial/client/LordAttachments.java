package local.primordial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import local.primordial.Primordial;
import local.primordial.boss.SinBoss;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * A Sin Lord's weapon and mount, models of their own (tools/art/SinLords.java), drawn where its "weapon" and "mount"
 * bones are: their place is noted as the lord's model is drawn, and they are drawn after it, each with its own texture
 * (stone on a statue) and glow. Envy's dagger is put away while it wields a weapon it has copied (drawn at "right_item").
 * Greed's mount is moved here, its legs in a gait as its rider goes, its head and tail swaying.
 */
public class LordAttachments<T extends SinBoss> extends GeoRenderLayer<T> {
    record Part(String bone,String model) {}
    static final Map<String,List<Part>> PARTS=Map.of(
        "envy_lord",List.of(new Part("weapon","envy_dagger")),
        "greed_lord",List.of(new Part("mount","greed_mount"),new Part("weapon","greed_lance")),
        "lust_lord",List.of(new Part("weapon","lust_sceptre")),
        "gluttony_lord",List.of(new Part("weapon","gluttony_lance")),
        "sloth_lord",List.of(new Part("weapon","sloth_greatsword")));
    /** where each attachment's bone was as the lord was drawn, this frame */
    private final Map<String,Matrix4f> poses=new HashMap<>();
    private final Map<String,Matrix3f> normals=new HashMap<>();
    public LordAttachments(GeoRenderer<T> renderer) {super(renderer);}

    @Override public void renderForBone(PoseStack pose,T lord,GeoBone bone,RenderType type,MultiBufferSource buffers,VertexConsumer buffer,float partialTick,int light,int overlay) {
        var parts=PARTS.get(lord.bossId());if(parts==null)return;
        for(var part:parts)if(part.bone().equals(bone.getName())){poses.put(part.bone(),new Matrix4f(pose.last().pose()));normals.put(part.bone(),new Matrix3f(pose.last().normal()));}
    }
    @Override public void render(PoseStack pose,T lord,BakedGeoModel model,RenderType type,MultiBufferSource buffers,VertexConsumer buffer,float partialTick,int light,int overlay) {
        var parts=PARTS.get(lord.bossId());if(parts==null){poses.clear();return;}
        for(var part:parts){
            var at=poses.get(part.bone());if(at==null)continue;
            if(part.model().equals("envy_dagger")&&!lord.getMainHandItem().isEmpty())continue;
            var baked=GeckoLibCache.getBakedModels().get(new ResourceLocation(Primordial.ID,"geo/entity/"+part.model()+".geo.json"));
            if(baked==null)continue;
            if(part.model().equals("greed_mount"))gait(baked,lord,partialTick);
            pose.pushPose();
            pose.last().pose().set(at);pose.last().normal().set(normals.get(part.bone()));
            var texture=new ResourceLocation(Primordial.ID,"textures/entity/"+part.model()+(lord.statue?"_statue":"")+".png");
            draw(pose,lord,baked,RenderType.entityCutoutNoCull(texture),buffers,partialTick,light,overlay);
            if(!lord.statue)draw(pose,lord,baked,RenderType.eyes(new ResourceLocation(Primordial.ID,"textures/entity/"+part.model()+"_glowmask.png")),buffers,partialTick,0xF000F0,overlay);
            pose.popPose();
        }
        poses.clear();normals.clear();
    }
    private void draw(PoseStack pose,T lord,BakedGeoModel baked,RenderType type,MultiBufferSource buffers,float partialTick,int light,int overlay) {
        var consumer=buffers.getBuffer(type);
        for(var bone:baked.topLevelBones())getRenderer().renderRecursively(pose,lord,bone,type,buffers,consumer,true,partialTick,light,overlay,1,1,1,1);
    }
    /** the mount's legs swing in diagonal pairs with its rider's stride; it breathes and swishes its tail standing still */
    static void gait(BakedGeoModel mount,SinBoss lord,float partialTick) {
        float stride=lord.walkAnimation.position(partialTick),speed=Math.min(1,lord.walkAnimation.speed(partialTick)*1.6F);
        float time=lord.tickCount+partialTick,swing=Mth.sin(stride*0.7F)*0.9F*speed;
        if(lord.statue||lord.dormant()){swing=0;speed=0;}
        set(mount,"mount_front_right",swing,0);set(mount,"mount_back_left",swing,0);
        set(mount,"mount_front_left",-swing,0);set(mount,"mount_back_right",-swing,0);
        for(String leg:new String[]{"front_right","front_left","back_right","back_left"}){
            float upper=mount.getBone("mount_"+leg).map(GeoBone::getRotX).orElse(0F);
            set(mount,"mount_"+leg+"_low",Math.max(0,-upper)*0.8F+0.1F*speed,0);
        }
        float breathe=lord.dormant()||lord.statue?0:Mth.sin(time*0.08F)*0.04F;
        set(mount,"mount_neck",(float)Math.toRadians(-20)+breathe+Mth.sin(stride*1.4F)*0.12F*speed,0);
        set(mount,"mount_head",lord.dormant()?0.5F:Mth.sin(time*0.05F)*0.05F,lord.dormant()?0:Mth.sin(time*0.03F)*0.12F);
        set(mount,"mount_jaw",lord.dormant()?0:Math.max(0,Mth.sin(time*0.06F))*0.15F+speed*0.25F,0);
        set(mount,"mount_tail",(float)Math.toRadians(30)+Mth.sin(time*0.1F)*0.1F,Mth.sin(time*0.13F)*0.35F+Mth.sin(stride)*0.3F*speed);
        float lift=lord.dormant()?-6:Math.abs(Mth.sin(stride*0.7F))*1.2F*speed;
        mount.getBone("mount_body").ifPresent(body->body.setPosY(lift));
    }
    private static void set(BakedGeoModel model,String bone,float x,float y) {
        // GeckoLib's bones turn the other way to Bedrock's on x and y
        model.getBone(bone).ifPresent(b->{b.setRotX(-x);b.setRotY(-y);});
    }
}
