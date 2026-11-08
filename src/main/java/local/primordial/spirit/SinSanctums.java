package local.primordial.spirit;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import local.primordial.affix.BossAffix;
import local.primordial.boss.SinBoss;
import local.primordial.boss.SinBosses;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * The seven sanctums of the Sin Lords: each in a cell of its own on a ring eight cells (about 900 blocks) out from the
 * arrival island, where no island grows. Each is a great floating arena built from its sin's own stone, with seven
 * pillars whose glass beams rise high into the sky, its sin's own ornaments, and its lord sleeping at the centre. The
 * lord is born with the arena when it is first generated, so there is one of each per world.
 */
public final class SinSanctums {
    public static final int RING=8,FLOOR=100,RADIUS=26,DEPTH=18,PILLAR=9,BEAM=32;
    static final Map<BossAffix,int[]> CELLS=new EnumMap<>(BossAffix.class);
    static {
        var sins=BossAffix.values();
        for(int i=0;i<sins.length;i++){
            int[][] scattered={{0,-7},{9,-10},{15,2},{6,13},{-5,8},{-14,5},{-10,-9}};
            CELLS.put(sins[i],scattered[i]);
        }
    }
    private SinSanctums() {}
    /** the sin whose sanctum is in this cell, or null */
    public static BossAffix sinOfCell(int cellX,int cellZ) {
        for(var e:CELLS.entrySet())if(e.getValue()[0]==cellX&&e.getValue()[1]==cellZ)return e.getKey();
        return null;
    }
    /** where a sin's lord sleeps: the centre of its arena floor */
    public static BlockPos seat(BossAffix sin) {
        var cell=CELLS.get(sin);
        return new BlockPos(cell[0]*SpiritIslands.CELL+SpiritIslands.CELL/2,FLOOR+1,cell[1]*SpiritIslands.CELL+SpiritIslands.CELL/2);
    }
    /** whether a column lies within a distance of any sanctum */
    public static boolean near(int x,int z,double distance) {
        for(var sin:BossAffix.values()){var seat=seat(sin);double dx=x+0.5-seat.getX()-0.5,dz=z+0.5-seat.getZ()-0.5;if(dx*dx+dz*dz<distance*distance)return true;}
        return false;
    }
    static EntityTypeRef lordOf(BossAffix sin) {
        return switch(sin){
            case PRIDE->SinBosses.PRIDE::get;case GREED->SinBosses.GREED::get;case SLOTH->SinBosses.SLOTH::get;case ENVY->SinBosses.ENVY::get;
            case WRATH->SinBosses.WRATH::get;case GLUTTONY->SinBosses.GLUTTONY::get;case LUST->SinBosses.LUST::get;
        };
    }
    interface EntityTypeRef {net.minecraft.world.entity.EntityType<? extends SinBoss> get();}

    /** each sin's stones: floor, second floor, inlay, heart, rim, pillar, cap, underside, beam glass */
    public record Palette(Block floor,Block floor2,Block inlay,Block heart,Block rim,Block pillar,Block cap,Block under,Block glass) {}
    public static Palette palette(BossAffix sin) {
        return switch(sin){
            case PRIDE->new Palette(Blocks.SMOOTH_QUARTZ,Blocks.QUARTZ_BRICKS,Blocks.GOLD_BLOCK,Blocks.GLOWSTONE,Blocks.CHISELED_QUARTZ_BLOCK,Blocks.QUARTZ_PILLAR,Blocks.GOLD_BLOCK,Blocks.CALCITE,Blocks.YELLOW_STAINED_GLASS);
            case GREED->new Palette(Blocks.SMOOTH_SANDSTONE,Blocks.CUT_SANDSTONE,Blocks.GOLD_BLOCK,Blocks.SHROOMLIGHT,Blocks.RAW_GOLD_BLOCK,Blocks.GOLD_BLOCK,Blocks.RAW_GOLD_BLOCK,Blocks.SANDSTONE,Blocks.ORANGE_STAINED_GLASS);
            case SLOTH->new Palette(Blocks.PACKED_ICE,Blocks.SNOW_BLOCK,Blocks.BLUE_ICE,Blocks.SEA_LANTERN,Blocks.CALCITE,Blocks.POLISHED_DIORITE,Blocks.CALCITE,Blocks.TUFF,Blocks.LIGHT_BLUE_STAINED_GLASS);
            case ENVY->new Palette(Blocks.DEEPSLATE_TILES,Blocks.POLISHED_DEEPSLATE,Blocks.EMERALD_BLOCK,Blocks.VERDANT_FROGLIGHT,Blocks.MOSSY_STONE_BRICKS,Blocks.DEEPSLATE_BRICKS,Blocks.MOSS_BLOCK,Blocks.DEEPSLATE,Blocks.GREEN_STAINED_GLASS);
            case WRATH->new Palette(Blocks.POLISHED_BLACKSTONE,Blocks.POLISHED_BLACKSTONE_BRICKS,Blocks.RED_NETHER_BRICKS,Blocks.MAGMA_BLOCK,Blocks.NETHER_BRICKS,Blocks.RED_NETHER_BRICKS,Blocks.CRIMSON_HYPHAE,Blocks.BASALT,Blocks.RED_STAINED_GLASS);
            case GLUTTONY->new Palette(Blocks.MUD_BRICKS,Blocks.PACKED_MUD,Blocks.HONEYCOMB_BLOCK,Blocks.OCHRE_FROGLIGHT,Blocks.BONE_BLOCK,Blocks.BONE_BLOCK,Blocks.HONEYCOMB_BLOCK,Blocks.MUD,Blocks.BROWN_STAINED_GLASS);
            case LUST->new Palette(Blocks.PINK_TERRACOTTA,Blocks.PINK_CONCRETE,Blocks.MAGENTA_TERRACOTTA,Blocks.PEARLESCENT_FROGLIGHT,Blocks.CHERRY_PLANKS,Blocks.STRIPPED_CHERRY_LOG,Blocks.CHERRY_LEAVES,Blocks.CALCITE,Blocks.PINK_STAINED_GLASS);
        };
    }

