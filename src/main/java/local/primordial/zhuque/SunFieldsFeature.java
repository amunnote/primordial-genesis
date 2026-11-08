package local.primordial.zhuque;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Code geometry derived from the supplied tree blueprint, never a template placement. */
public final class SunFieldsFeature extends Feature<NoneFeatureConfiguration> {
    public enum Kind {TREE,KNOLL,ROCK,RIFT}
    private final Kind kind;
    public SunFieldsFeature(Kind k){super(NoneFeatureConfiguration.CODEC);kind=k;}
    private static void put(WorldGenLevel l,BlockPos p,BlockState s){if(l.ensureCanWrite(p))l.setBlock(p,s,2);}
    private static BlockState log(Direction.Axis a){return ZhuqueContent.LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,a);}
    private static void crown(WorldGenLevel l,BlockPos p,double rx,double ry,double rz){
        var leaf=ZhuqueContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT,true).setValue(LeavesBlock.DISTANCE,1);
        for(int x=-(int)Math.ceil(rx);x<=rx;x++)for(int y=-(int)Math.ceil(ry);y<=ry;y++)for(int z=-(int)Math.ceil(rz);z<=rz;z++){
            double d=x*x/(rx*rx)+y*y/(ry*ry)+z*z/(rz*rz);var q=p.offset(x,y,z);
            if(d<=1&&(d<0.7||Math.floorMod(x*7+y*13+z*17,10)<6)&&l.getBlockState(q).isAir())put(l,q,leaf);
        }
    }
    public static void tree(WorldGenLevel l,BlockPos p,int h){tree(l,p,h,1);}
    /** a wutong of trunk height h, its limbs and crowns grown by scale (the court's at 1, the wild ones larger) */
    public static void tree(WorldGenLevel l,BlockPos p,int h,double scale){
        int thick=(int)Math.round(6*scale),split=(int)Math.round(11*scale);
        for(int y=0;y<h;y++){
            put(l,p.above(y),log(Direction.Axis.Y));
            if(y<thick)for(var d:Direction.Plane.HORIZONTAL)put(l,p.above(y).relative(d),log(Direction.Axis.Y));
            else if(y<split){put(l,p.above(y).relative(y%2==0?Direction.WEST:Direction.EAST),log(Direction.Axis.Y));put(l,p.above(y).relative(y%2==0?Direction.SOUTH:Direction.NORTH),log(Direction.Axis.Y));}
        }
        for(var d:Direction.Plane.HORIZONTAL)put(l,p.relative(d,2),log(d.getAxis()));
        int[] degrees={35,150,250,320},length={6,5,6,4},rise={3,3,4,3};
        for(int k=0;k<4;k++){
            double a=Math.toRadians(degrees[k]);int sy=h-9+k*2,reach=(int)Math.round(length[k]*scale);double climb=rise[k]*scale;BlockPos previous=p.above(sy);
            for(int i=1;i<=reach;i++){
                int x=(int)Math.round(i*Math.sin(a)),z=(int)Math.round(-i*Math.cos(a)),y=sy+(int)Math.round(i*climb/reach);
                var q=p.offset(x,y,z);var axis=Math.abs(Math.sin(a))>Math.abs(Math.cos(a))?Direction.Axis.X:Direction.Axis.Z;
                put(l,new BlockPos(q.getX(),previous.getY(),q.getZ()),log(axis));put(l,q,log(y==previous.getY()?axis:Direction.Axis.Y));previous=q;
            }crown(l,previous.above(),3.2*scale,1.8*scale,3.2*scale);
        }
        crown(l,p.above(h),5.5*scale,2.3*scale,5.5*scale);crown(l,p.above(h+(int)Math.round(3*scale)),3.5*scale,1.6*scale,3.5*scale);
        int foot=(int)Math.round(5*scale);
        for(int x=-foot;x<=foot;x++)for(int z=-foot;z<=foot;z++)if(x*x+z*z<=foot*foot+5&&l.getBlockState(p.offset(x,-1,z)).isSolidRender(l,p.offset(x,-1,z)))put(l,p.offset(x,-1,z),(Math.floorMod(x*13+z*7,7)==0?Blocks.ORANGE_TERRACOTTA:Blocks.TERRACOTTA).defaultBlockState());
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        if(local.primordial.qinglong.GuardianAnchors.nearSeat(c.origin(),64))return false;
        var l=c.level();var p=c.origin();var random=c.random();
        if(p.getY()<l.getMinBuildHeight()+5||p.getY()>l.getMaxBuildHeight()-40||!l.getBlockState(p.below()).isSolidRender(l,p.below()))return false;
        if(kind==Kind.TREE){
            // wild wutongs stand large and close: room for the trunk, and no other wutong trunk within six blocks
            int h=18+random.nextInt(9);double scale=1.3+random.nextDouble()*0.2;
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=0;y<h;y++){var q=p.offset(x,y,z);var s=l.getBlockState(q);
                if(s.is(ZhuqueContent.LOG.get())||Math.abs(x)<=2&&Math.abs(z)<=2&&!s.canBeReplaced())return false;}
            tree(l,p,h,scale);return true;
        }
        if(kind==Kind.RIFT){
            boolean alongX=random.nextBoolean();
            for(int i=-4;i<=4;i++){var q=p.offset(alongX?i:0,-1,alongX?0:i);if(l.getBlockState(q).isSolidRender(l,q)&&l.getBlockState(q.below()).isSolidRender(l,q.below())&&l.isEmptyBlock(q.above())){put(l,q,Blocks.AIR.defaultBlockState());put(l,q.below(),(Math.abs(i)%3==0?Blocks.RED_SANDSTONE:Blocks.MAGMA_BLOCK).defaultBlockState());}}return true;
        }
        int radius=kind==Kind.KNOLL?5:3;
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            double d=Math.hypot(x,z);if(d>radius)continue;
            var ground=l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,p.offset(x,0,z)).below();
            if(Math.abs(ground.getY()-p.getY()+1)>3)continue;
            int h=(int)Math.round((1-d/radius)*(kind==Kind.KNOLL?3:2));
            for(int y=0;y<=h;y++)put(l,ground.above(y),(kind==Kind.ROCK||y<h?Blocks.RED_SANDSTONE:Blocks.RED_TERRACOTTA).defaultBlockState());
        }return true;
    }
}
