package local.primordial.baihu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class WastesFeature extends Feature<NoneFeatureConfiguration> {
    enum Kind { BLADES,RIFT,ROCK,QUARTZ }
    private final Kind kind;
    WastesFeature(Kind k){super(NoneFeatureConfiguration.CODEC);kind=k;}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        if(local.primordial.qinglong.GuardianAnchors.nearSeat(c.origin(),64))return false;
        var l=c.level();var p=c.origin();var random=c.random();
        if(p.getY()<70||p.getY()+24>=l.getMaxBuildHeight()||!l.getBlockState(p.below()).isSolidRender(l,p.below()))return false;
        if(kind==Kind.BLADES){
            if(p.getY()<95)return false;
            double rotation=random.nextDouble()*Math.PI*2;
            for(int i=0;i<3;i++){
                int h=19-i*4;double a=rotation+i*Math.PI*2/3;
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x*x+z*z<=5){var q=p.offset((int)Math.round(3*Math.cos(a))+x,-1,(int)Math.round(3*Math.sin(a))+z);l.setBlock(q,Blocks.GRAVEL.defaultBlockState(),2);}
                for(int y=0;y<h;y++){
                    double radius=3+y*0.3;int width=y<h/2?1:0;
                    for(int w=-width;w<=width;w++){
                        var q=p.offset((int)Math.round(radius*Math.cos(a)-w*Math.sin(a)),y,(int)Math.round(radius*Math.sin(a)+w*Math.cos(a)));
                        if(l.getBlockState(q).canBeReplaced())l.setBlock(q,(y==h-1?Blocks.IRON_BLOCK:BaihuContent.BLADESTONE.get()).defaultBlockState(),2);
                    }
                }
            }return true;
        }
        if(kind==Kind.RIFT){
            // Short parallel surface claw marks: bounded footprint avoids a long cross-chunk trench.
            double a=random.nextDouble()*Math.PI*2;
            for(int n=-5;n<=5;n++)for(int lane=-1;lane<=1;lane++){
                int x=p.getX()+(int)Math.round(n*Math.cos(a)-lane*3*Math.sin(a));
                int z=p.getZ()+(int)Math.round(n*Math.sin(a)+lane*3*Math.cos(a));
                int y=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;var q=new BlockPos(x,y,z);
                if(Math.abs(y-p.getY())>3||!l.getBlockState(q).isSolidRender(l,q))continue;
                l.setBlock(q,Blocks.AIR.defaultBlockState(),2);l.setBlock(q.below(),(n%3==0?Blocks.GRAVEL:Blocks.DEEPSLATE).defaultBlockState(),2);
            }return true;
        }
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-3;y<=-1;y++){
            var q=p.offset(x,y,z);if(x*x+z*z>5||!l.getBlockState(q).isSolidRender(l,q))continue;
            // Quartz is exposed on a lower rock band, using overworld stone rather than netherrack ore.
            if(kind==Kind.QUARTZ&&(!l.isEmptyBlock(q.north())&&!l.isEmptyBlock(q.south())&&!l.isEmptyBlock(q.east())&&!l.isEmptyBlock(q.west())))continue;
            l.setBlock(q,(kind==Kind.QUARTZ?Blocks.QUARTZ_BLOCK:BaihuContent.BLADESTONE.get()).defaultBlockState(),2);
        }return true;
    }
}
