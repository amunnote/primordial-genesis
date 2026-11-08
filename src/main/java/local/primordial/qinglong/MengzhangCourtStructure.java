package local.primordial.qinglong;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
public final class MengzhangCourtStructure extends Structure {
    public static final Codec<MengzhangCourtStructure> CODEC=simpleCodec(MengzhangCourtStructure::new);
    public MengzhangCourtStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return QinglongWorldgen.COURT.get();}
    // the seat is the place: another mod's biome laid over it must not turn the court away
    @Override public Optional<GenerationStub> findValidGenerationPoint(GenerationContext c){return findGenerationPoint(c);}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        // Only the overworld holds a court: a borrowed biome in the Spirit Realm must never create one, or its boss.
        if(!local.primordial.qinglong.GuardianAnchors.overworld(c.chunkGenerator()))return Optional.empty();
        int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();
        if(x<800)return Optional.empty();
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
        Piece(int x,int y,int z,int min,int max){super(QinglongWorldgen.COURT_PIECE.get(),0,new BoundingBox(x-45,min,z-45,x+45,max,z+45));cx=x;cz=z;floor=y;}
        public Piece(CompoundTag t){super(QinglongWorldgen.COURT_PIECE.get(),t);cx=t.getInt("CX");cz=t.getInt("CZ");floor=t.getInt("Floor");anchored=t.getBoolean("Anchored");}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putInt("CX",cx);t.putInt("CZ",cz);t.putInt("Floor",floor);t.putBoolean("Anchored",anchored);}
        private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
        private static int ridge(int dx,int dz){
            double r=Math.hypot(dx,dz),a=Math.atan2(dz,dx);
            double peak=Math.max(20,Math.min(30,24+4*Math.sin(3*a+0.7)+2*Math.sin(7*a+1.3)));
            double profile=r<=35.5?smooth((r-30)/5.5):1-smooth((r-35.5)/9.5);
            double gap=dx>0?smooth((Math.abs(dz)-2.5)/3):1;
            return (int)Math.round(peak*profile*gap);
        }
        private static BlockState pattern(int dx,int dz){
            double r=Math.hypot(dx,dz),arm=Math.IEEEremainder(Math.atan2(dz,dx)*2-r*0.28,Math.PI*2);
            Block b=r<3?Blocks.SEA_LANTERN:r>28.5?Blocks.WAXED_OXIDIZED_CUT_COPPER:r>4&&r<27&&Math.abs(arm)<0.45?Blocks.WAXED_OXIDIZED_COPPER:r>4&&r<27&&Math.abs(arm)<0.65?Blocks.WAXED_WEATHERED_CUT_COPPER:Math.floorMod(dx+dz,2)==0?Blocks.CALCITE:Blocks.SMOOTH_QUARTZ;
            return b.defaultBlockState();
        }
        private void put(WorldGenLevel l,BoundingBox clip,int x,int y,int z,BlockState s){var p=new BlockPos(x,y,z);if(clip.isInside(p)&&y>=l.getMinBuildHeight()&&y<l.getMaxBuildHeight())l.setBlock(p,s,2);}
        @Override public void postProcess(WorldGenLevel l,StructureManager manager,ChunkGenerator gen,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            if(l.getLevel().dimension()!=Level.OVERWORLD)return;
            // Every call writes only the current chunk; the fixed floor was sampled from surrounding noise terrain.
            for(int x=Math.max(cx-45,clip.minX());x<=Math.min(cx+45,clip.maxX());x++)for(int z=Math.max(cz-45,clip.minZ());z<=Math.min(cz+45,clip.maxZ());z++){
                int dx=x-cx,dz=z-cz;double r=Math.hypot(dx,dz);if(r>45)continue;
                int old=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
                var skin=l.getBlockState(new BlockPos(x,old,z));
                if(!skin.is(Blocks.GRASS_BLOCK)&&!skin.is(Blocks.PODZOL)&&!skin.is(Blocks.CALCITE)&&!skin.is(Blocks.COARSE_DIRT))skin=Blocks.GRASS_BLOCK.defaultBlockState();
                double blend=1-smooth((r-30)/15);
                int top=r<=30?floor:(int)Math.round(old+(floor-old)*blend)+ridge(dx,dz);
                // Local microrelief fades out at both boundaries, preserving a continuous outer edge.
                if(r>30)top+=(int)Math.round(Math.sin(x*0.31)*Math.cos(z*0.27)*2*Math.sin(Math.PI*(r-30)/15));
                if(dx>0&&Math.abs(dz)<=2)top=(int)Math.round(old+(floor-old)*blend);
                int bottom=Math.min(old,top)-4;
                for(int y=bottom;y<=top;y++){
                    BlockState state=y<top-2?(y%6<2?Blocks.CALCITE:Blocks.STONE).defaultBlockState():y<top?Blocks.DIRT.defaultBlockState():skin;
                    if(y==top){
                        if(r<=30)state=pattern(dx,dz);
                        else if(dx>0&&Math.abs(dz)<=2)state=Blocks.DIRT_PATH.defaultBlockState();
                        else if(ridge(dx,dz)>12){
                            state=(Math.floorMod(x+z,11)==0?Blocks.OXIDIZED_CUT_COPPER:Math.floorMod(x-z,17)==0?QinglongContent.CLOUDROCK.get():Math.floorMod(x+z,4)==0?Blocks.PODZOL:Blocks.CALCITE).defaultBlockState();
                        }
                    }
                    put(l,clip,x,y,z,state);
                }
                // Remove the original mountain/trees above the incision; arena has at least 36 clear blocks.
                int clear=Math.min(l.getMaxBuildHeight()-1,Math.max(old+32,top+36));
                for(int y=top+1;y<=clear;y++)put(l,clip,x,y,z,Blocks.AIR.defaultBlockState());
            }
            // Eight 3x3-footed copper spires, shifted away from the eastern entrance.
            for(int i=0;i<8;i++){
                double a=(i+0.5)*Math.PI/4;int dx=(int)Math.round(34*Math.cos(a)),dz=(int)Math.round(34*Math.sin(a));
                int base=floor+ridge(dx,dz)+1;
                for(int ox=-1;ox<=1;ox++)for(int oz=-1;oz<=1;oz++)for(int y=base-4;y<=base;y++)put(l,clip,cx+dx+ox,y,cz+dz+oz,Blocks.CALCITE.defaultBlockState());
                for(int y=1;y<=18;y++)put(l,clip,cx+dx,base+y,cz+dz,((y-1)/4%2==0?Blocks.OXIDIZED_COPPER:Blocks.OXIDIZED_CUT_COPPER).defaultBlockState());
                put(l,clip,cx+dx,base+19,cz+dz,Blocks.SEA_LANTERN.defaultBlockState());
                put(l,clip,cx+dx,base+20,cz+dz,Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING,Direction.UP));
            }
            var home=new BlockPos(cx,floor+1,cz);
            if(!anchored&&clip.isInside(home)){
                var server=l.getLevel();var ledger=CourtAnchors.get(server);
                synchronized(ledger){
                    if(!ledger.contains(home)){
                        var boss=SinBosses.QINGLONG.get().create(server);
                        if(boss!=null){boss.moveTo(cx+0.5,floor+1,cz+0.5,90,0);boss.setHome(home);boss.setPersistenceRequired();
                            if(l.addFreshEntity(boss)){ledger.mark(home);anchored=true;}
                        }
                    }else anchored=true;
                }
            }
        }
    }
}
