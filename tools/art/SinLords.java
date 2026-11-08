import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * The six Sin Lords besides Pride, remade to his standard, and their weapons and Greed's mount as models of their own
 * (client/SinBossClient draws them at the lord's "weapon" and "mount" bones). Box-UV textures with glow masks, molang
 * animations, and stone statue textures. Usage, from the repository: java tools/art/SinLords.java src/main/resources/assets/primordial
 *
 * Bedrock conventions as GeckoLib shows them: the model faces -z, its right side is -x. Rotation x: positive tilts an
 * upright part forward and swings a hanging limb back. Rotation z: positive raises a limb sticking out to the right (-x).
 * A weapon or mount is drawn in its lord's model space: its cubes sit where the hand (or the saddle) is at rest.
 */
public class SinLords {
    static final int GLOW=1<<24;

    // ------------------------------------------------------------------ the model
    static final class Cube {
        double[] origin; int[] size; int[] uv=new int[2]; String style; double[] pivot,rot;
        Cube(double[] o,int[] s,String style){origin=o;size=s;this.style=style;}
        /** turn this cube alone about a pivot (degrees) */
        Cube turn(double px,double py,double pz,double rx,double ry,double rz){pivot=new double[]{px,py,pz};rot=new double[]{rx,ry,rz};return this;}
        int boxW(){return 2*(size[0]+size[2]);}
        int boxH(){return size[1]+size[2];}
        int[] face(String f){
            int u=uv[0],v=uv[1],w=size[0],h=size[1],d=size[2];
            return switch(f){
                case "up"->new int[]{u+d,v,w,d};case "down"->new int[]{u+d+w,v,w,d};
                case "east"->new int[]{u,v+d,d,h};case "north"->new int[]{u+d,v+d,w,h};
                case "west"->new int[]{u+d+w,v+d,d,h};case "south"->new int[]{u+2*d+w,v+d,w,h};
                default->throw new IllegalArgumentException(f);
            };
        }
    }
    static final class Bone {
        String name,parent; double[] pivot,rotation; List<Cube> cubes=new ArrayList<>();
        Cube c(double ox,double oy,double oz,int w,int h,int d,String style){Cube c=new Cube(new double[]{ox,oy,oz},new int[]{w,h,d},style);cubes.add(c);return c;}
        Bone rot(double x,double y,double z){rotation=new double[]{x,y,z};return this;}
    }
    record Deco(Cube cube,String face,int x,int y,int w,int h,int color,boolean glow){}
    static final class Model {
        final String id; final int tw; int th; final List<Bone> bones=new ArrayList<>(); final List<Deco> decos=new ArrayList<>();
        final Map<String,Anim> anims=new LinkedHashMap<>();
        Model(String id,int tw){this.id=id;this.tw=tw;}
        Bone bone(String name,String parent,double x,double y,double z){Bone b=new Bone();b.name=name;b.parent=parent;b.pivot=new double[]{x,y,z};bones.add(b);return b;}
        void deco(Cube c,String face,int x,int y,int w,int h,int color,boolean glow){decos.add(new Deco(c,face,x,y,w,h,color,glow));}
        void band(Cube c,int y,int h,int color,boolean glow){for(String f:new String[]{"north","south","east","west"}){int[] r=c.face(f);if(y<r[3])deco(c,f,0,y,r[2],Math.min(h,r[3]-y),color,glow);}}
        Anim anim(String name,boolean loop,double length){Anim a=new Anim(loop,length);anims.put("animation."+id+"."+name,a);return a;}
        int cubes(){int n=0;for(Bone b:bones)n+=b.cubes.size();return n;}
    }
    static final class Anim {
        final boolean loop; final double length; final Map<String,Map<String,Object>> bones=new LinkedHashMap<>();
        Anim(boolean loop,double length){this.loop=loop;this.length=length;}
        private Map<String,Object> ch(String bone){return bones.computeIfAbsent(bone,k->new LinkedHashMap<>());}
        Anim r(String bone,Object x,Object y,Object z){ch(bone).put("rotation",new Object[]{x,y,z});return this;}
        Anim p(String bone,Object x,Object y,Object z){ch(bone).put("position",new Object[]{x,y,z});return this;}
        Anim s(String bone,Object x,Object y,Object z){ch(bone).put("scale",new Object[]{x,y,z});return this;}
        @SuppressWarnings("unchecked")
        Anim rk(String bone,double t,double x,double y,double z){
            var map=ch(bone);Object cur=map.get("rotation");
            TreeMap<Double,Object[]> keys=cur instanceof TreeMap<?,?> k?(TreeMap<Double,Object[]>)k:new TreeMap<>();
            keys.put(t,new Object[]{x,y,z});map.put("rotation",keys);return this;
        }
        @SuppressWarnings("unchecked")
        Anim pk(String bone,double t,double x,double y,double z){
            var map=ch(bone);Object cur=map.get("position");
            TreeMap<Double,Object[]> keys=cur instanceof TreeMap<?,?> k?(TreeMap<Double,Object[]>)k:new TreeMap<>();
            keys.put(t,new Object[]{x,y,z});map.put("position",keys);return this;
        }
    }
    /** molang: a sine wave in degrees per second, with a phase */
    static String wave(double amp,double degPerSec,double phase,double offset){
        String s="math.sin(query.anim_time * "+num(degPerSec)+(phase!=0?" + "+num(phase):"")+") * "+num(amp);
        return offset!=0?num(offset)+" + "+s:s;
    }
    static String spin(double degPerSec){return "query.anim_time * "+num(degPerSec);}

    // ------------------------------------------------------------------ painting
    static int hash(int x,int y,int seed){int h=x*374761393+y*668265263+seed*1442695041;h=(h^(h>>>13))*1274126177;return h^(h>>>16);}
    static int shade(int rgb,double f){
        int r=(int)Math.max(0,Math.min(255,((rgb>>16)&255)*f)),g=(int)Math.max(0,Math.min(255,((rgb>>8)&255)*f)),b=(int)Math.max(0,Math.min(255,(rgb&255)*f));
        return (r<<16)|(g<<8)|b;
    }
    static int mix(int a,int b,double t){
        t=Math.max(0,Math.min(1,t));
        return (int)Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t)<<16|(int)Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t)<<8|(int)Math.round((a&255)*(1-t)+(b&255)*t);
    }
    static int jitter(int rgb,int amount,int n){
        int d=(Math.floorMod(n,2*amount+1))-amount;
        int r=Math.max(0,Math.min(255,((rgb>>16)&255)+d)),g=Math.max(0,Math.min(255,((rgb>>8)&255)+d)),b=Math.max(0,Math.min(255,(rgb&255)+d));
        return (r<<16)|(g<<8)|b;
    }
    static int hex(String s){return Integer.parseInt(s,16);}
    /** the colour of a pixel of a style; GLOW set where it shines */
    static int base(String style,String face,int x,int y,int w,int h,int n){
        boolean edge=x==0||y==0||x==w-1||y==h-1,side=!face.equals("up")&&!face.equals("down");
        int colon=style.indexOf(':');String kind=colon<0?style:style.substring(0,colon);int k=colon<0?0:hex(style.substring(colon+1));
        switch(kind){
            case "crystal":{int c=jitter(k,10,n);if(Math.floorMod(x+y,4)==0)c=mix(k,0xFFFFFF,0.55);else if(Math.floorMod(x-y,5)==0)c=shade(k,0.78);return c|GLOW;}
            case "glow":{return jitter(k,8,n)|GLOW;}
            case "plate":{   // armour: lit from above, a bevel round panels, a rivet at their corners
                double t=h<=1?0:y/(double)(h-1);int c=jitter(mix(mix(k,0xFFFFFF,0.22),shade(k,0.62),side?t:0.3),5,n);
                boolean panel=w>=5&&h>=5;
                if(panel&&(x==1||x==w-2)&&(y==1||y==h-2))return mix(k,0xFFFFFF,0.55);
                if(panel&&(x==1||y==1||x==w-2||y==h-2)&&!edge)c=shade(c,0.82);
                if(x==0||y==0)return mix(c,0xFFFFFF,0.25);
                if(x==w-1||y==h-1)return shade(c,0.6);
                return c;}
            case "cloth":{int c=jitter(k,5,n);if(x%3==2)c=shade(c,0.82);if(side&&y>=h-1)return shade(k,0.55);return edge?shade(c,0.88):c;}
            case "silk":{int c=jitter(mix(k,0xFFFFFF,0.15),4,n);if(x%4==3)c=shade(c,0.86);if(side&&y>=h-1)return jitter(0xF2D27A,5,n);return edge?shade(c,0.92):c;}
            case "hair":{int streak=Math.floorMod(hash(x,0,7),5);int c=streak==0?mix(k,0xFFFFFF,0.35):streak==1?shade(k,0.7):k;c=jitter(c,6,n);
                if(y==0&&!face.equals("down"))c=mix(k,0xFFFFFF,0.45);return edge?shade(c,0.85):c;}
            case "fur":{int c=jitter(k,12,n);if(Math.floorMod(n>>5,5)==0)c=mix(c,0xFFFFFF,0.25);return edge?shade(c,0.85):c;}
            case "wing":{   // a butterfly's wing: veins dark, its rim darker, bright eyes in it
                int c=jitter(k,6,n);
                if(Math.floorMod(x,4)==0||Math.floorMod(y-x/2,5)==0)c=shade(k,0.62);
                if(edge)return shade(k,0.45);
                if(Math.floorMod(hash(x/3,y/3,11),7)==0)return mix(k,0xFFFFFF,0.7)|GLOW;
                return c;}
            case "skin":{int c=jitter(k==0?0xE8D2C0:k,4,n);return edge?shade(c,0.92):c;}
        }
        switch(style){
            case "void":return jitter(0x06070A,3,n);
            case "shadow":{int c=jitter(0x14121C,4,n);if(Math.floorMod(n>>4,19)==0)return 0x3AFF8A|GLOW;if(Math.floorMod(x+2*y,5)==0)c=0x1E1A2A;return edge?shade(c,0.7):c;}
            case "rags":{int c=jitter(0x221E2C,5,n);if(x%3==1)c=jitter(0x2C2638,4,n);if(side&&y>=h-1&&Math.floorMod(x,2)==0)return 0x0E0C14;return edge?shade(c,0.8):c;}
            case "bandage":{int c=jitter(0x5A5466,5,n);if(Math.floorMod(x+y*2,4)==0)c=0x3E3A48;return c;}
            case "iron":{int c=jitter(0x4A4A52,6,n);if(Math.floorMod(n>>6,13)==0)c=0x6A3A22;if(y==0&&side)c=0x6A6A74;return edge?shade(c,0.7):c;}
            case "chain":{int c=Math.floorMod(x+y,3)==0?0x8A8A94:Math.floorMod(x+y,3)==1?0x5A5A64:0x2E2E34;return jitter(c,4,n);}
            case "char":{int c=jitter(0x2A2426,6,n);if(Math.floorMod(n>>6,9)==0)c=0x3C3436;return edge?shade(c,0.7):c;}
            case "magma":{   // charred hide split by glowing cracks
                int c=jitter(0x2E2224,6,n);
                if(Math.floorMod(x*3+y*5+(n>>13),17)==0||Math.floorMod(x-y*2+(n>>15),23)==0)return (Math.floorMod(n,3)==0?0xFFD27A:0xFF5A1F)|GLOW;
                return edge?shade(c,0.75):c;}
            case "ember":{int c=jitter(0xFF8A2A,14,n);if(Math.floorMod(x+y,3)==0)c=0xFFD27A;return c|GLOW;}
            case "bone":{int c=jitter(0xE2DAC4,8,n);if(Math.floorMod(n>>7,13)==0)c=0xB7AE95;return edge?shade(c,0.78):c;}
            case "horn":{double t=h<=1?0:y/(double)(h-1);int c=jitter(mix(0x2A2420,0xC8B89A,side?1-t:0.5),6,n);if(y%3==0&&side)c=shade(c,0.8);return c;}
            case "gold":{int c=jitter(0xD8B04A,8,n);if(y==0&&side)c=0xF4D27A;if((x==1||x==w-2)&&(y==1||y==h-2)&&w>3&&h>3)c=0xFFE9A8;return edge?shade(c,0.66):c;}
            case "coins":{int cx=Math.floorMod(x,3),cy=Math.floorMod(y,3);int c=cx==1&&cy==1?0x9A6A14:jitter(0xF2C440,10,n);if(cx==0&&cy==0)c=0xFFF0A0;return c;}
            case "flesh":{int c=jitter(0x5A0E14,8,n);if(Math.floorMod(n>>5,7)==0)c=0x8A2A30;if(Math.floorMod(x*7+y*3,13)==0)c=0xB04048;return c;}
            case "tongue":{int c=jitter(0xB04060,8,n);if(x==w/2)c=0x8A2A44;return c;}
            case "leather":{int c=jitter(0x5A3A22,6,n);if(x%4==3)c=0x4A2E1A;return edge?shade(c,0.75):c;}
            case "blade":{int c=jitter(0xC8CED8,4,n);if(x==w/2&&w>2)c=0x8E96A6;if(x==0||x==w-1)c=0xF2F6FF;return c;}
            case "obsidian":{int c=jitter(0x1A1424,5,n);if(Math.floorMod(n>>6,17)==0)c=0x3B2A5A;return edge?shade(c,0.75):c;}
            case "wood":{int c=jitter(0x6A4A2E,6,n);if(y%3==0)c=0x5A3E24;return edge?shade(c,0.8):c;}
            default:throw new IllegalArgumentException(style);
        }
    }
    static final String[] FACES={"up","down","east","north","west","south"};
    static void pack(Model m){
        List<Cube> all=new ArrayList<>();for(Bone b:m.bones)all.addAll(b.cubes);
        all.sort((a,b)->b.boxH()!=a.boxH()?b.boxH()-a.boxH():b.boxW()-a.boxW());
        int x=0,y=0,shelf=0;
        for(Cube c:all){
            if(c.boxW()>m.tw)throw new IllegalStateException(m.id+": cube too wide "+Arrays.toString(c.size));
            if(x+c.boxW()>m.tw){x=0;y+=shelf;shelf=0;}
            c.uv[0]=x;c.uv[1]=y;x+=c.boxW();shelf=Math.max(shelf,c.boxH());
        }
        int h=y+shelf,th=32;while(th<h)th*=2;m.th=th;
    }
    static void paint(Model m,File tex,File glow) throws Exception {
        BufferedImage img=new BufferedImage(m.tw,m.th,BufferedImage.TYPE_INT_ARGB),gm=new BufferedImage(m.tw,m.th,BufferedImage.TYPE_INT_ARGB);
        int seed=m.id.hashCode();
        for(Bone b:m.bones)for(Cube c:b.cubes){
            seed++;
            for(String f:FACES){
                int[] r=c.face(f);
                for(int x=0;x<r[2];x++)for(int y=0;y<r[3];y++){
                    int v=base(c.style,f,x,y,r[2],r[3],hash(x,y,seed+f.hashCode()));
                    int rgb=v&0xFFFFFF;
                    if((v&GLOW)==0)rgb=shade(rgb,switch(f){case "up"->1.12;case "down"->0.7;case "south"->0.9;case "east","west"->0.86;default->1.0;});
                    img.setRGB(r[0]+x,r[1]+y,0xFF000000|rgb);
                    if((v&GLOW)!=0)gm.setRGB(r[0]+x,r[1]+y,0xFF000000|rgb);
                }
            }
        }
        for(Deco d:m.decos){
            int[] r=d.cube.face(d.face);
            for(int x=0;x<d.w;x++)for(int y=0;y<d.h;y++){
                if(d.x+x>=r[2]||d.y+y>=r[3])continue;
                int px=r[0]+d.x+x,py=r[1]+d.y+y;
                img.setRGB(px,py,0xFF000000|d.color);gm.setRGB(px,py,d.glow?0xFF000000|d.color:0);
            }
        }
        ImageIO.write(img,"png",tex);ImageIO.write(gm,"png",glow);
    }
    /** the same texture turned to weathered stone, for the lord's statue */
    static void statue(File tex,File out,File glow,File glowOut) throws Exception {
        var src=ImageIO.read(tex);var img=new BufferedImage(src.getWidth(),src.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int x=0;x<src.getWidth();x++)for(int y=0;y<src.getHeight();y++){
            int p=src.getRGB(x,y),a=p>>>24;if(a==0)continue;
            double lum=(((p>>16)&255)*0.3+((p>>8)&255)*0.59+(p&255)*0.11)/255.0;
            int c=mix(0x3C3A3A,0xC8C4BC,0.22+lum*0.7);
            int n=hash(x,y,7);if(Math.floorMod(n,7)==0)c=mix(c,0x2A2828,0.3);else if(Math.floorMod(n>>5,11)==0)c=mix(c,0x5A6A4A,0.25);
            img.setRGB(x,y,a<<24|c);
        }
        ImageIO.write(img,"png",out);Files.copy(glow.toPath(),glowOut.toPath(),StandardCopyOption.REPLACE_EXISTING);
    }

    // ------------------------------------------------------------------ json
    static String num(double d){double r=Math.round(d*1000)/1000.0;return r==Math.rint(r)?Long.toString((long)r):String.format(Locale.ROOT,"%.3f",r).replaceAll("0+$","");}
    static String val(Object o){return o instanceof String s?"\""+s+"\"":num(((Number)o).doubleValue());}
    static String vec(double... v){StringBuilder s=new StringBuilder("[");for(int i=0;i<v.length;i++){if(i>0)s.append(", ");s.append(num(v[i]));}return s.append("]").toString();}
    static String vec(Object[] v){StringBuilder s=new StringBuilder("[");for(int i=0;i<v.length;i++){if(i>0)s.append(", ");s.append(val(v[i]));}return s.append("]").toString();}
    static String geo(Model m){
        StringBuilder s=new StringBuilder();
        s.append("{\n\t\"format_version\": \"1.12.0\",\n\t\"minecraft:geometry\": [\n\t\t{\n\t\t\t\"description\": {\n");
        s.append("\t\t\t\t\"identifier\": \"geometry.").append(m.id).append("\",\n\t\t\t\t\"texture_width\": ").append(m.tw)
         .append(",\n\t\t\t\t\"texture_height\": ").append(m.th).append(",\n\t\t\t\t\"visible_bounds_width\": 6,\n\t\t\t\t\"visible_bounds_height\": 6,\n\t\t\t\t\"visible_bounds_offset\": [0, 1.5, 0]\n\t\t\t},\n\t\t\t\"bones\": [\n");
        for(int i=0;i<m.bones.size();i++){
            Bone b=m.bones.get(i);
            s.append("\t\t\t\t{\"name\": \"").append(b.name).append("\"");
            if(b.parent!=null)s.append(", \"parent\": \"").append(b.parent).append("\"");
            s.append(", \"pivot\": ").append(vec(b.pivot));
            if(b.rotation!=null)s.append(", \"rotation\": ").append(vec(b.rotation));
            if(!b.cubes.isEmpty()){
                s.append(", \"cubes\": [");
                for(int j=0;j<b.cubes.size();j++){
                    Cube c=b.cubes.get(j);if(j>0)s.append(", ");
                    s.append("{\"origin\": ").append(vec(c.origin)).append(", \"size\": ").append(vec(c.size[0],c.size[1],c.size[2])).append(", \"uv\": ").append(vec(c.uv[0],c.uv[1]));
                    if(c.rot!=null)s.append(", \"pivot\": ").append(vec(c.pivot)).append(", \"rotation\": ").append(vec(c.rot));
                    s.append("}");
                }
                s.append("]");
            }
            s.append("}").append(i<m.bones.size()-1?",":"").append("\n");
        }
        return s.append("\t\t\t]\n\t\t}\n\t]\n}\n").toString();
    }
    @SuppressWarnings("unchecked")
    static String channel(Object value){
        if(value instanceof Object[] v)return vec(v);
        StringBuilder s=new StringBuilder("{");boolean first=true;
        for(var e:((TreeMap<Double,Object[]>)value).entrySet()){if(!first)s.append(", ");first=false;s.append("\"").append(num(e.getKey())).append("\": ").append(vec(e.getValue()));}
        return s.append("}").toString();
    }
    static String anims(Model m){
        StringBuilder s=new StringBuilder("{\n\t\"format_version\": \"1.8.0\",\n\t\"animations\": {\n");
        int i=0;
        for(var e:m.anims.entrySet()){
            Anim a=e.getValue();
            s.append("\t\t\"").append(e.getKey()).append("\": {\n");
            if(a.loop)s.append("\t\t\t\"loop\": true,\n");
            s.append("\t\t\t\"animation_length\": ").append(num(a.length)).append(",\n\t\t\t\"bones\": {\n");
            int j=0;
            for(var bone:a.bones.entrySet()){
                s.append("\t\t\t\t\"").append(bone.getKey()).append("\": {");
                int k=0;for(var ch:bone.getValue().entrySet()){if(k++>0)s.append(", ");s.append("\"").append(ch.getKey()).append("\": ").append(channel(ch.getValue()));}
                s.append("}").append(++j<a.bones.size()?",":"").append("\n");
            }
            s.append("\t\t\t}\n\t\t}").append(++i<m.anims.size()?",":"").append("\n");
        }
        return s.append("\t}\n}\n").toString();
    }

    // ------------------------------------------------------------------ shared pieces
    /** a ring of n bars round (cx,cy) at depth z, radius r, in the bone's x-y plane */
    static void ring(Bone b,double cx,double cy,double z,double r,int n,int length,int thick,String style){
        for(int i=0;i<n;i++){double a=Math.PI*2*i/n,x=cx+Math.cos(a)*r,y=cy+Math.sin(a)*r;
            b.c(x-length/2.0,y-thick/2.0,z,length,thick,1,style).turn(x,y,z+0.5,0,0,-(Math.toDegrees(a)+90));}
    }
    /** a hanging chain of links from (x,y,z) down, each link a bone of its own so it swings */
    static void chain(Model m,String name,String parent,double x,double y,double z,int links){
        String p=parent;
        for(int i=0;i<links;i++){
            Bone l=m.bone(name+i,p,x,y-i*3,z);
            if(i%2==0)l.c(x-0.5,y-i*3-3,z-1,1,3,2,"chain");else l.c(x-1,y-i*3-3,z-0.5,2,3,1,"chain");
            p=l.name;
        }
    }
    static Bone find(Model m,String name){for(Bone b:m.bones)if(b.name.equals(name))return b;throw new IllegalArgumentException(m.id+": no bone "+name);}
    /** a weapon socket: an empty bone where the lord's separate weapon model is drawn */
    static void socket(Model m,String parent,double x,double y,double z){m.bone("weapon",parent,x,y,z);}

    // ================================================================== 暴怒之主 · 狂王
    /**
     * A hulking horned beast, hunched forward: charred hide split by glowing cracks, great curling horns, a jaw that opens on
     * a furnace, broken shackles on wrists and ankles with chains that swing, spikes down its spine, a short tail.
     */
    static Model wrath(){
        Model m=new Model("wrath_lord",128);
        String HIDE="magma",IRON="iron",HORN="horn",BONE="bone",EYE="glow:FF3A1A",FIRE="ember";
        m.bone("root",null,0,0,0);
        Bone hips=m.bone("hips","root",0,24,2);
        hips.c(-6,21,-1,12,6,8,HIDE);hips.c(-6.5,22,-1.5,13,2,9,"leather");
        Bone loinF=m.bone("loin_front","hips",0,22,-1.5);loinF.c(-4,14,-2,8,8,1,"cloth:5A1414");
        Bone loinB=m.bone("loin_back","hips",0,22,7);loinB.c(-4.5,13,6.5,9,9,1,"cloth:5A1414");
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*4.5;
            Bone thigh=m.bone(n+"_thigh","hips",x,22,2).rot(-22,0,side*-6);
            thigh.c(x-3,13,-1,6,10,6,HIDE);thigh.c(x-3.5,18,-1.5,7,3,7,HIDE);
            Bone shin=m.bone(n+"_shin",n+"_thigh",x,13,2).rot(48,0,0);
            shin.c(x-2,4,0,4,9,4,HIDE);shin.c(x-2.6,6,-0.6,5,3,5,IRON);
            chain(m,n+"_ankle_chain",n+"_shin",x+side*2.6,6,2,2);
            Bone foot=m.bone(n+"_foot",n+"_shin",x,4,2).rot(-26,0,0);
            foot.c(x-2.5,0,-3,5,4,6,HIDE);
            for(int t=0;t<3;t++)foot.c(x-2+t*1.5,0,-5,1,2,2,BONE);
            foot.c(x-1,0,3,2,2,2,BONE);
        }
        Bone body=m.bone("body","hips",0,26,2).rot(28,0,0);
        body.c(-7,26,-3,14,10,9,HIDE);
        body.c(-6,27,-4,12,7,1,HIDE);
        for(int i=0;i<4;i++)body.c(-5+i*0.5,27+i*2,-4.4,10-i,1,1,BONE);   // ribs showing through
        for(int i=0;i<5;i++)body.c(-0.5,30+i*2.5,6,1,3,3,BONE).turn(0,31+i*2.5,6,-30,0,0);   // the spine's spikes
        Bone chest=m.bone("chest","body",0,36,1);
        chest.c(-9,35,-4,18,8,10,HIDE);
        Cube core=chest.c(-2,37,-4.6,4,4,1,"crystal:FF5A1F");
        chest.c(-9.5,40,-3,5,4,8,HIDE);chest.c(4.5,40,-3,5,4,8,HIDE);   // the shoulders' mass
        for(int i=0;i<4;i++)chest.c(-0.5,43,0.5+i*2.4,1,3+(i%2),2,BONE).turn(0,43,1+i*2.4,-25,0,0);
        // a broken collar of iron, its chain hanging from the front
        chest.c(-5,42,-4.5,10,2,1,IRON);chest.c(-5.5,42,-4,1,2,8,IRON);chest.c(4.5,42,-4,1,2,8,IRON);
        chain(m,"collar_chain","chest",0,42,-4.5,3);
        Bone neck=m.bone("neck","chest",0,43,-1).rot(-22,0,0);
        neck.c(-3,42,-4,6,5,6,HIDE);
        Bone head=m.bone("head","neck",0,46,-3);
        Cube skull=head.c(-4,44,-9,8,7,7,HIDE);
        head.c(-4.5,49,-9.5,9,2,4,HIDE);   // the brow, heavy over the eyes
        head.c(-3,44,-11,6,4,2,HIDE);   // the muzzle
        head.c(-1.5,46,-11.5,3,1,1,"char");
        Cube eyeL=head.c(1.5,48,-9.6,2,1,1,EYE),eyeR=head.c(-3.5,48,-9.6,2,1,1,EYE);
        for(int i=0;i<4;i++)head.c(-3+i*2,43.5,-11.3,1,2,1,BONE);   // upper fangs
        head.c(-4.5,47,-4,9,6,3,FIRE);head.c(-3.5,50,-2.5,7,3,3,FIRE);   // a burning mane behind the head
        Bone jaw=m.bone("jaw","head",0,44.5,-4);
        jaw.c(-3.5,42,-11,7,2,7,HIDE);jaw.c(-3,43.5,-10.5,6,1,5,"flesh");
        for(int i=0;i<3;i++)jaw.c(-2.5+i*2,44,-11,1,1,1,BONE);
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*4;
            // the horns: out, back, then up and forward, in four turning pieces
            Bone h0=m.bone(n+"_horn","head",x,50,-6).rot(0,0,side*-55);
            h0.c(x-1.5,50,-7.5,3,4,3,HORN);
            Bone h1=m.bone(n+"_horn1",n+"_horn",x,54,-6).rot(25,0,side*-25);h1.c(x-1.2,54,-7.2,2,4,2,HORN);h1.c(x-1.3,54.5,-7.3,2,1,2,BONE);
            Bone h2=m.bone(n+"_horn2",n+"_horn1",x,58,-6).rot(35,0,side*-10);h2.c(x-1,58,-7,2,3,2,HORN);
            Bone h3=m.bone(n+"_horn3",n+"_horn2",x,61,-6).rot(40,0,0);h3.c(x-0.5,61,-6.5,1,3,1,BONE);
        }
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*10;
            Bone arm=m.bone(n+"_arm","chest",x,41,0).rot(-10,0,side*-12);
            arm.c(x-3,31,-3,6,11,6,HIDE);arm.c(x-3.5,38,-3.5,7,4,7,HIDE);
            arm.c(x-1+side*2,40,-1,2,4,2,BONE);   // a spur at the shoulder
            Bone fore=m.bone(n+"_forearm",n+"_arm",x,31,0).rot(-30,0,0);
            fore.c(x-3.5,20,-3.5,7,11,7,HIDE);
            Cube shackle=fore.c(x-4,22,-4,8,3,8,IRON);fore.c(x-4.2,23,-4.2,8,1,8,"chain");
            chain(m,n+"_wrist_chain",n+"_forearm",x+side*4,22,0,4);
            Bone hand=m.bone(n+"_hand",n+"_forearm",x,20,0);
            hand.c(x-3,15,-3,6,5,6,HIDE);
            for(int f=0;f<4;f++)hand.c(x-2.5+f*1.4,12.5,-3.4,1,3,1,BONE).turn(x-2+f*1.4,15,-3,-25,0,0);
            hand.c(x+side*-3.2,14,-1,1,3,1,BONE);
        }
        Bone tail=m.bone("tail","hips",0,24,8).rot(-30,0,0);tail.c(-1.5,21,8,3,3,6,HIDE);
        Bone tail1=m.bone("tail1","tail",0,22.5,14).rot(-15,0,0);tail1.c(-1,21.5,14,2,2,6,HIDE);tail1.c(-0.5,23,15,1,2,2,BONE);
        Bone tail2=m.bone("tail2","tail1",0,22.5,20);tail2.c(-0.5,22,20,1,1,4,FIRE);
        // more of its fury: tongues of flame in its mane, spikes on its knuckles and elbows, iron bands, scars burning, embers circling
        for(int i=0;i<7;i++){double a=-0.9+i*0.3;head.c(Math.sin(a)*4-0.5,49+Math.cos(i)*1.5,-3+Math.cos(a)*2,1,3+(i%3),1,"ember").turn(Math.sin(a)*4,49,-3,-20-i*4,0,Math.toDegrees(a)*0.6);}
        for(int i=0;i<4;i++)head.c(-2.5+i*1.6,46,-9.8,1,1,1,"char");   // scars across its snout
        head.c(-4.5,45,-7,1,2,2,BONE);head.c(3.5,45,-7,1,2,2,BONE);   // tusks at the corners of its mouth
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*10;
            Bone fore=find(m,n+"_forearm");
            fore.c(x-3.8,27,-3.8,8,1,8,"chain");fore.c(x-0.5+side*3,28,-1,1,3,2,BONE).turn(x+side*3,29,0,0,0,side*-30);   // a band, an elbow spike
            Bone hand=find(m,n+"_hand");
            for(int f=0;f<4;f++)hand.c(x-2.5+f*1.4,19.5,-3.6,1,1,1,BONE);   // spikes on its knuckles
            Bone arm=find(m,n+"_arm");
            for(int i=0;i<3;i++)arm.c(x-0.5+side*(1.5+i),41+i,-1+i,1,2+i,1,BONE).turn(x+side*(1.5+i),41,0,-15*i,0,side*-20);   // a crown of spurs on its shoulder
            arm.c(x-3.3,33,-3.3,7,1,7,IRON);
            Bone thigh=find(m,n+"_thigh");
            thigh.c(x-0.5,15,-1.4,1,4,1,"magma");thigh.c(x+side*3-0.5,16,1,1,3,1,"magma");
        }
        for(int i=0;i<6;i++)body.c(-4+i*1.6,27.5+(i%2)*3,-4.5,1,3,1,"magma");   // scars across the chest, glowing
        for(int i=0;i<3;i++)chest.c(-6+i*6,36,5.5,1,3,1,"magma");
        for(int i=0;i<4;i++)chest.c(-7.5+i*4.6,43,-2+i%2,2,1,2,"char");   // the hide's knotted ridges
        for(int i=0;i<8;i++){double t=i/7.0;double x=-7+t*14,y=43-t*12;   // a broken chain wound across its chest, shoulder to flank
            chest.c(x-1,y-1,-5.2,i%2==0?2:1,i%2==0?1:2,1,"chain").turn(x,y,-5,0,0,-40);}
        for(int side=-1;side<=1;side+=2){String n=side<0?"right":"left";double x=side*4;
            Bone h1=find(m,n+"_horn1");h1.c(x-1.4,55.5,-7.4,2,1,2,IRON);
            Bone h0=find(m,n+"_horn");h0.c(x-1.7,52,-7.7,3,1,3,IRON);h0.c(x-1.7,50.5,-7.7,3,1,3,"chain");}
        Bone embers=m.bone("embers","root",0,30,0);
        for(int i=0;i<10;i++){double a=Math.PI*2*i/10;embers.c(Math.cos(a)*(11+i%3)-0.5,24+(i*7)%18,Math.sin(a)*(11+i%3)-0.5,1,1,1,i%3==0?"crystal:FFD27A":"ember");}
        m.deco(skull,"north",1,2,6,1,0x1A1012,false);
        m.deco(core,"north",1,1,2,2,0xFFF0B0,true);

        String[] chains={"right_wrist_chain","left_wrist_chain","collar_chain","right_ankle_chain","left_ankle_chain"};
        Anim idle=m.anim("idle",true,3);
        idle.s("chest",wave(0.02,120,0,1),wave(0.02,120,0,1),wave(0.02,120,0,1)).r("head",wave(3,60,0,0),wave(6,40,0,0),0).r("jaw",wave(4,120,0,6),0,0)
            .r("right_arm",wave(3,120,0,-10),0,12).r("left_arm",wave(3,120,60,-10),0,-12).r("right_forearm",-30,0,0).r("left_forearm",-30,0,0)
            .r("tail",wave(6,90,0,-30),wave(12,60,0,0),0).r("tail1",wave(8,90,40,-15),0,0)
            .r("loin_front",wave(3,120,0,0),0,0).r("loin_back",wave(4,120,30,8),0,0);
        for(String c:chains)for(int i=0;i<(c.contains("wrist")?4:c.contains("collar")?3:2);i++)idle.r(c+i,wave(6,90,i*40,0),0,wave(5,70,i*50,0));
        idle.r("embers",0,spin(30),0).p("embers",0,wave(1.5,60,0,0),0);
        Anim walk=m.anim("walk",true,1.4);
        walk.r("embers",0,spin(60),0);
        walk.p("root",0,wave(1,514,0,0),0).r("right_thigh",wave(28,257,0,-22),0,6).r("left_thigh",wave(28,257,180,-22),0,-6)
            .r("right_shin",wave(20,257,90,48),0,0).r("left_shin",wave(20,257,270,48),0,0)
            .r("right_arm",wave(22,257,180,-10),0,12).r("left_arm",wave(22,257,0,-10),0,-12).r("body",wave(3,514,0,30),wave(5,257,0,0),0)
            .r("head",wave(4,514,0,0),0,0).r("tail",-25,wave(18,257,0,0),0).r("loin_front",wave(12,257,0,-6),0,0).r("loin_back",wave(10,257,90,14),0,0);
        for(String c:chains)for(int i=0;i<(c.contains("wrist")?4:c.contains("collar")?3:2);i++)walk.r(c+i,wave(18,257,i*40,6),0,wave(10,257,i*50,0));
        m.anim("attack",false,0.6).rk("right_arm",0,-10,0,12).rk("right_arm",0.25,-170,0,20).rk("right_arm",0.45,-20,0,0).rk("right_arm",0.6,-10,0,12)
            .rk("right_forearm",0,-30,0,0).rk("right_forearm",0.25,-60,0,0).rk("right_forearm",0.45,0,0,0).rk("right_forearm",0.6,-30,0,0)
            .rk("body",0,0,0,0).rk("body",0.25,-12,0,0).rk("body",0.45,14,0,0).rk("body",0.6,0,0,0).rk("jaw",0,6,0,0).rk("jaw",0.3,28,0,0).rk("jaw",0.6,6,0,0);
        m.anim("charge",true,0.7).r("body",22,0,0).r("head",-10,0,0).r("jaw",22,0,0)
            .r("right_arm",55,0,20).r("left_arm",55,0,-20).r("right_forearm",-10,0,0).r("left_forearm",-10,0,0)
            .r("right_thigh",wave(40,514,0,-30),0,6).r("left_thigh",wave(40,514,180,-30),0,-6).r("right_shin",wave(30,514,90,40),0,0).r("left_shin",wave(30,514,270,40),0,0)
            .p("root",0,wave(1.5,1028,0,0),0).r("tail",-50,0,0);
        m.anim("slam",true,1).rk("right_arm",0,-170,0,25).rk("right_arm",0.5,-175,0,30).rk("right_arm",0.75,-20,0,-10).rk("right_arm",1,-170,0,25)
            .rk("left_arm",0,-170,0,-25).rk("left_arm",0.5,-175,0,-30).rk("left_arm",0.75,-20,0,10).rk("left_arm",1,-170,0,-25)
            .rk("body",0,-18,0,0).rk("body",0.5,-22,0,0).rk("body",0.75,36,0,0).rk("body",1,-18,0,0)
            .rk("jaw",0,10,0,0).rk("jaw",0.75,32,0,0).rk("jaw",1,10,0,0).r("right_forearm",-15,0,0).r("left_forearm",-15,0,0);
        m.anim("roar",true,1).r("head",-35,0,0).r("neck",-30,0,0).r("jaw",40,0,0).r("body",-10,0,0).r("chest",-8,0,0)
            .r("right_arm",-40,0,55).r("left_arm",-40,0,-55).r("right_forearm",-35,0,0).r("left_forearm",-35,0,0)
            .s("chest",wave(0.05,720,0,1.05),wave(0.05,720,0,1.05),wave(0.05,720,0,1.05)).r("tail",-60,wave(20,720,0,0),0);
        m.anim("stagger",true,1.5).r("head",wave(10,240,0,25),wave(18,120,0,0),wave(10,180,0,0)).r("jaw",18,0,0).r("body",wave(6,240,0,40),0,wave(8,120,0,0))
            .r("right_arm",20,0,30).r("left_arm",20,0,-30).r("right_forearm",-5,0,0).r("left_forearm",-5,0,0).p("root",0,-2,0);
        m.anim("dormant",true,4).p("root",0,-8,0).r("right_thigh",-80,0,10).r("left_thigh",-80,0,-10).r("right_shin",110,0,0).r("left_shin",110,0,0)
            .r("right_foot",-30,0,0).r("left_foot",-30,0,0).r("body",42,0,0).r("head",30,0,0).r("neck",10,0,0).r("jaw",2,0,0)
            .r("right_arm",-30,0,20).r("left_arm",-30,0,-20).r("right_forearm",-50,0,0).r("left_forearm",-50,0,0)
            .s("chest",wave(0.015,45,0,1),wave(0.015,45,0,1),wave(0.015,45,0,1)).r("tail",10,25,0);
        return m;
    }

    // ================================================================== 嫉妒之主 · 无貌之影
    /**
     * A faceless shade in a ragged hooded cloak: a void where the face should be, one green eye burning in it, tatters
     * hanging from the cloak's hem that stir, bandaged arms, a clawed left hand; shards of shadow circling it. Its dagger
     * is a model of its own; a copied weapon shows at "right_item" instead.
     */
    static Model envy(){
        Model m=new Model("envy_lord",128);
        String RAG="rags",WRAP="bandage",EYE="glow:6AFF9A";
        m.bone("root",null,0,0,0);
        Bone hips=m.bone("hips","root",0,14,0);
        hips.c(-3.5,13,-2,7,3,4,RAG);hips.c(-4,14,-2.5,8,1,5,"leather");hips.c(-1,14,-2.8,2,1,1,"crystal:3AFF8A");
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*1.8;
            Bone thigh=m.bone(n+"_thigh","hips",x,13,0).rot(4,0,side*-2);thigh.c(x-1.2,7,-1.2,2,6,2,WRAP);
            Bone shin=m.bone(n+"_shin",n+"_thigh",x,7,0).rot(6,0,0);shin.c(x-1.2,1,-1.2,2,6,2,"shadow");shin.c(x-1.5,0,-2.2,3,1,3,"rags");
        }
        // the cloak's hem: eight tatters round the waist, each its own bone so they stir
        for(int i=0;i<8;i++){double a=Math.PI*2*i/8,x=Math.sin(a)*3.4,z=-Math.cos(a)*2.6;int len=6+(i%3)*2;
            Bone t=m.bone("tatter"+i,"hips",x,13.5,z).rot(Math.cos(a)*-8,0,Math.sin(a)*8);
            t.c(x-1,13.5-len,z-0.5,2,len,1,RAG).turn(x,13.5,z,0,-Math.toDegrees(a),0);}
        Bone body=m.bone("body","hips",0,16,0).rot(8,0,0);
        body.c(-3,16,-1.5,6,8,3,"shadow");
        body.c(-3.5,17,-2,7,7,1,RAG);body.c(-3.5,17,1,7,7,1,RAG);
        for(int i=0;i<3;i++)body.c(-3+i*0.4,18+i*2,-2.3,6-i,1,1,WRAP);   // bindings across the chest
        Bone cloak=m.bone("cloak","body",0,24,1.5);cloak.c(-4.5,15,1.5,9,9,1,RAG);
        Bone cloak1=m.bone("cloak1","cloak",0,15,1.5);cloak1.c(-5,8,1.5,10,7,1,RAG);
        Bone cloak2=m.bone("cloak2","cloak1",0,8,1.5);for(int i=0;i<5;i++)cloak2.c(-5+i*2,2+(i%2)*2,1.5,2,6-(i%2)*2,1,RAG);
        Bone mantle=m.bone("mantle","body",0,24,0);mantle.c(-5,22,-2.5,10,3,5,RAG);mantle.c(-5.5,21,-2,1,3,4,RAG);mantle.c(4.5,21,-2,1,3,4,RAG);
        Bone head=m.bone("head","body",0,24,0);
        Cube face=head.c(-2.5,24.5,-2.6,5,6,5,"void");
        head.c(-3.5,24,-1.5,7,7,5,RAG);head.c(-3.5,30,-3.5,7,2,6,RAG);head.c(-3.5,24,-3.5,1,7,2,RAG);head.c(2.5,24,-3.5,1,7,2,RAG);
        Bone peak=m.bone("hood_peak","head",0,31,1).rot(-25,0,0);peak.c(-2,31,0,4,3,4,RAG);peak.c(-1,33,1.5,2,3,2,RAG);peak.c(-0.5,35,2.5,1,2,1,RAG);
        Cube eye=head.c(0.3,27.6,-2.8,2,1,1,EYE);head.c(-1.8,27.6,-2.7,1,1,1,"glow:1F8A48");
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*4.2;
            Bone arm=m.bone(n+"_arm","body",x,23,0).rot(5,0,side*-6);arm.c(x-1,15,-1,2,8,2,WRAP);arm.c(x-1.5,20,-1.5,3,3,3,RAG);
            Bone fore=m.bone(n+"_forearm",n+"_arm",x,15,0).rot(-20,0,0);fore.c(x-1,8,-1,2,7,2,WRAP);fore.c(x-1.3,12,-1.3,2,2,2,"shadow");
            Bone hand=m.bone(n+"_hand",n+"_forearm",x,8,0);hand.c(x-1,6,-1,2,2,2,"shadow");
            if(side>0)for(int f=0;f<3;f++)hand.c(x-1+f*0.8,3.5,-1.2,1,3,1,"obsidian").turn(x-0.5+f*0.8,6,-1,-20+f*10,0,0);
            else{m.bone("right_item","right_hand",x,6,-1);socket(m,"right_hand",x,7,0);}
        }
        // shards of shadow, circling
        Bone shards=m.bone("shards","root",0,18,0);
        for(int i=0;i<6;i++){double a=Math.PI*2*i/6,x=Math.cos(a)*7,z=Math.sin(a)*7;shards.c(x-0.5,16+(i%3)*3,z-0.5,1,2+(i%2),1,i%2==0?"crystal:3AFF8A":"obsidian");}
        // a second, inner hem of tatters; the hood in layers; the arms wound tighter; claws longer
        for(int i=0;i<8;i++){double a=Math.PI*2*(i+0.5)/8,x=Math.sin(a)*2.6,z=-Math.cos(a)*1.9;int len=4+(i%2)*2;
            Bone t=find(m,"tatter"+i);
            t.c(x-0.5,13.5-len,z-0.5,1,len,1,"shadow").turn(x,13.5,z,0,-Math.toDegrees(a),0);
            t.c(Math.sin(Math.PI*2*i/8)*3.4-0.5,13.5-(6+(i%3)*2)-1,-Math.cos(Math.PI*2*i/8)*2.6-0.5,1,1,1,"rags");}
        head.c(-3.8,25,-2.5,1,5,5,RAG);head.c(2.8,25,-2.5,1,5,5,RAG);head.c(-3,30.5,-3.8,6,1,1,RAG);   // the hood's inner fold
        for(int i=0;i<3;i++)head.c(-2+i*1.6,23.5,-3.6,1,2,1,RAG);   // its edge, frayed under the chin
        for(int side=-1;side<=1;side+=2){String n=side<0?"right":"left";double x=side*4.2;
            Bone arm=find(m,n+"_arm");for(int i=0;i<3;i++)arm.c(x-1.2,16+i*2,-1.2,2,1,2,"leather");
            Bone fore=find(m,n+"_forearm");for(int i=0;i<2;i++)fore.c(x-1.2,9+i*2,-1.2,2,1,2,"leather");
            fore.c(x-0.5,10,1,1,4,1,RAG).turn(x,13,1,20,0,0);}   // a loose strip trailing from each arm
        // what it has taken, hung from its belt: a ring, a little blade, a key, a locket
        Bone trinkets=m.bone("trinkets","hips",0,13,-2.5);
        trinkets.c(-3,11,-2.8,1,2,1,"gold");trinkets.c(-3.2,10,-3,1,1,1,"crystal:FF3348");
        trinkets.c(-1,10.5,-2.8,1,3,1,"blade");trinkets.c(1,11,-2.8,1,2,1,"iron");trinkets.c(1,10,-2.8,2,1,1,"iron");trinkets.c(2.5,11,-2.8,1,2,1,"gold");trinkets.c(2.5,10,-3,1,1,1,"crystal:6AA8FF");
        // a second, wider ring of shards, and eyes drifting in the dark about it
        Bone shards2=m.bone("shards2","root",0,20,0);
        for(int i=0;i<8;i++){double a=Math.PI*2*(i+0.5)/8,x=Math.cos(a)*10,z=Math.sin(a)*10;shards2.c(x-0.5,14+(i%4)*4,z-0.5,1,3,1,i%3==0?"crystal:3AFF8A":"obsidian");shards2.c(x-0.5,13+(i%4)*4,z-0.5,1,1,1,"shadow");}
        Bone eyes=m.bone("eyes","root",0,22,0);
        for(int i=0;i<5;i++){double a=Math.PI*2*i/5+0.3,x=Math.cos(a)*6,z=Math.sin(a)*6;eyes.c(x-1,20+(i%3)*4,z-0.5,2,1,1,"void");eyes.c(x-0.5,20+(i%3)*4,z-0.7,1,1,1,"glow:6AFF9A");}
        m.deco(face,"north",1,1,3,4,0x0A0A10,false);m.deco(eye,"north",0,0,2,1,0xD8FFE8,true);

        String[] tat=new String[8];for(int i=0;i<8;i++)tat[i]="tatter"+i;
        Anim idle=m.anim("idle",true,4);
        idle.p("root",0,wave(0.6,90,0,0),0).r("head",wave(3,45,0,6),wave(10,30,0,0),wave(4,60,0,0)).r("body",wave(2,90,0,8),0,0)
            .r("cloak",wave(4,90,0,6),0,0).r("cloak1",wave(6,90,40,6),0,0).r("cloak2",wave(8,90,80,6),0,0).r("hood_peak",wave(5,90,0,-25),0,0)
            .r("right_arm",wave(4,90,0,-10),0,10).r("right_forearm",-35,0,0).r("left_arm",wave(4,90,90,-5),0,-14).r("left_forearm",-25,0,0)
            .r("shards",0,spin(40),0);
        for(int i=0;i<8;i++)idle.r(tat[i],wave(6,90,i*45,0),0,wave(4,70,i*45,0));
        idle.r("shards2",0,spin(-25),0).r("eyes",0,spin(18),0).p("eyes",0,wave(1,60,0,0),0).r("trinkets",wave(8,120,0,0),0,wave(5,90,0,0));
        Anim walk=m.anim("walk",true,1);
        walk.r("shards2",0,spin(-80),0).r("eyes",0,spin(60),0).r("trinkets",wave(20,720,0,10),0,0);
        walk.p("root",0,wave(0.5,720,0,1),0).r("body",16,0,0).r("right_thigh",wave(25,360,0,0),0,0).r("left_thigh",wave(25,360,180,0),0,0)
            .r("right_shin",wave(15,360,90,15),0,0).r("left_shin",wave(15,360,270,15),0,0).r("right_arm",30,0,15).r("right_forearm",-20,0,0)
            .r("left_arm",wave(15,360,0,10),0,-12).r("cloak",30,0,0).r("cloak1",wave(10,720,0,15),0,0).r("cloak2",wave(14,720,60,15),0,0).r("shards",0,spin(120),0);
        for(int i=0;i<8;i++)walk.r(tat[i],wave(14,720,i*45,12),0,wave(6,720,i*45,0));
        m.anim("attack",false,0.4).rk("right_arm",0,-10,0,10).rk("right_arm",0.15,-100,0,-10).rk("right_arm",0.3,-60,0,0).rk("right_arm",0.4,-10,0,10)
            .rk("right_forearm",0,-35,0,0).rk("right_forearm",0.15,10,0,0).rk("right_forearm",0.4,-35,0,0).rk("body",0,8,0,0).rk("body",0.15,24,-15,0).rk("body",0.4,8,0,0);
        m.anim("vanish",true,1).p("root",0,-3,0).s("root",wave(0.08,720,0,0.92),wave(0.12,720,0,0.85),wave(0.08,720,0,0.92))
            .r("body",30,0,0).r("head",20,0,0).r("right_arm",-20,0,40).r("left_arm",-20,0,-40).r("shards",0,spin(720),0)
            .r("cloak",50,0,0).r("cloak1",30,0,0);
        m.anim("summon",true,1).r("right_arm",-150,0,30).r("left_arm",-150,0,-30).r("right_forearm",0,0,0).r("left_forearm",0,0,0)
            .r("head",-20,0,0).r("body",-6,0,0).r("shards",0,spin(360),0).s("shards",1.4,1.4,1.4);
        m.anim("usurp",true,1).r("left_arm",-85,-20,0).r("left_forearm",-10,0,0).r("head",-6,-15,0).r("body",10,15,0)
            .r("right_arm",-20,0,20).r("right_forearm",-40,0,0).r("shards",0,spin(240),0);
        m.anim("dormant",true,4).p("root",0,-5,0).r("right_thigh",-90,10,0).r("left_thigh",-90,-10,0).r("right_shin",100,0,0).r("left_shin",100,0,0)
            .r("body",35,0,0).r("head",25,0,0).r("right_arm",-40,30,0).r("left_arm",-40,-30,0).r("right_forearm",-60,0,0).r("left_forearm",-60,0,0)
            .r("cloak",40,0,0).r("cloak1",20,0,0).r("shards",0,spin(15),0).s("shards",0.6,0.6,0.6);
        return m;
    }
    /** Envy's dagger: a wavy kris of black glass, its edge burning green, an eye set in the guard. Drawn at the right hand. */
    static Model envyDagger(){
        Model m=new Model("envy_dagger",32);
        double x=-4.2;Bone b=m.bone("dagger",null,x,7,0).rot(-90,0,0);   // held along the forearm, so a thrust drives it forward
        b.c(x-0.5,6.5,-1.5,1,1,1,"crystal:3AFF8A");   // pommel
        b.c(x-0.5,6.5,-0.5,1,1,2,"leather");          // grip, held level, the blade forward
        b.c(x-1.5,6,-2,3,2,1,"obsidian");              // guard
        b.c(x-0.5,6.75,-2.5,1,1,1,"glow:6AFF9A");     // its eye
        for(int i=0;i<4;i++)b.c(x-0.5+(i%2==0?0:0.3),6.5,-3-i*1.6,1,1,2,"obsidian").turn(x,7,-3-i*1.6,0,i%2==0?8:-8,0);   // the wavy blade
        b.c(x-0.25,6.6,-9.6,1,1,1,"glow:6AFF9A");
        return m;
    }

    // ================================================================== 贪婪之主 · 黄金骑王 (the rider)
    /**
     * A crowned rider in heavy gold, sat high on its beast (a model of its own, at "mount"): a great crown, a coin-studded
     * cape, a fat sack of gold in its left hand, its lance (another model) couched in its right.
     */
    static Model greed(){
        Model m=new Model("greed_lord",128);
        String G="plate:D8A830",DG="plate:9A6A14",CLOTH="cloth:7A1A2A",RUBY="crystal:FF3348",TOPAZ="crystal:FFD86A";
        m.bone("root",null,0,0,0);
        m.bone("mount","root",0,0,0);
        Bone hips=m.bone("hips","root",0,24,2);
        hips.c(-4.5,23,-1,9,4,6,G);hips.c(-5,24,-1.5,10,1,7,"coins");
        for(int side=-1;side<=1;side+=2){   // the legs astride
            String n=side<0?"right":"left";double x=side*4;
            Bone thigh=m.bone(n+"_thigh","hips",x,24,1).rot(-70,0,side*-28);thigh.c(x-1.5,17,-0.5,3,7,3,G);thigh.c(x-2,20,-1,4,2,4,DG);
            Bone shin=m.bone(n+"_shin",n+"_thigh",x,17,1).rot(80,0,0);shin.c(x-1.5,10,-0.5,3,7,3,G);shin.c(x-1.5,10,-2.5,3,2,3,DG);
        }
        Bone body=m.bone("body","hips",0,27,2);
        body.c(-5,27,-1,10,10,6,G);
        Cube breast=body.c(-4,29,-1.6,8,7,1,G);body.c(-1,31,-2.1,2,2,1,RUBY);
        body.c(-5.5,27,-1.5,11,1,7,"coins");   // a girdle of coins
        body.c(-4,36,-1.5,8,2,7,DG);
        Bone cape=m.bone("cape","body",0,37,5);cape.c(-6,29,5,12,8,1,CLOTH);cape.c(-6.5,36,4.5,13,1,2,"coins");
        Bone cape1=m.bone("cape1","cape",0,29,5);cape1.c(-6.5,21,5,13,8,1,CLOTH);for(int i=0;i<4;i++)cape1.c(-5+i*3,22,5.4,1,1,1,"gold");
        Bone cape2=m.bone("cape2","cape1",0,21,5);cape2.c(-7,13,5,14,8,1,CLOTH);cape2.c(-7,13,5.3,14,1,1,"coins");
        Bone head=m.bone("head","body",0,37,1);
        head.c(-1.5,37,0,3,1,3,G);
        Cube face=head.c(-3,38,-1.5,6,6,6,"skin");
        head.c(-3.5,37.5,-2,7,2,7,"hair:C8A060");head.c(-3.5,39,4,7,5,1,"hair:C8A060");   // a beard and hair
        head.c(-3.6,38,-2.1,7,2,1,"hair:C8A060");
        // the great crown: a wide band, tall points, a gem in each, the biggest stone at the front
        Bone crown=m.bone("crown","head",0,44,1.5);
        crown.c(-4,43.5,-2.5,8,2,8,"gold");
        for(int i=0;i<8;i++){double a=Math.PI*2*i/8,x=Math.sin(a)*3.6,z=1.5-Math.cos(a)*3.6;int tall=i%2==0?5:3;
            crown.c(x-0.5,45.5,z-0.5,1,tall,1,"gold");crown.c(x-0.5,45.5+tall,z-0.5,1,1,1,i%2==0?RUBY:TOPAZ);}
        crown.c(-1,44,-2.9,2,2,1,RUBY);crown.c(-3,47,-1,6,1,5,"cloth:9A1A2A");
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*6.5;
            Bone pa=m.bone(n+"_pauldron","body",x,36,1);
            pa.c(x-2.5,34.5,-2,5,3,6,G).turn(x,36,1,0,0,side*18);pa.c(x-2,32.5,-1.8,4,2,5.5>0?5:5,DG).turn(x,33.5,1,0,0,side*24);
            pa.c(x-0.5,37,0.5,1,1,1,RUBY);
            Bone arm=m.bone(n+"_arm","body",x,35,1).rot(side<0?-50:-30,0,side*-10);arm.c(x-1.5,28,-0.5,3,7,3,G);
            Bone fore=m.bone(n+"_forearm",n+"_arm",x,28,1).rot(-40,0,0);fore.c(x-2,22,-1,4,6,4,G);fore.c(x-2.3,25,-1.3,4,1,4,"coins");fore.c(x-1.5,20,-0.5,3,2,3,DG);
            if(side<0)socket(m,n+"_forearm",x,20,1);
            else{   // a fat sack of gold, its neck tied, coins spilling
                Bone sack=m.bone("sack","left_forearm",x,20,1);
                sack.c(x-2.5,13,-2,5,6,5,"leather");sack.c(x-1.5,19,-1,3,1,3,"cloth:9A1A2A");sack.c(x-1,18.5,-0.5,2,1,2,"coins");
                sack.c(x-2,12.5,-2.2,1,1,1,"gold");sack.c(x+0.5,13,-2.4,1,1,1,"gold");
            }
        }
        // more gold: a gorget, faulds over its thighs, knee cops, gems on the greaves, chains of coins, rings, coins spilling from the sack
        body.c(-3.5,35,-2,7,2,5,DG);body.c(-3,35.5,-2.3,6,1,1,"coins");
        for(int i=0;i<3;i++)body.c(-4+i*0.5,28.5+i*2.4,-1.7,8-i,1,1,"gold");   // the breastplate's lames
        for(int i=0;i<5;i++)body.c(-4+i*2,33.5-Math.abs(i-2),-1.9,1,1,1,i==2?RUBY:"gold");   // a chain of coins across its chest
        for(int side=-1;side<=1;side+=2){String n=side<0?"right":"left";double x=side*4;
            Bone thigh=find(m,n+"_thigh");thigh.c(x-2.2,22,-1.2,4,2,4,G);thigh.c(x-0.5,22.5,-1.6,1,1,1,RUBY);
            Bone shin=find(m,n+"_shin");shin.c(x-2,15.5,-1,4,2,4,DG);shin.c(x-0.5,13,-0.9,1,1,1,TOPAZ);shin.c(x-1.5,10,1.5,3,1,2,"gold");
            double ax=side*6.5;Bone fore=find(m,n+"_forearm");
            for(int f=0;f<3;f++)fore.c(ax-1.5+f,19.5,-1.6,1,1,1,f==1?RUBY:"gold");   // a ring on each finger
            Bone pa=find(m,n+"_pauldron");
            for(int i=0;i<3;i++)pa.c(ax-0.5+side*(i-1)*1.5,37.2,-0.5+i,1,2,1,"gold");   // its crest
            pa.c(ax-2,34,-2.3,4,1,1,"coins");}
        Bone sack=find(m,"sack");
        for(int i=0;i<5;i++)sack.c(6.5-2+i,19+(i%2),-1.5+i%3,1,1,1,"gold");   // coins heaped at its mouth
        Bone spill=m.bone("spill","sack",6.5,13,1);for(int i=0;i<4;i++)spill.c(6.5-1+(i%2),11.5-i*1.6,0.5,1,1,1,"gold");   // coins falling from it
        crown.c(-0.5,47,-0.5,1,4,4,"gold");crown.c(-3.5,47,1,7,1,1,"gold");crown.c(-0.5,50.5,0.5,1,1,1,RUBY);crown.c(-0.5,51.5,0.5,1,2,1,"gold");   // its arches and cross
        for(int i=0;i<6;i++){(i<2?cape:i<4?cape1:cape2).c(-5+i*2,(i<2?33:i<4?25:17),5.5,1,1,1,i%2==0?"gold":RUBY);}
        m.deco(face,"north",1,2,1,1,0x6A3A12,false);m.deco(face,"north",4,2,1,1,0x6A3A12,false);
        m.deco(face,"north",1,1,1,1,0xFFC830,true);m.deco(face,"north",4,1,1,1,0xFFC830,true);m.deco(face,"north",2,4,2,1,0xB07A66,false);
        m.deco(breast,"north",3,1,2,5,0xFFF1B8,false);

        Anim idle=m.anim("idle",true,3);
        idle.p("hips",0,wave(0.4,120,0,0),0).r("head",wave(2,60,0,-3),wave(6,40,0,0),0).r("crown",0,0,wave(1.5,60,0,0))
            .r("cape",wave(3,120,0,5),0,0).r("cape1",wave(4,120,40,4),0,0).r("cape2",wave(5,120,80,4),0,0).r("sack",wave(6,120,0,0),0,wave(4,90,0,0));
        m.anim("walk",true,1).p("hips",0,wave(1,720,0,0),0).r("body",wave(3,720,0,4),0,0).r("head",wave(3,720,90,0),0,0)
            .r("cape",wave(6,720,0,16),0,0).r("cape1",wave(8,720,40,12),0,0).r("cape2",wave(10,720,80,10),0,0).r("sack",wave(15,720,0,0),0,0);
        m.anim("attack",false,0.5).rk("right_arm",0,-50,0,10).rk("right_arm",0.2,-80,0,0).rk("right_arm",0.35,-95,0,0).rk("right_arm",0.5,-50,0,10)
            .rk("right_forearm",0,-40,0,0).rk("right_forearm",0.2,-60,0,0).rk("right_forearm",0.35,0,0,0).rk("right_forearm",0.5,-40,0,0)
            .rk("body",0,0,0,0).rk("body",0.35,14,-10,0).rk("body",0.5,0,0,0);
        m.anim("charge",true,0.6).r("body",18,-8,0).r("head",-12,8,0).r("right_arm",-80,8,10).r("right_forearm",-5,0,0)
            .r("left_arm",-40,0,-20).r("cape",50,0,0).r("cape1",wave(10,1200,0,25),0,0).r("cape2",wave(14,1200,60,25),0,0).p("hips",0,wave(1.6,1200,0,0),0);
        m.anim("rain",true,1).r("left_arm",-170,0,-15).r("left_forearm",-5,0,0).r("sack",wave(25,720,0,180),0,wave(10,720,90,0)).p("spill",0,wave(3,720,0,-2),0)
            .r("right_arm",-120,0,25).r("right_forearm",-20,0,0).r("head",-25,0,0).r("body",-6,0,0);
        m.anim("dormant",true,4).p("hips",0,-1,0).r("body",28,0,0).r("head",30,0,0).r("right_arm",-10,0,15).r("left_arm",-10,0,-15)
            .r("right_forearm",-20,0,0).r("left_forearm",-20,0,0).r("cape",8,0,0).r("crown",wave(2,30,0,6),0,0);
        return m;
    }
    /** Greed's lance: a great golden lance, its vamplate a coin as wide as a shield, a ruby at its point. Couched in the right hand. */
    static Model greedLance(){
        Model m=new Model("greed_lance",64);
        double x=-6.5,y=21,z=1;Bone b=m.bone("lance",null,x,y,z).rot(-90,0,0);   // couched along the forearm
        b.c(x-1,y-1,z+3,2,2,4,"gold");                // the butt and grip, behind the hand
        b.c(x-1,y-1,z-1,2,2,4,"leather");
        Cube plate=b.c(x-3.5,y-3.5,z-2,7,7,1,"coins");  // the vamplate
        b.c(x-2.5,y-2.5,z-3,5,5,1,"gold");
        for(int i=0;i<5;i++){int w=i<2?2:1;b.c(x-w/2.0,y-w/2.0,z-6-i*4,w,w,4,i%2==0?"gold":"plate:D8A830");}   // the shaft, tapering
        b.c(x-0.5,y-0.5,z-25,1,1,2,"crystal:FF3348");
        b.c(x-1.5,y-0.5,z-14,3,1,1,"gold");b.c(x-0.5,y-1.5,z-14,1,3,1,"gold");
        m.deco(plate,"north",3,3,1,1,0xFF3348,true);
        return m;
    }
    /**
     * Greed's mount: a heavy golden beast, lion-maned and boar-tusked, its barding studded with coins and gems, a saddle
     * under its rider. Drawn at the rider's "mount" bone; its legs and head are moved by the game as it walks or charges.
     */
    static Model greedMount(){
        Model m=new Model("greed_mount",128);
        String HIDE="fur:B8862A",MANE="hair:F2C440",BARD="plate:D8A830",RUBY="crystal:FF3348";
        Bone body=m.bone("mount_body",null,0,18,2);
        body.c(-6,14,-8,12,10,20,HIDE);
        Cube bard=body.c(-6.5,15,-6,13,8,14,BARD);
        body.c(-6.7,14.5,-6.5,1,2,15,"coins");body.c(5.7,14.5,-6.5,1,2,15,"coins");
        for(int i=0;i<3;i++){body.c(-6.9,18,-3+i*4,1,2,2,RUBY);body.c(5.9,18,-3+i*4,1,2,2,RUBY);}
        body.c(-4.5,24,-3,9,2,8,"cloth:7A1A2A");body.c(-4,26,4,8,2,2,"leather");body.c(-4,26,-3,8,1,1,"leather");   // the saddle
        Bone neck=m.bone("mount_neck","mount_body",0,21,-8).rot(-20,0,0);
        neck.c(-3.5,19,-14,7,8,7,HIDE);
        for(int i=0;i<4;i++)neck.c(-4.5,20+i*2,-13+i*1.5,9,2,6,MANE);   // the mane
        Bone head=m.bone("mount_head","mount_neck",0,24,-14);
        Cube skull=head.c(-3.5,20,-21,7,7,7,HIDE);
        head.c(-2.5,20,-24,5,4,3,HIDE);head.c(-2,20.5,-24.6,4,2,1,"char");
        head.c(-4,26,-20,8,2,6,MANE);head.c(-4.5,21,-15,9,7,2,MANE);
        for(int side=-1;side<=1;side+=2){
            head.c(side*3-0.5,19,-24,1,3,1,"bone").turn(side*3,20,-23.5,-30,0,side*-20);   // tusks
            head.c(side*2.5-0.5,27,-17,1,4,1,"gold").turn(side*2.5,27,-16.5,-20,0,side*-25);   // little horns of gold
            head.c(side*2.5-0.5,31,-17,1,1,1,RUBY).turn(side*2.5,27,-16.5,-20,0,side*-25);
        }
        head.c(-3.6,24,-21.1,7,1,1,"gold");
        Bone jaw=m.bone("mount_jaw","mount_head",0,20,-17);jaw.c(-2.5,18,-23,5,2,5,HIDE);jaw.c(-2,19.5,-22.5,4,1,3,"flesh");
        String[][] legs={{"front_right","-4","-5"},{"front_left","4","-5"},{"back_right","-4","8"},{"back_left","4","8"}};
        for(String[] l:legs){double x=Double.parseDouble(l[1]),z=Double.parseDouble(l[2]);
            Bone up=m.bone("mount_"+l[0],"mount_body",x,15,z);up.c(x-2,8,z-2,4,8,4,HIDE);up.c(x-2.3,12,z-2.3,4,2,4,BARD);
            Bone low=m.bone("mount_"+l[0]+"_low","mount_"+l[0],x,8,z);low.c(x-1.5,2,z-1.5,3,6,3,HIDE);low.c(x-2,0,z-2.5,4,2,4,"gold");
        }
        Bone tail=m.bone("mount_tail","mount_body",0,21,12).rot(30,0,0);tail.c(-1,20,12,2,2,6,HIDE);tail.c(-1.5,19.5,17,3,3,3,MANE);
        // more of its splendour: coins hanging from its barding, a chain of gems over its chest, stirrups, a crest of gold down its neck
        for(int i=0;i<6;i++){body.c(-6.9,13,-5+i*3,1,2,1,"gold");body.c(5.9,13,-5+i*3,1,2,1,"gold");}
        Cube breastplate=body.c(-5,14,-9,10,7,1,BARD);body.c(-1,16,-9.4,2,2,1,RUBY);
        for(int i=0;i<5;i++)body.c(-4+i*2,13.5-Math.abs(i-2)*0.5,-9.3,1,1,1,i==2?RUBY:"gold");
        body.c(-6,21,-4,1,4,1,"leather");body.c(5,21,-4,1,4,1,"leather");body.c(-6.5,15,-4.5,2,1,2,"gold");body.c(4.5,15,-4.5,2,1,2,"gold");   // stirrups
        for(int i=0;i<4;i++)neck.c(-0.5,26+i*1.6,-14+i*1.8,1,2,1,"gold");   // a crest down its neck
        head.c(-3.6,22,-21.1,1,2,1,"gold");head.c(2.6,22,-21.1,1,2,1,"gold");head.c(-1,25,-21.3,2,1,1,RUBY);   // its chanfron
        tail.c(-1,17,19,2,3,2,"gold");
        m.deco(skull,"north",1,4,1,1,0xFFD040,true);m.deco(skull,"north",5,4,1,1,0xFFD040,true);
        m.deco(bard,"north",5,2,3,3,0xFF3348,true);
        return m;
    }

    // ================================================================== 色欲之主 · 魅影魔女
    /**
     * A floating enchantress on two pairs of butterfly wings: a gown of three falling tiers, long rose-gold hair, a heart
     * tiara, gloved arms; her rose sceptre is a model of its own.
     */
    static Model lust(){
        Model m=new Model("lust_lord",128);
        String GOWN="silk:B0307E",SILK="silk:F47AC0",HAIR="hair:F2A0B8",WING="wing:F66ABC",WING2="wing:C060E8",HEART="crystal:FF4A8A";
        m.bone("root",null,0,0,0);
        Bone hips=m.bone("hips","root",0,17,0);hips.c(-3,16,-2,6,3,4,GOWN);hips.c(-3.5,17,-2.3,7,1,5,"gold");
        Bone skirt=m.bone("skirt","hips",0,16,0);skirt.c(-4,11,-3,8,5,6,GOWN);skirt.c(-4.2,11,-3.2,8,1,6,SILK);
        Bone skirt1=m.bone("skirt1","skirt",0,11,0);skirt1.c(-5,6,-4,10,5,8,GOWN);skirt1.c(-5.2,6,-4.2,10,1,8,SILK);
        Bone skirt2=m.bone("skirt2","skirt1",0,6,0);skirt2.c(-6,1,-5,12,5,10,GOWN);skirt2.c(-6.2,1,-5.2,12,1,10,"gold");
        for(int i=0;i<6;i++){double a=Math.PI*2*i/6;skirt2.c(Math.sin(a)*5.6-0.5,0,-Math.cos(a)*4.6-0.5,1,1,1,HEART);}   // hearts at the hem
        Bone body=m.bone("body","hips",0,19,0);
        body.c(-2.5,19,-1.5,5,7,3,GOWN);
        Cube bodice=body.c(-2.7,19.5,-1.8,5,5,1,SILK);body.c(-0.5,22,-2.2,1,1,1,HEART);
        body.c(-3,25,-1.8,6,1,4,"gold");
        Bone head=m.bone("head","body",0,26,0);
        head.c(-1,26,-1,2,1,2,"skin");
        Cube face=head.c(-2.5,27,-2.5,5,5,5,"skin");
        head.c(-3,31,-3,6,2,6,HAIR);head.c(-3,27,1.5,6,5,2,HAIR);head.c(-3.1,28,-2.5,1,4,4,HAIR);head.c(2.1,28,-2.5,1,4,4,HAIR);
        head.c(-2.6,30.5,-2.8,2,1,1,HAIR);head.c(0.6,30.5,-2.8,2,1,1,HAIR);
        Bone tiara=m.bone("tiara","head",0,33,0);tiara.c(-2.5,32.5,-2.9,5,1,1,"gold");tiara.c(-0.5,33.5,-3,1,1,1,HEART);tiara.c(-1.5,33.5,-3,1,1,1,"gold");tiara.c(0.5,33.5,-3,1,1,1,"gold");
        // her hair falling long behind her, in three swaying lengths
        Bone hair=m.bone("hair","head",0,29,2.5);hair.c(-3,21,2.5,6,8,2,HAIR);
        Bone hair1=m.bone("hair1","hair",0,21,2.5);hair1.c(-2.5,14,2.5,5,7,2,HAIR);hair1.c(-0.5,16,3.6,1,1,1,HEART);
        Bone hair2=m.bone("hair2","hair1",0,14,2.5);hair2.c(-1.5,9,2.5,3,5,2,HAIR);
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*3.4;
            Bone arm=m.bone(n+"_arm","body",x,25,0).rot(0,0,side*-8);arm.c(x-0.9,19,-0.9,2,6,2,"skin");arm.c(x-1.2,23,-1.2,2,2,2,SILK);
            Bone fore=m.bone(n+"_forearm",n+"_arm",x,19,0).rot(-25,0,0);fore.c(x-0.9,13,-0.9,2,6,2,"silk:2A1020");fore.c(x-1.3,15,-1.3,2,1,2,"gold");
            if(side<0)socket(m,n+"_forearm",x,13,0);
        }
        // two pairs of wings, each its own bone so they beat
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";
            Bone up=m.bone(n+"_wing","body",side*1,24,1.5).rot(0,side*-25,0);
            for(int i=0;i<4;i++)up.c(side<0?-1-(i+1)*3.2:1+i*3.2,23+i*1.5,1.5,3,8-i,1,WING);
            up.c(side<0?-4:1,30,1.5,3,2,1,WING);up.c(side<0?-14:11,25,1.4,3,2,1,"glow:FFD0EC");
            Bone low=m.bone(n+"_wing_low","body",side*1,21,1.5).rot(0,side*-20,0);
            for(int i=0;i<3;i++)low.c(side<0?-1-(i+1)*2.6:1+i*2.6,15+i*0.8,1.5,3,6-i,1,WING2);
            low.c(side<0?-9:6,14,1.4,3,1,1,"glow:E8B0FF");
        }
        // roses in her hair, a veil, a necklace, ruffled hems, sleeves that fall open, eyes on her wings, petals and hearts about her
        for(int i=0;i<4;i++){double a=-0.8+i*0.55;head.c(Math.sin(a)*3-0.5,31.5+(i%2),Math.cos(a)*-0.5+1,1,1,1,"cloth:C81E4A");head.c(Math.sin(a)*3-0.5,31.5+(i%2),Math.cos(a)*-0.5+0,1,1,1,"cloth:E8406A");}
        Bone veil=m.bone("veil","head",0,32,-2.5).rot(10,0,0);veil.c(-3,26,-3.4,6,6,1,"silk:F8D0E4");
        body.c(-2,25.5,-2.1,4,1,1,"gold");body.c(-0.5,24.5,-2.3,1,1,1,HEART);   // a necklace with a heart on it
        for(int i=0;i<4;i++)body.c(-1.5+i,20+i%2,-2.1,1,1,1,"gold");   // the bodice's laces
        for(int i=0;i<8;i++){double a=Math.PI*2*i/8;   // ruffles at each tier's hem
            skirt.c(Math.sin(a)*4.3-0.5,10.5,-Math.cos(a)*3.3-0.5,1,1,1,SILK);skirt1.c(Math.sin(a)*5.3-0.5,5.5,-Math.cos(a)*4.3-0.5,1,1,1,SILK);skirt2.c(Math.sin(a)*6.3-1,0.5,-Math.cos(a)*5.3-0.5,2,1,1,SILK);}
        for(int side=-1;side<=1;side+=2){String n=side<0?"right":"left";double x=side*3.4;
            Bone sleeve=m.bone(n+"_sleeve",n+"_arm",x,19,0).rot(10,0,side*12);sleeve.c(x-1.5,13,-1.5,3,6,3,"silk:F8D0E4");   // the sleeve, falling open
            Bone fore=find(m,n+"_forearm");fore.c(x-0.5,12,-1.2,1,1,1,HEART);
            Bone up=find(m,n+"_wing");
            up.c(side<0?-8:5,26,1.3,3,3,1,"glow:FFFFFF");up.c(side<0?-7.5:5.5,26.5,1.2,2,2,1,"glow:F66ABC");   // an eye on each wing
            for(int i=0;i<4;i++)up.c(side<0?-1-(i+1)*3.2:1+i*3.2,22.5+i*1.5,1.6,3,1,1,"wing:A0307E");   // the dark hem of each wing
            Bone low=find(m,n+"_wing_low");
            low.c(side<0?-6:3,13,1.5,2,2,1,"wing:F66ABC");low.c(side<0?-11:8,13.5,1.5,2,1,1,"wing:C060E8");   // its tails
        }
        Bone petals=m.bone("petals","root",0,18,0);
        for(int i=0;i<12;i++){double a=Math.PI*2*i/12,r=8+(i%3)*1.5;petals.c(Math.cos(a)*r-0.5,10+(i*5)%20,Math.sin(a)*r-0.5,1,1,1,i%4==0?HEART:i%2==0?"cloth:FF9AC8":"cloth:E8406A");}
        m.deco(face,"north",1,2,1,1,0xFF3A8A,true);m.deco(face,"north",3,2,1,1,0xFF3A8A,true);
        m.deco(face,"north",2,4,1,1,0xD8406A,false);m.deco(bodice,"north",2,1,1,3,0xFFD0EC,false);

        Anim idle=m.anim("idle",true,3);
        idle.p("root",0,wave(1.2,120,0,3),0).r("right_wing",0,wave(18,240,0,-25),0).r("left_wing",0,wave(18,240,0,25),0)
            .r("right_wing_low",0,wave(14,240,30,-20),0).r("left_wing_low",0,wave(14,240,30,20),0)
            .r("skirt",wave(3,120,0,0),0,0).r("skirt1",wave(4,120,40,0),0,0).r("skirt2",wave(5,120,80,0),0,0)
            .r("hair",wave(4,120,0,6),0,0).r("hair1",wave(6,120,40,4),0,0).r("hair2",wave(8,120,80,4),0,0).r("head",wave(3,60,0,0),wave(8,40,0,0),wave(4,60,0,0))
            .r("right_arm",wave(4,120,0,-15),0,10).r("right_forearm",-40,0,0).r("left_arm",wave(4,120,90,-10),0,-14).r("left_forearm",-50,0,0)
            .r("petals",0,spin(30),0).p("petals",0,wave(1.5,60,0,0),0).r("veil",wave(4,120,0,10),0,0).r("right_sleeve",wave(6,120,0,10),0,0).r("left_sleeve",wave(6,120,90,10),0,0);
        m.anim("walk",true,1).r("petals",0,spin(90),0).r("veil",30,0,0).p("root",0,wave(0.8,720,0,3),0).r("body",12,0,0).r("right_wing",0,wave(30,720,0,-25),0).r("left_wing",0,wave(30,720,0,25),0)
            .r("right_wing_low",0,wave(24,720,30,-20),0).r("left_wing_low",0,wave(24,720,30,20),0).r("skirt",20,0,0).r("skirt1",wave(6,720,0,12),0,0).r("skirt2",wave(8,720,60,12),0,0)
            .r("hair",30,0,0).r("hair1",wave(8,720,0,18),0,0).r("hair2",wave(10,720,60,15),0,0).r("right_arm",-20,0,15).r("left_arm",20,0,-15);
        m.anim("attack",false,0.5).rk("right_arm",0,-15,0,10).rk("right_arm",0.2,-150,0,-20).rk("right_arm",0.4,-40,0,30).rk("right_arm",0.5,-15,0,10)
            .rk("body",0,0,0,0).rk("body",0.2,-8,-15,0).rk("body",0.4,10,20,0).rk("body",0.5,0,0,0);
        m.anim("charm",true,1).rk("left_arm",0,-100,30,0).rk("left_arm",0.4,-120,40,0).rk("left_arm",0.7,-80,-10,-20).rk("left_arm",1,-100,30,0)
            .r("left_forearm",-90,0,0).r("head",-10,-10,0).r("body",-6,0,0).r("right_wing",0,-55,0).r("left_wing",0,55,0);
        m.anim("cast",true,1).r("right_arm",-165,0,10).r("right_forearm",0,0,0).r("left_arm",-60,0,-50).r("left_forearm",-20,0,0)
            .r("head",-20,0,0).r("right_wing",0,wave(10,720,0,-60),0).r("left_wing",0,wave(10,720,0,60),0).r("right_wing_low",0,-50,0).r("left_wing_low",0,50,0)
            .p("root",0,wave(0.6,720,0,5),0);
        m.anim("mirage",true,1).r("root",0,spin(720),0).p("root",0,4,0).r("right_arm",-90,0,60).r("left_arm",-90,0,-60)
            .r("skirt1",-20,0,0).r("skirt2",-30,0,0).r("hair",50,0,0);
        m.anim("dormant",true,4).p("root",0,wave(0.5,60,0,0),0).r("right_wing",0,-75,0).r("left_wing",0,75,0).r("right_wing_low",0,-70,0).r("left_wing_low",0,70,0)
            .r("head",25,0,10).r("body",20,0,0).r("right_arm",-60,30,0).r("left_arm",-60,-30,0).r("right_forearm",-60,0,0).r("left_forearm",-60,0,0)
            .r("skirt",-10,0,0).r("hair",-10,0,0);
        return m;
    }
    /** Lust's sceptre: a slender rod of rose-gold wound with a ribbon, a heart of crystal crowned with a rose at its head. */
    static Model lustSceptre(){
        Model m=new Model("lust_sceptre",32);
        double x=-3.4,y=13,z=0;Bone b=m.bone("sceptre",null,x,y,z).rot(180,0,0);   // its head beyond the fist, so it rises as she raises her arm
        b.c(x-0.5,y-6,z-0.5,1,1,1,"crystal:FF4A8A");
        b.c(x-0.5,y-5,z-0.5,1,14,1,"gold");
        for(int i=0;i<4;i++)b.c(x-0.7,y-3+i*3,z-0.7,1,1,1,"cloth:E8406A");   // the ribbon wound round it
        b.c(x-1.5,y+9,z-1.5,3,1,3,"gold");
        b.c(x-1,y+10,z-1,2,3,2,"crystal:FF4A8A");   // the heart
        b.c(x-1.5,y+11,z-0.5,1,1,1,"crystal:FF4A8A");b.c(x+0.5,y+11,z-0.5,1,1,1,"crystal:FF4A8A");
        b.c(x-1,y+13,z-1,2,1,2,"cloth:C81E4A");b.c(x-0.5,y+14,z-0.5,1,1,1,"cloth:E8406A");   // the rose
        b.c(x-1.5,y+13,z-0.5,1,1,1,"cloth:3A7A3A");b.c(x+0.5,y+13,z-0.5,1,1,1,"cloth:3A7A3A");
        return m;
    }

    // ================================================================== 暴食之主 · 噬魂枪王
    /**
     * A bloated armoured lancer: a great round belly whose plates part on a maw of teeth and a lolling tongue, a small
     * helmed head with a burning slit, heavy pauldrons, stout legs. Its lance is a model of its own.
     */
    static Model gluttony(){
        Model m=new Model("gluttony_lord",128);
        String A="plate:8A6234",DA="plate:5A3E22",SOUL="glow:9AFF6A",FLESH="flesh",TOOTH="bone";
        m.bone("root",null,0,0,0);
        Bone hips=m.bone("hips","root",0,14,0);hips.c(-7,12,-5,14,4,10,DA);hips.c(-7.5,13,-5.5,15,1,11,"leather");
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*4.5;
            Bone thigh=m.bone(n+"_thigh","hips",x,13,0).rot(0,0,side*-4);thigh.c(x-2.5,7,-2.5,5,6,5,A);thigh.c(x-3,9,-3,6,2,6,DA);
            Bone shin=m.bone(n+"_shin",n+"_thigh",x,7,0);shin.c(x-2.5,1,-2.5,5,6,5,A);shin.c(x-3,0,-4,6,2,7,DA);shin.c(x-1,4,-3,2,2,1,"gold");
        }
        Bone belly=m.bone("belly","hips",0,16,0);
        belly.c(-9,15,-6,18,14,13,A);
        belly.c(-9.5,20,-6.5,19,1,14,"gold");belly.c(-9.5,26,-6.5,19,1,14,"gold");
        for(int i=0;i<4;i++)belly.c(-9.6,17+i*3,-2+i*0.5,1,1,1,"gold");
        // the maw: the belly's front plates are its jaws
        Bone upper=m.bone("maw_upper","belly",0,23,-6);
        upper.c(-7,23,-7,14,5,1,A);for(int i=0;i<7;i++)upper.c(-6+i*2,21.5,-6.8,1,2,1,TOOTH);
        upper.c(-7,22.5,-6.5,14,1,1,FLESH);
        Bone lower=m.bone("maw_lower","belly",0,22,-6);
        lower.c(-7,16,-7,14,6,1,A);for(int i=0;i<6;i++)lower.c(-5+i*2,22,-6.8,1,2,1,TOOTH);
        lower.c(-7,21.5,-6.5,14,1,1,FLESH);
        Bone inside=m.bone("maw_inside","belly",0,22,-5);inside.c(-6,18,-6,12,8,1,FLESH);inside.c(-1,19.5,-6.5,2,2,1,SOUL);
        Bone tongue=m.bone("tongue","maw_lower",0,20,-6);tongue.c(-2,19.5,-9,4,1,4,"tongue");tongue.c(-1.5,19,-11,3,1,2,"tongue");
        Bone chest=m.bone("chest","belly",0,29,0);
        chest.c(-8,29,-5,16,6,11,A);chest.c(-2,31,-5.6,4,3,1,"gold");chest.c(-1,31.5,-6,2,2,1,SOUL);
        Bone head=m.bone("head","chest",0,35,0);
        Cube helm=head.c(-3.5,35,-4,7,7,7,A);
        head.c(-4,38,-4.5,8,1,8,DA);head.c(-3.6,37.5,-4.4,7,1,1,SOUL);   // the slit, burning
        head.c(-1,42,-2,2,2,3,DA);
        Bone plume=m.bone("plume","head",0,43,0).rot(-20,0,0);plume.c(-0.5,43,-1,1,4,1,"cloth:5A8A2A");plume.c(-1,46,0,2,3,2,"cloth:5A8A2A");plume.c(-0.5,47,2,1,2,2,"cloth:3A6A1A");
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*10;
            Bone pa=m.bone(n+"_pauldron","chest",x,34,0);
            pa.c(x-3.5,32,-4.5,7,4,9,A).turn(x,34,0,0,0,side*15);pa.c(x-3,30,-4,6,2,8,DA).turn(x,31,0,0,0,side*22);
            for(int i=0;i<3;i++)pa.c(x-0.5+side*(i-1)*1.8,36,-0.5,1,2,1,TOOTH);   // teeth set along it, a trophy
            Bone arm=m.bone(n+"_arm","chest",x,32,0).rot(side<0?-35:-10,0,side*-12);arm.c(x-2.5,23,-2.5,5,9,5,A);
            Bone fore=m.bone(n+"_forearm",n+"_arm",x,23,0).rot(-35,0,0);fore.c(x-3,15,-3,6,8,6,A);fore.c(x-3.3,19,-3.3,6,1,6,"gold");fore.c(x-2.5,12,-2.5,5,3,5,DA);
            if(side<0)socket(m,n+"_forearm",x,13,0);
        }
        // rivets and bands on the belly, a second row of teeth, drool burning green, skulls of what it has eaten hung at its belt,
        // horns on its helm, spikes on its gauntlets and knees, souls circling it
        for(int i=0;i<6;i++){belly.c(-8.5+i*3.4,28,-6.6,1,1,1,"gold");belly.c(-8.5+i*3.4,15.5,-6.6,1,1,1,"gold");}
        belly.c(-9.5,17,-1,1,10,1,"gold");belly.c(8.5,17,-1,1,10,1,"gold");
        for(int i=0;i<6;i++)upper.c(-5+i*2,22,-6.2,1,1,1,TOOTH);
        for(int i=0;i<5;i++)lower.c(-4+i*2,21.5,-6.2,1,1,1,TOOTH);
        lower.c(-3,20.5,-7.2,1,2,1,SOUL);lower.c(2,20,-7.2,1,3,1,SOUL);   // drool, glowing
        Bone skulls=m.bone("skulls","hips",0,13,0);
        for(int i=0;i<3;i++){double x=-6+i*6;skulls.c(x-1,10,-6,2,2,2,TOOTH);skulls.c(x-0.5,10.5,-6.2,1,1,1,"void");skulls.c(x-0.5,12,-5.5,1,1,1,"chain");}
        for(int side=-1;side<=1;side+=2){
            head.c(side*3.5-0.5,40,-1,1,2,1,TOOTH).turn(side*3.5,40,-0.5,0,0,side*-35);head.c(side*4.5-0.5,41.5,-1,1,2,1,TOOTH).turn(side*4.5,41.5,-0.5,0,0,side*-55);   // horns
            String n=side<0?"right":"left";double x=side*10;
            Bone fore=find(m,n+"_forearm");
            for(int i=0;i<3;i++)fore.c(x+side*3-0.5,17+i*2,-0.5,1,1,1,TOOTH);   // spikes along its gauntlets
            double lx=side*4.5;Bone thigh=find(m,n+"_thigh");thigh.c(lx-1,7.5,-3.4,2,2,1,TOOTH);
            Bone pa=find(m,n+"_pauldron");pa.c(x-3.5,31,-4.8,7,1,1,"gold");
        }
        Bone souls=m.bone("souls","root",0,26,0);
        for(int i=0;i<8;i++){double a=Math.PI*2*i/8,r=13+(i%2)*2;souls.c(Math.cos(a)*r-0.5,20+(i*5)%14,Math.sin(a)*r-0.5,1,2,1,"glow:9AFF6A");souls.c(Math.cos(a)*r-0.5,22+(i*5)%14,Math.sin(a)*r-0.5,1,1,1,"glow:D8FFB0");}
        m.deco(helm,"north",1,5,5,1,0x2A1A0E,false);

        Anim idle=m.anim("idle",true,3);
        idle.r("maw_upper",wave(6,120,0,-6),0,0).r("maw_lower",wave(8,120,0,10),0,0).r("tongue",wave(10,180,0,10),wave(8,120,0,0),0)
            .s("belly",wave(0.025,120,0,1),wave(0.02,120,0,1),wave(0.025,120,0,1)).r("head",wave(3,60,0,0),wave(6,40,0,0),0)
            .r("right_arm",wave(3,120,0,-35),0,12).r("right_forearm",-35,0,0).r("left_arm",wave(3,120,90,-10),0,-12).r("left_forearm",-35,0,0).r("plume",wave(6,120,0,-20),0,0)
            .r("souls",0,spin(35),0).p("souls",0,wave(1.5,60,0,0),0).r("skulls",wave(5,120,0,0),0,0);
        m.anim("walk",true,1.2).r("root",0,0,wave(5,300,0,0)).p("root",0,wave(0.8,600,0,0),0)
            .r("right_thigh",wave(25,300,0,0),0,4).r("left_thigh",wave(25,300,180,0),0,-4).r("right_shin",wave(15,300,90,10),0,0).r("left_shin",wave(15,300,270,10),0,0)
            .r("left_arm",wave(15,300,0,-10),0,-12).r("maw_lower",wave(6,600,0,8),0,0).r("plume",wave(10,600,0,-20),0,0);
        m.anim("attack",false,0.6).rk("right_arm",0,-35,0,12).rk("right_arm",0.25,-60,0,5).rk("right_arm",0.4,-90,0,0).rk("right_arm",0.6,-35,0,12)
            .rk("right_forearm",0,-35,0,0).rk("right_forearm",0.25,-70,0,0).rk("right_forearm",0.4,0,0,0).rk("right_forearm",0.6,-35,0,0)
            .rk("body",0,0,0,0).rk("chest",0.4,10,-12,0).rk("chest",0.6,0,0,0);
        m.anim("lunge",true,0.8).r("hips",20,0,0).r("chest",10,-10,0).r("head",-15,10,0).r("right_arm",-90,0,0).r("right_forearm",0,0,0)
            .r("left_arm",30,0,-30).r("right_thigh",-40,0,0).r("left_thigh",35,0,0).r("right_shin",30,0,0).r("maw_lower",25,0,0).r("maw_upper",-15,0,0);
        m.anim("gorge",true,1).r("maw_upper",wave(6,720,0,-40),0,0).r("maw_lower",wave(8,720,0,55),0,0).r("tongue",wave(20,720,0,30),wave(15,360,0,0),0)
            .r("right_arm",-60,0,60).r("left_arm",-60,0,-60).r("head",-25,0,0).s("belly",wave(0.05,720,0,1.08),wave(0.04,720,0,1.05),wave(0.05,720,0,1.08));
        m.anim("throw",true,1).rk("right_arm",0,-170,0,10).rk("right_arm",0.5,-175,0,15).rk("right_arm",0.7,-40,0,0).rk("right_arm",1,-170,0,10)
            .r("right_forearm",-10,0,0).rk("chest",0,-12,20,0).rk("chest",0.7,10,-20,0).rk("chest",1,-12,20,0).r("left_arm",-60,0,-30);
        m.anim("dormant",true,4).p("root",0,-6,0).r("right_thigh",-85,0,15).r("left_thigh",-85,0,-15).r("right_shin",85,0,0).r("left_shin",85,0,0)
            .r("head",25,0,0).r("chest",10,0,0).r("right_arm",-50,0,20).r("left_arm",-20,0,-20).r("maw_upper",0,0,0).r("maw_lower",2,0,0)
            .s("belly",wave(0.02,45,0,1),wave(0.015,45,0,1),wave(0.02,45,0,1));
        return m;
    }
    /** Gluttony's lance: long and heavy, a jagged head of soul-iron barbed with bone, a green rune burning down its channel. */
    static Model gluttonyLance(){
        Model m=new Model("gluttony_lance",64);
        double x=-10,y=14,z=0;Bone b=m.bone("lance",null,x,y,z).rot(-90,0,0);   // couched along the forearm
        b.c(x-1,y-1,z+6,2,2,4,"plate:5A3E22");b.c(x-1,y-1,z+2,2,2,4,"leather");b.c(x-1,y-1,z-2,2,2,4,"plate:5A3E22");
        Cube cone=b.c(x-2.5,y-2.5,z-4,5,5,2,"plate:8A6234");b.c(x-2,y-2,z-6,4,4,2,"plate:8A6234");b.c(x-1.5,y-1.5,z-8,3,3,2,"plate:8A6234");
        for(int i=0;i<4;i++)b.c(x-1,y-1,z-12-i*4,2,2,4,"iron");
        b.c(x-0.5,y-0.5,z-27,1,1,11,"glow:9AFF6A");   // the rune channel
        Cube head=b.c(x-1.5,y-1.5,z-32,3,3,4,"obsidian");b.c(x-1,y-1,z-35,2,2,3,"obsidian");b.c(x-0.5,y-0.5,z-37,1,1,2,"iron");
        for(int side=-1;side<=1;side+=2){b.c(x+side*2-0.5,y-0.5,z-31,1,1,3,"bone").turn(x+side*2,y,z-29,0,side*25,0);b.c(x-0.5,y+side*2-0.5,z-30,1,1,3,"bone").turn(x,y+side*2,z-28,side*-25,0,0);}
        for(int i=0;i<4;i++)b.c(x-1.2,y-1.2,z-13-i*4,2,1,1,"chain");   // chains bound round the shaft
        for(int i=0;i<3;i++)b.c(x+(i-1)*1.8-0.5,y+2,z-5,1,2,1,"bone");   // teeth set round the vamplate
        b.c(x-0.5,y-3,z-5,1,2,1,"bone");
        m.deco(cone,"north",2,2,1,1,0x9AFF6A,true);m.deco(head,"north",1,1,1,1,0x9AFF6A,true);
        return m;
    }

    // ================================================================== 懒惰之主 · 沉眠剑王
    /**
     * A sleep-walking knight, slumped: dull blue-grey plate, a helm whose visor droops half shut on two pale slits, a
     * drooping plume like a nightcap's tassel, a blanket of a cloak spotted with stars. Its vast sword is a model of its own.
     */
    static Model sloth(){
        Model m=new Model("sloth_lord",128);
        String A="plate:8A8EB8",DA="plate:4A4E7A",BLANKET="cloth:3A3E6A",EYE="glow:CFE6FF",MOON="crystal:E8ECFF";
        m.bone("root",null,0,0,0);
        Bone hips=m.bone("hips","root",0,20,0);hips.c(-4.5,18,-2.5,9,4,5,DA);hips.c(-5,19,-3,10,1,6,"leather");
        Bone faulds=m.bone("faulds","hips",0,19,0);faulds.c(-4.5,14,-3,9,5,1,A);faulds.c(-4.5,14,2,9,5,1,A);faulds.c(-5,14,-2.5,1,5,5,A);faulds.c(4,14,-2.5,1,5,5,A);
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*2.5;
            Bone thigh=m.bone(n+"_thigh","hips",x,19,0).rot(6,0,side*-3);thigh.c(x-1.5,11,-1.5,3,8,3,DA);thigh.c(x-2,13,-2.2,4,5,1,A);
            Bone shin=m.bone(n+"_shin",n+"_thigh",x,11,0).rot(8,0,0);Cube greave=shin.c(x-2,2,-2,4,9,4,A);shin.c(x-2.5,10,-2.5,5,2,5,A);shin.c(x-2,0,-4,4,2,6,DA);
            m.deco(greave,"north",1,1,2,6,0xC8CCEC,false);
        }
        Bone body=m.bone("body","hips",0,22,0).rot(14,0,0);
        body.c(-4.5,22,-2.5,9,11,5,DA);
        Cube breast=body.c(-5,24,-3.2,10,8,1,A);body.c(-4,32,-3,8,2,6,A);
        body.c(-1,27,-3.7,2,2,1,MOON);
        // the blanket worn as a cloak, three folds, stars on it
        Bone cloak=m.bone("cloak","body",0,33,3);cloak.c(-6,24,3,12,9,1,BLANKET);cloak.c(-6.5,32,2.5,13,2,2,BLANKET);
        Bone cloak1=m.bone("cloak1","cloak",0,24,3);cloak1.c(-6.5,15,3,13,9,1,BLANKET);
        Bone cloak2=m.bone("cloak2","cloak1",0,15,3);cloak2.c(-7,4,3,14,11,1,BLANKET);cloak2.c(-7,4,3.2,14,1,1,"cloth:9294BA");
        for(int i=0;i<5;i++){(i<2?cloak:i<4?cloak1:cloak2).c(-4.5+i*2.2,(i<2?27:i<4?18:8)+(i%2)*2,3.6,1,1,1,"glow:E8ECFF");}
        Bone head=m.bone("head","body",0,34,-0.5).rot(18,0,0);
        head.c(-1.5,34,-1.5,3,1,3,DA);
        Cube helm=head.c(-3.5,35,-3.5,7,7,7,A);
        head.c(-3.8,39,-4,7,1,1,DA);head.c(-3.6,37.5,-3.9,7,2,1,DA);   // the drooping visor
        Cube eyeL=head.c(1,38.3,-4.2,2,1,1,EYE),eyeR=head.c(-3,38.3,-4.2,2,1,1,EYE);
        head.c(-4,41,-4,8,2,8,DA);head.c(-0.5,43,-0.5,1,1,1,MOON);
        Bone plume=m.bone("plume","head",0,43,0).rot(30,0,0);plume.c(-1,43,-1,2,3,2,"cloth:9294BA");
        Bone plume1=m.bone("plume1","plume",0,46,0).rot(70,0,0);plume1.c(-1,46,-1,2,4,2,"cloth:9294BA");
        Bone plume2=m.bone("plume2","plume1",0,50,0).rot(40,0,0);plume2.c(-0.5,50,-0.5,1,3,1,"cloth:B8BCE0");plume2.c(-1,53,-1,2,2,2,"glow:E8ECFF");   // a tassel, like a nightcap's
        for(int side=-1;side<=1;side+=2){
            String n=side<0?"right":"left";double x=side*6;
            Bone pa=m.bone(n+"_pauldron","body",x,32,0);pa.c(x-2.5,30.5,-3,5,3,6,A).turn(x,32,0,0,0,side*25);pa.c(x-2,29,-2.7,4,2,5,DA).turn(x,30,0,0,0,side*32);
            Bone arm=m.bone(n+"_arm","body",x,31,0).rot(side<0?-15:-5,0,side*-4);arm.c(x-1.5,23,-1.5,3,8,3,DA);
            Bone fore=m.bone(n+"_forearm",n+"_arm",x,23,0).rot(-10,0,0);fore.c(x-2,16,-2,4,7,4,A);fore.c(x-2.3,21,-2.3,4,2,4,DA);fore.c(x-1.5,13,-1.5,3,3,3,DA);
            if(side<0)socket(m,n+"_forearm",x,14,0);
        }
        // pauldrons in three tiers, tassets, a crest on its helm, gauntlet fingers, a pillow strapped to its back, a little lantern
        // gone dim at its belt, patches and a fringe on its blanket, stars and moons drifting about it as it dozes
        for(int side=-1;side<=1;side+=2){String n=side<0?"right":"left";double x=side*6;
            Bone pa=find(m,n+"_pauldron");
            pa.c(x-1.5,27.5,-2.4,3,2,5,A).turn(x,28.5,0,0,0,side*38);pa.c(x-0.5,33.5,-0.5,1,1,1,MOON);pa.c(x-3,31.5,-3.4,6,1,1,"plate:C8CCEC").turn(x,32,0,0,0,side*25);
            Bone fore=find(m,n+"_forearm");
            for(int f=0;f<3;f++)fore.c(x-1.5+f,12,-1.8,1,2,1,DA);
            Bone tas=m.bone(n+"_tasset","hips",side*4.5,19,0);tas.c(side*4.5-1.5,13,-2.5,3,6,5,A);tas.c(side*4.5-1.8,13,-2.8,3,1,5,"plate:C8CCEC");
        }
        head.c(-0.5,42,-4,1,3,8,DA);head.c(-1,41.5,-4.4,2,1,1,"plate:C8CCEC");   // the crest
        body.c(-3.5,30,-3.4,7,1,1,"plate:C8CCEC");body.c(-3,25,-3.5,6,1,1,"plate:C8CCEC");
        Bone pillow=m.bone("pillow","body",0,29,4);pillow.c(-4,25,4,8,6,3,"cloth:E8ECFF");pillow.c(-4.5,27,5,9,1,2,"leather");pillow.c(-1,25.5,6.6,2,1,1,"cloth:9294BA");
        Bone lantern=m.bone("lantern","hips",3.5,18,-2);lantern.c(3,13,-3,2,3,2,"iron");lantern.c(3.25,13.5,-3.2,1,2,1,"glow:FFE8A0");lantern.c(3.5,16,-2.5,1,2,1,"chain");
        cloak2.c(-6,8,3.2,3,3,1,"cloth:5A5E8A");cloak1.c(2,18,3.2,3,2,1,"cloth:6A4A6A");   // patches
        for(int i=0;i<7;i++)cloak2.c(-7+i*2,3,3,1,1,1,"cloth:9294BA");   // its fringe
        Bone dreams=m.bone("dreams","head",0,46,0);
        for(int i=0;i<6;i++){double a=Math.PI*2*i/6;dreams.c(Math.cos(a)*6-0.5,45+(i%3)*2,Math.sin(a)*6-0.5,1,1,1,i%2==0?"glow:E8ECFF":"glow:9EB8FF");}
        dreams.c(4,49,-0.5,2,2,1,MOON);dreams.c(5,50,-0.5,1,1,1,"void");   // a little crescent
        m.deco(breast,"north",4,1,2,6,0xC8CCEC,false);m.deco(helm,"north",1,6,5,1,0x2A2C50,false);

        Anim idle=m.anim("idle",true,5);
        idle.r("head",wave(8,72,0,18),wave(6,36,0,0),wave(5,72,90,0)).r("body",wave(3,72,0,14),0,wave(3,36,0,0)).p("root",0,wave(0.4,72,0,0),0)
            .r("right_arm",wave(3,72,0,-15),0,4).r("right_forearm",-10,0,0).r("left_arm",wave(5,72,60,-5),0,-4).r("left_forearm",-5,0,0)
            .r("plume",wave(5,72,0,30),0,0).r("plume1",wave(8,72,40,70),0,0).r("plume2",wave(10,72,80,40),0,0)
            .r("cloak",wave(2,72,0,6),0,0).r("cloak1",wave(3,72,40,4),0,0).r("cloak2",wave(4,72,80,4),0,0)
            .r("dreams",0,spin(20),0).p("dreams",0,wave(1,45,0,0),0).r("lantern",wave(8,72,0,0),0,wave(5,72,90,0));
        m.anim("walk",true,2).r("dreams",0,spin(40),0).r("lantern",wave(18,360,0,0),0,0).p("root",0,wave(0.6,360,0,0),0).r("body",18,wave(4,180,0,0),0)
            .r("right_thigh",wave(16,180,0,0),0,0).r("left_thigh",wave(16,180,180,0),0,0).r("right_shin",wave(10,180,90,12),0,0).r("left_shin",wave(10,180,270,12),0,0)
            .r("right_arm",35,0,5).r("right_forearm",10,0,0).r("left_arm",wave(10,180,0,-5),0,-4).r("head",wave(6,360,0,22),0,0)
            .r("cloak",wave(4,360,0,10),0,0).r("cloak1",wave(6,360,40,8),0,0).r("plume1",wave(12,360,0,70),0,0);
        m.anim("attack",false,0.9).rk("right_arm",0,-15,0,4).rk("right_arm",0.4,-160,0,-20).rk("right_arm",0.65,-30,0,30).rk("right_arm",0.9,-15,0,4)
            .rk("right_forearm",0,-10,0,0).rk("right_forearm",0.4,-30,0,0).rk("right_forearm",0.65,0,0,0).rk("right_forearm",0.9,-10,0,0)
            .rk("body",0,14,0,0).rk("body",0.4,0,-15,0).rk("body",0.65,25,20,0).rk("body",0.9,14,0,0);
        m.anim("doze",true,3).r("head",wave(4,120,0,45),0,15).r("body",wave(3,120,0,25),0,0).r("right_arm",-40,0,10).r("right_forearm",-30,0,0)
            .r("left_arm",-35,0,-20).r("left_forearm",-50,0,0).p("root",0,-1,0).r("plume1",90,0,0).r("plume2",60,0,0);
        m.anim("oath",true,1).r("right_arm",-175,0,-5).r("right_forearm",0,0,0).r("left_arm",-170,0,5).r("left_forearm",0,0,0)
            .r("head",-15,0,0).r("body",-5,0,0).r("plume1",30,0,0).r("cloak",wave(6,720,0,20),0,0).r("cloak1",wave(8,720,40,15),0,0);
        m.anim("sweep",true,1).rk("body",0,10,60,0).rk("body",0.5,10,-70,0).rk("body",1,10,60,0)
            .r("right_arm",-90,0,40).r("right_forearm",0,0,0).r("left_arm",-80,0,-20).r("head",0,0,0).r("cloak",30,0,0);
        m.anim("dormant",true,5).p("root",0,-8,0).r("right_thigh",-90,15,0).r("left_thigh",-85,-20,0).r("right_shin",20,0,0).r("left_shin",15,0,0)
            .r("body",20,0,0).r("head",wave(3,45,0,40),0,20).r("right_arm",-70,0,0).r("right_forearm",-30,0,0).r("left_arm",-20,0,-15)
            .r("plume1",100,0,0).r("plume2",80,0,0);
        return m;
    }
    /** Sloth's vast sword: a blade near as tall as its bearer, pale runes asleep down its fuller, a moon for a pommel, a guard of closed eyes. */
    static Model slothSword(){
        Model m=new Model("sloth_greatsword",64);
        double x=-6,y=14,z=0;Bone b=m.bone("sword",null,x,y,z);
        b.c(x-1.5,y+1,z-1.5,3,3,3,"crystal:E8ECFF");   // the moon pommel
        b.c(x-1,y-5,z-1,2,6,2,"leather");                // the grip, held point down
        Cube guard=b.c(x-5,y-7,z-1.5,10,2,3,"plate:4A4E7A");b.c(x-6,y-8,z-1,2,2,2,"plate:8A8EB8");b.c(x+4,y-8,z-1,2,2,2,"plate:8A8EB8");
        b.c(x-1.5,y-8,z-2,3,2,1,"glow:CFE6FF");
        Cube blade=b.c(x-2,y-36,z-0.5,4,28,1,"blade");
        b.c(x-0.5,y-34,z-0.7,1,24,1,"glow:9EB8FF");   // the fuller's runes
        b.c(x-1.5,y-39,z-0.5,3,3,1,"blade");b.c(x-0.5,y-41,z-0.5,1,2,1,"blade");
        // the guard's sleeping eyes, ridges down the blade, a moon's horns on the pommel, a tassel from it
        b.c(x-4,y-6.5,z-1.8,2,1,1,"plate:C8CCEC");b.c(x+2,y-6.5,z-1.8,2,1,1,"plate:C8CCEC");
        for(int i=0;i<5;i++)b.c(x-2.3,y-33+i*5,z-0.6,1,2,1,"plate:C8CCEC");
        for(int i=0;i<5;i++)b.c(x+1.3,y-31+i*5,z-0.6,1,2,1,"plate:C8CCEC");
        b.c(x-2.5,y+3,z-0.5,1,2,1,"crystal:E8ECFF");b.c(x+1.5,y+3,z-0.5,1,2,1,"crystal:E8ECFF");
        b.c(x-0.5,y-2,z+1,1,4,1,"cloth:9294BA").turn(x,y,z+1,20,0,0);b.c(x-1,y-3,z+1.5,2,2,1,"glow:E8ECFF");
        m.deco(guard,"north",3,0,1,1,0x2A2C50,false);m.deco(guard,"north",6,0,1,1,0x2A2C50,false);
        return m;
    }

    // ------------------------------------------------------------------
    static void write(Path assets,Model m,boolean statue) throws Exception {
        pack(m);
        Files.writeString(assets.resolve("geo/entity/"+m.id+".geo.json"),geo(m));
        if(!m.anims.isEmpty())Files.writeString(assets.resolve("animations/entity/"+m.id+".animation.json"),anims(m));
        File tex=assets.resolve("textures/entity/"+m.id+".png").toFile(),glow=assets.resolve("textures/entity/"+m.id+"_glowmask.png").toFile();
        paint(m,tex,glow);
        if(statue)statue(tex,assets.resolve("textures/entity/"+m.id+"_statue.png").toFile(),glow,assets.resolve("textures/entity/"+m.id+"_statue_glowmask.png").toFile());
        System.out.println("wrote "+m.id+" ("+m.bones.size()+" bones, "+m.cubes()+" cubes, texture "+m.tw+"x"+m.th+(m.anims.isEmpty()?"":", "+m.anims.size()+" animations")+")");
    }
    public static void main(String[] args) throws Exception {
        Path assets=Path.of(args[0]);
        for(Model m:List.of(wrath(),envy(),greed(),lust(),gluttony(),sloth(),envyDagger(),greedLance(),greedMount(),lustSceptre(),gluttonyLance(),slothSword()))write(assets,m,true);
    }
}
