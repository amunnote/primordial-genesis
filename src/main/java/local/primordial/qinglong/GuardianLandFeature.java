package local.primordial.qinglong;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The landmarks that make each guardian's land its own, drawn in code. Each stays within 14 blocks of its origin, so a
 * feature never writes past the chunks next to its own.
 * <ul>
 * <li>Azure Dragon: floating thunder-rocks (grassy islets of copper-veined cloudrock hung with vines and azalea) and
 *     lightning-struck elders (bare dark-oak trunks crowned with a rod, glow lichen in their scars).</li>
 * <li>White Tiger: giant swords driven into the ground, and stone forests of blade-sharp spires.</li>
 * <li>Vermilion Bird: lava rifts (a sunken crack with a sealed lava floor), and young wutong shrubs.</li>
 * <li>Black Tortoise: shell islets (hex-seamed domes rising from the sea), ice floes, and jade spires glowing on the
 *     sea floor.</li>
 * </ul>
 */
public final class GuardianLandFeature extends Feature<NoneFeatureConfiguration> {
    public enum Kind {FLOATING_ROCK,STRUCK_ELDER,GIANT_SWORD,STONE_FOREST,LAVA_RIFT,WUTONG_SHRUB,SHELL_ISLET,ICE_FLOE,JADE_SPIRES}
    private final Kind kind;
    public GuardianLandFeature(Kind kind) {super(NoneFeatureConfiguration.CODEC);this.kind=kind;}

