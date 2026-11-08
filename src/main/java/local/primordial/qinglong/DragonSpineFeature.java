package local.primordial.qinglong;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Each chunk writes only its own slice of a seeded 150-300 block ridge.
 * The same 512-block cell defines its entire path: never writes across the generation region. */
public final class DragonSpineFeature extends Feature<NoneFeatureConfiguration> {
    public DragonSpineFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var l=c.level();int chunkX=c.origin().getX()>>4,chunkZ=c.origin().getZ()>>4;
        int cellX=Math.floorDiv(c.origin().getX(),512),cellZ=Math.floorDiv(c.origin().getZ(),512);
        long seed=l.getSeed()^cellX*341873128712L^cellZ*132897987541L;
        var r=net.minecraft.util.RandomSource.create(seed);int length=150+r.nextInt(151),width=7+r.nextInt(5),height=25+r.nextInt(21);
        int startX=cellX*512+96+r.nextInt(64),startZ=cellZ*512+160+r.nextInt(160);double phase=r.nextDouble()*6.28;boolean wrote=false;
        for(int x=chunkX*16;x<chunkX*16+16;x++){
            int t=x-startX;if(t<0||t>=length)continue;
            double spineZ=startZ+Math.sin(t/35.0+phase)*23+Math.sin(t/13.0)*4;
            for(int z=chunkZ*16;z<chunkZ*16+16;z++){
                if(!l.getBiome(new BlockPos(x,100,z)).is(QinglongWorldgen.BIOME))continue;
                if(GuardianAnchors.nearSeat(new BlockPos(x,0,z),64))continue;   // not through a court
                double d=Math.abs(z-spineZ),half=width/2.0;if(d>half)continue;
                int base=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;if(base<64)continue;
                double end=Math.min(1,Math.min((t+1)/18.0,(length-t)/18.0));
                int rise=(int)(height*end*(0.65+0.35*Math.cos(d/half*Math.PI/2))+Math.sin(t/9.0)*2);
                for(int y=base;y<=Math.min(base+rise,l.getMaxBuildHeight()-2);y++){
                    var b=y==base+rise&&t%8<2?Blocks.OXIDIZED_CUT_COPPER:y>base+rise-3?Blocks.CALCITE:Blocks.STONE;
                    if(y==base+rise&&t%13==0)b=QinglongContent.CLOUDROCK.get();
                    l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);wrote=true;
                }
            }
        }return wrote;
    }
}
