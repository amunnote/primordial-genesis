package local.primordial.zhuque;
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
public final class LingguangCourtStructure extends Structure {
    public static final Codec<LingguangCourtStructure> CODEC=simpleCodec(LingguangCourtStructure::new);
    public LingguangCourtStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return ZhuqueWorldgen.COURT.get();}
    // the seat is the place: another mod's biome laid over it must not turn the court away
    @Override public Optional<GenerationStub> findValidGenerationPoint(GenerationContext c){return findGenerationPoint(c);}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        // Only the overworld holds a court: a borrowed biome in the Spirit Realm must never create one, or its boss.
        if(!local.primordial.qinglong.GuardianAnchors.overworld(c.chunkGenerator()))return Optional.empty();
        int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();
        if(z<800)return Optional.empty();
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
        Piece(int x,int y,int z,int min,int max){super(ZhuqueWorldgen.COURT_PIECE.get(),0,new BoundingBox(x-45,min,z-45,x+45,max,z+45));cx=x;cz=z;floor=y;}
        public Piece(CompoundTag t){super(ZhuqueWorldgen.COURT_PIECE.get(),t);cx=t.getInt("CX");cz=t.getInt("CZ");floor=t.getInt("Floor");anchored=t.getBoolean("Anchored");}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putInt("CX",cx);t.putInt("CZ",cz);t.putInt("Floor",floor);t.putBoolean("Anchored",anchored);}
        private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
        private static BlockState pattern(int dx,int dz){
            double r=Math.hypot(dx,dz);
            if(r<0.5)return Blocks.SEA_LANTERN.defaultBlockState();
            if(r<3.5)return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            if(r<5)return Blocks.TUFF.defaultBlockState();
            if(r<7)return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            if(r<8.5)return Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState();
            if(r<10.5)return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            double a=Math.atan2(dx,-dz),nearest=Math.round(a/(Math.PI/8))*(Math.PI/8);
            if(r<12)return (r*Math.abs(a-nearest)<0.75?Blocks.GOLD_BLOCK:Blocks.CHISELED_RED_SANDSTONE).defaultBlockState();
            if(r<13)return Blocks.RED_TERRACOTTA.defaultBlockState();
            double ray=Math.round(a/(Math.PI/4))*(Math.PI/4);
            if(r<=29&&r*Math.abs(Math.sin(a-ray))<0.75)return Blocks.RED_SANDSTONE.defaultBlockState();
            if(r<29.5)return (((int)((r-13)/3))%2==0?Blocks.TERRACOTTA:Blocks.CUT_RED_SANDSTONE).defaultBlockState();
            return Blocks.RED_SAND.defaultBlockState();
        }

        private void put(WorldGenLevel l,BoundingBox clip,int x,int y,int z,BlockState s){var p=new BlockPos(x,y,z);if(clip.isInside(p)&&y>=l.getMinBuildHeight()&&y<l.getMaxBuildHeight())l.setBlock(p,s,2);}
        @Override public void postProcess(WorldGenLevel l,StructureManager manager,ChunkGenerator gen,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            if(l.getLevel().dimension()!=Level.OVERWORLD)return;
            // Every call writes only the current chunk; the fixed floor was sampled from surrounding noise terrain.
            for(int x=Math.max(cx-45,clip.minX());x<=Math.min(cx+45,clip.maxX());x++)for(int z=Math.max(cz-45,clip.minZ());z<=Math.min(cz+45,clip.maxZ());z++){
                int dx=x-cx,dz=z-cz;double r=Math.hypot(dx,dz);if(r>45)continue;
                int old=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
                var skin=Blocks.RED_TERRACOTTA.defaultBlockState();
                double angle=Math.atan2(dx,-dz),southDistance=Math.acos(Math.cos(angle-Math.PI));
                double gap=smooth((southDistance-Math.PI/6)/Math.toRadians(25));
                double crest=13+2.2*Math.sin(3*angle+0.7)+0.8*Math.sin(7*angle+1.9);
                double wall=crest*gap*smooth((r-30)/7);
                int top=r<=30?floor:r<=38?(int)Math.round(floor+wall):(int)Math.round(floor+wall+(old-floor-wall)*smooth((r-38)/7));
                int bottom=Math.min(old,top)-4;
                for(int y=bottom;y<=top;y++){
                    BlockState state=y<top-2?(Math.floorMod(y,6)<2?Blocks.RED_TERRACOTTA:Blocks.RED_SANDSTONE).defaultBlockState():y<top?Blocks.TERRACOTTA.defaultBlockState():skin;
                    if(y==top){
                        if(r<=30)state=pattern(dx,dz);
                        else if(dz>0&&southDistance<Math.PI/6)state=Blocks.CUT_RED_SANDSTONE.defaultBlockState();

                    }
                    put(l,clip,x,y,z,state);
                }
                // Remove the original mountain/trees above the incision; arena has at least 36 clear blocks.
                int clear=Math.min(l.getMaxBuildHeight()-1,Math.max(old+32,top+36));
                for(int y=top+1;y<=clear;y++)put(l,clip,x,y,z,Blocks.AIR.defaultBlockState());
            }
            // Six short wutong pillars at blueprint radius 26, clipped per chunk.
            for(int i=0;i<6;i++){
                double a=Math.toRadians(30+i*60);int px=cx+(int)Math.round(26*Math.sin(a)),pz=cz-(int)Math.round(26*Math.cos(a)),h=6+i%3;
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)put(l,clip,px+x,floor,pz+z,Blocks.CHISELED_RED_SANDSTONE.defaultBlockState());
                for(int y=1;y<=h;y++)put(l,clip,px,floor+y,pz,ZhuqueContent.LOG.get().defaultBlockState());
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=2;y++)if(x*x+z*z+y*y*1.96<=4.6&&!(x==0&&z==0&&y<=0))put(l,clip,px+x,floor+h+y,pz+z,ZhuqueContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT,true).setValue(LeavesBlock.DISTANCE,1));
            }
            var home=new BlockPos(cx,floor+1,cz);
            if(!anchored&&clip.isInside(home)){
                var server=l.getLevel();var ledger=CourtAnchors.get(server,"lingguang");
                synchronized(ledger){
                    if(!ledger.contains(home)){
                        var boss=SinBosses.ZHUQUE.get().create(server);
                        if(boss!=null){boss.moveTo(cx+0.5,floor+1,cz+0.5,90,0);boss.setHome(home);boss.setPersistenceRequired();
                            if(l.addFreshEntity(boss)){ledger.mark(home);anchored=true;}
                        }
                    }else anchored=true;
                }
            }
        }
    }
}
