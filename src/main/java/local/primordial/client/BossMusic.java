package local.primordial.client;

import local.primordial.Primordial;
import local.primordial.boss.SinBoss;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Battle music: while a boss within {@link #RANGE} blocks is awake, its track plays (on the music volume) and the game's
 * own music is held back. A new track fades in as the old one fades out; when the fight ends the music fades away.
 * The First Sage changes track with each stage of the Great Work.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class BossMusic {
    public static final double RANGE=64;
    /** volume gained or lost per tick: about a second and a half to fade in, two and a half to fade out */
    static final float FADE_IN=0.035F,FADE_OUT=0.02F;
    private static Track playing;
    private static long started;
    private BossMusic() {}

    /** a looping track heard everywhere alike, fading in, and out once told to leave */
    static final class Track extends AbstractTickableSoundInstance {
        final SoundEvent event;boolean leaving;
        Track(SoundEvent event) {
            super(event,SoundSource.MUSIC,SoundInstance.createUnseededRandom());
            this.event=event;looping=true;delay=0;relative=true;attenuation=Attenuation.NONE;volume=0.01F;x=0;y=0;z=0;
        }
        void leave() {leaving=true;}
        @Override public void tick() {
            if(leaving){volume-=FADE_OUT;if(volume<=0){volume=0;stop();}}
            else if(volume<1)volume=Math.min(1,volume+FADE_IN);
        }
        @Override public boolean canStartSilent() {return true;}
    }

    /** the music the nearest awake boss wants, or null */
    static SoundEvent wanted(Minecraft mc) {
        if(mc.level==null||mc.player==null)return null;
        SinBoss nearest=null;double best=RANGE*RANGE;SoundEvent music=null;
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof SinBoss boss)||!boss.isAlive()||boss.dormant()||boss.statue)continue;
            double distance=boss.distanceToSqr(mc.player);if(distance>=best)continue;
            var track=boss.battleMusic();if(track==null)continue;
            best=distance;nearest=boss;music=track;
        }
        return nearest==null?null:music;
    }

    /** what is playing, for debugging: the track, its volume, and whether the sound engine has it */
    public static String describe() {
        if(playing==null)return "none";
        return playing.event.getLocation()+" volume="+String.format(java.util.Locale.ROOT,"%.2f",playing.getVolume())+" leaving="+playing.leaving
            +" active="+Minecraft.getInstance().getSoundManager().isActive(playing);
    }
    @SubscribeEvent public static void bossMusicTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();var sounds=mc.getSoundManager();
        long now=mc.level==null?0:mc.level.getGameTime();
        // the engine dropped it (a world left, the sounds reloaded): start afresh
        if(playing!=null&&!playing.leaving&&Math.abs(now-started)>40&&!sounds.isActive(playing))playing=null;
        if(playing!=null&&playing.isStopped())playing=null;
        var want=wanted(mc);
        if(want==null){if(playing!=null&&!playing.leaving)playing.leave();return;}
        if(playing==null||playing.leaving||playing.event!=want){
            if(playing!=null&&!playing.leaving)playing.leave();   // it fades out on its own while the new one fades in
            playing=new Track(want);started=now;
            sounds.play(playing);
        }
        mc.getMusicManager().stopPlaying();
    }
}
