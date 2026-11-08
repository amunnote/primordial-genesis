package local.primordial.spirit;

import local.primordial.boss.SinBoss;
import local.primordial.boss.SinBosses;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The two guardians between the seven sins and the origin, each on a floating terrace of its own beside the Primordial
 * Temple, in its own quarter: 白虎 the White Tiger to the west (metal), 玄武 the Black Tortoise to the north (water).
 * Any player who has felled the seven may face them; only one who has felled both as well may enter the temple.
 */
public enum Guardian {
    BAIHU("baihu",-112,0,0xF4F4F0,-6,0),
    XUANWU("xuanwu",0,-112,0x3AB8D8,6,0),
    QINGLONG("qinglong",112,0,0x3AD8A0,4,6),
    ZHUQUE("zhuque",0,112,0xFF5A2A,-4,6);
    public static final int FLOOR=176,RADIUS=30,DEPTH=20,CLEAR=36;
    public final String id;public final int cx,cz,colour;
    /** where its gate stands on the arrival island, from where travellers arrive */
    public final int gateDx,gateDz;
    Guardian(String id,int cx,int cz,int colour,int gateDx,int gateDz) {this.id=id;this.cx=cx;this.cz=cz;this.colour=colour;this.gateDx=gateDx;this.gateDz=gateDz;}

    public static Guardian byId(String id){for(var g:values())if(g.id.equals(id))return g;return null;}
    public net.minecraft.resources.ResourceLocation shrine(){return new net.minecraft.resources.ResourceLocation("primordial",switch(this){case QINGLONG->"mengzhang_court";case BAIHU->"jianbing_court";case ZHUQUE->"lingguang_court";case XUANWU->"zhiming_court";});}
    public String direction(){return switch(this){case QINGLONG->"east";case BAIHU->"west";case ZHUQUE->"south";case XUANWU->"north";};}
    public EntityType<? extends SinBoss> type() {
        return switch(this){case BAIHU->SinBosses.BAIHU.get();case XUANWU->SinBosses.XUANWU.get();case QINGLONG->SinBosses.QINGLONG.get();case ZHUQUE->SinBosses.ZHUQUE.get();};
    }
    public static Guardian of(EntityType<?> type) {for(var g:values())if(g.type()==type)return g;return null;}
    /** where it sleeps, the terrace's heart */
    public BlockPos seat() {return new BlockPos(cx,FLOOR+1,cz);}
    /** the side that faces the temple, where the gate stands and travellers are set down */
    Vec3 inward() {double d=Math.sqrt(cx*cx+cz*cz);return new Vec3(-cx/d,0,-cz/d);}
    public BlockPos entry() {var in=inward();return new BlockPos(cx+(int)Math.round(in.x*(RADIUS-5)),FLOOR+1,cz+(int)Math.round(in.z*(RADIUS-5)));}
    public BlockPos gate() {var in=inward();return new BlockPos(cx+(int)Math.round(in.x*(RADIUS-2)),FLOOR+1,cz+(int)Math.round(in.z*(RADIUS-2)));}
    /** the yaw that faces into the terrace from its entry */
    public float entryYaw() {var in=inward();return (float)Math.toDegrees(Math.atan2(in.z,in.x))+90;}

    public boolean within(Vec3 at) {
        double dx=at.x-cx,dz=at.z-cz,r=Math.sqrt(dx*dx+dz*dz),dy=at.y-FLOOR;
        if(dy>=-3)return r<RADIUS+6;
        return dy>=-DEPTH&&r<=RADIUS*(1+dy/(DEPTH+1.0))+3;
    }
    public static Guardian within(Vec3 at,boolean any) {for(var g:values())if(g.within(at))return g;return null;}

