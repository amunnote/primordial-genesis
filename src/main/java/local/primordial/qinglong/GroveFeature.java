package local.primordial.qinglong;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class GroveFeature extends Feature<NoneFeatureConfiguration> {
    enum Kind{SPIRE,TREE,ROCK}private final Kind kind;
    GroveFeature(Kind k){super(NoneFeatureConfiguration.CODEC);kind=k;}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        if(local.primordial.qinglong.GuardianAnchors.nearSeat(c.origin(),64))return false;
        var l=c.level();var p=c.origin();var r=c.random();
        if(p.getY()<64||!l.getBlockState(p.below()).isSolidRender(l,p.below()))return false;
        if(kind!=Kind.TREE&&p.getY()<95)return false;
        if(kind==Kind.SPIRE){
            int h=16+r.nextInt(9);if(p.getY()+h+2>=l.getMaxBuildHeight())return false;
            for(int y=1;y<=h+2;y++)if(!l.isEmptyBlock(p.above(y)))return false;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)l.setBlock(p.offset(x,0,z),Blocks.CALCITE.defaultBlockState(),2);
            for(int y=1;y<=h;y++)l.setBlock(p.above(y),((y-1)/4%2==0?Blocks.OXIDIZED_COPPER:Blocks.OXIDIZED_CUT_COPPER).defaultBlockState(),2);
            l.setBlock(p.above(h+1),Blocks.SEA_LANTERN.defaultBlockState(),2);l.setBlock(p.above(h+2),Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING,Direction.UP),2);return true;
        }
        if(kind==Kind.ROCK){for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-2;y<=0;y++)if(x*x+z*z<=5&&l.getBlockState(p.offset(x,y,z)).isSolidRender(l,p.offset(x,y,z)))l.setBlock(p.offset(x,y,z),QinglongContent.CLOUDROCK.get().defaultBlockState(),2);return true;}
        int h=20+r.nextInt(13);if(p.getY()+h+4>=l.getMaxBuildHeight())return false;
        for(int y=0;y<=h+3;y++)for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)if(!l.getBlockState(p.offset(x,y,z)).canBeReplaced())return false;
        for(int y=0;y<h;y++)for(int x=0;x<=1;x++)for(int z=0;z<=1;z++)l.setBlock(p.offset(x,y,z),Blocks.OAK_LOG.defaultBlockState(),2);
        for(int y=h-5;y<=h+2;y++)for(int x=-4;x<=5;x++)for(int z=-4;z<=5;z++)if((x-0.5)*(x-0.5)+(z-0.5)*(z-0.5)+(y-h+2)*(y-h+2)<26&&l.getBlockState(p.offset(x,y,z)).canBeReplaced())l.setBlock(p.offset(x,y,z),Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,1),2);
        for(int y=3;y<h-3;y++)l.setBlock(p.offset(-1,y,0),Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST,true),2);
        // Short root arms over shallow gullies.
        for(var dir:Direction.Plane.HORIZONTAL)for(int n=1;n<=4;n++){var root=p.relative(dir,n).below(n/3);if(l.getBlockState(root).canBeReplaced())l.setBlock(root,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,dir.getAxis()),2);}
        return true;
    }
}
