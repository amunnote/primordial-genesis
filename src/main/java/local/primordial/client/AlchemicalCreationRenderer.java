package local.primordial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import local.primordial.Primordial;
import local.primordial.entity.alchemical.AlchemicalCreation;
import local.primordial.entity.alchemical.AlchemicalCreations;
import local.primordial.entity.alchemical.AlchemicalCreeper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * GeckoLib renderer for the alchemical creations: the model turns its "head" bone, glows where its _glowmask texture
 * is lit, holds its main-hand item at the "right_item" bone and an enderman's carried block at "held_block".
 */
public class AlchemicalCreationRenderer<T extends Mob&AlchemicalCreation> extends GeoEntityRenderer<T> {
    final String id;
    public AlchemicalCreationRenderer(EntityRendererProvider.Context context,String id,float shadow) {
        super(context,new DefaultedEntityGeoModel<>(new ResourceLocation(Primordial.ID,id),true));
        this.id=id;
        shadowRadius=shadow;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        addRenderLayer(new BlockAndItemGeoLayer<>(this,
            (bone,mob)->bone.getName().equals("right_item")?mob.getMainHandItem():null,
            (bone,mob)->bone.getName().equals("held_block")&&mob instanceof EnderMan enderman?enderman.getCarriedBlock():null) {
            @Override protected ItemDisplayContext getTransformTypeForStack(GeoBone bone,ItemStack stack,T mob) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }
            @Override protected void renderStackForBone(PoseStack pose,GeoBone bone,ItemStack stack,T mob,MultiBufferSource buffers,
                                                        float partialTick,int light,int overlay) {
                // as the vanilla item-in-hand layer: the item points forward out of the fist
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                super.renderStackForBone(pose,bone,stack,mob,buffers,partialTick,light,overlay);
            }
        });
    }

    /** the creation, then its magic: the circle under its feet and its halo */
    @Override public void render(T mob,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
        super.render(mob,yaw,partialTick,pose,buffers,light);
        ArcaneClient.render(mob,id,partialTick,pose,buffers);
    }

    /** the creeper's swelling and white flashing before each blast, as the vanilla creeper renderer does it */
    public static final class Creeper extends AlchemicalCreationRenderer<AlchemicalCreeper> {
        public Creeper(EntityRendererProvider.Context context) {super(context,"alchemical_creeper",0.5F);}
        @Override public void scaleModelForRender(float width,float height,PoseStack pose,AlchemicalCreeper creeper,BakedGeoModel model,
                                                  boolean reRender,float partialTick,int light,int overlay) {
            float swelling=creeper.getSwelling(partialTick);
            float wobble=1.0F+Mth.sin(swelling*100.0F)*swelling*0.01F;
            swelling=Mth.clamp(swelling,0.0F,1.0F);
            swelling*=swelling;swelling*=swelling;
            super.scaleModelForRender(width*(1.0F+swelling*0.4F)*wobble,height*(1.0F+swelling*0.1F)/wobble,pose,creeper,model,reRender,partialTick,light,overlay);
        }
        @Override public int getPackedOverlay(AlchemicalCreeper creeper,float u,float partialTick) {
            float swelling=creeper.getSwelling(partialTick);
            float white=(int)(swelling*10.0F)%2==0?0.0F:Mth.clamp(swelling,0.5F,1.0F);
            return OverlayTexture.pack(OverlayTexture.u(white),OverlayTexture.v(creeper.hurtTime>0||creeper.deathTime>0));
        }
    }

    /** the magma cube is modelled at size one, and squashes and stretches as it lands and leaps, as the vanilla slime renderer does it */
    public static final class MagmaCube extends AlchemicalCreationRenderer<local.primordial.entity.alchemical.AlchemicalMagmaCube> {
        public MagmaCube(EntityRendererProvider.Context context) {super(context,"alchemical_magma_cube",0.25F);}
        @Override public void scaleModelForRender(float width,float height,PoseStack pose,local.primordial.entity.alchemical.AlchemicalMagmaCube cube,BakedGeoModel model,
                                                  boolean reRender,float partialTick,int light,int overlay) {
            float size=cube.getSize();
            float squish=Mth.lerp(partialTick,cube.oSquish,cube.squish)/(size*0.5F+1.0F);
            float wide=1.0F/(squish+1.0F);
            super.scaleModelForRender(width*wide*size,height/wide*size,pose,cube,model,reRender,partialTick,light,overlay);
        }
        @Override public void render(local.primordial.entity.alchemical.AlchemicalMagmaCube cube,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
            shadowRadius=0.25F*cube.getSize();
            super.render(cube,yaw,partialTick,pose,buffers,light);
        }
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class AlchemicalCreationClientRegistration {
        @SubscribeEvent public static void registerAlchemicalCreationRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(AlchemicalCreations.ZOMBIE.get(),context->new AlchemicalCreationRenderer<>(context,"alchemical_zombie",0.5F));
            event.registerEntityRenderer(AlchemicalCreations.SKELETON.get(),context->new AlchemicalCreationRenderer<>(context,"alchemical_skeleton",0.5F));
            event.registerEntityRenderer(AlchemicalCreations.SPIDER.get(),context->new AlchemicalCreationRenderer<>(context,"alchemical_spider",0.8F));
            event.registerEntityRenderer(AlchemicalCreations.CREEPER.get(),Creeper::new);
            event.registerEntityRenderer(AlchemicalCreations.ENDERMAN.get(),context->new AlchemicalCreationRenderer<>(context,"alchemical_enderman",0.5F));
            event.registerEntityRenderer(AlchemicalCreations.BLAZE.get(),context->new AlchemicalCreationRenderer<>(context,"alchemical_blaze",0.5F));
            // the ghast is modelled at a quarter of its size, as vanilla's is
            event.registerEntityRenderer(AlchemicalCreations.GHAST.get(),context->new AlchemicalCreationRenderer<>(context,"alchemical_ghast",1.5F).withScale(4.0F));
            event.registerEntityRenderer(AlchemicalCreations.WITHER_SKELETON.get(),context->new AlchemicalCreationRenderer<>(context,"alchemical_wither_skeleton",0.5F));
            event.registerEntityRenderer(AlchemicalCreations.MAGMA_CUBE.get(),MagmaCube::new);
        }
    }
}
