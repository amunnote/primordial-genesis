package local.primordial.client;

import local.primordial.Primordial;
import local.primordial.affix.BossAffix;
import local.primordial.boss.SinParticles;
import local.primordial.spirit.PrimordialTemple;
import local.primordial.spirit.SpiritRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Primordial Temple's braziers as each player sees them: lit, in their sin's colour, for every Sin Lord this player
 * has felled; cold and smoking for the rest. The server tells each client which lords it has felled.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class PrimordialTempleClient {
    private static int defeated;
    private PrimordialTempleClient() {}
    public static void defeated(int mask) {defeated=mask;}
    public static boolean lit(BossAffix sin) {return (defeated&1<<sin.ordinal())!=0;}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();
        if(event.phase!=TickEvent.Phase.END||mc.level==null||mc.player==null||mc.isPaused()||mc.level.dimension()!=SpiritRealm.LEVEL)return;
        if(mc.player.position().horizontalDistanceSqr()>160*160||mc.level.getGameTime()%2!=0)return;
        var random=mc.level.random;
        for(var sin:BossAffix.values()){
            var at=PrimordialTemple.brazier(sin);
            if(lit(sin)){
                mc.level.addAlwaysVisibleParticle(ParticleTypes.FLAME,at.x+(random.nextDouble()-0.5)*0.6,at.y+random.nextDouble()*0.3,at.z+(random.nextDouble()-0.5)*0.6,0,0.04,0);
                mc.level.addAlwaysVisibleParticle(SinParticles.FLARE.get().of(sin.color()),at.x+(random.nextDouble()-0.5)*0.8,at.y+0.3+random.nextDouble()*0.6,at.z+(random.nextDouble()-0.5)*0.8,0,0.05,0);
            }else if(random.nextInt(6)==0)
                mc.level.addAlwaysVisibleParticle(ParticleTypes.SMOKE,at.x+(random.nextDouble()-0.5)*0.4,at.y,at.z+(random.nextDouble()-0.5)*0.4,0,0.03,0);
        }
    }
}
