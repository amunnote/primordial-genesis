package local.primordial.baihu;
import local.primordial.qinglong.CourtAnchors;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import local.primordial.boss.SinBosses;

/** A registered, locatable terrain incision. The art NBT is never loaded or placed. */
public final class JianbingCourtStructure extends Structure {
    public static final Codec<JianbingCourtStructure> CODEC=simpleCodec(JianbingCourtStructure::new);
    public JianbingCourtStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return BaihuWorldgen.COURT.get();}
    // the seat is the place: another mod's biome laid over it must not turn the court away
    @Override public Optional<GenerationStub> findValidGenerationPoint(GenerationContext c){return findGenerationPoint(c);}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        // Only the overworld holds a court: a borrowed biome in the Spirit Realm must never create one, or its boss.
        if(!local.primordial.qinglong.GuardianAnchors.overworld(c.chunkGenerator()))return Optional.empty();
        int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();
        if(x>-800)return Optional.empty();
        int sum=0;
        for(int i=0;i<16;i++){
            double a=i*Math.PI/8;
            sum+=c.chunkGenerator().getBaseHeight(x+(int)(Math.cos(a)*40),z+(int)(Math.sin(a)*40),Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState())-1;
        }
        int y=Math.max(70,Math.min(c.heightAccessor().getMaxBuildHeight()-75,sum/16));
        return Optional.of(new GenerationStub(new BlockPos(x,y,z),b->b.addPiece(new Piece(x,y,z,c.heightAccessor().getMinBuildHeight(),c.heightAccessor().getMaxBuildHeight()-1))));
    }
    public static final class Piece extends StructurePiece {
        private final int cx,cz,floor;private boolean anchored;
        Piece(int x,int y,int z,int min,int max){super(BaihuWorldgen.COURT_PIECE.get(),0,new BoundingBox(x-45,min,z-45,x+45,max,z+45));cx=x;cz=z;floor=y;}
        public Piece(CompoundTag t){super(BaihuWorldgen.COURT_PIECE.get(),t);cx=t.getInt("CX");cz=t.getInt("CZ");floor=t.getInt("Floor");anchored=t.getBoolean("Anchored");}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putInt("CX",cx);t.putInt("CZ",cz);t.putInt("Floor",floor);t.putBoolean("Anchored",anchored);}
        private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
        private static BlockState pattern(int dx,int dz){
            double r=Math.hypot(dx,dz);
            if(r>28.5)return Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
            if(r<3)return Blocks.SEA_LANTERN.defaultBlockState();
            if(Math.abs(r-10)<0.5||Math.abs(r-20)<0.5)return Blocks.IRON_BLOCK.defaultBlockState();
            for(int i=0;i<4;i++){double a=i*Math.PI/2+Math.PI/4,along=dx*Math.cos(a)+dz*Math.sin(a),across=-dx*Math.sin(a)+dz*Math.cos(a);
                for(int k=-1;k<=1;k++)if(along>11&&along<19&&Math.abs(across-k*2.2-(along-15)*0.25)<0.5)return Blocks.POLISHED_DEEPSLATE.defaultBlockState();}
            return (Math.floorMod(dx+dz,3)==0?Blocks.CALCITE:Math.floorMod(dx+dz,2)==0?Blocks.SMOOTH_QUARTZ:Blocks.QUARTZ_BRICKS).defaultBlockState();
        }
        private void put(WorldGenLevel l,BoundingBox clip,int x,int y,int z,BlockState s){var p=new BlockPos(x,y,z);if(clip.isInside(p)&&y>=l.getMinBuildHeight()&&y<l.getMaxBuildHeight())l.setBlock(p,s,2);}
        @Override public void postProcess(WorldGenLevel l,StructureManager manager,ChunkGenerator gen,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            if(l.getLevel().dimension()!=Level.OVERWORLD)return;
            // Every call writes only the current chunk; the fixed floor was sampled from surrounding noise terrain.
            for(int x=Math.max(cx-45,clip.minX());x<=Math.min(cx+45,clip.maxX());x++)for(int z=Math.max(cz-45,clip.minZ());z<=Math.min(cz+45,clip.maxZ());z++){
                int dx=x-cx,dz=z-cz;double r=Math.hypot(dx,dz);if(r>45)continue;
                int old=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
                var skin=(Math.floorMod(x+z,5)==0?Blocks.CALCITE:Blocks.TUFF).defaultBlockState();
                double blend=1-smooth((r-30)/15);
                int top=r<=30?floor:(int)Math.round(old+(floor-old)*blend);
                // Small apron relief fades at both boundaries.
                if(r>30)top+=(int)Math.round(Math.sin(x*0.31)*Math.cos(z*0.27)*2*Math.sin(Math.PI*(r-30)/15));
                if(dx<0&&Math.abs(dz)<=3)top=(int)Math.round(old+(floor-old)*blend);
                int bottom=Math.min(old,top)-4;
                for(int y=bottom;y<=top;y++){
                    BlockState state=y<top-2?(y%6<2?Blocks.CALCITE:Blocks.STONE).defaultBlockState():y<top?Blocks.ANDESITE.defaultBlockState():skin;
                    if(y==top){
                        if(r<=30)state=pattern(dx,dz);
                        else if(dx<0&&Math.abs(dz)<=3)state=Blocks.CALCITE.defaultBlockState();

                    }
                    put(l,clip,x,y,z,state);
                }
                // Remove the original mountain/trees above the incision; arena has at least 36 clear blocks.
                int clear=Math.min(l.getMaxBuildHeight()-1,Math.max(old+32,top+36));
                for(int y=top+1;y<=clear;y++)put(l,clip,x,y,z,Blocks.AIR.defaultBlockState());
            }
            // Eight outward leaning thin blades. Geometry is code, never a template placement.
            for(int i=0;i<8;i++){
                double a=(i+0.5)*Math.PI/4;int h=14+i%5;
                for(int y=-8;y<=h;y++){
                    int x=cx+(int)Math.round((33+Math.max(0,y)*0.32)*Math.cos(a)),z=cz+(int)Math.round((33+Math.max(0,y)*0.32)*Math.sin(a));
                    int width=y< h/2?1:0;
                    for(int w=-width;w<=width;w++)put(l,clip,x+(int)Math.round(-Math.sin(a)*w),floor+1+y,z+(int)Math.round(Math.cos(a)*w),(y==h?Blocks.IRON_BLOCK:y<0?Blocks.CALCITE:BaihuContent.BLADESTONE.get()).defaultBlockState());
                }
            }
            var home=new BlockPos(cx,floor+1,cz);
            if(!anchored&&clip.isInside(home)){
                var server=l.getLevel();var ledger=CourtAnchors.get(server,"jianbing");
                synchronized(ledger){
                    if(!ledger.contains(home)){
                        var boss=SinBosses.BAIHU.get().create(server);
                        if(boss!=null){boss.moveTo(cx+0.5,floor+1,cz+0.5,90,0);boss.setHome(home);boss.setPersistenceRequired();
                            if(l.addFreshEntity(boss)){ledger.mark(home);anchored=true;}
                        }
                    }else anchored=true;
                }
            }
        }
    }
}
