package local.primordial.sound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import local.primordial.Primordial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The mod's own sounds: each creature's voice (ambient, hurt, death) and each boss's battle music. In sounds.json they
 * are built from vanilla sounds, pitched to fit; every one has its own id, so a resource pack can give any of them a
 * sound of its own (assets/primordial/sounds.json) without touching the code.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,Primordial.ID);
    /** the creatures with voices of their own */
    public static final List<String> BOSSES=List.of("pride_lord","wrath_lord","envy_lord","greed_lord","lust_lord","gluttony_lord","sloth_lord",
        "primordial_lord","baihu","xuanwu","qinglong","zhuque","first_sage");
    public static final List<String> OTHERS=List.of("envy_shade","sin_pillar","alchemical_zombie","alchemical_skeleton","alchemical_spider","alchemical_creeper",
        "alchemical_enderman","alchemical_blaze","alchemical_ghast","alchemical_magma_cube","alchemical_wither_skeleton");
    public static final List<String> KINDS=List.of("ambient","hurt","death");
    /** battle music: one track per boss, and the First Sage's four stages */
    public static final List<String> TRACKS=List.of("pride_lord","wrath_lord","envy_lord","greed_lord","lust_lord","gluttony_lord","sloth_lord",
        "primordial_lord","baihu","xuanwu","qinglong","zhuque","first_sage_nigredo","first_sage_albedo","first_sage_citrinitas","first_sage_rubedo");
    private static final Map<String,RegistryObject<SoundEvent>> VOICES=new HashMap<>(),MUSIC=new HashMap<>();
    static {
        for(var id:BOSSES)for(var kind:KINDS)registerVoice(id,kind);
        for(var id:OTHERS)for(var kind:KINDS)registerVoice(id,kind);
        for(var track:TRACKS){var name="music.boss."+track;MUSIC.put(track,SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(Primordial.ID,name))));}
    }
    private ModSounds() {}
    private static void registerVoice(String id,String kind) {
        var name="entity."+id+"."+kind;
        VOICES.put(id+"."+kind,SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(Primordial.ID,name))));
    }
    public static void register(IEventBus bus) {SOUNDS.register(bus);}
    /** a creature's voice: kind is ambient, hurt or death; null if it has none of its own */
    public static SoundEvent voice(String id,String kind) {var sound=VOICES.get(id+"."+kind);return sound==null||!sound.isPresent()?null:sound.get();}
    /** a battle track, or null */
    public static SoundEvent music(String track) {var sound=MUSIC.get(track);return sound==null||!sound.isPresent()?null:sound.get();}
}
