package local.primordial.spirit;

import local.primordial.boss.FirstSage;
import local.primordial.boss.SinBosses;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 真理之庭 The Court of Truth: where the First Sage waits, a black island floating north of the Primordial Temple. Its
 * floor is one great transmutation circle in gold on black stone (circle, square and triangle), with the four elements'
 * circles at its quarters (water north, wood east, fire south, metal west, where the four guardians stand); eight towers
 * of books ring it, and a ring of glass in the elements' colours floats overhead.
 * <p>
 * Its gate stands on the temple floor north of the dais and opens only to a player who has fought the Primordial Lord
 * to its fall; a gate on the court's south rim leads back down.
 */
public final class SageSanctum {
    public static final int CX=0,CZ=-224,FLOOR=196,RADIUS=40,DEPTH=24,ARENA=30,TOWERS=35,CLEAR=40,HALO=30,HALO_RADIUS=22;
    public static final BlockPos SEAT=new BlockPos(CX,FLOOR+2,CZ),ENTRY=new BlockPos(CX,FLOOR+1,CZ+33),RIM_GATE=new BlockPos(CX,FLOOR+1,CZ+37);
    /** the gate to it, on the temple floor between the arena and the colonnade, due north */
    public static final BlockPos TEMPLE_GATE=new BlockPos(0,PrimordialTemple.FLOOR+1,-40);
    private SageSanctum() {}

    static BlockState s(Block block) {return block.defaultBlockState();}
    static final Block[] ELEMENT_STONE={Blocks.PRISMARINE_BRICKS,Blocks.MOSS_BLOCK,Blocks.RED_NETHER_BRICKS,Blocks.IRON_BLOCK};
    static final Block[] ELEMENT_GLASS={Blocks.CYAN_STAINED_GLASS,Blocks.LIME_STAINED_GLASS,Blocks.RED_STAINED_GLASS,Blocks.WHITE_STAINED_GLASS};
    /** distance from a point to a segment, in the plane */
    static double segment(double px,double pz,double ax,double az,double bx,double bz) {
        double vx=bx-ax,vz=bz-az,t=((px-ax)*vx+(pz-az)*vz)/(vx*vx+vz*vz);t=Math.max(0,Math.min(1,t));
        double dx=px-ax-vx*t,dz=pz-az-vz*t;return Math.sqrt(dx*dx+dz*dz);
    }
    /** the block the court puts at a spot, or null where it leaves the world alone */
    public static BlockState at(int x,int y,int z) {
        int dx=x-CX,dz=z-CZ,dy=y-FLOOR;
        double r=Math.sqrt((double)dx*dx+(double)dz*dz),angle=Math.atan2(dz,dx);
        // the ring of coloured glass overhead
        if(Math.abs(dy-HALO)<=2){
            double q=r-HALO_RADIUS,v=dy-HALO;
            if(q*q+v*v<=2.2){
                for(int i=0;i<4;i++){double a=Math.atan2(FirstSage.CIRCLES[i][1],FirstSage.CIRCLES[i][0]);
                    if(Math.abs(Math.IEEEremainder(angle-a,Math.PI*2))*HALO_RADIUS<2.5)return s(q*q+v*v<0.7?Blocks.SEA_LANTERN:ELEMENT_GLASS[i]);}
                return s(q*q+v*v<0.7?Blocks.GLOWSTONE:Blocks.BLACK_STAINED_GLASS);
            }
        }
        if(r>RADIUS+0.5)return null;
        // the black rock it floats on, veined with crying obsidian
        if(dy<0){
            if(dy<-DEPTH||r>RADIUS*(1+dy/(DEPTH+1.0))+0.5)return null;
            int h=PrimordialTemple.hash(x,y,z);
            if(Math.floorMod(h,13)==0)return s(Blocks.CRYING_OBSIDIAN);
            return s(dy==-1?Blocks.POLISHED_BLACKSTONE:Math.floorMod(h>>4,3)==0?Blocks.DEEPSLATE:Blocks.BLACKSTONE);
        }
        if(dy==0)return floor(dx,dz,r);
        var built=above(dx,dy,dz,r);
        if(built!=null)return built;
        return dy<=CLEAR?s(Blocks.AIR):null;
    }
    static BlockState floor(int dx,int dz,double r) {
        if(r>RADIUS-1.5)return s(Blocks.CHISELED_POLISHED_BLACKSTONE);
        if(r>ARENA+0.5)return s(Math.floorMod(dx,4)<2^Math.floorMod(dz,4)<2?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.QUARTZ_BRICKS);
        // the four elements' circles
        for(int i=0;i<4;i++){
            double e=Math.hypot(dx-FirstSage.CIRCLES[i][0],dz-FirstSage.CIRCLES[i][1]);
            if(e<=0.5)return s(Blocks.SEA_LANTERN);
            if(e>=FirstSage.CIRCLE_RADIUS-0.5&&e<=FirstSage.CIRCLE_RADIUS+0.5)return s(ELEMENT_STONE[i]);
        }
        // the great circle: rings, an inscribed square and triangle, all in gold
        if(r>=ARENA-0.5||Math.abs(r-20)<0.55||Math.abs(r-6)<0.5)return s(Blocks.GOLD_BLOCK);
        if(Math.abs(Math.max(Math.abs(dx),Math.abs(dz))-14)<0.5&&r<20.5)return s(Blocks.GOLD_BLOCK);
        for(int k=0;k<3;k++){
            double a=-Math.PI/2+Math.PI*2*k/3,b=-Math.PI/2+Math.PI*2*(k+1)/3;
            if(segment(dx,dz,Math.cos(a)*20,Math.sin(a)*20,Math.cos(b)*20,Math.sin(b)*20)<0.55)return s(Blocks.GOLD_BLOCK);
        }
        // black without, white within: the work begins in darkness and is whitened
        if(r<6)return s(Math.floorMod(dx+dz,2)==0?Blocks.SMOOTH_QUARTZ:Blocks.QUARTZ_BRICKS);
        return s(Math.floorMod(dx+dz,2)==0?Blocks.POLISHED_BLACKSTONE:Blocks.POLISHED_DEEPSLATE);
    }
    static BlockState above(int dx,int dy,int dz,double r) {
        if(dy==1&&r>RADIUS-1.2)return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        // the dais where it waits
        if(dy==1&&r<=3.5)return s(r>2.5?Blocks.GOLD_BLOCK:r<=0.5?Blocks.SEA_LANTERN:Blocks.CHISELED_QUARTZ_BLOCK);
        // eight towers of books
        for(int i=0;i<8;i++){
            double a=Math.PI*2*i/8+Math.PI/8;
            int px=(int)Math.round(Math.cos(a)*TOWERS),pz=(int)Math.round(Math.sin(a)*TOWERS);
            if(Math.abs(dx-px)>1||Math.abs(dz-pz)>1||dy>15)continue;
            boolean corner=Math.abs(dx-px)==1&&Math.abs(dz-pz)==1;
            if(dy==1)return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            if(dy<=12)return s(corner?Blocks.DARK_OAK_LOG:dy%4==0?Blocks.CHISELED_BOOKSHELF:Blocks.BOOKSHELF);
            if(dy==13)return s(corner?Blocks.GOLD_BLOCK:Blocks.CHISELED_POLISHED_BLACKSTONE);
            if(dy==14)return dx==px&&dz==pz?s(Blocks.ENCHANTING_TABLE):null;
            return null;
        }
        return null;
    }