    private static void put(WorldGenLevel l,BlockPos p,BlockState s) {if(l.ensureCanWrite(p))l.setBlock(p,s,2);}
    private static boolean replaceable(WorldGenLevel l,BlockPos p) {var s=l.getBlockState(p);return s.isAir()||s.canBeReplaced();}
    private static int ground(WorldGenLevel l,int x,int z) {return l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;}

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
        if(GuardianAnchors.nearSeat(c.origin(),64))return false;
        var l=c.level();var o=c.origin();var r=c.random();
        if(o.getY()<=l.getMinBuildHeight()+8||o.getY()>=l.getMaxBuildHeight()-48)return false;
        return switch(kind){
            case FLOATING_ROCK->floatingRock(l,o,r);
            case STRUCK_ELDER->struckElder(l,o,r);
            case GIANT_SWORD->giantSword(l,o,r);
            case STONE_FOREST->stoneForest(l,o,r);
            case LAVA_RIFT->lavaRift(l,o,r);
            case WUTONG_SHRUB->wutongShrub(l,o,r);
            case SHELL_ISLET->shellIslet(l,o,r,c.chunkGenerator().getSeaLevel());
            case ICE_FLOE->iceFloe(l,o,r,c.chunkGenerator().getSeaLevel());
            case JADE_SPIRES->jadeSpires(l,o,r,c.chunkGenerator().getSeaLevel());
        };
    }

    // ------------------------------------------------------------------ the Azure Dragon
    private static boolean floatingRock(WorldGenLevel l,BlockPos o,RandomSource r) {
        int rx=5+r.nextInt(5),rz=5+r.nextInt(5),depth=5+r.nextInt(4);
        var top=o.above(26+r.nextInt(18));
        if(top.getY()+6>=l.getMaxBuildHeight())return false;
        for(int x=-rx;x<=rx;x++)for(int z=-rz;z<=rz;z++)for(int y=-depth;y<=4;y++)if(!l.isEmptyBlock(top.offset(x,y,z)))return false;
        var rock=QinglongContent.CLOUDROCK.get().defaultBlockState();
        for(int x=-rx;x<=rx;x++)for(int z=-rz;z<=rz;z++){
            double d=(double)x*x/(rx*rx)+(double)z*z/(rz*rz);if(d>1)continue;
            // a cone underneath: deeper at the heart, ragged at the rim
            int under=(int)Math.round(depth*(1-Math.sqrt(d))+r.nextInt(2));
            for(int y=-under;y<=0;y++){
                BlockState s=y==0?Blocks.GRASS_BLOCK.defaultBlockState():y>=-2?Blocks.DIRT.defaultBlockState():(r.nextInt(4)==0?Blocks.STONE:rock.getBlock()).defaultBlockState();
                put(l,top.offset(x,y,z),s);
            }
            var above=top.offset(x,1,z);
            if(d<0.6&&r.nextInt(9)==0)put(l,above,(r.nextBoolean()?Blocks.FLOWERING_AZALEA:Blocks.AZALEA).defaultBlockState());
            else if(r.nextInt(5)==0)put(l,above,Blocks.MOSS_CARPET.defaultBlockState());
            else if(r.nextInt(7)==0)put(l,above,Blocks.BLUE_ORCHID.defaultBlockState());
            // vines hang from the rim
            if(d>0.7&&r.nextInt(3)==0)for(var dir:Direction.Plane.HORIZONTAL){
                var side=top.offset(x,-1,z).relative(dir);
                if(l.isEmptyBlock(side)){int len=2+r.nextInt(5);
                    for(int k=0;k<len&&l.isEmptyBlock(side.below(k));k++)put(l,side.below(k),Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(dir.getOpposite()),true));
                    break;}
            }
        }
        // a copper rod at the heart calls the storm down on it
        if(r.nextInt(3)==0){put(l,top.above(1),Blocks.OXIDIZED_CUT_COPPER.defaultBlockState());put(l,top.above(2),Blocks.LIGHTNING_ROD.defaultBlockState());}
        return true;
    }
    private static boolean struckElder(WorldGenLevel l,BlockPos o,RandomSource r) {
        if(!l.getBlockState(o.below()).isSolidRender(l,o.below()))return false;
        int h=8+r.nextInt(6);
        for(int y=0;y<h+3;y++)if(!replaceable(l,o.above(y)))return false;
        var log=Blocks.DARK_OAK_LOG.defaultBlockState();var bare=Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
        for(int y=0;y<h;y++){
            put(l,o.above(y),y>h-4||y%5==2?bare:log);
            // the scar the bolt left glows
            if(y%3==1){var dir=Direction.Plane.HORIZONTAL.getRandomDirection(r);var side=o.above(y).relative(dir);
                if(l.isEmptyBlock(side))put(l,side,Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(dir.getOpposite()),true));}
        }
        // two broken limbs and a few clumps of leaves left
        for(int k=0;k<2;k++){
            var dir=Direction.Plane.HORIZONTAL.getRandomDirection(r);var at=o.above(h-2-k*2);
            for(int n=1;n<=2+r.nextInt(2);n++){at=at.relative(dir);if(n==2)at=at.above();if(replaceable(l,at))put(l,at,(n==1?bare:log).setValue(RotatedPillarBlock.AXIS,dir.getAxis()));}
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(r.nextInt(3)>0&&replaceable(l,at.offset(x,1,z)))put(l,at.offset(x,1,z),Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));
        }
        put(l,o.above(h),Blocks.LIGHTNING_ROD.defaultBlockState());
        // scorched ground round the foot
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){if(x*x+z*z>9||r.nextInt(3)==0)continue;
            var g=new BlockPos(o.getX()+x,ground(l,o.getX()+x,o.getZ()+z),o.getZ()+z);
            if(Math.abs(g.getY()-o.getY()+1)<=2&&l.getBlockState(g).is(Blocks.GRASS_BLOCK))put(l,g,(r.nextBoolean()?Blocks.COARSE_DIRT:Blocks.ROOTED_DIRT).defaultBlockState());}
        return true;
    }

    // ------------------------------------------------------------------ the White Tiger
    private static boolean giantSword(WorldGenLevel l,BlockPos o,RandomSource r) {
        if(!l.getBlockState(o.below()).isSolidRender(l,o.below()))return false;
        boolean alongX=r.nextBoolean();int blade=16+r.nextInt(9),buried=4;
        for(int y=0;y<blade+10;y++)if(!replaceable(l,o.above(y)))return false;
        var edge=Blocks.IRON_BLOCK.defaultBlockState();var core=local.primordial.baihu.BaihuContent.BLADESTONE.get().defaultBlockState();
        for(int y=-buried;y<blade;y++){
            int half=y<blade-4?1:0;   // the point narrows to one
            for(int w=-half;w<=half;w++){var p=alongX?o.offset(w,y,0):o.offset(0,y,w);put(l,p,w==0?core:edge);}
        }
        // the guard, the grip and the pommel
        int guard=blade;
        for(int w=-3;w<=3;w++){var p=alongX?o.offset(w,guard,0):o.offset(0,guard,w);put(l,p,(Math.abs(w)==3?Blocks.GOLD_BLOCK:Blocks.POLISHED_DEEPSLATE).defaultBlockState());}
        for(int y=1;y<=5;y++)put(l,o.above(guard+y),(y%2==0?Blocks.CHAIN.defaultBlockState():Blocks.POLISHED_BLACKSTONE.defaultBlockState()));
        put(l,o.above(guard+6),Blocks.GOLD_BLOCK.defaultBlockState());
        // the ground cracked where it struck
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){if(x*x+z*z>16||r.nextInt(3)==0)continue;
            int y=ground(l,o.getX()+x,o.getZ()+z);var g=new BlockPos(o.getX()+x,y,o.getZ()+z);
            if(Math.abs(y-o.getY()+1)<=3)put(l,g,(r.nextInt(3)==0?Blocks.GRAVEL:Blocks.COARSE_DIRT).defaultBlockState());}
        return true;
    }
    private static boolean stoneForest(WorldGenLevel l,BlockPos o,RandomSource r) {
        int spires=4+r.nextInt(6);boolean any=false;
        for(int i=0;i<spires;i++){
            int x=o.getX()+r.nextInt(17)-8,z=o.getZ()+r.nextInt(17)-8,y=ground(l,x,z);
            var base=new BlockPos(x,y,z);
            if(Math.abs(y-o.getY())>6||!l.getBlockState(base).isSolidRender(l,base))continue;
            int h=6+r.nextInt(12),foot=h>12?2:1;
            for(int k=1;k<=h;k++){
                int radius=k<h*0.35?foot:k<h*0.75?Math.min(foot,1):0;
                var s=(k%4==0?local.primordial.baihu.BaihuContent.BLADESTONE.get():k<3?Blocks.TUFF:Blocks.CALCITE).defaultBlockState();
                for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)if(dx*dx+dz*dz<=radius*radius&&replaceable(l,base.offset(dx,k,dz)))put(l,base.offset(dx,k,dz),s);
            }
            any=true;
        }
        return any;
    }

    // ------------------------------------------------------------------ the Vermilion Bird
    private static boolean lavaRift(WorldGenLevel l,BlockPos o,RandomSource r) {
        boolean alongX=r.nextBoolean();int half=8+r.nextInt(5),depth=5+r.nextInt(3);
        int surface=o.getY()-1;
        for(int t=-half;t<=half;t++){
            double taper=1-Math.abs(t)/(double)(half+1);
            int width=Math.max(0,(int)Math.round(taper*2.4+Math.sin(t*0.9)*0.6));
            int deep=Math.max(2,(int)Math.round(depth*Math.sqrt(taper)));
            for(int w=-width-1;w<=width+1;w++){
                int x=o.getX()+(alongX?t:w),z=o.getZ()+(alongX?w:t),top=ground(l,x,z);
                if(Math.abs(top-surface)>3)continue;
                boolean inside=Math.abs(w)<=width;
                if(!inside){   // the lip: scorched stone to the floor of the crack
                    for(int y=top-deep;y<=top;y++)put(l,new BlockPos(x,y,z),(y==top?Blocks.RED_SANDSTONE:r.nextInt(3)==0?Blocks.MAGMA_BLOCK:Blocks.BLACKSTONE).defaultBlockState());
                    continue;
                }
                for(int y=top-deep+2;y<=top;y++)put(l,new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                put(l,new BlockPos(x,top-deep+1,z),(taper>0.35?Blocks.LAVA:Blocks.MAGMA_BLOCK).defaultBlockState());
                put(l,new BlockPos(x,top-deep,z),Blocks.BLACKSTONE.defaultBlockState());
            }
            // the ends close over the lava
            if(Math.abs(t)==half)for(int w=-3;w<=3;w++){int x=o.getX()+(alongX?t:w),z=o.getZ()+(alongX?w:t),top=ground(l,x,z);
                for(int y=top-deep;y<=top;y++)if(l.getBlockState(new BlockPos(x,y,z)).isAir()||l.getFluidState(new BlockPos(x,y,z)).isSource())put(l,new BlockPos(x,y,z),Blocks.BLACKSTONE.defaultBlockState());}
        }
        return true;
    }
    private static boolean wutongShrub(WorldGenLevel l,BlockPos o,RandomSource r) {
        if(!l.getBlockState(o.below()).isSolidRender(l,o.below()))return false;
        int h=2+r.nextInt(2);
        for(int y=0;y<h+3;y++)if(!replaceable(l,o.above(y)))return false;
        var log=local.primordial.zhuque.ZhuqueContent.LOG.get().defaultBlockState();
        var leaf=local.primordial.zhuque.ZhuqueContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
        for(int y=0;y<h;y++)put(l,o.above(y),log);
        for(int x=-2;x<=2;x++)for(int y=-1;y<=1;y++)for(int z=-2;z<=2;z++){
            if(x*x+y*y*2+z*z>5||r.nextInt(6)==0)continue;var p=o.offset(x,h+y,z);if(replaceable(l,p))put(l,p,leaf);}
        if(r.nextInt(3)==0){var p=o.relative(Direction.Plane.HORIZONTAL.getRandomDirection(r),2);if(l.isEmptyBlock(p)&&l.getBlockState(p.below()).isSolidRender(l,p.below()))put(l,p,Blocks.TORCHFLOWER.defaultBlockState());}
        return true;
    }

    // ------------------------------------------------------------------ the Black Tortoise
    private static boolean shellIslet(WorldGenLevel l,BlockPos o,RandomSource r,int sea) {
        int radius=7+r.nextInt(5),rise=3+r.nextInt(4);
        for(int dx=-radius;dx<=radius;dx+=radius)for(int dz=-radius;dz<=radius;dz+=radius)if(ground(l,o.getX()+dx,o.getZ()+dz)>=sea-2)return false;
        var shell=local.primordial.xuanwu.XuanwuContent.ROCK.get().defaultBlockState();
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            double d=Math.hypot(dx,dz)/radius;if(d>1)continue;
            int x=o.getX()+dx,z=o.getZ()+dz,bed=ground(l,x,z);
            int top=sea+(int)Math.round(rise*Math.sqrt(1-d*d))-1;
            for(int y=bed;y<=top;y++){
                BlockState s=y==top?(local.primordial.xuanwu.TideSeaFeature.seam(dx,dz,2.6)?Blocks.DEEPSLATE_TILES.defaultBlockState():shell):y>=sea-2?shell:Blocks.STONE.defaultBlockState();
                put(l,new BlockPos(x,y,z),s);
            }
            if(top>=sea&&d<0.75&&r.nextInt(4)==0)put(l,new BlockPos(x,top+1,z),Blocks.SNOW.defaultBlockState());
        }
        return true;
    }
    private static boolean iceFloe(WorldGenLevel l,BlockPos o,RandomSource r,int sea) {
        int radius=3+r.nextInt(5);boolean any=false;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            double d=Math.hypot(dx,dz)+r.nextDouble()*1.5;if(d>radius)continue;
            var p=new BlockPos(o.getX()+dx,sea-1,o.getZ()+dz);
            if(!l.getFluidState(p).isSource()||!l.isEmptyBlock(p.above()))continue;
            put(l,p,(d<radius*0.5?Blocks.PACKED_ICE:Blocks.ICE).defaultBlockState());
            if(d<radius*0.6&&r.nextInt(3)==0)put(l,p.above(),Blocks.SNOW.defaultBlockState());
            any=true;
        }
        return any;
    }
    private static boolean jadeSpires(WorldGenLevel l,BlockPos o,RandomSource r,int sea) {
        int count=3+r.nextInt(4);boolean any=false;
        for(int i=0;i<count;i++){
            int x=o.getX()+r.nextInt(11)-5,z=o.getZ()+r.nextInt(11)-5,bed=ground(l,x,z);
            int h=3+r.nextInt(8);if(bed+h+2>=sea)continue;
            for(int k=1;k<=h;k++)put(l,new BlockPos(x,bed+k,z),(k%3==0?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE).defaultBlockState());
            put(l,new BlockPos(x,bed+h+1,z),Blocks.SEA_LANTERN.defaultBlockState());
            var foot=new BlockPos(x+1,bed+1,z);
            if(l.getFluidState(foot).isSource()&&l.getBlockState(foot.below()).isSolidRender(l,foot.below()))put(l,foot,Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES,1+r.nextInt(4)).setValue(SeaPickleBlock.WATERLOGGED,true));
            any=true;
        }
        return any;
    }
}
