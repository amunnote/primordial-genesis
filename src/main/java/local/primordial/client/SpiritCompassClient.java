package local.primordial.client;
import local.primordial.Primordial;
import local.primordial.guide.NextStep;
import local.primordial.spirit.SpiritRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class SpiritCompassClient {
    @Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Properties {
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(Primordial.SPIRIT_COMPASS.get(),new ResourceLocation("angle"),(stack,level,entity,seed)->{
            // in a menu or a JEI page: upright; held with nothing to point at: turning slowly, searching
            if(entity==null)return 0.5F;
            if(!stack.hasTag()||!stack.getTag().contains("GuideTargetPos")||!entity.level().dimension().location().toString().equals(stack.getTag().getString("GuideTargetDimension")))
                return (float)net.minecraft.util.Mth.positiveModulo(entity.level().getGameTime()/80.0,1.0);
            var pos=BlockPos.of(stack.getTag().getLong("GuideTargetPos"));
            double bearing=Math.atan2(pos.getZ()+0.5-entity.getZ(),pos.getX()+0.5-entity.getX())/(Math.PI*2);
            return (float)net.minecraft.util.Mth.positiveModulo(0.5-(entity.getYRot()/360.0-0.25-bearing),1.0);
        }));}
    }
    @SubscribeEvent public static void hud(RenderGuiOverlayEvent.Post e){
        if(!e.getOverlay().id().equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id()))return;
        var mc=Minecraft.getInstance();var p=mc.player;if(p==null||mc.options.hideGui)return;
        var stack=p.getMainHandItem();if(!stack.is(Primordial.SPIRIT_COMPASS.get()))stack=p.getOffhandItem();
        if(!stack.is(Primordial.SPIRIT_COMPASS.get())||!stack.hasTag())return;
        var tag=stack.getTag();var name=Component.Serializer.fromJson(tag.getString("GuideTargetName"));if(name==null)return;
        boolean same=p.level().dimension().location().toString().equals(tag.getString("GuideTargetDimension")),found=tag.contains("GuideTargetPos");
        var pos=found?BlockPos.of(tag.getLong("GuideTargetPos")):BlockPos.ZERO;
        var distance=!same?NextStep.t("other_dimension"):!found?NextStep.t("not_found"):NextStep.t("distance",(int)Math.sqrt(pos.distToCenterSqr(p.getX(),p.getY(),p.getZ())));
        var line=distance.copy().append(" · ").append(tag.getString("GuideTargetDimension"));
        if(!tag.getString("GuideDirection").isEmpty())line.append(" · ").append(NextStep.t(tag.getString("GuideDirection")));
        if(same&&found)line.append(" · XYZ "+pos.getX()+" / "+pos.getY()+" / "+pos.getZ());
        var g=e.getGuiGraphics();g.fill(3,3,Math.max(mc.font.width(name),mc.font.width(line))+11,30,0x99000000);g.drawString(mc.font,name,7,6,0xFFE6B65A);g.drawString(mc.font,line,7,18,0xFFFFFFFF);
    }
}
