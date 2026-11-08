package local.primordial.xuanwu;
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
public final class ZhimingCourtStructure extends Structure {
    public static final Codec<ZhimingCourtStructure> CODEC=simpleCodec(ZhimingCourtStructure::new);
    public ZhimingCourtStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return XuanwuWorldgen.COURT.get();}
    // the seat is the place: another mod's biome laid over it must not turn the court away
    @Override public Optional<GenerationStub> findValidGenerationPoint(GenerationContext c){return findGenerationPoint(c);}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        // Only the overworld holds a court: a borrowed biome in the Spirit Realm must never create one, or its boss.
        if(!local.primordial.qinglong.GuardianAnchors.overworld(c.chunkGenerator()))return Optional.empty();
        int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();
        if(z> -800)return Optional.empty();
        int y=c.chunkGenerator().getSeaLevel()+3;
        return Optional.of(new GenerationStub(new BlockPos(x,y,z),b->b.addPiece(new Piece(x,y,z,c.heightAccessor().getMinBuildHeight(),c.heightAccessor().getMaxBuildHeight()-1))));
    }
    public static final class Piece extends StructurePiece {
        private final int cx,cz,floor;private boolean anchored;
        Piece(int x,int y,int z,int min,int max){super(XuanwuWorldgen.COURT_PIECE.get(),0,new BoundingBox(x-44,min,z-44,x+44,max,z+44));cx=x;cz=z;floor=y;}
        public Piece(CompoundTag t){super(XuanwuWorldgen.COURT_PIECE.get(),t);cx=t.getInt("CX");cz=t.getInt("CZ");floor=t.getInt("Floor");anchored=t.getBoolean("Anchored");}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putInt("CX",cx);t.putInt("CZ",cz);t.putInt("Floor",floor);t.putBoolean("Anchored",anchored);}
        private static BlockState pattern(int dx,int dz){
            double r=Math.hypot(dx,dz);
            if(dx==0&&dz==0)return Blocks.SEA_LANTERN.defaultBlockState();
            if(r>=29&&r<=32)return (Math.sin(Math.atan2(dz,dx)*12+(r-29)*1.5)>0?Blocks.PRISMARINE:Blocks.DARK_PRISMARINE).defaultBlockState();
            return (r>=3&&TideSeaFeature.seam(dx,dz,5)?Blocks.DEEPSLATE_TILES:XuanwuContent.ROCK.get()).defaultBlockState();
        }
        private void put(WorldGenLevel l,BoundingBox clip,int x,int y,int z,BlockState s){var p=new BlockPos(x,y,z);if(clip.isInside(p)&&y>=l.getMinBuildHeight()&&y<l.getMaxBuildHeight())l.setBlock(p,s,2);}
        @Override public void postProcess(WorldGenLevel l,StructureManager manager,ChunkGenerator gen,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            if(l.getLevel().dimension()!=Level.OVERWORLD)return;
            // Fill from the actual ocean floor, then drain every arena column. All writes are chunk-clipped.
            for(int x=Math.max(cx-44,clip.minX());x<=Math.min(cx+44,clip.maxX());x++)for(int z=Math.max(cz-44,clip.minZ());z<=Math.min(cz+44,clip.maxZ());z++){
                int dx=x-cx,dz=z-cz;double r=Math.hypot(dx,dz);boolean bridge=Math.abs(dx)<=2&&dz< -27&&dz>=-44;
                if(r>40&&!bridge)continue;
                int bed=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
                int top=bridge?floor-Math.max(0,(-dz-28)/4):r<=28?floor:r<30?floor+1:floor-Math.min(5,(int)((r-30)/2));
                for(int y=Math.min(bed,top)-3;y<=top;y++)put(l,clip,x,y,z,y==top?pattern(dx,dz):(Math.floorMod(x*11+z*7+y,7)<4?XuanwuContent.ROCK.get():Blocks.STONE).defaultBlockState());
                // The dry disc and raised rim sit above sea level. Clear water, ice, plants and overhangs.
                int clear=Math.min(l.getMaxBuildHeight()-1,Math.max(bed+24,top+32));
                for(int y=top+1;y<=clear;y++)put(l,clip,x,y,z, y<=floor-3&&r>30?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());
            }
            for(int i=0;i<4;i++){
                int px=cx+(i==1?34:i==3?-34:0),pz=cz+(i==0?-34:i==2?34:0),h=6+i%3;
                for(int dy=-2;dy<=h;dy++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(dy<h-1||dx==0&&dz==0)
                    put(l,clip,px+dx,floor+dy,pz+dz,(dy<=0?XuanwuContent.ROCK.get():dy%2==0?Blocks.BLUE_ICE:Blocks.PACKED_ICE).defaultBlockState());
            }
            var home=new BlockPos(cx,floor+1,cz);
            if(!anchored&&clip.isInside(home)){
                var server=l.getLevel();var ledger=CourtAnchors.get(server,"zhiming");
                synchronized(ledger){
                    if(!ledger.contains(home)){
                        var boss=SinBosses.XUANWU.get().create(server);
                        if(boss!=null){boss.moveTo(cx+0.5,floor+1,cz+0.5,90,0);boss.setHome(home);boss.setPersistenceRequired();
                            if(l.addFreshEntity(boss)){ledger.mark(home);anchored=true;}
                        }
                    }else anchored=true;
                }
            }
        }
    }
}
