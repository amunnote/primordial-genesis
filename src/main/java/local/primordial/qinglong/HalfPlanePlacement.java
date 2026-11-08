package local.primordial.qinglong;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.*;

/** The start chunk's centre is the court centre; direction defaults to east for existing worlds. */
public final class HalfPlanePlacement extends RandomSpreadStructurePlacement {
    public static final Codec<HalfPlanePlacement> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.intRange(1,4096).fieldOf("spacing").forGetter(HalfPlanePlacement::spacing),
        Codec.intRange(0,4095).fieldOf("separation").forGetter(HalfPlanePlacement::separation),
        Codec.intRange(0,Integer.MAX_VALUE).fieldOf("salt").forGetter(HalfPlanePlacement::salt),
        Codec.STRING.optionalFieldOf("direction","east").forGetter(p->p.direction)
    ).apply(i,HalfPlanePlacement::new));
    private final String direction;
    public HalfPlanePlacement(int spacing,int separation,int salt,String direction){super(spacing,separation,RandomSpreadType.LINEAR,salt);this.direction=direction;if(!java.util.Set.of("east","west","north","south").contains(direction))throw new IllegalArgumentException("Invalid half-plane direction");if(separation>=spacing)throw new IllegalArgumentException("separation must be smaller than spacing");}
    @Override protected boolean isPlacementChunk(ChunkGeneratorStructureState s,int x,int z){long bx=x*16L+8,bz=z*16L+8;return (switch(direction){case "west"->bx<=-800;case "north"->bz<=-800;case "south"->bz>=800;default->bx>=800;})&&super.isPlacementChunk(s,x,z);}
    @Override public StructurePlacementType<?> type(){return QinglongWorldgen.HALF_PLANE.get();}
}
