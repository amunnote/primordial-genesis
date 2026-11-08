package local.primordial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Map;
import local.primordial.Primordial;
import local.primordial.entity.alchemical.AlchemicalCreation;
import local.primordial.entity.alchemical.ArcaneParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * The alchemical creations' magic, as it shows: each has its own colour, a rune circle turning under its feet (two rings,
 * counter-rotating), the humanoid ones a halo behind the head, and glyphs that rise off the circle and sparks that gather
 * round it. All of it smoulders while the creation idles and blazes up while it channels (hunts, charges, swells, leaps).
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class ArcaneClient {
    static final ResourceLocation CIRCLE=new ResourceLocation(Primordial.ID,"textures/entity/arcane_circle.png");
    static final ResourceLocation INNER=new ResourceLocation(Primordial.ID,"textures/entity/arcane_circle_inner.png");
    static final ResourceLocation HALO=new ResourceLocation(Primordial.ID,"textures/entity/arcane_halo.png");

    /** which wear a halo, and how big */
    static final Map<String,Float> HALOS=Map.of("alchemical_zombie",0.85F,"alchemical_skeleton",0.8F,"alchemical_enderman",1.0F,
        "alchemical_wither_skeleton",0.85F,"alchemical_blaze",0.9F);
    private ArcaneClient() {}

    public static int colour(String id) {return ArcaneParticles.colour(id);}
    static float circleRadius(LivingEntity mob) {return Math.max(0.9F,mob.getBbWidth()*0.75F+0.45F);}
    /** 0..1: how brightly its magic burns now */
    static float glow(LivingEntity mob,float partialTick) {
        float base=mob instanceof AlchemicalCreation creation&&creation.channeling()?1F:0.55F;
        float pulse=0.85F+0.15F*Mth.sin((mob.tickCount+partialTick)*0.15F);
        float dying=mob.deathTime>0?Math.max(0,1-(mob.deathTime+partialTick)/20F):1;
        return base*pulse*dying;
    }

    /** draw the circle under it and its halo; called by its renderer, with the pose at its feet */
    public static void render(LivingEntity mob,String id,float partialTick,PoseStack pose,MultiBufferSource buffers) {
        int c=colour(id);float glow=glow(mob,partialTick);
        if(glow<=0.01F)return;
        float r=((c>>16)&255)/255F*glow,g=((c>>8)&255)/255F*glow,b=(c&255)/255F*glow;
        float time=(mob.tickCount+partialTick)/20F;
        float radius=circleRadius(mob);
        pose.pushPose();
        pose.translate(0,0.03,0);
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(time*25));
        flat(buffers.getBuffer(RenderType.eyes(CIRCLE)),pose,radius,r,g,b);
        pose.popPose();
        pose.pushPose();pose.translate(0,0.01,0);pose.mulPose(Axis.YP.rotationDegrees(-time*40));
        flat(buffers.getBuffer(RenderType.eyes(INNER)),pose,radius*0.62F,r,g,b);
        pose.popPose();
        pose.popPose();
        Float halo=HALOS.get(id);
        if(halo!=null){
            float yaw=Mth.rotLerp(partialTick,mob.yBodyRotO,mob.yBodyRot);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            // turned with its body, +z is its front: the halo hangs just behind its head
            pose.translate(0,mob.getBbHeight()*0.86,-0.32);
            pose.mulPose(Axis.ZP.rotationDegrees(time*30));
            upright(buffers.getBuffer(RenderType.eyes(HALO)),pose,halo*0.5F,r,g,b);
            pose.popPose();
        }
    }
    private static void flat(VertexConsumer buffer,PoseStack pose,float s,float r,float g,float b) {
        Matrix4f m=pose.last().pose();
        vertex(buffer,m,-s,0,-s,r,g,b,0,0);vertex(buffer,m,-s,0,s,r,g,b,0,1);vertex(buffer,m,s,0,s,r,g,b,1,1);vertex(buffer,m,s,0,-s,r,g,b,1,0);
        // and its underside, so it shows from below too
        vertex(buffer,m,s,0,-s,r,g,b,1,0);vertex(buffer,m,s,0,s,r,g,b,1,1);vertex(buffer,m,-s,0,s,r,g,b,0,1);vertex(buffer,m,-s,0,-s,r,g,b,0,0);
    }
    private static void upright(VertexConsumer buffer,PoseStack pose,float s,float r,float g,float b) {
        Matrix4f m=pose.last().pose();
        vertex(buffer,m,-s,-s,0,r,g,b,0,1);vertex(buffer,m,s,-s,0,r,g,b,1,1);vertex(buffer,m,s,s,0,r,g,b,1,0);vertex(buffer,m,-s,s,0,r,g,b,0,0);
        vertex(buffer,m,-s,s,0,r,g,b,0,0);vertex(buffer,m,s,s,0,r,g,b,1,0);vertex(buffer,m,s,-s,0,r,g,b,1,1);vertex(buffer,m,-s,-s,0,r,g,b,0,1);
    }
    private static void vertex(VertexConsumer buffer,Matrix4f m,float x,float y,float z,float r,float g,float b,float u,float v) {
        buffer.vertex(m,x,y,z).color(r,g,b,1F).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,1,0).endVertex();
    }

    // ------------------------------------------------------------------ particles
    static SpriteSet glyphs,sparks;
    /** a glyph or a spark of one creation's colour, lit by nothing but itself */
    static final class Arcane extends TextureSheetParticle {
        final SpriteSet sprites;final float peak,size;final boolean spin;
        Arcane(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites,int colour,int life,float size,float peak,boolean spin) {
            super(level,x,y,z);
            this.sprites=sprites;this.peak=peak;this.size=size;this.spin=spin;
            xd=vx;yd=vy;zd=vz;lifetime=life;gravity=0;friction=0.96F;hasPhysics=false;quadSize=size;alpha=0;
            rCol=((colour>>16)&255)/255F;gCol=((colour>>8)&255)/255F;bCol=(colour&255)/255F;
            pickSprite(sprites);
            if(spin){roll=random.nextFloat()*6.28F;oRoll=roll;}
        }
        @Override public void tick() {
            super.tick();
            if(removed)return;
            float t=age/(float)lifetime;
            alpha=peak*Math.min(1,t*6)*Math.min(1,(1-t)*2.5F);
            quadSize=size*(spin?1:0.6F+0.4F*Mth.sin(age*0.6F)*Mth.sin(age*0.6F));
            if(spin){oRoll=roll;roll+=0.05F;}
        }
        @Override public ParticleRenderType getRenderType() {return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
        @Override protected int getLightColor(float partialTick) {return 0xF000F0;}
    }
    /** glyphs and sparks sent by the server, in the colour they carry */
    static ParticleProvider<ArcaneParticles.Options> glyph(SpriteSet sprites) {
        glyphs=sprites;
        return (options,level,x,y,z,vx,vy,vz)->new Arcane(level,x,y,z,vx,vy,vz,sprites,lighten(options.colour()),30+level.random.nextInt(20),0.1F+level.random.nextFloat()*0.06F,0.95F,true);
    }
    static ParticleProvider<ArcaneParticles.Options> spark(SpriteSet sprites) {
        sparks=sprites;
        return (options,level,x,y,z,vx,vy,vz)->new Arcane(level,x,y,z,vx,vy,vz,sprites,options.colour(),14+level.random.nextInt(8),0.07F+level.random.nextFloat()*0.04F,1F,false);
    }

    /** glyphs rise off every creation's circle; while it channels they pour off it, and sparks gather in to its heart */
    @SubscribeEvent public static void arcaneAir(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();
        if(event.phase!=TickEvent.Phase.END||mc.isPaused()||mc.level==null||mc.player==null||glyphs==null||sparks==null)return;
        var level=mc.level;var random=level.random;
        for(var entity:level.entitiesForRendering()){
            if(!(entity instanceof Mob mob)||!(entity instanceof AlchemicalCreation creation)||mob.isDeadOrDying()||mob.distanceToSqr(mc.player)>40*40)continue;
            int c=colour(creation.creationId());
            boolean channeling=creation.channeling();
            float radius=circleRadius(mob);
            int glyphCount=channeling?(random.nextInt(3)==0?2:1):(random.nextInt(5)==0?1:0);
            for(int i=0;i<glyphCount;i++){
                double a=random.nextDouble()*Math.PI*2,r=radius*(0.75+random.nextDouble()*0.2);
                mc.particleEngine.add(new Arcane(level,mob.getX()+Math.cos(a)*r,mob.getY()+0.05,mob.getZ()+Math.sin(a)*r,
                    0,0.025+random.nextDouble()*0.03,0,glyphs,ArcaneClient.lighten(c),30+random.nextInt(20),0.09F+random.nextFloat()*0.05F,0.9F,true));
            }
            if(channeling&&random.nextInt(2)==0){
                // a spark drawn in from the edge of its circle to its heart
                double a=random.nextDouble()*Math.PI*2,r=radius*1.1;
                double sx=mob.getX()+Math.cos(a)*r,sy=mob.getY()+random.nextDouble()*mob.getBbHeight(),sz=mob.getZ()+Math.sin(a)*r;
                double tx=mob.getX(),ty=mob.getY(0.55),tz=mob.getZ();
                mc.particleEngine.add(new Arcane(level,sx,sy,sz,(tx-sx)/14,(ty-sy)/14,(tz-sz)/14,sparks,c,14,0.07F,1F,false));
            }
        }
    }
    static int lighten(int c) {
        return (int)(((c>>16)&255)*0.6+255*0.4)<<16|(int)(((c>>8)&255)*0.6+255*0.4)<<8|(int)((c&255)*0.6+255*0.4);
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class ArcaneClientRegistration {
        @SubscribeEvent public static void registerArcaneParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ArcaneParticles.GLYPH.get(),ArcaneClient::glyph);
            event.registerSpriteSet(ArcaneParticles.SPARK.get(),ArcaneClient::spark);
        }
    }
}
