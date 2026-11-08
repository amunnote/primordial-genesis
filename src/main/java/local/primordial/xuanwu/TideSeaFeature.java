package local.primordial.xuanwu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
public final class TideSeaFeature extends Feature<NoneFeatureConfiguration> {
    enum Kind {REEF,ICE,ROCK}
    private final Kind kind;
    TideSeaFeature(Kind k){super(NoneFeatureConfiguration.CODEC);kind=k;}
    // Same nearest-two hex-centre boundary test as the delivered reef blueprint.
    public static boolean seam(int x,int z,double spacing){double first=Double.MAX_VALUE,second=Double.MAX_VALUE;for(int a=-12;a<=12;a++)for(int b=-12;b<=12;b++){
        double dx=x-spacing*Math.sqrt(3)*(a+b/2.0),dz=z-spacing*1.5*b,d=dx*dx+dz*dz;if(d<first){second=first;first=d;}else if(d<second)second=d;
    }return second-first<spacing*1.7;}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        if(local.primordial.qinglong.GuardianAnchors.nearSeat(c.origin(),64))return false;
        var l=c.level();var o=c.origin();var rand=c.random();int sea=c.chunkGenerator().getSeaLevel(),bed=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,o.getX(),o.getZ())-1;
        if(bed>=sea||!l.getBiome(new BlockPos(o.getX(),sea-2,o.getZ())).is(XuanwuWorldgen.BIOME))return false;
        int radius=kind==Kind.REEF?7:kind==Kind.ROCK?3:2,h=6+rand.nextInt(9);
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)
            if(l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,o.getX()+dx,o.getZ()+dz)-1>=sea)return false;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            double d=Math.hypot(dx,dz);if(d>radius)continue;int x=o.getX()+dx,z=o.getZ()+dz;
            int bottom=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
            int top=kind==Kind.REEF?sea+(d<5?2:1):kind==Kind.ROCK?bottom+2:sea+h-(int)(d*3);
            for(int y=bottom;y<=top;y++){
                Block b=kind==Kind.ICE?(y>sea?((y&1)==0?Blocks.BLUE_ICE:Blocks.PACKED_ICE):XuanwuContent.ROCK.get()):y==top?(seam(dx,dz,2.5)?Blocks.DEEPSLATE_TILES:XuanwuContent.ROCK.get()):Blocks.STONE;
                l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);
            }
        }
        // Living tube coral is placed under water at the reef foot, not exposed on the dry cap.
        if(kind==Kind.REEF)for(int dx=-2;dx<=2;dx++)l.setBlock(new BlockPos(o.getX()+dx,sea-2,o.getZ()+6),Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),2);
        return true;
    }
}
