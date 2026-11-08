package local.primordial.spirit;

/**
 * The shape of the Spirit Realm: one floating island per 112-block cell (most cells have one; the cell at the origin
 * always does). Pure arithmetic on the cell coordinates, so the chunk generator, the biome source and the arrival point
 * all agree without sharing state.
 */
public final class SpiritIslands {
    public static final int CELL=112;
    private SpiritIslands() {}

    /** one island: centre, radius, the height of its top and how deep its underside hangs */
    public record Island(int cellX,int cellZ,double centerX,double centerZ,double radius,int top,double depth,long seed) {
        /** the island's outline wobbles: its radius toward a given direction */
        double radiusAt(double angle) {
            if(!isOrigin(cellX,cellZ)) {
                double sum=0,weight=0;
                for(int i=0;i<4;i++) {
                    int harmonic=switch(i){case 0->2;case 1->3;case 2->5;default->7;};
                    double amplitude=0.5+unit(seed,10+i*7);
                    sum+=amplitude*Math.sin(angle*harmonic+unit(seed,i*11)*Math.PI*2);
                    weight+=amplitude;
                }
                return radius*(1+0.28*sum/weight);
            }
            double a=((seed>>>3)&1023)/1023.0*Math.PI*2,b=((seed>>>13)&1023)/1023.0*Math.PI*2;
            return radius*(1+0.09*Math.sin(angle*3+a)+0.06*Math.sin(angle*5+b));
        }
        /** the column at (x,z): {bottom, surface} block heights, or null off the island */
        public int[] column(int x,int z) {
            if(!isOrigin(cellX,cellZ))return irregularColumn(x,z);
            double dx=x+0.5-centerX,dz=z+0.5-centerZ,d=Math.sqrt(dx*dx+dz*dz);
            double r=radiusAt(Math.atan2(dz,dx));
            if(d>=r)return null;
            double edge=d/r;
            // a low dome on top, a long tapering root underneath
            int surface=top+(int)Math.round(2.2*Math.sin(x*0.09+(seed&255))*Math.cos(z*0.08+((seed>>8)&255))*(1-edge)-edge*edge*3);
            int bottom=(int)Math.round(top-depth*Math.pow(1-edge*edge,0.8))-1;
            return new int[]{Math.min(bottom,surface-1),surface};
        }