    static int hash(int x,int y,int z) {int h=x*73856093^y*19349663^z*83492791;h^=h>>>13;h*=0x5bd1e995;return h^(h>>>15);}
    static BlockState s(Block block) {return block.defaultBlockState();}
    /** the terrace's block at a spot, or null where it leaves the world alone */
    public BlockState at(int x,int y,int z) {
        int dx=x-cx,dz=z-cz,dy=y-FLOOR;
        double r=Math.sqrt((double)dx*dx+(double)dz*dz),angle=Math.atan2(dz,dx);
        if(r>RADIUS+0.5)return null;
        if(dy<0){
            if(dy<-DEPTH||r>RADIUS*(1+dy/(DEPTH+1.0))+0.5)return null;
            int h=Math.floorMod(hash(x,y,z),9);
            return switch(this){
                case BAIHU->s(h==0?Blocks.IRON_BLOCK:dy==-1?Blocks.QUARTZ_BRICKS:Blocks.CALCITE);
                case XUANWU->s(h==0?Blocks.PRISMARINE:dy==-1?Blocks.DARK_PRISMARINE:Blocks.POLISHED_BLACKSTONE);
                case QINGLONG->s(h==0?Blocks.WAXED_OXIDIZED_COPPER:dy==-1?Blocks.WAXED_OXIDIZED_CUT_COPPER:Blocks.CALCITE);
                case ZHUQUE->s(h==0?Blocks.SHROOMLIGHT:dy==-1?Blocks.RED_NETHER_BRICKS:Blocks.POLISHED_BLACKSTONE);
            };
        }
        var built=switch(this){
            case BAIHU->baihu(x,dy,z,dx,dz,r,angle);case XUANWU->xuanwu(x,dy,z,dx,dz,r,angle);
            case QINGLONG->qinglong(x,dy,z,dx,dz,r,angle);case ZHUQUE->zhuque(x,dy,z,dx,dz,r,angle);
        };
        if(built!=null)return built;
        return dy>=1&&dy<=CLEAR?s(Blocks.AIR):null;
    }
    /** the White Tiger's terrace: white stone and steel, a forest of eight blade-stones round its edge */
    BlockState baihu(int x,int dy,int z,int dx,int dz,double r,double angle) {
        if(dy==0){
            if(r>RADIUS-1.5)return s(Blocks.CHISELED_QUARTZ_BLOCK);
            if(r<3)return s(Blocks.SEA_LANTERN);
            if(r>=9.5&&r<10.5||r>=19.5&&r<20.5)return s(Blocks.IRON_BLOCK);
            // four claw-marks raked across the floor
            for(int i=0;i<4;i++){double a=Math.PI/2*i+Math.PI/4,along=dx*Math.cos(a)+dz*Math.sin(a),across=-dx*Math.sin(a)+dz*Math.cos(a);
                for(int k=-1;k<=1;k++)if(along>11&&along<19&&Math.abs(across-k*2.2-(along-15)*0.25)<0.5)return s(Blocks.POLISHED_DEEPSLATE);}
            return s(Math.floorMod(x+z,2)==0?Blocks.SMOOTH_QUARTZ:Blocks.QUARTZ_BRICKS);
        }
        if(dy==1&&r>RADIUS-1.2)return s(Blocks.QUARTZ_BRICKS);
        // the blade-stones: slender white spires, steel-tipped, leaning out like drawn swords
        for(int i=0;i<8;i++){
            double a=Math.PI*2*i/8+Math.PI/8;
            double lean=dy*0.08;
            double px=cx+Math.cos(a)*(RADIUS-4+lean),pz=cz+Math.sin(a)*(RADIUS-4+lean);
            double d=Math.hypot(x+0.5-px-0.5,z+0.5-pz-0.5);
            int tall=11+Math.floorMod(hash(i,7,3),4);
            if(dy>=1&&dy<=tall&&d<=(dy<3?1.3:dy>tall-3?0.6:0.95))return s(dy>tall-3?Blocks.IRON_BLOCK:dy==1?Blocks.CHISELED_QUARTZ_BLOCK:Blocks.QUARTZ_PILLAR);
        }
        return null;
    }
    /** the Azure Dragon's terrace: white stone swirled with clouds of green-bronze, eight copper spires crowned with lightning rods */
    BlockState qinglong(int x,int dy,int z,int dx,int dz,double r,double angle) {
        if(dy==0){
            if(r>RADIUS-1.5)return s(Blocks.WAXED_OXIDIZED_CUT_COPPER);
            if(r<3)return s(Blocks.SEA_LANTERN);
            // a spiral of cloud: two arms of green-bronze winding out from the heart
            double arm=Math.IEEEremainder(angle*2-r*0.28,Math.PI*2);
            if(r>4&&r<RADIUS-3&&Math.abs(arm)<0.45)return s(Blocks.WAXED_OXIDIZED_COPPER);
            if(r>4&&r<RADIUS-3&&Math.abs(arm)<0.65)return s(Blocks.WAXED_WEATHERED_CUT_COPPER);
            return s(Math.floorMod(x+z,2)==0?Blocks.CALCITE:Blocks.SMOOTH_QUARTZ);
        }
        if(dy==1&&r>RADIUS-1.2)return s(Blocks.WAXED_OXIDIZED_CUT_COPPER);
        for(int i=0;i<8;i++){
            double a=Math.PI*2*i/8,px=cx+Math.cos(a)*(RADIUS-4),pz=cz+Math.sin(a)*(RADIUS-4);
            if(Math.abs(x+0.5-px)<1&&Math.abs(z+0.5-pz)<1){
                int tall=10+Math.floorMod(hash(i,3,11),3);
                if(dy>=1&&dy<tall)return s(dy%4==0?Blocks.WAXED_OXIDIZED_CUT_COPPER:Blocks.WAXED_OXIDIZED_COPPER);
                if(dy==tall)return s(Blocks.SEA_LANTERN);
                if(dy==tall+1)return s(Blocks.LIGHTNING_ROD);
            }
        }
        return null;
    }
    /** the Vermilion Bird's terrace: dark stone and red brick, a sun of gold rays, eight braziers of living flame */
    BlockState zhuque(int x,int dy,int z,int dx,int dz,double r,double angle) {
        if(dy==0){
            if(r>RADIUS-1.5)return s(Blocks.RED_NETHER_BRICKS);
            if(r<3.5)return s(Blocks.SHROOMLIGHT);
            if(r<6)return s(Blocks.GOLD_BLOCK);
            // twelve rays of gold out from the sun at the heart, and a ring of red round them
            double ray=Math.abs(Math.IEEEremainder(angle,Math.PI*2/12))*r;
            if(r<RADIUS-4&&ray<0.55+r*0.02)return s(Blocks.GOLD_BLOCK);
            if(r>=18&&r<19.2)return s(Blocks.RED_NETHER_BRICKS);
            return s(Math.floorMod(x+z,2)==0?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.POLISHED_BLACKSTONE);
        }
        if(dy==1&&r>RADIUS-1.2)return s(Blocks.RED_NETHER_BRICKS);
        for(int i=0;i<8;i++){
            double a=Math.PI*2*i/8+Math.PI/8,px=cx+Math.cos(a)*(RADIUS-4),pz=cz+Math.sin(a)*(RADIUS-4);
            if(Math.abs(x+0.5-px)<1&&Math.abs(z+0.5-pz)<1){
                if(dy>=1&&dy<=5)return s(dy==5?Blocks.GOLD_BLOCK:Blocks.NETHER_BRICKS);
                if(dy==6)return Blocks.CAMPFIRE.defaultBlockState();
            }
        }
        return null;
    }
    /** the Black Tortoise's terrace: dark stone, a ring of still water, spires of ice and dark prismarine */
    BlockState xuanwu(int x,int dy,int z,int dx,int dz,double r,double angle) {
        if(dy==0){
            if(r>RADIUS-1.5)return s(Blocks.DARK_PRISMARINE);
            if(r<3)return s(Blocks.SEA_LANTERN);
            if(r>=22&&r<25)return s(Blocks.WATER);
            if(r>=21&&r<22||r>=25&&r<26)return s(Blocks.PRISMARINE_BRICKS);
            // the hexagons of a tortoise shell, inlaid across the arena
            double hx=dx/6.0,hz=dz/6.0-(Math.floorMod((int)Math.floor(hx),2)==0?0:0.5);
            double fx=hx-Math.floor(hx),fz=hz-Math.floor(hz);
            if(fx<0.12||fz<0.1)return s(Blocks.PRISMARINE);
            return s(Math.floorMod(x+z,2)==0?Blocks.POLISHED_BLACKSTONE:Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        if(dy==-1&&r>=22&&r<25)return s(Blocks.PRISMARINE_BRICKS);
        if(dy==1&&r>RADIUS-1.2)return s(Blocks.DARK_PRISMARINE);
        // six spires of ice rising out of the water ring
        for(int i=0;i<6;i++){
            double a=Math.PI*2*i/6,px=cx+Math.cos(a)*23.5,pz=cz+Math.sin(a)*23.5;
            double d=Math.hypot(x+0.5-px,z+0.5-pz);
            int tall=8+Math.floorMod(hash(i,5,9),5);
            if(dy>=0&&dy<=tall&&d<=1.8*(1-dy/(tall+1.0))+0.4)return s(dy>tall-2?Blocks.BLUE_ICE:Blocks.PACKED_ICE);
        }
        return null;
    }
}
