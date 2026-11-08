package local.primordial.qinglong;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A small ruin in each guardian's land, with a chest of its land's spoils (loot_tables/chests/guardian_*):
 * the Azure Dragon's thunder altar (a ring of weathered copper rods), the White Tiger's sword grave (broken blades round
 * a cairn), the Vermilion Bird's fire altar (campfires at the corners of a red stone dais), and the Black Tortoise's
 * sunken shrine (a prismarine room on the sea floor). Each spans at most 11 blocks.
 */
public final class GuardianShrineFeature extends Feature<NoneFeatureConfiguration> {
    public enum Kind {THUNDER_ALTAR,SWORD_GRAVE,FIRE_ALTAR,SUNKEN_SHRINE}
    private final Kind kind;
    public GuardianShrineFeature(Kind kind) {super(NoneFeatureConfiguration.CODEC);this.kind=kind;}

    private static void put(WorldGenLevel l,BlockPos p,BlockState s) {if(l.ensureCanWrite(p))l.setBlock(p,s,2);}
    private static void chest(WorldGenLevel l,BlockPos p,RandomSource r,String table,boolean wet) {
        put(l,p,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.Plane.HORIZONTAL.getRandomDirection(r)).setValue(ChestBlock.WATERLOGGED,wet));
        RandomizableContainerBlockEntity.setLootTable(l,r,p,new ResourceLocation(local.primordial.Primordial.ID,"chests/"+table));
    }
    /** level ground: the dais floor at the median height, the rest filled under or cleared over */
    private static boolean level(WorldGenLevel l,BlockPos o,int radius,BlockState floor,BlockState fill) {
        int[] heights=new int[(2*radius+1)*(2*radius+1)];int n=0;
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)heights[n++]=l.getHeight(Heightmap.Types.WORLD_SURFACE_WG,o.getX()+x,o.getZ()+z)-1;
        java.util.Arrays.sort(heights);int y=heights[heights.length/2];
        if(heights[heights.length-1]-heights[0]>6)return false;
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(x*x+z*z>radius*radius+1)continue;
            for(int k=y-4;k<y;k++)if(!l.getBlockState(new BlockPos(o.getX()+x,k,o.getZ()+z)).isSolidRender(l,new BlockPos(o.getX()+x,k,o.getZ()+z)))put(l,new BlockPos(o.getX()+x,k,o.getZ()+z),fill);
            put(l,new BlockPos(o.getX()+x,y,o.getZ()+z),floor);
            for(int k=y+1;k<=y+6;k++)put(l,new BlockPos(o.getX()+x,k,o.getZ()+z),Blocks.AIR.defaultBlockState());
        }
        return true;
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
        if(GuardianAnchors.nearSeat(c.origin(),64))return false;
        var l=c.level();var o=c.origin();var r=c.random();int sea=c.chunkGenerator().getSeaLevel();
        if(o.getY()<=l.getMinBuildHeight()+8||o.getY()>=l.getMaxBuildHeight()-20)return false;
        switch(kind){
            case THUNDER_ALTAR->{
                if(o.getY()<sea||!level(l,o,5,Blocks.CALCITE.defaultBlockState(),Blocks.STONE.defaultBlockState()))return false;
                int y=l.getHeight(Heightmap.Types.WORLD_SURFACE_WG,o.getX(),o.getZ())-1;var base=new BlockPos(o.getX(),y,o.getZ());
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(x*x+z*z<=9)put(l,base.offset(x,0,z),(Math.floorMod(x+z,2)==0?Blocks.WAXED_OXIDIZED_CUT_COPPER:Blocks.WAXED_WEATHERED_CUT_COPPER).defaultBlockState());
                for(int i=0;i<6;i++){
                    double a=i*Math.PI/3;var p=base.offset((int)Math.round(Math.cos(a)*4.5),0,(int)Math.round(Math.sin(a)*4.5));
                    int h=2+r.nextInt(3);if(r.nextInt(4)==0)h=1;   // some fallen short
                    for(int k=1;k<=h;k++)put(l,p.above(k),Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState());
                    if(h>1)put(l,p.above(h+1),Blocks.LIGHTNING_ROD.defaultBlockState());
                }
                put(l,base.above(),Blocks.CHISELED_DEEPSLATE.defaultBlockState());
                chest(l,base.above(2),r,"guardian_thunder_altar",false);
            }
            case SWORD_GRAVE->{
                if(o.getY()<sea||!level(l,o,5,Blocks.COARSE_DIRT.defaultBlockState(),Blocks.STONE.defaultBlockState()))return false;
                int y=l.getHeight(Heightmap.Types.WORLD_SURFACE_WG,o.getX(),o.getZ())-1;var base=new BlockPos(o.getX(),y,o.getZ());
                for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)if(x*x+z*z<=25&&r.nextInt(3)==0)put(l,base.offset(x,0,z),Blocks.GRAVEL.defaultBlockState());
                // the cairn over the chest
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)put(l,base.offset(x,1,z),Blocks.COBBLED_DEEPSLATE.defaultBlockState());
                put(l,base.above(2),Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
                chest(l,base,r,"guardian_sword_grave",false);
                // blades driven in round it, some snapped
                for(int i=0;i<9;i++){
                    double a=r.nextDouble()*Math.PI*2,d=2.5+r.nextDouble()*2.5;var p=base.offset((int)Math.round(Math.cos(a)*d),1,(int)Math.round(Math.sin(a)*d));
                    if(!l.isEmptyBlock(p))continue;
                    int h=1+r.nextInt(3);
                    for(int k=0;k<h;k++)put(l,p.above(k),Blocks.IRON_BARS.defaultBlockState());
                    if(h==3){put(l,p.above(3),Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());put(l,p.above(4),Blocks.CHAIN.defaultBlockState());}
                }
            }
            case FIRE_ALTAR->{
                if(o.getY()<sea||!level(l,o,5,Blocks.RED_SANDSTONE.defaultBlockState(),Blocks.RED_SANDSTONE.defaultBlockState()))return false;
                int y=l.getHeight(Heightmap.Types.WORLD_SURFACE_WG,o.getX(),o.getZ())-1;var base=new BlockPos(o.getX(),y,o.getZ());
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
                    boolean rim=Math.max(Math.abs(x),Math.abs(z))==3;
                    put(l,base.offset(x,1,z),(rim?Blocks.CUT_RED_SANDSTONE:Math.floorMod(x*3+z,4)==0?Blocks.ORANGE_TERRACOTTA:Blocks.RED_TERRACOTTA).defaultBlockState());
                }
                for(int x=-3;x<=3;x+=6)for(int z=-3;z<=3;z+=6){put(l,base.offset(x,2,z),Blocks.CHISELED_RED_SANDSTONE.defaultBlockState());put(l,base.offset(x,3,z),Blocks.CAMPFIRE.defaultBlockState());}
                // the fire pit and the offering beside it
                put(l,base.offset(0,1,0),Blocks.MAGMA_BLOCK.defaultBlockState());
                put(l,base.offset(0,2,0),Blocks.CAMPFIRE.defaultBlockState());
                chest(l,base.offset(0,2,2),r,"guardian_fire_altar",false);
            }
            case SUNKEN_SHRINE->{
                int bed=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,o.getX(),o.getZ())-1;
                if(bed+8>=sea)return false;
                var base=new BlockPos(o.getX(),bed,o.getZ());
                for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
                    for(int k=-2;k<=0;k++)put(l,base.offset(x,k,z),Blocks.PRISMARINE_BRICKS.defaultBlockState());
                    boolean wall=Math.max(Math.abs(x),Math.abs(z))==4,door=wall&&(x==0||z==0);
                    for(int k=1;k<=4;k++){
                        BlockState s=wall&&!(door&&k<=2)?(Math.abs(x)==4&&Math.abs(z)==4?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE_BRICKS).defaultBlockState():Blocks.WATER.defaultBlockState();
                        // the years have broken the walls
                        if(wall&&k>2&&r.nextInt(3)==0)s=Blocks.WATER.defaultBlockState();
                        put(l,base.offset(x,k,z),s);
                    }
                    put(l,base.offset(x,5,z),Math.abs(x)+Math.abs(z)<=2?Blocks.DARK_PRISMARINE.defaultBlockState():r.nextInt(4)==0?Blocks.WATER.defaultBlockState()
                        :Blocks.PRISMARINE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED,true));
                }
                put(l,base.above(1),local.primordial.xuanwu.XuanwuContent.ROCK.get().defaultBlockState());
                put(l,base.above(4),Blocks.SEA_LANTERN.defaultBlockState());
                chest(l,base.above(2),r,"guardian_sunken_shrine",true);
                for(int k=0;k<4;k++){var p=base.offset(r.nextInt(7)-3,1,r.nextInt(7)-3);if(l.getFluidState(p).isSource()&&!p.equals(base.above(1)))put(l,p,Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES,2).setValue(SeaPickleBlock.WATERLOGGED,true));}
            }
        }
        return true;
    }
}
