package local.primordial.affix;

import java.util.Map;
import java.util.WeakHashMap;
import local.primordial.Primordial;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * What a hit looks and sounds like by its kind, where it lands (after every reduction, so a hit taken to nothing shows
 * nothing): physical a grey burst (the game's own hit sound is enough), magic violet sparks with a crystal chime, true
 * damage golden light with a clear bell, fixed damage dark red embers with a dull iron thud. A hit Causality turned
 * (adaptive) adds a cyan ring and a rising tone. The more harm, the more particles; each creature makes each kind's
 * sound at most once every four ticks, so damage over time does not drone. Sent from the server: everyone near sees it.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class DamageFeedback {
    public enum Look {
        PHYSICAL(new DustParticleOptions(new Vector3f(0.78F,0.78F,0.82F),0.9F),ParticleTypes.POOF,null,1F,1F),
        MAGIC(new DustParticleOptions(new Vector3f(0.62F,0.32F,0.95F),1F),ParticleTypes.WITCH,SoundEvents.AMETHYST_CLUSTER_HIT,0.7F,1.4F),
        TRUE(new DustParticleOptions(new Vector3f(1F,0.84F,0.3F),1.1F),ParticleTypes.END_ROD,SoundEvents.NOTE_BLOCK_BELL.value(),0.6F,1.7F),
        FIXED(new DustParticleOptions(new Vector3f(0.55F,0.05F,0.08F),1.3F),ParticleTypes.SMOKE,SoundEvents.ANVIL_PLACE,0.35F,0.55F),
        ADAPTIVE(new DustParticleOptions(new Vector3f(0.3F,0.9F,0.85F),1F),ParticleTypes.ENCHANTED_HIT,SoundEvents.AMETHYST_BLOCK_RESONATE,0.6F,1.8F);
        final ParticleOptions dust,accent;final SoundEvent sound;final float volume,pitch;
        Look(ParticleOptions dust,ParticleOptions accent,SoundEvent sound,float volume,float pitch){
            this.dust=dust;this.accent=accent;this.sound=sound;this.volume=volume;this.pitch=pitch;
        }
    }
    private static final int SOUND_GAP=4;
    /** when each creature last sounded each look (server thread only) */
    private static final Map<LivingEntity,long[]> SOUNDED=new WeakHashMap<>();
    private DamageFeedback() {}

    /** the look of this hit: fixed and true by their types, then magic, then physical; null for the rest (fire, falls...) */
    public static Look look(DamageSource source) {
        if(AffixDamage.isFixed(source))return Look.FIXED;
        if(AffixDamage.isTrue(source))return Look.TRUE;
        if(source.is(AffixCombat.MAGIC))return Look.MAGIC;
        if(AffixCombat.physical(source))return Look.PHYSICAL;
        return null;
    }

    @SubscribeEvent(priority=EventPriority.LOWEST) public static void landed(LivingDamageEvent event) {
        var victim=event.getEntity();float amount=event.getAmount();
        if(amount<=0||!(victim.level() instanceof ServerLevel level)||!AffixConfig.DAMAGE_FEEDBACK.get())return;
        var source=event.getSource();var look=look(source);
        if(look!=null)show(level,victim,look,amount);
        if(look!=Look.FIXED&&look!=Look.TRUE&&AffixDamage.kind(source)==AffixDamage.Kind.ADAPTIVE)show(level,victim,Look.ADAPTIVE,amount);
    }

    static void show(ServerLevel level,LivingEntity victim,Look look,float amount) {
        double x=victim.getX(),y=victim.getY()+victim.getBbHeight()*0.6,z=victim.getZ(),w=victim.getBbWidth()*0.4;
        int count=Math.min(18,3+(int)(amount/2));
        level.sendParticles(look.dust,x,y,z,count,w,victim.getBbHeight()*0.25,w,0.02);
        level.sendParticles(look.accent,x,y,z,Math.max(1,count/3),w,victim.getBbHeight()*0.2,w,look==Look.TRUE||look==Look.PHYSICAL?0.03:0.1);
        if(look==Look.ADAPTIVE)for(int i=0;i<12;i++){   // a ring round the middle
            double a=i*Math.PI/6,r=victim.getBbWidth()*0.8+0.2;
            level.sendParticles(look.dust,x+Math.cos(a)*r,y,z+Math.sin(a)*r,1,0,0,0,0);
        }
        if(look.sound==null||!AffixConfig.DAMAGE_FEEDBACK_SOUNDS.get())return;
        long now=level.getGameTime();var last=SOUNDED.computeIfAbsent(victim,v->new long[Look.values().length]);
        if(now-last[look.ordinal()]<SOUND_GAP)return;
        last[look.ordinal()]=now;
        level.playSound(null,x,y,z,look.sound,SoundSource.PLAYERS,look.volume,look.pitch*(0.95F+victim.getRandom().nextFloat()*0.1F));
    }
}
