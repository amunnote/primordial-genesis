package local.primordial.client;

import java.util.ArrayList;
import java.util.List;
import local.primordial.Primordial;
import local.primordial.affix.AffixStatuses;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A row of 9x9 icons for the active independent statuses, right above the armor bar (above health if there is no armor),
 * on the same left-hand column. An icon blinks in its last three seconds.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class StatusHud {
    private static ResourceLocation icon(String name){return new ResourceLocation(Primordial.ID,"textures/gui/status/hud/"+name+".png");}
    private static final ResourceLocation MALICE=icon("malice"),DESATURATION=icon("desaturation"),FEEBLE=icon("feebleness"),
        BLOOD_PACT=icon("blood_pact"),BEFUDDLED=icon("befuddlement");
    private record Shown(ResourceLocation icon,int ticks){}
    private static final java.util.function.Supplier<net.minecraft.world.item.ItemStack> GROWTH=()->
        new net.minecraft.world.item.ItemStack(local.primordial.affix.AffixAccessories.ITEMS.get(local.primordial.affix.MobAffix.GROWTH).get());
    private StatusHud() {}

    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.ARMOR_LEVEL.id(),"affix_statuses",(gui,graphics,partialTick,width,height)->{
            var minecraft=gui.getMinecraft();var player=minecraft.player;
            if(player==null||minecraft.options.hideGui||!gui.shouldDrawSurvivalElements())return;
            List<Shown> shown=new ArrayList<>();
            if(AffixStatuses.malice(player)>0)shown.add(new Shown(MALICE,AffixStatuses.malice(player)));
            if(AffixStatuses.desaturation(player)>0)shown.add(new Shown(DESATURATION,AffixStatuses.desaturation(player)));
            if(AffixStatuses.feeble(player)>0)shown.add(new Shown(FEEBLE,AffixStatuses.feeble(player)));
            if(AffixStatuses.bloodPact(player)>0)shown.add(new Shown(BLOOD_PACT,AffixStatuses.bloodPact(player)));
            if(AffixStatuses.befuddled(player)>0)shown.add(new Shown(BEFUDDLED,AffixStatuses.befuddled(player)));
            // Growth's accessory: its stacks and the seconds left before they are all lost
            int growthAttack=AffixStatuses.growthAttack(player),growthHealth=AffixStatuses.growthHealth(player);
            boolean growth=growthAttack+growthHealth>0;
            if(shown.isEmpty()&&!growth)return;
            gui.setupOverlayRenderState(true,false);
            int left=width/2-91,top=height-gui.leftHeight;
            long time=player.level().getGameTime();
            for(int i=0;i<shown.size();i++){
                var s=shown.get(i);
                if(s.ticks()<60&&(time/5)%2==0)continue;   // running out: blink
                graphics.blit(s.icon(),left+i*8,top,0,0,9,9,9,9);
            }
            if(growth){
                int x=left+shown.size()*8+(shown.isEmpty()?0:2);
                var pose=graphics.pose();pose.pushPose();pose.translate(x,top,0);pose.scale(9/16F,9/16F,1);
                graphics.renderItem(GROWTH.get(),0,0);
                pose.popPose();
                int left20=(AffixStatuses.growthLeft(player)+19)/20;
                var text=growthAttack+"/"+growthHealth+" "+left20+"s";
                graphics.drawString(gui.getFont(),text,x+10,top+1,left20<=2&&(time/5)%2==0?0xFFF07070:0xFF8BCB4A,true);
            }
            gui.leftHeight+=10;
        });
    }
}
