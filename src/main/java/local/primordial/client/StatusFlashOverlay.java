package local.primordial.client;

import com.mojang.blaze3d.systems.RenderSystem;
import local.primordial.Primordial;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * When a negative status first takes hold, its icon looms over the screen for two seconds in a tinted haze, with a
 * sound, the way an elder guardian's curse does. Renewing a status that is still running shows nothing (the server
 * only sends the flash for a fresh one).
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class StatusFlashOverlay {
    private record Look(String icon,int color,SoundEvent sound,float pitch){}
    // malice, desaturation, feeble, blood pact, befuddled (AffixStatuses.FLASH_*)
    private static final Look[] LOOKS={
        new Look("malice",0x9A1CC8,SoundEvents.WITHER_AMBIENT,0.6F),
        new Look("desaturation",0x9A9AA4,SoundEvents.ELDER_GUARDIAN_CURSE,1.2F),
        new Look("feebleness",0xA89A6A,SoundEvents.PLAYER_ATTACK_WEAK,0.5F),
        new Look("blood_pact",0xB0101C,SoundEvents.WARDEN_HEARTBEAT,0.7F),
        new Look("befuddlement",0xC88CE0,SoundEvents.ILLUSIONER_PREPARE_MIRROR,1.0F)};
    private static final long LENGTH=2000;
    private static int kind=-1;
    private static long started;
    private StatusFlashOverlay() {}

    public static void show(int which) {
        if(which<0||which>=LOOKS.length)return;
        kind=which;started=Util.getMillis();
        var minecraft=Minecraft.getInstance();
        if(minecraft.player!=null)minecraft.player.playSound(LOOKS[which].sound(),1F,LOOKS[which].pitch());
    }

    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("affix_status_flash",(gui,graphics,partialTick,width,height)->{
            if(kind<0)return;
            float t=(Util.getMillis()-started)/(float)LENGTH;
            if(t>=1){kind=-1;return;}
            // quick rise, a hold, then a slow fade
            float alpha=t<0.15F?t/0.15F:t<0.55F?1:1-(t-0.55F)/0.45F;
            var look=LOOKS[kind];
            int tint=((int)(alpha*70)<<24)|look.color();
            RenderSystem.enableBlend();
            // the haze: darker at the edges
            graphics.fillGradient(0,0,width,height/3,tint,0);
            graphics.fillGradient(0,height*2/3,width,height,0,tint);
            graphics.fill(0,0,width,height,((int)(alpha*28)<<24)|look.color());
            // the icon, growing slightly as it fades
            float size=Math.min(width,height)*0.32F*(0.9F+0.2F*t);
            var pose=graphics.pose();pose.pushPose();
            pose.translate(width/2F,height/2F-height*0.06F,0);pose.scale(size/18F,size/18F,1);
            graphics.setColor(1,1,1,alpha*0.85F);
            graphics.blit(new ResourceLocation(Primordial.ID,"textures/gui/status/"+look.icon()+".png"),-9,-9,0,0,18,18,18,18);
            graphics.setColor(1,1,1,1);
            pose.popPose();
            RenderSystem.disableBlend();
        });
    }
}