        private int[] irregularColumn(int x,int z) {
            double dx=x+0.5-centerX,dz=z+0.5-centerZ;
            // Conservative bound includes the largest main outline and both satellites.
            if(Math.abs(dx)>116||Math.abs(dz)>116)return null;
            if(SinSanctums.sinOfCell(cellOf(x),cellOf(z))!=null)return null;
            double axis=unit(seed,4)*Math.PI*2,c=Math.cos(axis),s=Math.sin(axis);
            double stretch=0.78+0.54*unit(seed,18);
            double u=(dx*c+dz*s)/stretch,v=-dx*s+dz*c;
            double d=Math.sqrt(u*u+v*v),angle=Math.atan2(v,u);
            double r=radiusAt(angle)+jagged(x,z,seed);
            if(d<r) {
                double edge=d/r,core=1-edge*edge;
                double wave=3.5*Math.sin(x*0.09+(seed&255))*Math.cos(z*0.08+((seed>>8)&255));
                double detail=0.8*Math.sin(x*(0.09/0.35)+unit(seed,27)*6.28)
                    *Math.cos(z*(0.08/0.35)+unit(seed,37)*6.28);
                double shape=switch((int)(seed&3)) {
                    case 0->wave*(1-edge);
                    case 1->3*Math.exp(-Math.pow((edge-0.76)/0.16,2))+wave*0.18;
                    case 2->10*Math.pow(1-edge,3)+wave*0.25*(1-edge);
                    default->-4*core+3*Math.sin(Math.PI*edge)+wave*0.2*(1-edge);
                };
                int surface=Math.min(115,top+(int)Math.round(shape+detail*(1-edge)-3*edge*edge));
                double exponent=0.55+0.85*unit(seed,31);
                double root=depth*Math.pow(core,exponent);
                if(((seed>>>2)&3)==0) {
                    double a=unit(seed,43)*Math.PI*2;
                    double ru=u-radius/3*Math.cos(a),rv=v-radius/3*Math.sin(a);
                    double q=(ru*ru+rv*rv)/(radius*radius*0.16);
                    // The secondary cone starts lower, but has a shorter taper; min bottom = max depth.
                    if(q<1)root=Math.max(root,depth*(0.32+0.55*Math.pow(1-q,0.75)));
                }
                int step=2+(int)((seed>>>6)&1);
                int bottom=top-(int)(Math.round(root/step)*step)-1;
                return new int[]{Math.min(bottom,surface-1),surface};
            }
            if(Math.floorMod(seed,3)!=0)return null;
            int count=1+(int)((seed>>>7)&1);
            for(int i=0;i<count;i++) {
                long h=hash(cellX,cellZ,seed+i+101);
                double a=unit(h,0)*Math.PI*2;
                double sr=4+5*unit(h,12),gap=6+10*unit(h,24);
                // Map the outline point back from main-axis space before placing the satellite.
                double rim=radiusAt(a),su=Math.cos(a)*rim*stretch,sv=Math.sin(a)*rim;
                double length=Math.sqrt(su*su+sv*sv),offset=1+(sr+gap)/length;
                double sx=(su*c-sv*s)*offset,sz=(su*s+sv*c)*offset;
                double rx=dx-sx,rz=dz-sz,dist=Math.sqrt(rx*rx+rz*rz);
                double limit=sr*(1+0.12*Math.sin(3*Math.atan2(rz,rx)+unit(h,36)*6.28))+0.5*jagged(x,z,h);
                if(dist>=limit)continue;
                double edge=dist/limit;
                int st=top-6+(int)Math.floorMod(h>>>44,11);
                int surface=Math.min(115,st+(int)Math.round(Math.sin(x*0.3+z*0.2)*(1-edge)-2*edge*edge));
                int bottom=st-(int)(Math.round(sr*0.85*Math.pow(1-edge*edge,0.8)/2)*2)-1;
                return new int[]{Math.min(bottom,surface-1),surface};
            }
            return null;
        }
    }

    /** Fixed-width seed slices: no mutable random source or world access. */
    private static double unit(long seed,int shift) {return ((seed>>>shift)&1023)/1023.0;}
    private static double jagged(int x,int z,long seed) {return 3*unit(hash(x>>1,z>>1,seed),0)-1.5;}

    static long hash(long x,long z,long salt) {
        long h=x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL^salt*0x165667B19E3779F9L;
        h^=h>>>31;h*=0x7FB5D329728EA185L;h^=h>>>27;h*=0x81DADEF4BC2DD44DL;h^=h>>>33;
        return h;
    }
    public static boolean isOrigin(int cellX,int cellZ) {return cellX==0&&cellZ==0;}
    /** the island of a cell, or null for the one cell in five left empty */
    public static Island island(int cellX,int cellZ) {
        long h=hash(cellX,cellZ,17);
        if(!isOrigin(cellX,cellZ)&&Math.floorMod(h,5)==0)return null;
        // the Sin Lords' sanctums float alone
        if(SinSanctums.sinOfCell(cellX,cellZ)!=null)return null;
        double ox=isOrigin(cellX,cellZ)?0:(Math.floorMod(h>>>8,41)-20),oz=isOrigin(cellX,cellZ)?0:(Math.floorMod(h>>>16,41)-20);
        double radius=isOrigin(cellX,cellZ)?34:19+Math.floorMod(h>>>24,26);
        int top=isOrigin(cellX,cellZ)?96:66+Math.floorMod((int)(h>>>32),47);
        double depth=radius*(0.7+Math.floorMod(h>>>40,5)*0.08);
        return new Island(cellX,cellZ,cellX*CELL+CELL/2.0+ox,cellZ*CELL+CELL/2.0+oz,radius,top,depth,h);
    }
    public static int cellOf(int block) {return Math.floorDiv(block,CELL);}
    /** the island whose column covers (x,z), checking its own cell and the neighbours an island can reach over from */
    public static Island islandAt(int x,int z) {
        int cx=cellOf(x),cz=cellOf(z);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            var island=island(cx+dx,cz+dz);
            if(island!=null&&island.column(x,z)!=null)return island;
        }
        return null;
    }
    /** where a traveller arrives: the middle of the origin island */
    public static Island arrival() {return island(0,0);}
}
