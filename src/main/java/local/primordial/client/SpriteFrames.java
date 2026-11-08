package local.primordial.client;

/**
 * Splits the supplied four-column, two-row sheet without cutting its uneven glow margins. Each frame is cropped to what
 * the item shader draws of it (alpha 0.1 and up): the rest it discards, so the cropped frame looks the same and takes less.
 */
public final class SpriteFrames {
    public static final int FRAME_COUNT=8;
    public static final long FRAME_NANOS=200_000_000L;
    private static final int COLUMNS=4;
    /** the lowest alpha the item shader draws (0.1) */
    private static final int VISIBLE=26;
    public record Frame(int width,int height,int[] pixels,int originX,int originY,
                        double centerX,double centerY,int visibleSize) {}
    private SpriteFrames() {}
    public static int frameIndex(long elapsedNanos) {
        return (int)((Math.max(0,elapsedNanos)/FRAME_NANOS)%FRAME_COUNT);
    }
    public static int displaySize(Frame[] frames) {
        int size=0;
        for(var frame:frames)size=Math.max(size,frame.visibleSize());
        return size+4;
    }
    public static Frame[] split(int width,int height,int[] pixels) {
        if(width<COLUMNS||height<2||(long)width*height!=pixels.length)
            throw new IllegalArgumentException("Expected a four-column, two-row sprite sheet");
        var frames=new Frame[FRAME_COUNT];
        for(int column=0;column<COLUMNS;column++) {
            int x0=(int)((long)column*width/COLUMNS),x1=(int)((long)(column+1)*width/COLUMNS);
            int split=middleGap(width,height,pixels,x0,x1);
            for(int row=0;row<2;row++) {
                int y0=row==0?0:split,y1=row==0?split:height,w=x1-x0,h=y1-y0;
                int[] frame=new int[w*h];
                int minX=w,minY=h,maxX=-1,maxY=-1;
                for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                    int pixel=pixels[(y+y0)*width+x+x0];frame[y*w+x]=pixel;
                    // Minecraft's translucent item shader discards alpha below 0.1
                    if((pixel>>>24)>=VISIBLE) {
                        minX=Math.min(minX,x);minY=Math.min(minY,y);
                        maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
                    }
                }
                if(maxX<0)throw new IllegalArgumentException("Empty sprite frame "+(row*COLUMNS+column));
                int cw=maxX-minX+1,ch=maxY-minY+1;int[] cropped=new int[cw*ch];
                for(int y=0;y<ch;y++)for(int x=0;x<cw;x++){int pixel=frame[(y+minY)*w+x+minX];cropped[y*cw+x]=(pixel>>>24)>=VISIBLE?pixel:0;}
                frames[row*COLUMNS+column]=new Frame(cw,ch,cropped,x0+minX,y0+minY,cw/2.0,ch/2.0,Math.max(cw,ch));
            }
        }
        return frames;
    }
    private static int middleGap(int width,int height,int[] pixels,int x0,int x1) {
        int lower=height/3,upper=height*2/3,start=-1,chosen=height/2;
        double distance=Double.MAX_VALUE;
        for(int y=lower;y<=upper;y++) {
            boolean clear=y<upper;
            if(clear)for(int x=x0;x<x1;x++)if((pixels[y*width+x]>>>24)>=VISIBLE){clear=false;break;}
            if(clear&&start<0)start=y;
            if(!clear&&start>=0) {
                int center=(start+y)/2;
                double candidate=Math.abs(center-height/2.0);
                if(candidate<distance){distance=candidate;chosen=center;}
                start=-1;
            }
        }
        return chosen;
    }
}
