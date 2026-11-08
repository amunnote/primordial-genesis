package local.primordial.qinglong;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.*;

/**
 * One court a world, at its guardian's seat (GuardianAnchors). A random spread in form, so /locate and the compass find
 * it the way they find any structure: every cell of its very wide grid names the same chunk.
 */
public final class GuardianPlacement extends RandomSpreadStructurePlacement {
    public static final Codec<GuardianPlacement> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.intRange(0,Integer.MAX_VALUE).fieldOf("salt").forGetter(GuardianPlacement::salt),
        Codec.STRING.fieldOf("seat").forGetter(p->p.seat.name().toLowerCase(java.util.Locale.ROOT))
    ).apply(i,(salt,seat)->new GuardianPlacement(salt,GuardianAnchors.Seat.of(seat))));
    private final GuardianAnchors.Seat seat;
    public GuardianPlacement(int salt,GuardianAnchors.Seat seat){super(2048,1,RandomSpreadType.LINEAR,salt);this.seat=seat;}
    @Override public ChunkPos getPotentialStructureChunk(long seed,int x,int z){
        var chunk=GuardianAnchors.seat(seat);
        // no seat (a world without a multi-noise overworld): a chunk no court will take
        return chunk!=null?chunk:new ChunkPos(Integer.MAX_VALUE>>5,Integer.MAX_VALUE>>5);
    }
    @Override public StructurePlacementType<?> type(){return QinglongWorldgen.GUARDIAN_SEAT.get();}
}
