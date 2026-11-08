package local.primordial.spirit;

import java.util.ArrayList;
import java.util.List;
import local.primordial.Primordial;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * What floats above the Spirit Realm's islands, built block by block from arithmetic on the cell, like the islands:
 * <ul>
 * <li>Sin Spires (七罪尖塔) in the cells with no island: a dark tower of one sin's coloured glass on a rock, lit from
 *     within, crowned with light, a chest at its foot;</li>
 * <li>Sky Sanctums (浮空圣所) over some islands (always over the arrival island): a quartz temple on a floating platform,
 *     reached from the island by a spiral stair of floating steps around a hanging chain, a chest on its altar;</li>
 * <li>Spirit Halos (灵环) over others: a great tilted ring of amethyst with glowing nodes, round a floating crystal;</li>
 * <li>Spirit Lanterns (灵灯) scattered through the sky: soul lanterns hanging under little floating stones.</li>
 * </ul>
 * Built after the islands are decorated, so trees and flowers never grow on them.
 */
public final class SpiritSkyStructures {
    public static final ResourceLocation SPIRE_LOOT=new ResourceLocation(Primordial.ID,"chests/sin_spire");
    public static final ResourceLocation SANCTUM_LOOT=new ResourceLocation(Primordial.ID,"chests/sky_sanctum");
    private SpiritSkyStructures() {}

    /** one structure: its bounds, its blocks (null where it leaves the world as it is), and its chests */
    public abstract static class Piece {
        public final int x0,y0,z0,x1,y1,z1;
        Piece(int x0,int y0,int z0,int x1,int y1,int z1) {this.x0=x0;this.y0=y0;this.z0=z0;this.x1=x1;this.y1=y1;this.z1=z1;}
        abstract BlockState at(int x,int y,int z);
        public List<BlockPos> chests() {return List.of();}
        public ResourceLocation loot() {return null;}
        /** anything besides blocks it brings into this chunk (creatures) */
        void populate(WorldGenLevel level,ChunkAccess chunk) {}
    }

    enum Kind {NONE,SANCTUM,HALO}
    static Kind kindOver(SpiritIslands.Island island) {
        if(SpiritIslands.isOrigin(island.cellX(),island.cellZ()))return Kind.SANCTUM;
        int roll=(int)Math.floorMod(SpiritIslands.hash(island.cellX(),island.cellZ(),101),4);
        return roll==1?Kind.HALO:roll==2?Kind.SANCTUM:Kind.NONE;
    }
    /** every structure belonging to a cell */
    public static List<Piece> piecesOfCell(int cellX,int cellZ) {
        var sin=SinSanctums.sinOfCell(cellX,cellZ);
        if(sin!=null)return SinSanctums.piecesOfCell(sin);
        var pieces=new ArrayList<Piece>();
        var island=SpiritIslands.island(cellX,cellZ);
        long h=SpiritIslands.hash(cellX,cellZ,103);
        if(island==null){
            int x=cellX*SpiritIslands.CELL+SpiritIslands.CELL/2+(int)Math.floorMod(h>>>4,31)-15;
            int z=cellZ*SpiritIslands.CELL+SpiritIslands.CELL/2+(int)Math.floorMod(h>>>12,31)-15;
            pieces.add(new Spire(x,120+(int)Math.floorMod(h>>>20,30),z,(int)Math.floorMod(h>>>28,7)));
        }else switch(kindOver(island)){
            case SANCTUM->pieces.add(new Sanctum(island,island.top()+38+(int)Math.floorMod(h>>>20,10)));
            case HALO->pieces.add(new Halo(island,h));
            default->{}
        }
        // a few lanterns drifting in every cell's sky
        for(int i=0;i<4;i++){
            long l=SpiritIslands.hash(cellX*31+i,cellZ,107);
            int x=cellX*SpiritIslands.CELL+(int)Math.floorMod(l,SpiritIslands.CELL),z=cellZ*SpiritIslands.CELL+(int)Math.floorMod(l>>>8,SpiritIslands.CELL);
            var lantern=new Lantern(x,125+(int)Math.floorMod(l>>>16,50),z);
            if(pieces.stream().noneMatch(p->overlap(p,lantern)))pieces.add(lantern);
        }
        return pieces;
    }

