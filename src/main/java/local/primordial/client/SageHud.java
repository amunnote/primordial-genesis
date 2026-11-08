package local.primordial.client;
import local.primordial.Primordial;
import local.primordial.sage.SageHeart;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class SageHud {
    private static float goldenBlood;
    private static int goldenEntity=-1;
    private static final net.minecraft.resources.ResourceLocation ICONS=new net.minecraft.resources.ResourceLocation("textures/gui/icons.png");
    public static void goldenBlood(int entity,float amount) {goldenEntity=entity;goldenBlood=amount;}
    @SubscribeEvent public static void goldenHearts(RenderGuiOverlayEvent.Post event) {
        if(!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()))return;
        var minecraft=Minecraft.getInstance();var player=minecraft.player;
        if(player==null||player.getId()!=goldenEntity||goldenBlood<=0||minecraft.gameMode==null||!minecraft.gameMode.canHurtPlayer())return;
        int health=net.minecraft.util.Mth.ceil(healthSlots(player.getMaxHealth(),player));
        int absorption=net.minecraft.util.Mth.ceil(player.getAbsorptionAmount());
        int hearts=net.minecraft.util.Mth.ceil(goldenBlood/2),base=(health+1)/2+(absorption+1)/2;
        int rows=net.minecraft.util.Mth.ceil((health+absorption)/20F),spacing=Math.max(10-(rows-2),3);
        int x=event.getWindow().getGuiScaledWidth()/2-91,y=event.getWindow().getGuiScaledHeight()-39;
        for(int i=0;i<hearts;i++){
            int index=base+i,px=x+(index%10)*8,py=y-(index/10)*spacing;
            event.getGuiGraphics().blit(ICONS,px,py,16,0,9,9);
            event.getGuiGraphics().blit(ICONS,px,py,i==hearts-1&&net.minecraft.util.Mth.ceil(goldenBlood)%2==1?169:160,0,9,9);
        }
    }
    public static float healthSlots(float requested,net.minecraft.world.entity.player.Player player) {
        if(player==null||!SageHeart.active(player)||local.primordial.effect.Protection.active(player))return requested;
        return Math.min(requested,Math.min(player.getMaxHealth(),(float)Math.max(1,player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK))));
    }
    @SubscribeEvent public static void hunger(RenderGuiOverlayEvent.Pre event) {
        if(event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())&&SageHeart.active(Minecraft.getInstance().player))event.setCanceled(true);
    }
}
