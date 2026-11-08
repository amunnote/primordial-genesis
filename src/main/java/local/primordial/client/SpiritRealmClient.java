package local.primordial.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import local.primordial.Primordial;
import local.primordial.affix.BossAffix;
import local.primordial.mixin.SpiritCompositeStateAccessor;
import local.primordial.mixin.SpiritCompositeTypeAccessor;
import local.primordial.mixin.SpiritTextureShardAccessor;
import local.primordial.spirit.SpiritRealm;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * How the Spirit Realm looks: no day or night, a sky that flows through the seven colours of the seven sins with
 * ribbons of them drifting overhead, and every creature drawn see-through.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class SpiritRealmClient {
    /** how see-through spirits are */
    public static final float SPIRIT_ALPHA=0.3F;
    /** how far a spirit's colours are washed toward a ghostly pale blue */
    static final float SPIRIT_TINT=0.3F;
    static final float TINT_R=0.72F,TINT_G=0.88F,TINT_B=1F;
    /** seconds each sin's colour holds the sky */
    static final double COLOUR_SECONDS=10;
    private SpiritRealmClient() {}

    static int sin(int i) {var all=BossAffix.values();return all[Math.floorMod(i,all.length)].color();}
    static int mix(int a,int b,double t) {
        return (int)Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t)<<16|(int)Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t)<<8|(int)Math.round((a&255)*(1-t)+(b&255)*t);
    }
    /** the sky's colour now: one sin fading smoothly into the next */
    static int skyColour(double seconds) {
        double phase=seconds/COLOUR_SECONDS;int i=(int)Math.floor(phase);double t=phase-i;
        t=t*t*(3-2*t);
        return mix(sin(i),sin(i+1),t);
    }
    static double seconds() {return Util.getMillis()/1000.0;}

    // ------------------------------------------------------------------ spirits drawn see-through
    public static MultiBufferSource spiritBuffers(Entity entity,MultiBufferSource buffers) {
        if(!(entity instanceof LivingEntity)||entity instanceof Player||entity instanceof local.primordial.boss.SinEntity
            ||entity.level().dimension()!=SpiritRealm.LEVEL)return buffers;
        return type->{
            ResourceLocation texture=fadable(type)?texture(type):null;
            if(texture==null)return buffers.getBuffer(type);
            return new Faded(buffers.getBuffer(RenderType.entityTranslucent(texture)),SPIRIT_ALPHA);
        };
    }
    /**
     * Only the body itself fades: the entity_* model types, which share entityTranslucent's vertex format. Name tags
     * (text, which fills no overlay or normal: redrawn as an entity it crashes the game), glowing eyes and magic circles
     * (additive) and everything else pass through untouched.
     */
    static boolean fadable(RenderType type) {
        return type.format()==com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY&&type.toString().startsWith("RenderType[entity_");
    }
    static ResourceLocation texture(RenderType type) {
        if(!(type instanceof SpiritCompositeTypeAccessor composite))return null;
        var shard=((SpiritCompositeStateAccessor)(Object)composite.angel$state()).angel$texture();
        return shard==null?null:((SpiritTextureShardAccessor)shard).angel$cutoutTexture().orElse(null);
    }
    static int tint(int c,float to) {return (int)(c*(1-SPIRIT_TINT)+255*to*SPIRIT_TINT);}
    static float tint(float c,float to) {return c*(1-SPIRIT_TINT)+to*SPIRIT_TINT;}
    /** passes every vertex on washed pale blue, with its alpha scaled down */
    record Faded(VertexConsumer to,float alpha) implements VertexConsumer {
        @Override public VertexConsumer vertex(double x,double y,double z) {to.vertex(x,y,z);return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a) {to.color(tint(r,TINT_R),tint(g,TINT_G),tint(b,TINT_B),(int)(a*alpha));return this;}
        @Override public VertexConsumer uv(float u,float v) {to.uv(u,v);return this;}
        @Override public VertexConsumer overlayCoords(int u,int v) {to.overlayCoords(u,v);return this;}
        @Override public VertexConsumer uv2(int u,int v) {to.uv2(u,v);return this;}
        @Override public VertexConsumer normal(float x,float y,float z) {to.normal(x,y,z);return this;}
        @Override public void endVertex() {to.endVertex();}
        @Override public void vertex(float x,float y,float z,float r,float g,float b,float a,float u,float v,int overlay,int light,float nx,float ny,float nz) {
            to.vertex(x,y,z,tint(r,TINT_R),tint(g,TINT_G),tint(b,TINT_B),a*alpha,u,v,overlay,light,nx,ny,nz);
        }
        @Override public void defaultColor(int r,int g,int b,int a) {to.defaultColor(tint(r,TINT_R),tint(g,TINT_G),tint(b,TINT_B),(int)(a*alpha));}
        @Override public void unsetDefaultColor() {to.unsetDefaultColor();}
    }

    // ------------------------------------------------------------------ the sky
    /** the night the aurora hangs in */
    static final int NIGHT=0x060914;
    /** the sky itself: deep night, only tinted by the sin of the moment, so the aurora can glow against it */
    static int skyBase(double seconds) {return mix(NIGHT,skyColour(seconds),0.22);}
    static int lighten(int c,double t) {return mix(c,0xFFFFFF,t);}

    @SubscribeEvent public static void spiritFog(ViewportEvent.ComputeFogColor event) {
        var level=Minecraft.getInstance().level;
        if(level==null||level.dimension()!=SpiritRealm.LEVEL)return;
        int c=skyBase(seconds());
        event.setRed(((c>>16)&255)/255F);event.setGreen(((c>>8)&255)/255F);event.setBlue((c&255)/255F);
    }

    static final class Sky extends DimensionSpecialEffects {
        Sky() {super(Float.NaN,false,SkyType.NONE,false,false);}
        @Override public Vec3 getBrightnessDependentFogColor(Vec3 fog,float brightness) {
            int c=skyBase(seconds());return new Vec3(((c>>16)&255)/255.0,((c>>8)&255)/255.0,(c&255)/255.0);
        }
        @Override public boolean isFoggyAt(int x,int z) {return false;}
        static final int STARS=360;
        static final float[][] STAR=new float[STARS][];
        static {
            var random=new java.util.Random(0x5EA1L);
            for(int i=0;i<STARS;i++){
                double azimuth=random.nextDouble()*Math.PI*2,elevation=Math.asin(random.nextDouble()*1.05-0.05);
                STAR[i]=new float[]{(float)azimuth,(float)elevation,0.18F+random.nextFloat()*0.35F,random.nextFloat()*6.28F,0.6F+random.nextFloat()*2F};
            }
        }
        /**
         * An aurora sky: a dark dome with twinkling stars, a horizon glowing in the current sin's colour, and curtains of
         * light whose bright lower hems ripple and fold while their colour climbs from one sin into the next.
         */
        @Override public boolean renderSky(ClientLevel level,int ticks,float partialTick,PoseStack pose,Camera camera,Matrix4f projection,boolean foggy,Runnable setupFog) {
            double time=seconds();
            // the sky hangs beyond the fog; draw it clear of it, glowing (additive), then put the fog back
            net.minecraft.client.renderer.FogRenderer.setupNoFog();
            RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.disableCull();
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var builder=Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            Matrix4f m=pose.last().pose();
            horizon(builder,m,time);
            stars(builder,m,time);
            for(int curtain=0;curtain<5;curtain++)curtain(builder,m,time,curtain);
            BufferUploader.drawWithShader(builder.end());
            RenderSystem.defaultBlendFunc();RenderSystem.enableCull();RenderSystem.disableBlend();RenderSystem.depthMask(true);
            setupFog.run();
            return true;
        }
        /** a soft band of the current colour along the horizon */
        private static void horizon(com.mojang.blaze3d.vertex.BufferBuilder builder,Matrix4f m,double time) {
            int c=skyColour(time);
            int segments=48;
            for(int s=0;s<segments;s++){
                double a0=Math.PI*2*s/segments,a1=Math.PI*2*(s+1)/segments;
                quad(builder,m,a0,a1,-0.35,-0.02,110,110,c,c,0.10F,0.10F,0.30F,0.30F);
                quad(builder,m,a0,a1,-0.02,0.5,110,110,c,c,0.30F,0.30F,0,0);
            }
        }
        private static void stars(com.mojang.blaze3d.vertex.BufferBuilder builder,Matrix4f m,double time) {
            for(var star:STAR){
                float twinkle=(float)(0.45+0.4*Math.sin(time*star[4]+star[3]));
                float a=star[0],e=star[1],size=star[2]*0.01F;
                float glow=twinkle*Math.min(1,(e+0.05F)*6);
                vertex(builder,m,105,a-size,e-size,1,0.97F,0.92F,glow);vertex(builder,m,105,a-size,e+size,1,0.97F,0.92F,glow);
                vertex(builder,m,105,a+size,e+size,1,0.97F,0.92F,glow);vertex(builder,m,105,a+size,e-size,1,0.97F,0.92F,glow);
            }
        }
        /**
         * One curtain of aurora: a long wavering line across the sky, its radius folding in and out, hung with light
         * that is brightest just above its hem, striped by flickering rays, and fades out as it rises.
         */
        private static void curtain(com.mojang.blaze3d.vertex.BufferBuilder builder,Matrix4f m,double time,int k) {
            int segments=90;double span=1.9;
            double start=k*Math.PI*2/5+time*0.012*(k%2==0?1:-1)+0.4*Math.sin(time*0.05+k);
            // the hem takes the sin of the moment, the crown the next one: the colours climb the curtain as they turn
            double shift=k*COLOUR_SECONDS*1.4;
            int hem=lighten(skyColour(time+shift),0.25),crown=skyColour(time+shift+COLOUR_SECONDS);
            double[] prev=null;
            for(int s=0;s<=segments;s++){
                double u=s/(double)segments;
                double azimuth=start+span*u;
                double base=0.24+0.07*Math.sin(s*0.19+time*0.35+k*1.7)+0.035*Math.sin(s*0.47-time*0.8+k);
                double radius=100*(1+0.16*Math.sin(s*0.11+time*0.22+k*1.3)+0.05*Math.sin(s*0.37-time*0.5));
                double height=0.30+0.10*Math.sin(s*0.08+time*0.18+k*2.1);
                double rays=0.55+0.45*Math.sin(s*1.7+time*1.9+k*3)*Math.sin(s*0.43-time*0.7+k);
                double fade=Math.pow(Math.sin(Math.PI*u),0.8);
                double[] here={azimuth,base,radius,height,0.8*fade*rays};
                if(prev!=null){
                    float a0=(float)prev[4],a1=(float)here[4];
                    // just under the hem a faint glow, then the bright hem, then the curtain rising into the next colour
                    quad(builder,m,prev[0],here[0],prev[1]-0.02,prev[1],prev[2],here[2],here[1]-0.02,here[1],hem,hem,0,0,a0*0.5F,a1*0.5F);
                    quad(builder,m,prev[0],here[0],prev[1],prev[1]+0.035,prev[2],here[2],here[1],here[1]+0.035,hem,hem,a0*0.5F,a1*0.5F,a0,a1);
                    quad(builder,m,prev[0],here[0],prev[1]+0.035,prev[1]+prev[3],prev[2],here[2],here[1]+0.035,here[1]+here[3],hem,crown,a0,a1,0,0);
                }
                prev=here;
            }
        }
        /** a band between two azimuths, bottom and top elevations given, on a sphere of one radius */
        private static void quad(com.mojang.blaze3d.vertex.BufferBuilder builder,Matrix4f m,double a0,double a1,double bottom,double top,double r0,double r1,int lower,int upper,float al0,float al1,float au0,float au1) {
            quad(builder,m,a0,a1,bottom,top,r0,r1,bottom,top,lower,upper,al0,al1,au0,au1);
        }
        /** a band whose two edges stand at their own elevations and radii */
        private static void quad(com.mojang.blaze3d.vertex.BufferBuilder builder,Matrix4f m,double a0,double a1,double bottom0,double top0,double r0,double r1,double bottom1,double top1,
                                 int lower,int upper,float al0,float al1,float au0,float au1) {
            float lr=((lower>>16)&255)/255F,lg=((lower>>8)&255)/255F,lb=(lower&255)/255F;
            float ur=((upper>>16)&255)/255F,ug=((upper>>8)&255)/255F,ub=(upper&255)/255F;
            vertex(builder,m,(float)r0,a0,bottom0,lr,lg,lb,al0);vertex(builder,m,(float)r0,a0,top0,ur,ug,ub,au0);
            vertex(builder,m,(float)r1,a1,top1,ur,ug,ub,au1);vertex(builder,m,(float)r1,a1,bottom1,lr,lg,lb,al1);
        }
        private static void vertex(com.mojang.blaze3d.vertex.BufferBuilder builder,Matrix4f m,float radius,double azimuth,double elevation,float r,float g,float b,float a) {
            float x=(float)(Math.cos(elevation)*Math.cos(azimuth)*radius),y=(float)(Math.sin(elevation)*radius),z=(float)(Math.cos(elevation)*Math.sin(azimuth)*radius);
            builder.vertex(m,x,y,z).color(r,g,b,a).endVertex();
        }
    }

    // ------------------------------------------------------------------ particles
    /**
     * A soft point of light that never takes the world's shade. Motes drift slowly in the realm's air in the seven
     * sins' colours, twinkling; wisps rise pale off the spirits, like breath.
     */
    static final class Glow extends net.minecraft.client.particle.TextureSheetParticle {
        final net.minecraft.client.particle.SpriteSet sprites;final float peak,size;final int twinkle;
        Glow(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,net.minecraft.client.particle.SpriteSet sprites,int colour,int life,float size,float peak) {
            super(level,x,y,z);
            this.sprites=sprites;this.peak=peak;this.size=size;this.twinkle=random.nextInt(1000);
            xd=vx;yd=vy;zd=vz;lifetime=life;gravity=0;friction=0.985F;hasPhysics=false;quadSize=size;
            rCol=((colour>>16)&255)/255F;gCol=((colour>>8)&255)/255F;bCol=(colour&255)/255F;alpha=0;
            pickSprite(sprites);
        }
        @Override public void tick() {
            super.tick();
            if(removed)return;
            float t=age/(float)lifetime;
            float shimmer=0.75F+0.25F*(float)Math.sin((age+twinkle)*0.35);
            alpha=peak*Math.min(1,t*5)*Math.min(1,(1-t)*3)*shimmer;
            quadSize=size*(0.8F+0.2F*shimmer);
            if((age+twinkle)%6==0)pickSprite(sprites);
        }
        @Override public net.minecraft.client.particle.ParticleRenderType getRenderType() {return net.minecraft.client.particle.ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
        @Override protected int getLightColor(float partialTick) {return 0xF000F0;}
    }
    static net.minecraft.client.particle.ParticleProvider<net.minecraft.core.particles.SimpleParticleType> mote(net.minecraft.client.particle.SpriteSet sprites) {
        return (type,level,x,y,z,vx,vy,vz)->{
            var random=level.random;
            int colour=lighten(sin(random.nextInt(7)),0.35);
            return new Glow(level,x,y,z,vx+(random.nextDouble()-0.5)*0.01,vy+0.004+random.nextDouble()*0.01,vz+(random.nextDouble()-0.5)*0.01,
                sprites,colour,80+random.nextInt(100),0.05F+random.nextFloat()*0.07F,0.9F);
        };
    }
    static net.minecraft.client.particle.ParticleProvider<net.minecraft.core.particles.SimpleParticleType> wisp(net.minecraft.client.particle.SpriteSet sprites) {
        return (type,level,x,y,z,vx,vy,vz)->{
            var random=level.random;
            return new Glow(level,x,y,z,vx+(random.nextDouble()-0.5)*0.01,vy+0.02+random.nextDouble()*0.02,vz+(random.nextDouble()-0.5)*0.01,
                sprites,0xD8ECFF,25+random.nextInt(20),0.035F+random.nextFloat()*0.035F,0.5F);
        };
    }
    /** the realm's air is never empty: motes drift around the player, and every spirit sheds a few wisps */
    @SubscribeEvent public static void spiritAir(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||mc.isPaused()||mc.level==null||mc.player==null||mc.level.dimension()!=SpiritRealm.LEVEL)return;
        var level=mc.level;var random=level.random;var player=mc.player;
        var pos=new net.minecraft.core.BlockPos.MutableBlockPos();
        for(int i=0;i<5;i++){
            double x=player.getX()+(random.nextDouble()-0.5)*40,y=player.getY()+(random.nextDouble()-0.4)*20,z=player.getZ()+(random.nextDouble()-0.5)*40;
            if(level.getBlockState(pos.set(x,y,z)).isAir())level.addParticle(SpiritRealm.SPIRIT_MOTE.get(),x,y,z,0,0,0);
        }
        for(var entity:level.entitiesForRendering()){
            if(!(entity instanceof LivingEntity)||entity instanceof Player||entity.distanceToSqr(player)>48*48||random.nextInt(5)!=0)continue;
            var box=entity.getBoundingBox();
            level.addParticle(SpiritRealm.SPIRIT_WISP.get(),box.minX+random.nextDouble()*box.getXsize(),box.minY+random.nextDouble()*box.getYsize(),box.minZ+random.nextDouble()*box.getZsize(),0,0,0);
        }
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class SpiritRealmClientRegistration {
        @SubscribeEvent public static void registerSpiritSky(RegisterDimensionSpecialEffectsEvent event) {
            event.register(new ResourceLocation(Primordial.ID,"spirit_realm"),new Sky());
        }
        @SubscribeEvent public static void registerSpiritParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(SpiritRealm.SPIRIT_MOTE.get(),SpiritRealmClient::mote);
            event.registerSpriteSet(SpiritRealm.SPIRIT_WISP.get(),SpiritRealmClient::wisp);
        }
    }
}
