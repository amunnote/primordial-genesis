import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * 寻灵罗盘's textures: 32 frames, drawn as the game's compass is (its case seen from above and in front), its needle a cyan
 * spirit-flame with a violet tail over a starry violet face in a case of gold; and the item models choosing a frame by the "angle" property (as the game's compass).
 * Frame 16 points away (straight ahead), frame 8 left, frame 0 toward you, frame 24 right.
 * Usage, from the repository: java tools/art/SpiritCompass.java src/main/resources/assets/primordial
 */
public class SpiritCompass {
    static final int FRAMES=32;
    static final double CX=7.5,CY=8.5;
    static int mix(int a,int b,double t){t=Math.max(0,Math.min(1,t));
        return (int)Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t)<<16|(int)Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t)<<8|(int)Math.round((a&255)*(1-t)+(b&255)*t);}
    static void px(BufferedImage img,int x,int y,int c){if(x>=0&&y>=0&&x<16&&y<16)img.setRGB(x,y,0xFF000000|c);}

    // the case seen from above and in front, as the game's compass: its face an ellipse, its gold side showing below
    static final double FX=7.5,FY=6.5,RX=7.0,RY=4.5,DEPTH=3.0;
    static boolean inFace(double x,double y,double shrinkX,double shrinkY){double dx=(x-FX)/(RX-shrinkX),dy=(y-FY)/(RY-shrinkY);return dx*dx+dy*dy<=1;}
    static boolean inCase(double x,double y){for(double d=0;d<=DEPTH;d+=0.25)if(inFace(x,y-d,0,0))return true;return false;}

    static BufferedImage frame(int i){
        var img=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int x=0;x<16;x++)for(int y=0;y<16;y++){
            double cx=x+0.5,cy=y+0.5;
            if(!inCase(cx,cy))continue;
            if(inFace(cx,cy,0,0)){
                // the face: a rim of gold lit from the left, the glass within deep violet
                if(!inFace(cx,cy,1.4,1.1))px(img,x,y,cx<FX-2?0xFFE9A8:cx<FX+3?0xD8B04A:0xA8781E);
                else if(!inFace(cx,cy,2.0,1.6))px(img,x,y,0x1A1030);
                else px(img,x,y,mix(0x15102E,0x3E3282,(cy-FY+RY)/(2*RY)));
            }else{
                // the side of the case, light on the left and dark on the right, its foot darker
                double t=(cx-(FX-RX))/(2*RX);int c=t<0.3?0xFFF1C0:t<0.55?0xE8C25A:t<0.8?0xB8862A:0x7A5410;
                if(!inCase(cx,cy+1))c=mix(c,0x2A1A06,0.45);
                px(img,x,y,c);
            }
        }
        // a dark edge round the whole case
        var copy=new boolean[16][16];for(int x=0;x<16;x++)for(int y=0;y<16;y++)copy[x][y]=(img.getRGB(x,y)>>>24)!=0;
        for(int x=0;x<16;x++)for(int y=0;y<16;y++){if(!copy[x][y])continue;
            boolean edge=x==0||y==0||x==15||y==15||!copy[x-1][y]||!copy[x+1][y]||!copy[x][y-1]||!copy[x][y+1];if(edge)px(img,x,y,0x2A1A06);}
        // stars in the glass
        px(img,4,6,0x8A80C8);px(img,10,5,0x8A80C8);px(img,11,7,0x6A60A8);
        // the needle, foreshortened like the face: a head of cyan spirit-flame white at the tip, a violet tail.
        // As the game's: frame 16 points away (up), frame 8 to the left, frame 0 toward you (down), frame 24 to the right.
        double turn=Math.toRadians((i-16)*360.0/FRAMES);double ux=Math.sin(turn)*0.95,uy=-Math.cos(turn)*0.6;
        for(double t=-2.6;t<=5.2;t+=0.1){
            int x=(int)Math.floor(FX+ux*t),y=(int)Math.floor(FY+uy*t);
            int c=t>4.0?0xFFFFFF:t>1.6?0x7AF0FF:t>0.4?0x2AA8D0:t>-0.5?0xFFFFFF:t>-1.8?0xB07AF0:0x7A4AC8;
            px(img,x,y,c);
        }
        return img;
    }

    public static void main(String[] args) throws Exception {
        Path assets=Path.of(args[0]);
        for(int i=0;i<FRAMES;i++){
            String id=String.format(Locale.ROOT,"spirit_compass_%02d",i);
            ImageIO.write(frame(i),"png",assets.resolve("textures/item/"+id+".png").toFile());
            Files.writeString(assets.resolve("models/item/"+id+".json"),"{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"primordial:item/"+id+"\"}}\n");
        }
        // the item: frame 16 (straight ahead) in hand without a target and in menus; each frame for its arc of angles
        var s=new StringBuilder("{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\"layer0\": \"primordial:item/spirit_compass_16\"},\n  \"overrides\": [\n");
        for(int i=0;i<FRAMES;i++){
            double from=Math.max(0,(2*i-1)/64.0);
            s.append(String.format(Locale.ROOT,"    {\"predicate\": {\"angle\": %s}, \"model\": \"primordial:item/spirit_compass_%02d\"},\n",from==0?"0":String.valueOf(from),i));
        }
        s.append("    {\"predicate\": {\"angle\": 0.984375}, \"model\": \"primordial:item/spirit_compass_00\"}\n  ]\n}\n");
        Files.writeString(assets.resolve("models/item/spirit_compass.json"),s.toString());
        System.out.println("wrote spirit_compass: "+FRAMES+" frames and their models");
    }
}