    /** the arena of one sin, as a structure piece */
    public static final class Arena extends SpiritSkyStructures.Piece {
        final BossAffix sin;final int cx,cz;final Palette p;
        Arena(BossAffix sin) {
            super(seat(sin).getX()-RADIUS-2,FLOOR-DEPTH-1,seat(sin).getZ()-RADIUS-2,seat(sin).getX()+RADIUS+2,FLOOR+PILLAR+BEAM+2,seat(sin).getZ()+RADIUS+2);
            this.sin=sin;this.cx=seat(sin).getX();this.cz=seat(sin).getZ();this.p=palette(sin);
        }
        static BlockState s(Block b) {return b.defaultBlockState();}
        static int hash(int x,int z,int salt) {int h=x*73856093^z*19349663^salt*83492791;h^=h>>>13;h*=0x5bd1e995;return h^(h>>>15);}
        @Override BlockState at(int x,int y,int z) {
            int dx=x-cx,dz=z-cz,dy=y-FLOOR;
            double r=Math.sqrt(dx*dx+dz*dz),angle=Math.atan2(dz,dx);
            // the stone it floats on
            if(dy<0){
                if(dy<-DEPTH||r>RADIUS*(1+dy/(DEPTH+1.0))+0.5)return null;
                return s(Math.floorMod(hash(x,z,dy),7)==0?Blocks.STONE:p.under);
            }
            // the seven pillars and their beams
            for(int i=0;i<7;i++){
                double a=Math.PI*2*i/7+Math.PI/14;
                int px=(int)Math.round(cx+Math.cos(a)*(RADIUS-5)),pz=(int)Math.round(cz+Math.sin(a)*(RADIUS-5));
                int ox=Math.abs(x-px),oz=Math.abs(z-pz);
                if(ox<=1&&oz<=1&&dy>=1&&dy<=PILLAR)return s(dy==PILLAR?p.cap:p.pillar);
                if(ox==0&&oz==0&&dy==PILLAR+1)return s(p.heart);
                if(ox==0&&oz==0&&dy>PILLAR+1&&dy<=PILLAR+BEAM)return s((dy-PILLAR)%8==0?p.heart:p.glass);
            }
            if(r>RADIUS)return null;
            if(dy==0){
                if(r>RADIUS-1.2)return s(p.rim);
                if(r<2.5)return s(p.heart);
                if(r>=6&&r<7||r>=14&&r<15||r>=21&&r<21.8)return s(p.inlay);
                double spoke=Math.abs(Math.IEEEremainder(angle,Math.PI*2/7))*r;
                if(r>7&&r<21&&spoke<0.6)return s(p.inlay);
                return s(Math.floorMod(dx+dz,2)==0?p.floor:p.floor2);
            }
            if(dy==1&&r>RADIUS-1.2)return s(p.rim);
            return ornament(x,y,z,dx,dy,dz,r,angle);
        }
        /** each sin's own furnishing of its arena */
        BlockState ornament(int x,int y,int z,int dx,int dy,int dz,double r,double angle) {
            int h=hash(x,z,sin.ordinal()+11);
            switch(sin){
                case PRIDE->{ // a golden throne at the north edge
                    int back=-(RADIUS-4);
                    if(Math.abs(dx)<=2&&dz>=back&&dz<=back+3){
                        if(dz==back&&dy>=1&&dy<=6)return s(dy==6?Blocks.GOLD_BLOCK:Blocks.QUARTZ_PILLAR);
                        if(dy==1)return s(Blocks.GOLD_BLOCK);
                        if(dy==2&&dz<=back+1)return s(Blocks.QUARTZ_BRICKS);
                        if(dy==3&&Math.abs(dx)==2&&dz<=back+2)return s(Blocks.GOLD_BLOCK);
                    }
                }
                case WRATH->{ // jagged spikes breaking out of the floor
                    if(r>9&&r<RADIUS-3&&Math.floorMod(h,23)==0){int tall=1+Math.floorMod(h>>5,4);if(dy>=1&&dy<=tall)return s(dy==tall?Blocks.MAGMA_BLOCK:Blocks.BLACKSTONE);}
                }
                case ENVY->{ // tall mirrors between the pillars
                    for(int i=0;i<7;i++){
                        double a=Math.PI*2*i/7+Math.PI/14+Math.PI/7;
                        double mx=cx+Math.cos(a)*(RADIUS-4),mz=cz+Math.sin(a)*(RADIUS-4);
                        double along=-(x+0.5-mx)*Math.sin(a)+(z+0.5-mz)*Math.cos(a),across=(x+0.5-mx)*Math.cos(a)+(z+0.5-mz)*Math.sin(a);
                        if(Math.abs(along)<=1.6&&Math.abs(across)<=0.5&&dy>=1&&dy<=5)return s(dy==5?Blocks.MOSSY_STONE_BRICKS:Blocks.TINTED_GLASS);
                    }
                    if(dy==1&&r<RADIUS-2&&r>4&&Math.floorMod(h,13)==0)return s(Blocks.MOSS_CARPET);
                }
                case GREED->{ // heaps of gold
                    if(r>16&&r<RADIUS-2&&Math.floorMod(h,9)==0){int tall=1+Math.floorMod(h>>4,2);if(dy>=1&&dy<=tall)return s(dy==tall?Blocks.GOLD_BLOCK:Blocks.RAW_GOLD_BLOCK);}
                }
                case LUST->{ // petals over the floor and blossom round the pillars
                    if(dy==1&&r<RADIUS-2&&r>3&&Math.floorMod(h,5)==0)return s(Blocks.PINK_PETALS);
                    if(dy>=1&&dy<=3&&r>RADIUS-8&&r<RADIUS-2&&Math.floorMod(h,11)==0)return s(Blocks.CHERRY_LEAVES).setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true);
                }
                case GLUTTONY->{ // great ribs arching in from the rim
                    for(int i=0;i<7;i++){
                        double a=Math.PI*2*i/7;
                        double diff=Math.abs(Math.IEEEremainder(angle-a,Math.PI*2))*r;
                        if(diff<0.7&&r>RADIUS-10&&r<RADIUS-1){
                            int height=(int)Math.round(7*Math.sin(Math.PI*(RADIUS-1-r)/9));
                            if(dy>=Math.max(1,height-1)&&dy<=height)return s(Blocks.BONE_BLOCK);
                        }
                    }
                }
                case SLOTH->{ // drifts of snow and spires of ice
                    if(dy==1&&r<RADIUS-2&&r>3&&Math.floorMod(h,4)==0)return s(Blocks.SNOW);
                    if(r>RADIUS-9&&r<RADIUS-2&&Math.floorMod(h,17)==0){int tall=2+Math.floorMod(h>>4,4);if(dy>=1&&dy<=tall)return s(dy==tall?Blocks.BLUE_ICE:Blocks.PACKED_ICE);}
                }
            }
            return null;
        }
        /** the lord comes into the world with its arena */
        @Override void populate(WorldGenLevel level,ChunkAccess chunk) {
            var seat=seat(sin);
            if(seat.getX()>>4!=chunk.getPos().x||seat.getZ()>>4!=chunk.getPos().z)return;
            var lord=lordOf(sin).get().create(level.getLevel());
            if(lord==null)return;
            lord.moveTo(seat.getX()+0.5,seat.getY(),seat.getZ()+0.5,180,0);
            lord.setHome(seat);
            level.addFreshEntity(lord);
        }
    }
    public static List<SpiritSkyStructures.Piece> piecesOfCell(BossAffix sin) {return List.of(new Arena(sin));}
}
