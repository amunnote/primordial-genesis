package local.primordial.qinglong;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

/**
 * Where each of the four guardians holds court: one seat a world, in its own quarter (the Azure Dragon east, the White
 * Tiger west, the Vermilion Bird south, the Black Tortoise north), 1600 to 3200 blocks out. The seat is chosen once,
 * before the levels are made, from the terrain the world will have (the tortoise wants deep sea, the others fairly
 * level land, all of them their own biome if it lies near) and kept in the save, so it never moves when the mods
 * change; in an old world it keeps clear of the regions already generated. Round each seat the guardian's biome is
 * laid over whatever was there (see MultiNoiseGuardianMixin), so the court always stands in its own land.
 *
 * Beyond the seats the four lands lie in wide territories, a few to a world: every other 2560-block cell or so holds
 * one, 1000 to 1500 blocks across. A sea short of the warm ones is the Black Tortoise's; land goes to one of the other three
 * by lot. Inside it the guardian takes its own kind of ground, all the land or all the sea, and rivers and coasts run
 * through as they were.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=local.primordial.Primordial.ID)
public final class GuardianAnchors {
    private static final Logger LOGGER=LogUtils.getLogger();
    public enum Seat {
        QINGLONG(0,QinglongWorldgen.BIOME,false,1),BAIHU(180,local.primordial.baihu.BaihuWorldgen.BIOME,false,2),
        ZHUQUE(90,local.primordial.zhuque.ZhuqueWorldgen.BIOME,false,3),XUANWU(270,local.primordial.xuanwu.XuanwuWorldgen.BIOME,true,4);
        /** degrees, x=cos z=sin: east 0, south 90, west 180, north 270 */
        final double heading;public final ResourceKey<Biome> biome;final boolean sea;final long salt;
        Seat(double heading,ResourceKey<Biome> biome,boolean sea,long salt){this.heading=heading;this.biome=biome;this.sea=sea;this.salt=salt;}
        public static Seat of(String name){for(var s:values())if(s.name().equalsIgnoreCase(name))return s;throw new IllegalArgumentException("Unknown guardian seat "+name);}
    }
    /** the guardian's own land round its seat, in blocks (its edge wavers by up to a quarter of this) */
    public static final int RADIUS=176;
    private static final String FILE="angel_charm_guardian_seats.dat";

    private static volatile Map<Seat,ChunkPos> seats=Map.of();
    private static volatile Map<Seat,Holder<Biome>> biomes=Map.of();
    private static volatile BiomeSource overworldBiomes;
    private static volatile ChunkGenerator overworldGenerator;
    private static volatile BiomeSource overworldOuter;
    private static volatile net.minecraft.world.level.levelgen.NoiseGeneratorSettings overworldSettings;
    private static volatile long seed;
    private GuardianAnchors() {}

    public static ChunkPos seat(Seat seat) {return seats.get(seat);}
    public static boolean overworld(ChunkGenerator generator) {return generator!=null&&generator==overworldGenerator;}
    /** whether these are the overworld's noise settings, whose surface rules take the four lands' ground first */
    public static boolean overworldSettings(net.minecraft.world.level.levelgen.NoiseGeneratorSettings settings) {return settings!=null&&settings==overworldSettings;}

    /** a chunk's biome resolver with the four lands first, when it is another mod's wrapper round the overworld's source */
    public static net.minecraft.world.level.biome.BiomeResolver resolver(net.minecraft.world.level.biome.BiomeResolver resolver) {
        var inner=overworldBiomes;
        if(resolver!=overworldOuter||inner==null||resolver==inner)return resolver;
        return (x,y,z,sampler)->{var land=forced(inner,x,y,z,sampler);return land!=null?land:resolver.getNoiseBiome(x,y,z,sampler);};
    }
    /** whether a block lies within this many blocks of a court's heart: landmarks keep out of the arenas */
    public static boolean nearSeat(net.minecraft.core.BlockPos pos,int radius) {
        for(var chunk:seats.values()){double dx=pos.getX()-chunk.getMiddleBlockX(),dz=pos.getZ()-chunk.getMiddleBlockZ();if(dx*dx+dz*dz<(double)radius*radius)return true;}
        return false;
    }
    /** the guardian's biome at this quart position of the overworld's own multi-noise source, or null */
    public static Holder<Biome> forced(BiomeSource source,int quartX,int quartY,int quartZ,Climate.Sampler sampler) {
        if(source!=overworldBiomes||quartY<0)return null;
        int x=quartX<<2,z=quartZ<<2;
        for(var entry:seats.entrySet()){
            var chunk=entry.getValue();
            double dx=x-chunk.getMiddleBlockX(),dz=z-chunk.getMiddleBlockZ(),far=dx*dx+dz*dz;
            if(far>RADIUS*RADIUS*1.6)continue;
            if(far<edge(dx,dz,entry.getKey().salt*1.7+(seed&1023)*0.01,RADIUS))return biomes.get(entry.getKey());
        }
        // the wide lands: a territory takes its own kind of ground and leaves the rest (rivers, coasts, the other kind)
        var land=territory(Math.floorDiv(x,CELL),Math.floorDiv(z,CELL),sampler);
        if(land==null)return null;
        double dx=x-land.x,dz=z-land.z;
        if(dx*dx+dz*dz>=edge(dx,dz,land.phase,land.radius))return null;
        var climate=sampler.sample(quartX,quartY,quartZ);
        float continent=Climate.unquantizeCoord(climate.continentalness()),weird=Climate.unquantizeCoord(climate.weirdness());
        boolean fits=land.seat.sea?continent>=-1.05F&&continent< -0.19F:continent>=-0.11F&&Math.abs(weird)>=0.05F;
        return fits?biomes.get(land.seat):null;
    }
    /** the squared radius of a wavering edge in this direction */
    private static double edge(double dx,double dz,double phase,double radius) {
        double a=Math.atan2(dz,dx),r=radius*(1+0.14*Math.sin(3*a+phase)+0.08*Math.sin(7*a+phase*2)+0.035*Math.sin(17*a+phase*3)+0.02*Math.sin(31*a+phase*5));
        return r*r;
    }

    // ------------------------------------------------------------------ the wide lands
    /** one cell in this many blocks a side may hold a territory; it lies wholly inside its cell */
    public static final int CELL=2560;
    // the share of cells that hold one, and how far one reaches (before its edge wavers): WorldConfig
    record Territory(Seat seat,int x,int z,int radius,double phase) {}
    private static final Territory NONE=new Territory(null,0,0,0,0);
    private static final java.util.concurrent.ConcurrentHashMap<Long,Territory> TERRITORIES=new java.util.concurrent.ConcurrentHashMap<>();
    /** the territory in this cell, or null; its kind is the climate at its heart */
    static Territory territory(int cellX,int cellZ,Climate.Sampler sampler) {
        var land=TERRITORIES.computeIfAbsent(ChunkPos.asLong(cellX,cellZ),key->{
            var random=RandomSource.create(seed^cellX*341873128712L^cellZ*132897987541L^0x5EA7L);
            if(random.nextDouble()>=WorldConfig.get(WorldConfig.TERRITORY_CHANCE))return NONE;
            int x=cellX*CELL+920+random.nextInt(CELL-1840),z=cellZ*CELL+920+random.nextInt(CELL-1840);
            if((long)x*x+(long)z*z<1200L*1200L)return NONE;   // the land round spawn stays as it is
            var climate=sampler.sample(x>>2,64>>2,z>>2);
            float t=Climate.unquantizeCoord(climate.temperature()),c=Climate.unquantizeCoord(climate.continentalness());
            // a sea short of the warm ones is the tortoise's; land goes to the other three in equal shares, so no one climate
            // of a world crowds the others out (the land is laid over the ground, whatever its climate)
            Seat seat=c< -0.19F?(t<0.2F?Seat.XUANWU:null):new Seat[]{Seat.QINGLONG,Seat.BAIHU,Seat.ZHUQUE}[random.nextInt(3)];
            int min=WorldConfig.get(WorldConfig.TERRITORY_MIN_RADIUS),max=Math.max(min,WorldConfig.get(WorldConfig.TERRITORY_MAX_RADIUS));
            return seat==null?NONE:new Territory(seat,x,z,min+random.nextInt(max-min+1),random.nextDouble()*6.28);
        });
        return land==NONE?null:land;
    }

    // ------------------------------------------------------------------ what the overworld can make
    /** the registry's holders, from before anything asks a biome source what it can make */
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void aboutToStart(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
        var registry=event.getServer().registryAccess().registryOrThrow(Registries.BIOME);
        var holders=new EnumMap<Seat,Holder<Biome>>(Seat.class);
        for(var s:Seat.values())registry.getHolder(s.biome).ifPresent(h->holders.put(s,h));
        biomes=holders;TERRITORIES.clear();seats=Map.of();overworldBiomes=null;overworldGenerator=null;overworldSettings=null;overworldOuter=null;
    }
    /** an overworld-like multi-noise source can make the four lands too (they are laid over it, see forced) */
    public static java.util.stream.Stream<Holder<Biome>> withGuardianLands(java.util.List<Holder<Biome>> made) {
        if(biomes.isEmpty()||made.stream().noneMatch(h->h.is(net.minecraft.world.level.biome.Biomes.PLAINS)))return made.stream();
        return java.util.stream.Stream.concat(made.stream(),biomes.values().stream()).distinct();
    }

    /** the multi-noise source held in a wrapper's fields (another mod's, round the overworld's own), a few wrappers deep at most */
    public static MultiNoiseBiomeSource unwrap(BiomeSource source,int depth) {
        if(source instanceof MultiNoiseBiomeSource multi)return multi;
        if(source==null||depth<=0)return null;
        for(Class<?> type=source.getClass();type!=null&&type!=BiomeSource.class;type=type.getSuperclass())
            for(var field:type.getDeclaredFields()){
                if(java.lang.reflect.Modifier.isStatic(field.getModifiers())||!BiomeSource.class.isAssignableFrom(field.getType()))continue;
                try{
                    field.setAccessible(true);
                    var found=unwrap((BiomeSource)field.get(source),depth-1);
                    if(found!=null)return found;
                }catch(ReflectiveOperationException|RuntimeException ignored){}
            }
        return null;
    }

    /** before the levels are made: find (or read back) the four seats */
    public static void prepare(MinecraftServer server) {
        seats=Map.of();overworldBiomes=null;overworldGenerator=null;overworldSettings=null;overworldOuter=null;
        var access=server.registryAccess();
        var stem=access.registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
        if(stem==null||!(stem.generator() instanceof NoiseBasedChunkGenerator generator))return;
        // the ground goes on whatever the source: settings another mod put in its place included
        overworldSettings=generator.generatorSettings().value();
        var inner=unwrap(generator.getBiomeSource(),3);
        if(inner==null)return;
        var registry=access.registryOrThrow(Registries.BIOME);
        var holders=new EnumMap<Seat,Holder<Biome>>(Seat.class);
        for(var s:Seat.values()){var h=registry.getHolder(s.biome);if(h.isEmpty())return;holders.put(s,h.get());}
        seed=server.getWorldData().worldGenOptions().seed();
        var file=server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE);
        var found=new EnumMap<Seat,ChunkPos>(Seat.class);
        try{
            if(Files.exists(file)){var tag=NbtIo.readCompressed(file.toFile());for(var s:Seat.values())if(tag.contains(s.name()))found.put(s,new ChunkPos(tag.getLong(s.name())));}
        }catch(Exception e){LOGGER.warn("Could not read the guardian seats, choosing them again",e);found.clear();}
        if(found.size()<Seat.values().length){
            var type=stem.type().value();
            var heights=LevelHeightAccessor.create(type.minY(),type.height());
            var state=RandomState.create(generator.generatorSettings().value(),access.lookupOrThrow(Registries.NOISE),seed);
            var regions=server.getWorldPath(LevelResource.ROOT).resolve("region");
            for(var s:Seat.values())if(!found.containsKey(s))found.put(s,choose(s,generator,inner,state,heights,regions,holders.get(s)));
            try{
                var tag=new CompoundTag();found.forEach((s,c)->tag.putLong(s.name(),c.toLong()));
                Files.createDirectories(file.getParent());NbtIo.writeCompressed(tag,file.toFile());
            }catch(Exception e){LOGGER.warn("Could not save the guardian seats",e);}
        }
        biomes=holders;seats=Map.copyOf(found);overworldBiomes=inner;overworldGenerator=generator;overworldOuter=generator.getBiomeSource();
        found.forEach((s,c)->LOGGER.info("Guardian seat {} at {} {}",s,c.getMiddleBlockX(),c.getMiddleBlockZ()));
    }

    private static ChunkPos choose(Seat seat,NoiseBasedChunkGenerator generator,BiomeSource biomes,RandomState state,LevelHeightAccessor heights,
            java.nio.file.Path regions,Holder<Biome> own) {
        var random=RandomSource.create(seed^seat.salt*0x9E3779B97F4A7C15L);
        int sea=generator.getSeaLevel();
        ChunkPos best=null;double bestScore=Double.NEGATIVE_INFINITY;
        // rings outward: an old world pushes the seat past what it has generated
        int first=WorldConfig.get(WorldConfig.SEAT_DISTANCE);
        for(int start=first;start<=first+8000&&best==null;start+=1600){
            int spread=seat.sea?4:2;   // open sea is rarer: the tortoise looks wider round its heading
            for(int distance=start;distance<=start+1600;distance+=200)for(int step=-spread;step<=spread;step++){
                double a=Math.toRadians(seat.heading+step*12.5+random.nextDouble()*4-2);
                int x=(int)(Math.cos(a)*distance),z=(int)(Math.sin(a)*distance);
                var chunk=new ChunkPos(x>>4,z>>4);
                if(Files.exists(regions.resolve("r."+(chunk.x>>5)+"."+(chunk.z>>5)+".mca")))continue;
                int cx=chunk.getMiddleBlockX(),cz=chunk.getMiddleBlockZ();
                int centre=generator.getBaseHeight(cx,cz,Heightmap.Types.OCEAN_FLOOR_WG,heights,state);
                int low=centre,high=centre;
                for(int k=0;k<8;k++){
                    double b=k*Math.PI/4;
                    int h=generator.getBaseHeight(cx+(int)(Math.cos(b)*40),cz+(int)(Math.sin(b)*40),Heightmap.Types.OCEAN_FLOOR_WG,heights,state);
                    low=Math.min(low,h);high=Math.max(high,h);
                }
                double score=-Math.abs(distance-2200)/400.0-Math.abs(step)*0.3+random.nextDouble()*0.2;
                if(seat.sea){
                    if(high>=sea-4)continue;   // the tortoise's court floats on open sea
                    score+=Mth.clamp((sea-high)/10.0,0,3);
                }else{
                    if(low<sea+1||centre>150)continue;   // dry, not a peak
                    score-=(high-low)/8.0;
                }
                var sampler=state.sampler();
                if(biomes.getNoiseBiome(cx>>2,(Math.max(centre,sea))>>2,cz>>2,sampler)==own)score+=4;
                if(score>bestScore){bestScore=score;best=chunk;}
            }
        }
        if(best==null){   // nothing fit: straight out on the heading
            double a=Math.toRadians(seat.heading);best=new ChunkPos(((int)(Math.cos(a)*2200))>>4,((int)(Math.sin(a)*2200))>>4);
        }
        return best;
    }
}