    /** raise the court, its gates and the First Sage: once, in the realm, after the temple */
    static void build(ServerLevel level) {
        var pos=new BlockPos.MutableBlockPos();
        int r=RADIUS+1;
        for(int cx=(CX-r)>>4;cx<=(CX+r)>>4;cx++)for(int cz=(CZ-r)>>4;cz<=(CZ+r)>>4;cz++){
            level.getChunk(cx,cz);
            for(int x=cx<<4;x<(cx<<4)+16;x++)for(int z=cz<<4;z<(cz<<4)+16;z++)
                for(int y=FLOOR-DEPTH;y<=FLOOR+CLEAR;y++){
                    var state=at(x,y,z);
                    if(state!=null)level.setBlock(pos.set(x,y,z),state,Block.UPDATE_CLIENTS);
                }
        }
        level.setBlock(RIM_GATE,PrimordialTemple.gate(PrimordialGateBlock.Target.TEMPLE),Block.UPDATE_ALL);
        level.getChunk(TEMPLE_GATE.getX()>>4,TEMPLE_GATE.getZ()>>4);
        level.setBlock(TEMPLE_GATE,PrimordialTemple.gate(PrimordialGateBlock.Target.SAGE),Block.UPDATE_ALL);
        var sage=SinBosses.FIRST_SAGE.get().create(level);
        if(sage!=null){sage.moveTo(SEAT.getX()+0.5,SEAT.getY(),SEAT.getZ()+0.5,0,0);sage.setHome(SEAT);level.addFreshEntity(sage);}
        for(var player:level.players())player.sendSystemMessage(Component.translatable("message.primordial.sage_sanctum.risen").withStyle(s->s.withColor(FirstSage.COLOUR).withBold(true)));
    }
    /** is a spot within the court's bounds */
    public static boolean within(Vec3 at) {
        double dx=at.x-CX-0.5,dz=at.z-CZ-0.5,r=Math.sqrt(dx*dx+dz*dz),dy=at.y-FLOOR;
        return dy>=-DEPTH&&dy<=CLEAR+8&&r<RADIUS+6;
    }
    public static void up(ServerPlayer player,ServerLevel level) {
        player.teleportTo(level,ENTRY.getX()+0.5,ENTRY.getY(),ENTRY.getZ()+0.5,180,0);
        player.fallDistance=0;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,ENTRY.getX()+0.5,ENTRY.getY()+1,ENTRY.getZ()+0.5,40,0.5,1,0.5,0.05);
        level.playSound(null,ENTRY,SoundEvents.BEACON_POWER_SELECT,SoundSource.PLAYERS,1.2F,0.8F);
        player.displayClientMessage(Component.translatable("message.primordial.sage_gate.up").withStyle(s->s.withColor(FirstSage.COLOUR)),true);
        local.primordial.achievement.Achievements.enteredCourt(player);
    }
}
