package local.primordial.entity.alchemical;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import java.util.Map;
import local.primordial.Primordial;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The particles of the alchemical creations' magic: rune glyphs and sparks of arcane light, each carrying a colour, so the
 * server can send them in the colour of the creation that casts them.
 */
public final class ArcaneParticles {
    static final DeferredRegister<ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,Primordial.ID);
    public static final RegistryObject<Type> GLYPH=PARTICLES.register("arcane_glyph",Type::new);
    public static final RegistryObject<Type> SPARK=PARTICLES.register("arcane_spark",Type::new);
    /** each creation's colour of magic */
    public static final Map<String,Integer> COLOURS=Map.of(
        "alchemical_zombie",0x6CFF8A,"alchemical_skeleton",0x7FF3FF,"alchemical_spider",0xB57CFF,
        "alchemical_creeper",0xC8FF5A,"alchemical_enderman",0xE079FA,"alchemical_blaze",0xFFB347,
        "alchemical_ghast",0x6FE3E8,"alchemical_wither_skeleton",0x8A7CFF,"alchemical_magma_cube",0xFF6A2A);
    private ArcaneParticles() {}
    public static void register(IEventBus bus) {PARTICLES.register(bus);}
    public static int colour(String id) {return COLOURS.getOrDefault(id,0xFFFFFF);}

    /** a glyph or a spark, and its colour */
    public record Options(ParticleType<Options> type,int colour) implements ParticleOptions {
        @Override public ParticleType<?> getType() {return type;}
        @Override public void writeToNetwork(FriendlyByteBuf buffer) {buffer.writeInt(colour);}
        @Override public String writeToString() {return BuiltInRegistries.PARTICLE_TYPE.getKey(type)+" "+colour;}
    }
    @SuppressWarnings("deprecation")
    static final ParticleOptions.Deserializer<Options> DESERIALIZER=new ParticleOptions.Deserializer<>() {
        @Override public Options fromCommand(ParticleType<Options> type,StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');return new Options(type,reader.readInt());
        }
        @Override public Options fromNetwork(ParticleType<Options> type,FriendlyByteBuf buffer) {return new Options(type,buffer.readInt());}
    };
    public static final class Type extends ParticleType<Options> {
        private final Codec<Options> codec=Codec.INT.xmap(c->new Options(this,c),Options::colour);
        @SuppressWarnings("deprecation")
        public Type() {super(false,DESERIALIZER);}
        @Override public Codec<Options> codec() {return codec;}
        public Options of(int colour) {return new Options(this,colour);}
    }
}