    static boolean overlap(Piece a,Piece b) {
        return a.x0<=b.x1&&b.x0<=a.x1&&a.y0<=b.y1&&b.y0<=a.y1&&a.z0<=b.z1&&b.z0<=a.z1;
    }
    /** build this chunk's share of every structure that reaches into it */
    public static void place(WorldGenLevel level,ChunkAccess chunk) {
        int bx=chunk.getPos().getMinBlockX(),bz=chunk.getPos().getMinBlockZ();
        int cx=SpiritIslands.cellOf(bx),cz=SpiritIslands.cellOf(bz);
        var pos=new BlockPos.MutableBlockPos();
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(var piece:piecesOfCell(cx+dx,cz+dz)){
            int x0=Math.max(piece.x0,bx),x1=Math.min(piece.x1,bx+15),z0=Math.max(piece.z0,bz),z1=Math.min(piece.z1,bz+15);
            if(x0>x1||z0>z1)continue;
            int y0=Math.max(piece.y0,level.getMinBuildHeight()),y1=Math.min(piece.y1,level.getMaxBuildHeight()-1);
            for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)for(int y=y0;y<=y1;y++){
                var state=piece.at(x,y,z);
                if(state!=null)level.setBlock(pos.set(x,y,z),state,Block.UPDATE_CLIENTS);
            }
            for(var chest:piece.chests()){
                if(chest.getX()<bx||chest.getX()>bx+15||chest.getZ()<bz||chest.getZ()>bz+15)continue;
                level.setBlock(chest,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,net.minecraft.core.Direction.EAST),Block.UPDATE_CLIENTS);
                RandomizableContainerBlockEntity.setLootTable(level,RandomSource.create(SpiritIslands.hash(chest.getX(),chest.getZ(),chest.getY())),chest,piece.loot());
            }
            piece.populate(level,chunk);
        }
    }

    static BlockState s(Block block) {return block.defaultBlockState();}

    // ------------------------------------------------------------------ Sin Spire
    /** each sin's glass and its accent block, in BossAffix order: pride, greed, sloth, envy, wrath, gluttony, lust */
    static final Block[] SIN_GLASS={Blocks.YELLOW_STAINED_GLASS,Blocks.ORANGE_STAINED_GLASS,Blocks.LIGHT_GRAY_STAINED_GLASS,Blocks.GREEN_STAINED_GLASS,
        Blocks.RED_STAINED_GLASS,Blocks.BROWN_STAINED_GLASS,Blocks.PINK_STAINED_GLASS};
    static final Block[] SIN_ACCENT={Blocks.GOLD_BLOCK,Blocks.RAW_GOLD_BLOCK,Blocks.BLUE_ICE,Blocks.EMERALD_BLOCK,Blocks.REDSTONE_BLOCK,Blocks.HONEYCOMB_BLOCK,Blocks.PINK_CONCRETE};
    public static final class Spire extends Piece {
        final int cx,base,cz,sin;
        static final int TOWER=28;
        Spire(int cx,int base,int cz,int sin) {super(cx-11,base-13,cz-11,cx+11,base+TOWER+12,cz+11);this.cx=cx;this.base=base;this.cz=cz;this.sin=sin;}
        @Override BlockState at(int x,int y,int z) {
            int dx=x-cx,dz=z-cz,dy=y-base,ax=Math.abs(dx),az=Math.abs(dz);
            double r=Math.sqrt(dx*dx+dz*dz);
            Block frame=Blocks.POLISHED_BLACKSTONE_BRICKS,glass=SIN_GLASS[sin],accent=SIN_ACCENT[sin];
            // the rock it stands on: a floor of dark brick on an inverted cone
            if(dy==0&&r<=7.2)return s(r>6.2?Blocks.CHISELED_POLISHED_BLACKSTONE:frame);
            if(dy<0&&dy>=-12&&r<=7.2*(1+dy/12.5))return s((x+y+z)%5==0?Blocks.TUFF:Blocks.CALCITE);
            // four shards of the sin's glass floating round it
            if(dy>=4&&dy<=7&&((ax==10&&dz==0)||(az==10&&dx==0)))return s(dy==7?accent:glass);
            // the tower: dark corners, glass sides, a band of the accent every seven floors, a doorway facing east
            if(dy>=1&&dy<=TOWER&&ax<=2&&az<=2){
                if(ax==2&&az==2)return s(frame);
                if(ax<2&&az<2)return dy%7==0&&ax==0&&az==0?s(Blocks.SEA_LANTERN):dy==TOWER?s(frame):s(Blocks.AIR);
                if(dx==2&&dz==0&&dy<=2)return s(Blocks.AIR);
                if(dy%7==0||dy==TOWER)return s(accent);
                return s(glass);
            }
            // a stepped roof of the accent, a lantern under its point, and a rod of light on top
            int above=dy-TOWER;
            if(above>=1&&above<=6){int half=2-(above-1)/2;if(ax<=half&&az<=half)return s(above==6?Blocks.SEA_LANTERN:accent);}
            if(above>=7&&above<=9&&dx==0&&dz==0)return s(Blocks.END_ROD);
            return null;
        }
        @Override public List<BlockPos> chests() {return List.of(new BlockPos(cx,base+1,cz));}
        @Override public ResourceLocation loot() {return SPIRE_LOOT;}
    }

    // ------------------------------------------------------------------ Sky Sanctum
    public static final class Sanctum extends Piece {
        final int cx,floor,cz,axisX,axisZ,ground;
        static final int HALF=7,STAIR_R=6;
        Sanctum(SpiritIslands.Island island,int floor) {
            this(island,(int)Math.floor(island.centerX()),floor,(int)Math.floor(island.centerZ())-13);
        }
        private Sanctum(SpiritIslands.Island island,int cx,int floor,int cz) {
            super(cx-HALF-1,Math.min(floor-9,groundOf(island,cx+14,cz)),cz-HALF-1,cx+14+STAIR_R+1,floor+11,cz+HALF+1);
            this.cx=cx;this.floor=floor;this.cz=cz;this.axisX=cx+14;this.axisZ=cz;this.ground=groundOf(island,cx+14,cz);
        }
        /** the island's surface under the stair, or its top if the stair stands off its edge */
        static int groundOf(SpiritIslands.Island island,int x,int z) {var column=island.column(x,z);return column==null?island.top():column[1];}
        @Override BlockState at(int x,int y,int z) {
            int dx=x-cx,dz=z-cz,dy=y-floor,ax=Math.abs(dx),az=Math.abs(dz);
            var stair=stair(x,y,z);
            if(stair!=null)return stair;
            // the stair's last step meets the platform here
            if(dy==0&&dx==HALF+1&&az<=1)return s(Blocks.CALCITE);
            if(ax>HALF||az>HALF)return null;
            // the platform and the rounded rock under it
            if(dy==0)return s(ax==HALF||az==HALF?Blocks.CHISELED_QUARTZ_BLOCK:ax==az?Blocks.PURPUR_BLOCK:Blocks.SMOOTH_QUARTZ);
            if(dy<0&&dy>=-8){double r=Math.sqrt(dx*dx+dz*dz);if(r<=HALF*(1+dy/9.0)+0.5)return s(dy==-1?Blocks.QUARTZ_BRICKS:Blocks.CALCITE);return null;}
            // four pillars, an altar step under the chest, and the roof: a quartz pyramid lit from beneath, topped with a rod of light
            if(dy>=1&&dy<=6&&ax==5&&az==5)return s(Blocks.QUARTZ_PILLAR);
            if(dy==7&&ax<=6&&az<=6)return s(ax==0&&az==0?Blocks.SEA_LANTERN:Blocks.QUARTZ_BRICKS);
            if(dy==8&&ax<=4&&az<=4)return s(Blocks.QUARTZ_BRICKS);
            if(dy==9&&ax<=2&&az<=2)return s(ax==0&&az==0?Blocks.AMETHYST_BLOCK:Blocks.QUARTZ_BRICKS);
            if(dy==10&&ax==0&&az==0)return s(Blocks.END_ROD);
            if(dy>=1&&dy<=6&&ax<=6&&az<=6)return s(Blocks.AIR);
            return null;
        }
        /** a spiral of floating steps round a hanging chain, from the island up to the platform's east edge */
        BlockState stair(int x,int y,int z) {
            int rise=floor-ground;
            if(rise<=0)return null;
            double dx=x+0.5-(axisX+0.5),dz=z+0.5-(axisZ+0.5),r=Math.sqrt(dx*dx+dz*dz);
            if(x==axisX&&z==axisZ&&y>ground&&y<=floor+3)return s(Blocks.CHAIN);
            int k=y-ground;
            if(k<1||k>rise||r<3.6||r>STAIR_R+0.4)return null;
            double angle=Math.PI-0.32*(rise-k);
            double diff=Math.atan2(dz,dx)-angle;diff=Math.atan2(Math.sin(diff),Math.cos(diff));
            if(Math.abs(diff)>0.2)return null;
            return s(k%8==0?Blocks.AMETHYST_BLOCK:Blocks.CALCITE);
        }
        @Override public List<BlockPos> chests() {return List.of(new BlockPos(cx,floor+1,cz));}
        @Override public ResourceLocation loot() {return SANCTUM_LOOT;}
    }

    // ------------------------------------------------------------------ Spirit Halo
    public static final class Halo extends Piece {
        final double cx,cy,cz,radius,tilt,turn;
        Halo(SpiritIslands.Island island,long h) {
            this(island.centerX(),island.top()+55+Math.floorMod(h>>>20,20),island.centerZ(),16+Math.floorMod(h>>>28,8),0.15+Math.floorMod(h>>>36,6)*0.05,Math.floorMod(h>>>44,628)/100.0);
        }
        private Halo(double cx,double cy,double cz,double radius,double tilt,double turn) {
            super((int)Math.floor(cx-radius-3),(int)Math.floor(cy-radius*Math.sin(tilt)-4),(int)Math.floor(cz-radius-3),
                  (int)Math.ceil(cx+radius+3),(int)Math.ceil(cy+radius*Math.sin(tilt)+4),(int)Math.ceil(cz+radius+3));
            this.cx=cx;this.cy=cy;this.cz=cz;this.radius=radius;this.tilt=tilt;this.turn=turn;
        }
        @Override BlockState at(int x,int y,int z) {
            double dx=x+0.5-cx,dy=y+0.5-cy,dz=z+0.5-cz;
            // the crystal at the heart: an octahedron of amethyst round a glowing core
            double crystal=Math.abs(dx)+Math.abs(dy)/1.8+Math.abs(dz);
            if(crystal<=3.2)return s(crystal<=1.2?Blocks.GLOWSTONE:Blocks.AMETHYST_BLOCK);
            // the ring: a torus turned about the vertical and tipped about its own axis
            double u=dx*Math.cos(turn)+dz*Math.sin(turn),w=-dx*Math.sin(turn)+dz*Math.cos(turn);
            double up=dy*Math.cos(tilt)-w*Math.sin(tilt),across=dy*Math.sin(tilt)+w*Math.cos(tilt);
            double q=Math.sqrt(u*u+across*across)-radius;
            if(q*q+up*up>1.7*1.7)return null;
            double around=(Math.atan2(across,u)/(Math.PI*2)+1)*8;
            return s(around-Math.floor(around)<0.07?Blocks.SEA_LANTERN:q<0?Blocks.PURPUR_BLOCK:Blocks.AMETHYST_BLOCK);
        }
    }

    // ------------------------------------------------------------------ Spirit Lantern
    public static final class Lantern extends Piece {
        final int x,y,z;
        Lantern(int x,int y,int z) {super(x-1,y,z-1,x+1,y+3,z+1);this.x=x;this.y=y;this.z=z;}
        @Override BlockState at(int bx,int by,int bz) {
            int dx=bx-x,dz=bz-z,dy=by-y;
            if(dy==3&&Math.abs(dx)+Math.abs(dz)<=1)return s(dx==0&&dz==0?Blocks.AMETHYST_BLOCK:Blocks.CALCITE);
            if(dx!=0||dz!=0)return null;
            if(dy==1||dy==2)return s(Blocks.CHAIN);
            if(dy==0)return Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true);
            return null;
        }
    }
}
