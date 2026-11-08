package local.primordial.spirit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Biomes;

/**
 * One biome per island: six in ten from the overworld's biomes, a quarter from the Nether's, the rest from the End's,
 * all from the given tags (so biomes other mods add to those dimensions come along). The arrival island is plains.
 */
public final class SpiritBiomeSource extends BiomeSource {
    public static final Codec<SpiritBiomeSource> CODEC=RecordCodecBuilder.create(i->i.group(
        RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("overworld").forGetter(s->s.overworld),
        RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("nether").forGetter(s->s.nether),
        RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("end").forGetter(s->s.end)
    ).apply(i,SpiritBiomeSource::new));
    private final HolderSet<Biome> overworld,nether,end;
    private List<Holder<Biome>> overworldList,netherList,endList;
    private Holder<Biome> origin;

    public SpiritBiomeSource(HolderSet<Biome> overworld,HolderSet<Biome> nether,HolderSet<Biome> end) {
        this.overworld=overworld;this.nether=nether;this.end=end;
    }
    private void lists() {
        if(overworldList!=null)return;
        overworldList=overworld.stream().toList();netherList=nether.stream().toList();endList=end.stream().toList();
        origin=overworldList.stream().filter(b->b.is(Biomes.PLAINS)).findFirst().orElse(overworldList.isEmpty()?null:overworldList.get(0));
    }
    @Override protected Codec<? extends BiomeSource> codec() {return CODEC;}
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() {return Stream.of(overworld,nether,end).flatMap(HolderSet::stream).distinct();}

    /** the biome of a cell's island */
    public Holder<Biome> biomeOfCell(int cellX,int cellZ) {
        lists();
        if(SpiritIslands.isOrigin(cellX,cellZ)&&origin!=null)return origin;
        long h=SpiritIslands.hash(cellX,cellZ,71);
        int roll=(int)Math.floorMod(h,100);
        List<Holder<Biome>> pool=roll<60?overworldList:roll<85?netherList:endList;
        if(pool.isEmpty())pool=overworldList;
        return pool.get((int)Math.floorMod(h>>>12,pool.size()));
    }
    public Holder<Biome> biomeAtBlock(int x,int z) {
        var island=SpiritIslands.islandAt(x,z);
        return island!=null?biomeOfCell(island.cellX(),island.cellZ()):biomeOfCell(SpiritIslands.cellOf(x),SpiritIslands.cellOf(z));
    }
    @Override public Holder<Biome> getNoiseBiome(int quartX,int quartY,int quartZ,Climate.Sampler sampler) {
        return biomeAtBlock((quartX<<2)+2,(quartZ<<2)+2);
    }
}
