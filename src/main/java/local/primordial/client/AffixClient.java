package local.primordial.client;

import local.primordial.Primordial;
import local.primordial.affix.AffixGrowth;
import local.primordial.affix.AffixStatuses;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client side of the affixes: Growth sizes, and Befuddlement's shuffled controls. */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class AffixClient {
    private AffixClient() {}

    public static void growth(int entityId,float growth) {
        var level=Minecraft.getInstance().level;if(level==null)return;
        var entity=level.getEntity(entityId);if(entity==null)return;
        if(growth<=1)entity.getPersistentData().remove(AffixGrowth.KEY);else entity.getPersistentData().putFloat(AffixGrowth.KEY,growth);
        entity.refreshDimensions();
    }

    /** client game time until which each sealed mob (by entity id) has its affixes sealed */
    private static final java.util.Map<Integer,Long> SEALED=new java.util.HashMap<>();
    public static void sealed(int entityId,int ticks) {
        var level=Minecraft.getInstance().level;if(level==null)return;
        SEALED.put(entityId,level.getGameTime()+ticks);
    }
    public static boolean sealed(net.minecraft.world.entity.Entity entity) {
        Long until=SEALED.get(entity.getId());
        if(until==null)return false;
        if(entity.level().getGameTime()>=until){SEALED.remove(entity.getId());return false;}
        return true;
    }

    /** while befuddled, attack and use swap places (see BefuddleKeysMixin) */
    public static boolean keysSwapped() {
        var player=Minecraft.getInstance().player;
        return player!=null&&AffixStatuses.befuddled(player)>0;
    }
    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        var player=event.getEntity();
        if(AffixStatuses.befuddled(player)<=0)return;
        var input=event.getInput();
        boolean[] held={input.up,input.down,input.left,input.right},moved=new boolean[4];
        int[] to=AffixStatuses.shuffledControls(AffixStatuses.befuddleSeed(player));
        for(int i=0;i<4;i++)if(held[i])moved[to[i]]=true;
        // keep the size of the impulse (sneaking, using an item) and only change its direction
        float scale=Math.max(Math.abs(input.forwardImpulse),Math.abs(input.leftImpulse));if(scale==0)scale=1;
        input.up=moved[0];input.down=moved[1];input.left=moved[2];input.right=moved[3];
        input.forwardImpulse=((moved[0]?1:0)-(moved[1]?1:0))*scale;
        input.leftImpulse=((moved[2]?1:0)-(moved[3]?1:0))*scale;
    }
}
