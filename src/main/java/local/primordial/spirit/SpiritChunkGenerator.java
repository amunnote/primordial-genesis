package local.primordial.spirit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * The Spirit Realm's terrain: floating islands over the void (see SpiritIslands), each built from its biome's ground —
 * grass and stone, sand, nylium and netherrack, end stone... Trees, ores, flowers, structures and mobs then come from
 * the biome itself through the usual decoration and structure steps, so every island looks like a piece of its world.
 */
public final class SpiritChunkGenerator extends ChunkGenerator {
    public static final Codec<SpiritChunkGenerator> CODEC=RecordCodecBuilder.create(i->i.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(g->g.biomeSource)
    ).apply(i,SpiritChunkGenerator::new));
    public static final int MIN_Y=0,HEIGHT=256;

    public SpiritChunkGenerator(BiomeSource source) {super(source);}
    @Override protected Codec<? extends ChunkGenerator> codec() {return CODEC;}
    private SpiritBiomeSource spiritBiomes() {return biomeSource instanceof SpiritBiomeSource s?s:null;}

    /** an island's ground: deep rock, the layer under the top, the top, how thick the middle is, and a pond in the middle */
    record Palette(BlockState stone,BlockState filler,BlockState top,int fillerDepth,boolean pond) {}
    static Palette palette(Holder<Biome> biome) {
        if(biome==null)return new Palette(Blocks.STONE.defaultBlockState(),Blocks.DIRT.defaultBlockState(),Blocks.GRASS_BLOCK.defaultBlockState(),3,false);
        String path=biome.unwrapKey().map(k->k.location().getPath()).orElse("");
        if(biome.is(BiomeTags.IS_END))return new Palette(Blocks.END_STONE.defaultBlockState(),Blocks.END_STONE.defaultBlockState(),Blocks.END_STONE.defaultBlockState(),0,false);
        if(biome.is(BiomeTags.IS_NETHER)){
            if(path.contains("crimson"))return new Palette(Blocks.NETHERRACK.defaultBlockState(),Blocks.NETHERRACK.defaultBlockState(),Blocks.CRIMSON_NYLIUM.defaultBlockState(),0,false);
            if(path.contains("warped"))return new Palette(Blocks.NETHERRACK.defaultBlockState(),Blocks.NETHERRACK.defaultBlockState(),Blocks.WARPED_NYLIUM.defaultBlockState(),0,false);
            if(path.contains("soul"))return new Palette(Blocks.NETHERRACK.defaultBlockState(),Blocks.SOUL_SOIL.defaultBlockState(),Blocks.SOUL_SAND.defaultBlockState(),3,false);
            if(path.contains("basalt"))return new Palette(Blocks.BLACKSTONE.defaultBlockState(),Blocks.BASALT.defaultBlockState(),Blocks.BASALT.defaultBlockState(),3,false);
            return new Palette(Blocks.NETHERRACK.defaultBlockState(),Blocks.NETHERRACK.defaultBlockState(),Blocks.NETHERRACK.defaultBlockState(),0,false);
        }
        BlockState stone=Blocks.STONE.defaultBlockState(),dirt=Blocks.DIRT.defaultBlockState();
        if(path.contains("deep_dark"))return new Palette(Blocks.DEEPSLATE.defaultBlockState(),Blocks.SCULK.defaultBlockState(),Blocks.SCULK.defaultBlockState(),1,false);
        if(path.contains("lush_caves"))return new Palette(stone,Blocks.MOSS_BLOCK.defaultBlockState(),Blocks.MOSS_BLOCK.defaultBlockState(),2,false);
        if(path.contains("dripstone"))return new Palette(stone,Blocks.DRIPSTONE_BLOCK.defaultBlockState(),Blocks.DRIPSTONE_BLOCK.defaultBlockState(),2,false);
        if(biome.is(BiomeTags.IS_OCEAN)||biome.is(BiomeTags.IS_DEEP_OCEAN))return new Palette(stone,Blocks.SAND.defaultBlockState(),Blocks.SAND.defaultBlockState(),3,true);
        if(biome.is(BiomeTags.IS_RIVER))return new Palette(stone,Blocks.GRAVEL.defaultBlockState(),Blocks.SAND.defaultBlockState(),2,true);
        if(biome.is(BiomeTags.IS_BEACH)||path.contains("desert"))return new Palette(Blocks.SANDSTONE.defaultBlockState(),Blocks.SAND.defaultBlockState(),Blocks.SAND.defaultBlockState(),3,false);
        if(biome.is(BiomeTags.IS_BADLANDS))return new Palette(Blocks.TERRACOTTA.defaultBlockState(),Blocks.ORANGE_TERRACOTTA.defaultBlockState(),Blocks.RED_SAND.defaultBlockState(),3,false);
        if(path.contains("mushroom"))return new Palette(stone,dirt,Blocks.MYCELIUM.defaultBlockState(),3,false);
        if(path.contains("mangrove"))return new Palette(stone,Blocks.MUD.defaultBlockState(),Blocks.MUD.defaultBlockState(),3,false);
        if(path.contains("peaks")||path.contains("stony"))return new Palette(stone,stone,stone,0,false);
        if(path.contains("snowy_slopes")||path.contains("ice_spikes")||path.contains("frozen"))return new Palette(stone,Blocks.PACKED_ICE.defaultBlockState(),Blocks.SNOW_BLOCK.defaultBlockState(),2,false);
        return new Palette(stone,dirt,Blocks.GRASS_BLOCK.defaultBlockState(),3,false);
    }
    /** the block at height y of column (x,z), air if none */
    BlockState blockAt(int x,int y,int z) {
        var island=SpiritIslands.islandAt(x,z);
        if(island==null)return Blocks.AIR.defaultBlockState();
        int[] column=island.column(x,z);
        if(y<column[0]||y>column[1])return Blocks.AIR.defaultBlockState();
        var biomes=spiritBiomes();
        var palette=biomes==null?palette(null):palette(biomes.biomeOfCell(island.cellX(),island.cellZ()));
        int surface=column[1];
        if(palette.pond()){
            double dx=x+0.5-island.centerX(),dz=z+0.5-island.centerZ();
            if(dx*dx+dz*dz<Math.pow(island.radius()*0.45,2)){
                if(y>surface-1)return Blocks.AIR.defaultBlockState();
                if(y>=surface-3)return Blocks.WATER.defaultBlockState();
                if(y>=surface-5)return palette.filler();
                return palette.stone();
            }
        }
        int fromTop=surface-y;
        if(fromTop==0)return palette.top();
        if(fromTop<=palette.fillerDepth())return palette.filler();
        return palette.stone();
    }

    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk) {
        var pos=new BlockPos.MutableBlockPos();
        Heightmap ocean=chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG),surface=chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int baseX=chunk.getPos().getMinBlockX(),baseZ=chunk.getPos().getMinBlockZ();
        for(int lx=0;lx<16;lx++)for(int lz=0;lz<16;lz++){
            int x=baseX+lx,z=baseZ+lz;
            var island=SpiritIslands.islandAt(x,z);if(island==null)continue;
            int[] column=island.column(x,z);
            for(int y=Math.max(MIN_Y,column[0]);y<=Math.min(MIN_Y+HEIGHT-1,column[1]);y++){
                var state=blockAt(x,y,z);if(state.isAir())continue;
                chunk.setBlockState(pos.set(x,y,z),state,false);
                ocean.update(lx,y,lz,state);surface.update(lx,y,lz,state);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor level,RandomState random) {
        var island=SpiritIslands.islandAt(x,z);
        if(island==null)return level.getMinBuildHeight();
        int[] column=island.column(x,z);
        return column[1]+1;
    }
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor level,RandomState random) {
        var states=new BlockState[level.getHeight()];
        for(int i=0;i<states.length;i++)states[i]=blockAt(x,level.getMinBuildHeight()+i,z);
        return new NoiseColumn(level.getMinBuildHeight(),states);
    }
    /** the islands are decorated by their biomes first; then the sky's own buildings go up, untouched by trees and flowers */
    @Override public void applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel level,ChunkAccess chunk,StructureManager structures) {
        super.applyBiomeDecoration(level,chunk,structures);
        SpiritSkyStructures.place(level,chunk);
    }
    /** no creatures come with the chunk: the realm's spirits are brought by SpiritSpawner, from any world */
    @Override public void spawnOriginalMobs(WorldGenRegion region) {}
    @Override public void applyCarvers(WorldGenRegion region,long seed,RandomState random,BiomeManager biomes,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving step) {}
    @Override public void buildSurface(WorldGenRegion region,StructureManager structures,RandomState random,ChunkAccess chunk) {}
    @Override public int getGenDepth() {return HEIGHT;}
    @Override public int getSeaLevel() {return MIN_Y-1;}
    @Override public int getMinY() {return MIN_Y;}
    @Override public void addDebugScreenInfo(List<String> lines,RandomState random,BlockPos pos) {
        var island=SpiritIslands.islandAt(pos.getX(),pos.getZ());
        lines.add("Spirit island: "+(island==null?"none":island.cellX()+","+island.cellZ()));
    }
}
