package local.primordial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import local.primordial.Primordial;
import local.primordial.boss.EnvyShade;
import local.primordial.boss.SinBoss;
import local.primordial.boss.SinBosses;
import local.primordial.boss.SinParticles;
import local.primordial.boss.TreasureWeapon;
import local.primordial.entity.alchemical.ArcaneParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/** How the Sin Lords look: their models, what they hold, the weapons they hurl, and their seven particles. */
public final class SinBossClient {
    private SinBossClient() {}

    /** a lord: its model turns its head, glows where its glow mask is lit, and holds its weapon at "right_item" */
    public static class LordRenderer<T extends SinBoss> extends GeoEntityRenderer<T> {
        final ResourceLocation stone;
        public LordRenderer(EntityRendererProvider.Context context,String id,float shadow) {
            super(context,new DefaultedEntityGeoModel<>(new ResourceLocation(Primordial.ID,id),true));
            stone=new ResourceLocation(Primordial.ID,"textures/entity/"+id+"_statue.png");
            shadowRadius=shadow;
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            addRenderLayer(new LordAttachments<>(this));
            addRenderLayer(new BlockAndItemGeoLayer<>(this,(bone,mob)->bone.getName().equals("right_item")?mob.getMainHandItem():null,(bone,mob)->null) {
                @Override protected ItemDisplayContext getTransformTypeForStack(GeoBone bone,ItemStack stack,T mob) {return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;}
                @Override protected void renderStackForBone(PoseStack pose,GeoBone bone,ItemStack stack,T mob,MultiBufferSource buffers,float partialTick,int light,int overlay) {
                    pose.mulPose(Axis.XP.rotationDegrees(-90));
                    super.renderStackForBone(pose,bone,stack,mob,buffers,partialTick,light,overlay);
                }
            });
        }
        /** a lord's statue wears stone instead of its own colours */
        @Override public ResourceLocation getTextureLocation(T lord) {return lord.statue?stone:super.getTextureLocation(lord);}
    }
    /** a lord's statue wears its stone texture */
    static ResourceLocation statueTexture(GeoEntityRenderer<?> renderer,SinBoss lord,ResourceLocation normal) {
        return lord.statue&&renderer instanceof LordRenderer<?> l?l.stone:normal;
    }
    /**
     * A fallen lord's statue: the lord's own model, turned to stone, standing in its sleeping pose on the pedestal. The
     * figure is a stand-in made on the client only; it settles into its pose and then holds still.
     */
    public static class StatueRenderer implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<local.primordial.boss.SinStatueBlock.Statue> {
        record Figure(SinBoss lord,long born) {}
        private final java.util.Map<local.primordial.boss.SinStatueBlock.Statue,Figure> figures=new java.util.WeakHashMap<>();
        public StatueRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {}
        @Override public void render(local.primordial.boss.SinStatueBlock.Statue statue,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            var level=statue.getLevel();var type=statue.lordType();
            if(level==null||type==null)return;
            var figure=figures.get(statue);
            if(figure==null||figure.lord().getType()!=type){
                var lord=type.create(level);
                if(lord==null)return;
                lord.statue=true;
                figure=new Figure(lord,level.getGameTime());figures.put(statue,figure);
            }
            var lord=figure.lord();var pos=statue.getBlockPos();
            lord.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
            // let it settle into its pose for a moment, then hold still
            lord.tickCount=(int)Math.min(40,level.getGameTime()-figure.born());
            float yaw=statue.yaw();
            lord.setYRot(yaw);lord.yRotO=yaw;lord.yBodyRot=yaw;lord.yBodyRotO=yaw;lord.yHeadRot=yaw;lord.yHeadRotO=yaw;
            pose.pushPose();
            pose.translate(0.5,1,0.5);
            var renderer=net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(lord);
            renderer.render(lord,yaw,lord.tickCount>=40?0:partialTick,pose,buffers,net.minecraft.client.renderer.LevelRenderer.getLightColor(level,pos.above()));
            pose.popPose();
        }
        @Override public boolean shouldRenderOffScreen(local.primordial.boss.SinStatueBlock.Statue statue) {return true;}
        @Override public int getViewDistance() {return 96;}
    }
    /** the Vermilion Bird: while it lies as an egg of fire only the egg shows, and otherwise never the egg */
    public static class ZhuqueRenderer extends LordRenderer<local.primordial.boss.ZhuqueLord> {
        public ZhuqueRenderer(EntityRendererProvider.Context context) {super(context,"zhuque",2.0F*local.primordial.boss.ZhuqueLord.MODEL_SCALE);withScale(local.primordial.boss.ZhuqueLord.MODEL_SCALE);}
        @Override public void preRender(PoseStack pose,local.primordial.boss.ZhuqueLord lord,BakedGeoModel model,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                        boolean reRender,float partialTick,int light,int overlay,float red,float green,float blue,float alpha) {
            boolean egg=lord.egg()&&!lord.statue;
            model.getBone("egg").ifPresent(bone->bone.setHidden(!egg));
            model.getBone("bird").ifPresent(bone->bone.setHidden(egg));
            super.preRender(pose,lord,model,buffers,buffer,reRender,partialTick,light,overlay,red,green,blue,alpha);
        }
    }
    /** the Primordial Lord: its halo is gone once broken, and its echoes are pale and see-through */
    public static class PrimordialRenderer extends LordRenderer<local.primordial.boss.PrimordialLord> {
        public PrimordialRenderer(EntityRendererProvider.Context context) {super(context,"primordial_lord",1.0F);}
        @Override public void preRender(PoseStack pose,local.primordial.boss.PrimordialLord lord,BakedGeoModel model,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                        boolean reRender,float partialTick,int light,int overlay,float red,float green,float blue,float alpha) {
            model.getBone("halo").ifPresent(bone->bone.setHidden(lord.phase()==3&&!lord.statue));
            super.preRender(pose,lord,model,buffers,buffer,reRender,partialTick,light,overlay,red,green,blue,alpha);
        }
        @Override public RenderType getRenderType(local.primordial.boss.PrimordialLord lord,ResourceLocation texture,MultiBufferSource buffers,float partialTick) {
            return lord.echo()?RenderType.entityTranslucent(texture):super.getRenderType(lord,texture,buffers,partialTick);
        }
        @Override public Color getRenderColor(local.primordial.boss.PrimordialLord lord,float partialTick,int light) {
            return lord.echo()?Color.ofRGBA(230,240,255,110):super.getRenderColor(lord,partialTick,light);
        }
    }
    /** a Sin Pillar: a column of its sin's stone, its heart-stone turning on top */
    public static class PillarRenderer extends EntityRenderer<local.primordial.boss.SinPillar> {
        public PillarRenderer(EntityRendererProvider.Context context) {super(context);shadowRadius=0.8F;}
        @Override public void render(local.primordial.boss.SinPillar pillar,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
            var palette=local.primordial.spirit.SinSanctums.palette(pillar.sin());
            var blocks=net.minecraft.client.Minecraft.getInstance().getBlockRenderer();
            float hurt=pillar.hurtTime>0?0.92F:1F;
            pose.pushPose();
            pose.scale(1.6F*hurt,1F,1.6F*hurt);
            pose.translate(-0.5,0,-0.5);
            blocks.renderSingleBlock(palette.rim().defaultBlockState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);
            pose.translate(0,1,0);blocks.renderSingleBlock(palette.pillar().defaultBlockState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);
            pose.translate(0,1,0);blocks.renderSingleBlock(palette.pillar().defaultBlockState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);
            pose.translate(0,1,0);blocks.renderSingleBlock(palette.cap().defaultBlockState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);
            pose.popPose();
            // the sin's heart-stone, turning and bobbing over it, at full light
            float time=pillar.tickCount+partialTick;
            pose.pushPose();
            pose.translate(0,4.6+Math.sin(time*0.08)*0.15,0);
            pose.mulPose(Axis.YP.rotationDegrees(time*3));pose.mulPose(Axis.XP.rotationDegrees(45));pose.mulPose(Axis.ZP.rotationDegrees(35));
            pose.scale(0.7F,0.7F,0.7F);pose.translate(-0.5,-0.5,-0.5);
            blocks.renderSingleBlock(palette.heart().defaultBlockState(),pose,buffers,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.popPose();
            super.render(pillar,yaw,partialTick,pose,buffers,light);
        }
        @Override public ResourceLocation getTextureLocation(local.primordial.boss.SinPillar pillar) {return TextureAtlas.LOCATION_BLOCKS;}
    }
    /** a shade of Envy: its lord's model, half see-through */
    public static class ShadeRenderer extends GeoEntityRenderer<EnvyShade> {
        public ShadeRenderer(EntityRendererProvider.Context context) {
            super(context,new DefaultedEntityGeoModel<>(new ResourceLocation(Primordial.ID,"envy_lord"),true));
            shadowRadius=0.3F;
        }
        @Override public RenderType getRenderType(EnvyShade shade,ResourceLocation texture,MultiBufferSource buffers,float partialTick) {return RenderType.entityTranslucent(texture);}
        @Override public Color getRenderColor(EnvyShade shade,float partialTick,int light) {return Color.ofRGBA(150,255,170,120);}
    }
    /** a hurled weapon: the item itself, point first along its flight, quivering once it has struck */
    public static class TreasureRenderer extends EntityRenderer<TreasureWeapon> {
        private final ItemRenderer items;
        public TreasureRenderer(EntityRendererProvider.Context context) {super(context);items=context.getItemRenderer();}
        @Override public void render(TreasureWeapon weapon,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
            var stack=weapon.weapon();
            if(stack.isEmpty())return;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick,weapon.yRotO,weapon.getYRot())-90));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick,weapon.xRotO,weapon.getXRot())));
            if(weapon.stuck())pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin((weapon.tickCount+partialTick)*1.5F)*3));
            // in the fixed display an item's tip points up and back (-x,+y): turn it onto +x, the flight (checked in game: a sword flying sideways leads with its point)
            pose.mulPose(Axis.ZP.rotationDegrees(-135));
            pose.scale(1.4F,1.4F,1.4F);
            items.renderStatic(stack,ItemDisplayContext.FIXED,0xF000F0,OverlayTexture.NO_OVERLAY,pose,buffers,weapon.level(),weapon.getId());
            pose.popPose();
            super.render(weapon,yaw,partialTick,pose,buffers,light);
        }
        @SuppressWarnings("deprecation")
        @Override public ResourceLocation getTextureLocation(TreasureWeapon weapon) {return TextureAtlas.LOCATION_BLOCKS;}
    }

    /**
     * A gate of the King's Treasury: a golden ripple spreading outward in three rings, a core of light, two rune rings
     * turning against each other, and the weapon sliding out of it point first. All of it drawn glowing (additive), from
     * both sides; it springs open and folds shut.
     */
    public static class GateRenderer extends EntityRenderer<local.primordial.boss.TreasuryGate> {
        static final ResourceLocation RING=new ResourceLocation(Primordial.ID,"textures/entity/treasury_gate_ring.png");
        static final ResourceLocation RIPPLE=new ResourceLocation(Primordial.ID,"textures/entity/treasury_gate_ripple.png");
        static final ResourceLocation CORE=new ResourceLocation(Primordial.ID,"textures/entity/treasury_gate_core.png");
        private final ItemRenderer items;
        public GateRenderer(EntityRendererProvider.Context context) {super(context);items=context.getItemRenderer();}
        static float springOpen(float t) {float c=1.7F;t-=1;return 1+(c+1)*t*t*t+c*t*t;}
        @Override public void render(local.primordial.boss.TreasuryGate gate,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
            float open=gate.openness(partialTick);
            if(open<=0.001F)return;
            float size=gate.size()*(gate.closing()>=0?open:springOpen(open));
            float time=(gate.tickCount+partialTick)/20F;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick,gate.yRotO,gate.getYRot())));
            pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick,gate.xRotO,gate.getXRot())));
            // local +z now faces the foe
            var ripple=buffers.getBuffer(RenderType.eyes(RIPPLE));
            for(int i=0;i<3;i++){
                float phase=(time*0.9F+i/3F)%1F;
                float fade=(1-phase)*open;
                disc(ripple,pose,size*(0.3F+phase*0.95F),i*0.004F,time*25+i*40,1F*fade,0.78F*fade,0.32F*fade);
            }
            disc(buffers.getBuffer(RenderType.eyes(CORE)),pose,size*0.75F,0.012F,-time*15,1F*open,0.93F*open,0.7F*open);
            var ring=buffers.getBuffer(RenderType.eyes(RING));
            disc(ring,pose,size*1.35F,0.006F,time*35,1F*open,0.72F*open,0.22F*open);
            disc(ring,pose,size*0.98F,0.009F,-time*55,0.9F*open,0.6F*open,0.15F*open);
            // the weapon, sliding out point first
            var stack=gate.weapon();
            if(!stack.isEmpty()){
                float out=gate.emergence(partialTick);
                pose.pushPose();
                pose.translate(0,0,-0.8F+out*1.15F);
                pose.mulPose(Axis.YP.rotationDegrees(-90));
                pose.mulPose(Axis.ZP.rotationDegrees(-135));
                pose.scale(1.4F*gate.size(),1.4F*gate.size(),1.4F*gate.size());
                items.renderStatic(stack,ItemDisplayContext.FIXED,0xF000F0,OverlayTexture.NO_OVERLAY,pose,buffers,gate.level(),gate.getId());
                pose.popPose();
            }
            pose.popPose();
            super.render(gate,yaw,partialTick,pose,buffers,light);
        }
        /** a square of texture in the gate's plane, turned about its axis, seen from both sides */
        static void disc(com.mojang.blaze3d.vertex.VertexConsumer buffer,PoseStack pose,float half,float z,float turn,float r,float g,float b) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(turn));
            var m=pose.last().pose();
            float[][] corners={{-half,-half,0,1},{half,-half,1,1},{half,half,1,0},{-half,half,0,0}};
            for(int side=0;side<2;side++)for(int i=0;i<4;i++){
                var c=corners[side==0?i:3-i];
                buffer.vertex(m,c[0],c[1],z).color(r,g,b,1F).uv(c[2],c[3]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,0,1).endVertex();
            }
            pose.popPose();
        }
        @Override public boolean shouldRender(local.primordial.boss.TreasuryGate gate,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z) {return true;}
        @Override public ResourceLocation getTextureLocation(local.primordial.boss.TreasuryGate gate) {return RING;}
    }

    /**
     * A shockwave: a ring of cracked fire spreading and fading (flat on the ground, or standing up for a roar), a
     * brighter thin rim leading it, both glowing, seen from both sides.
     */
    public static class ShockwaveRenderer extends EntityRenderer<local.primordial.boss.SinShockwave> {
        static final ResourceLocation RING=new ResourceLocation(Primordial.ID,"textures/entity/wrath_shockwave.png");
        static final ResourceLocation RIM=new ResourceLocation(Primordial.ID,"textures/entity/wrath_shockwave_rim.png");
        public ShockwaveRenderer(EntityRendererProvider.Context context) {super(context);}
        @Override public void render(local.primordial.boss.SinShockwave wave,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
            float t=wave.progress(partialTick);
            float eased=1-(1-t)*(1-t)*(1-t);
            float radius=0.6F+wave.radius()*eased;
            float fade=t<0.15F?t/0.15F:(1-t)/(0.85F);
            fade=Math.max(0,fade);
            int c=wave.colour();
            float r=((c>>16)&255)/255F*fade,g=((c>>8)&255)/255F*fade,b=(c&255)/255F*fade;
            pose.pushPose();
            if(wave.upright()){pose.mulPose(Axis.YP.rotationDegrees(-wave.getYRot()));}
            else pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(wave.getId()*37%360));
            GateRenderer.disc(buffers.getBuffer(RenderType.eyes(RING)),pose,radius,0,0,r,g,b);
            GateRenderer.disc(buffers.getBuffer(RenderType.eyes(RIM)),pose,radius*1.04F,0.01F,0,Math.min(1,r*1.4F+0.2F*fade),Math.min(1,g*1.4F+0.15F*fade),b);
            pose.popPose();
            super.render(wave,yaw,partialTick,pose,buffers,light);
        }
        @Override public boolean shouldRender(local.primordial.boss.SinShockwave wave,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z) {return true;}
        @Override public ResourceLocation getTextureLocation(local.primordial.boss.SinShockwave wave) {return RING;}
    }

    // ------------------------------------------------------------------ particles
    /** glowing particles: added onto what is behind them, so gold shines instead of greying */
    static final ParticleRenderType ADDITIVE=new ParticleRenderType() {
        @Override public void begin(com.mojang.blaze3d.vertex.BufferBuilder builder,net.minecraft.client.renderer.texture.TextureManager textures) {
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,TextureAtlas.LOCATION_PARTICLES);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            builder.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE);
        }
        @Override public void end(com.mojang.blaze3d.vertex.Tesselator tesselator) {
            tesselator.end();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        }
        @Override public String toString() {return "ANGEL_CHARM_ADDITIVE";}
    };
    /** one particle of a sin: how it moves is set by its kind */
    static final class Mote extends TextureSheetParticle {
        final SpriteSet sprites;final float peak,size,grow,sway;final boolean bright,animate;final int swayPhase;
        boolean additive;float spin,twinkle;
        Mote additive() {additive=true;return this;}
        Mote spin(float perTick) {spin=perTick;return this;}
        /** shimmer: its brightness flickers this hard */
        Mote twinkle(float amount) {twinkle=amount;return this;}
        Mote(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites,int colour,int life,float size,
             float gravity,float friction,float peak,float grow,float sway,boolean bright,boolean animate,boolean physics) {
            super(level,x,y,z);
            this.sprites=sprites;this.peak=peak;this.size=size;this.grow=grow;this.sway=sway;this.bright=bright;this.animate=animate;
            swayPhase=random.nextInt(100);
            xd=vx;yd=vy;zd=vz;lifetime=life;this.gravity=gravity;this.friction=friction;hasPhysics=physics;quadSize=size;alpha=0;
            rCol=((colour>>16)&255)/255F;gCol=((colour>>8)&255)/255F;bCol=(colour&255)/255F;
            if(animate)setSpriteFromAge(sprites);else pickSprite(sprites);
            roll=random.nextFloat()*6.28F;oRoll=roll;
        }
        @Override public void tick() {
            super.tick();
            if(removed)return;
            float t=age/(float)lifetime;
            alpha=peak*Math.min(1,t*6)*Math.min(1,(1-t)*2.5F);
            if(twinkle>0)alpha*=1-twinkle*(0.5F+0.5F*Mth.sin((age+swayPhase)*0.9F));
            quadSize=size*(1+grow*t);
            if(spin!=0){oRoll=roll;roll+=spin;}
            if(sway!=0){xd+=Mth.sin((age+swayPhase)*0.2F)*sway;zd+=Mth.cos((age+swayPhase)*0.17F)*sway;oRoll=roll;roll+=sway*4;}
            if(animate)setSpriteFromAge(sprites);
        }
        @Override public ParticleRenderType getRenderType() {return additive?ADDITIVE:ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
        @Override protected int getLightColor(float partialTick) {return bright?0xF000F0:super.getLightColor(partialTick);}
    }
    interface Kind {Mote make(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites,int colour);}
    static ParticleProvider<ArcaneParticles.Options> provider(SpriteSet sprites,Kind kind) {
        return (options,level,x,y,z,vx,vy,vz)->kind.make(level,x,y,z,vx,vy,vz,sprites,options.colour());
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class SinBossClientRegistration {
        @SubscribeEvent public static void registerSinLordRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(SinBosses.PRIDE.get(),context->new LordRenderer<>(context,"pride_lord",0.6F));
            event.registerEntityRenderer(SinBosses.WRATH.get(),context->new LordRenderer<>(context,"wrath_lord",1.4F).withScale(1.1F));
            event.registerEntityRenderer(SinBosses.ENVY.get(),context->new LordRenderer<>(context,"envy_lord",0.55F).withScale(1.15F));
            event.registerEntityRenderer(SinBosses.GREED.get(),context->new LordRenderer<>(context,"greed_lord",1.2F));
            event.registerEntityRenderer(SinBosses.LUST.get(),context->new LordRenderer<>(context,"lust_lord",0.5F));
            event.registerEntityRenderer(SinBosses.GLUTTONY.get(),context->new LordRenderer<>(context,"gluttony_lord",1.2F));
            event.registerEntityRenderer(SinBosses.SLOTH.get(),context->new LordRenderer<>(context,"sloth_lord",0.8F));
            event.registerEntityRenderer(SinBosses.PRIMORDIAL.get(),PrimordialRenderer::new);
            event.registerEntityRenderer(SinBosses.BAIHU.get(),context->new LordRenderer<>(context,"baihu",1.8F*local.primordial.boss.BaihuLord.MODEL_SCALE).withScale(local.primordial.boss.BaihuLord.MODEL_SCALE));
            event.registerEntityRenderer(SinBosses.XUANWU.get(),context->new LordRenderer<>(context,"xuanwu",3.0F*local.primordial.boss.XuanwuLord.MODEL_SCALE).withScale(local.primordial.boss.XuanwuLord.MODEL_SCALE));
            event.registerEntityRenderer(SinBosses.QINGLONG.get(),context->new LordRenderer<>(context,"qinglong",2.4F*local.primordial.boss.QinglongLord.MODEL_SCALE).withScale(local.primordial.boss.QinglongLord.MODEL_SCALE));
            event.registerEntityRenderer(SinBosses.ZHUQUE.get(),ZhuqueRenderer::new);
            event.registerEntityRenderer(SinBosses.FIRST_SAGE.get(),context->new LordRenderer<>(context,"first_sage",1.2F));
            event.registerEntityRenderer(SinBosses.SIN_PILLAR.get(),PillarRenderer::new);
            event.registerEntityRenderer(SinBosses.ENVY_SHADE.get(),ShadeRenderer::new);
            event.registerEntityRenderer(SinBosses.TREASURE_WEAPON.get(),TreasureRenderer::new);
            event.registerEntityRenderer(SinBosses.SIN_ORB.get(),NoopRenderer::new);
            event.registerEntityRenderer(SinBosses.TREASURY_GATE.get(),GateRenderer::new);
            event.registerBlockEntityRenderer(SinBosses.STATUE_ENTITY.get(),StatueRenderer::new);
            event.registerEntityRenderer(SinBosses.SHOCKWAVE.get(),ShockwaveRenderer::new);
        }
        @SubscribeEvent public static void registerSinParticles(RegisterParticleProvidersEvent event) {
            // Pride: star glints that flash and turn where they hang, fine gold dust that shimmers as it drifts, soft regal flares
            event.registerSpriteSet(SinParticles.GLINT.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy,vz,s,c,14+level.random.nextInt(10),0.18F+level.random.nextFloat()*0.1F,0,0.9F,1,-0.4F,0,true,true,false).additive().spin(0.04F)));
            event.registerSpriteSet(SinParticles.GOLD_DUST.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy,vz,s,c,20+level.random.nextInt(20),0.035F+level.random.nextFloat()*0.03F,0,0.94F,1,0,0,true,false,false).additive().twinkle(0.6F)));
            event.registerSpriteSet(SinParticles.FLARE.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy,vz,s,c,10+level.random.nextInt(6),0.5F+level.random.nextFloat()*0.3F,0,0.9F,0.8F,0.6F,0,true,false,false).additive()));
            // Wrath: embers that rise, flicker and burn down; sparks flung hard that fall and bounce; dark smoke rolling up and spreading
            event.registerSpriteSet(SinParticles.EMBER.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy+0.03,vz,s,c,16+level.random.nextInt(14),0.07F+level.random.nextFloat()*0.06F,-0.015F,0.95F,1,-0.6F,0.002F,true,true,false).additive().twinkle(0.35F)));
            event.registerSpriteSet(SinParticles.SPARK.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy,vz,s,c,10+level.random.nextInt(10),0.04F+level.random.nextFloat()*0.03F,0.9F,0.97F,1,-0.5F,0,true,false,true).additive()));
            event.registerSpriteSet(SinParticles.SMOKE.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy,vz,s,c,22+level.random.nextInt(16),0.16F+level.random.nextFloat()*0.12F,-0.002F,0.92F,0.5F,0.9F,0.001F,false,false,false).spin(0.01F)));
            // Envy: shadow smoke that coils and swells as it fades; mirror shards that glint and turn as they fall
            event.registerSpriteSet(SinParticles.WISP.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy+0.008,vz,s,c,28+level.random.nextInt(20),0.2F+level.random.nextFloat()*0.15F,0,0.93F,0.75F,1.4F,0.002F,true,false,false).spin(0.02F)));
            event.registerSpriteSet(SinParticles.SHARD.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy,vz,s,c,18+level.random.nextInt(14),0.07F+level.random.nextFloat()*0.05F,0.35F,0.94F,1,-0.3F,0,true,true,false).additive().spin(0.15F).twinkle(0.5F)));
            // Greed: spinning coins that fall and bounce
            event.registerSpriteSet(SinParticles.COIN.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx+(level.random.nextDouble()-0.5)*0.08,vy,vz+(level.random.nextDouble()-0.5)*0.08,s,c,40+level.random.nextInt(20),0.1F,0.9F,0.98F,1,0,0,true,true,true)));
            // Lust: petals drifting and turning as they sink
            event.registerSpriteSet(SinParticles.PETAL.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy,vz,s,c,40+level.random.nextInt(30),0.1F,0.03F,0.95F,0.95F,0,0.004F,true,false,false)));
            // Gluttony: void bubbles that swell and pop
            event.registerSpriteSet(SinParticles.BUBBLE.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy,vz,s,c,16+level.random.nextInt(12),0.12F,0,0.99F,0.85F,0.8F,0,false,true,false)));
            // Sloth: drowsy Zs that drift up and away
            event.registerSpriteSet(SinParticles.DREAM.get(),sprites->provider(sprites,(level,x,y,z,vx,vy,vz,s,c)->
                new Mote(level,x,y,z,vx,vy+0.01,vz,s,c,40+level.random.nextInt(20),0.14F,-0.004F,0.98F,0.9F,0.4F,0.002F,true,false,false)));
        }
    }
}
