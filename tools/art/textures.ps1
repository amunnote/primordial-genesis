param([string]$OutDir,[string]$Sheet)
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
public static class Tex {
    static double H(int x,int y,int s){unchecked{uint h=(uint)(x*374761393+y*668265263+s*1442695041);h=(h^(h>>13))*1274126177u;h^=h>>16;return (h&0xFFFF)/65535.0;}}
    static int M(int a,int p){return ((a%p)+p)%p;}
    // tileable value noise: cells of size c on a 16-pixel tile
    static double VN(double x,double y,int c,int s){
        int p=16/c;double fx=x/c,fy=y/c;int x0=(int)Math.Floor(fx),y0=(int)Math.Floor(fy);double tx=fx-x0,ty=fy-y0;
        tx=tx*tx*(3-2*tx);ty=ty*ty*(3-2*ty);
        double a=H(M(x0,p),M(y0,p),s),b=H(M(x0+1,p),M(y0,p),s),cc=H(M(x0,p),M(y0+1,p),s),d=H(M(x0+1,p),M(y0+1,p),s);
        return a+(b-a)*tx+(cc-a)*ty+(a-b-cc+d)*tx*ty;
    }
    static double Fbm(int x,int y,int s){return 0.5*VN(x,y,8,s)+0.3*VN(x,y,4,s+7)+0.2*H(x,y,s+13);}
    static Color Ramp(string[] r,double v){v=Math.Max(0,Math.Min(0.999,v));return ColorTranslator.FromHtml(r[(int)(v*r.Length)]);}
    static Bitmap New(){return new Bitmap(16,16,System.Drawing.Imaging.PixelFormat.Format32bppArgb);}

    public static Bitmap Cloudrock(){
        var b=New();string[] stone={"#8a8f92","#9da2a4","#b0b4b5","#c3c6c6","#d5d7d6","#e4e6e4"};
        string[] vein={"#245a50","#2f7666","#3d9a83","#5cc4a6","#8ee6c9"};
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            double v=Fbm(x,y,3)*0.9+0.08+(H(x,y,5)<0.08?-0.12:0);
            Color c=Ramp(stone,v);
            // a ridged vein network
            double n=VN(x,y,8,11)*0.7+VN(x,y,4,17)*0.3,ridge=Math.Abs(n-0.5);
            if(ridge<0.028){double t=1-ridge/0.028;c=Ramp(vein,0.35+t*0.55+(H(x,y,23)-0.5)*0.2);
                if(H(x,y,29)>0.86)c=ColorTranslator.FromHtml(H(x,y,31)>0.5?"#c46a3c":"#e08b52");}
            else if(ridge<0.045)c=Ramp(stone,v-0.12);   // the vein's shadowed rim
            b.SetPixel(x,y,c);
        }
        return b;
    }
    public static Bitmap Bladestone(){
        var b=New();string[] stone={"#8f949a","#a3a8ad","#b7bbbf","#cbced1","#dde0e2","#eceef0"};
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            // layered: bands running along the diagonal
            double band=Math.Sin((x*0.55+y*1.0)*0.9+VN(x,y,8,41)*3)*0.12;
            Color c=Ramp(stone,Fbm(x,y,43)*0.75+0.2+band);
            // blade slashes: thin dark cuts with a bright lip
            int d=M(x*2-y*3,16);
            if((d==0||d==1)&&VN(x,y,8,47)>0.3)c=ColorTranslator.FromHtml(d==0?"#3e434a":"#5c626a");
            else if(d==2&&VN(x,y,8,47)>0.3)c=ColorTranslator.FromHtml("#f6f8f9");
            // iron flecks
            if(H(x,y,53)>0.93)c=ColorTranslator.FromHtml(H(x,y,59)>0.5?"#6b7078":"#868b92");
            b.SetPixel(x,y,c);
        }
        return b;
    }
    public static Bitmap Leaves(){
        // the way vanilla leaves are drawn: a mat of small leaves, each lit from the upper left, darker where they
        // overlap, with a few gaps to see through
        var b=New();string[] leaf={"#4a140d","#6e1f11","#962c14","#bd4119","#dc5f21","#ef8230","#f9aa45"};
        int n=24;var px=new double[n];var py=new double[n];
        for(int i=0;i<n;i++){px[i]=H(i,0,131)*16;py[i]=H(i,1,131)*16;}
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            double d1=1e9,d2=1e9,ox=0,oy=0;
            for(int i=0;i<n;i++)for(int sx=-16;sx<=16;sx+=16)for(int sy=-16;sy<=16;sy+=16){
                double dx=x+0.5-(px[i]+sx),dy=y+0.5-(py[i]+sy),d=Math.Sqrt(dx*dx+dy*dy);
                if(d<d1){d2=d1;d1=d;ox=dx;oy=dy;}else if(d<d2)d2=d;}
            if(d1>2.3&&H(x,y,137)>0.35||H(x,y,139)>0.93){b.SetPixel(x,y,Color.FromArgb(0,0,0,0));continue;}
            double lit=0.62-(ox+oy)*0.16-(d2-d1<0.7?0.28:0)+(H(x,y,149)-0.5)*0.18;
            b.SetPixel(x,y,Ramp(leaf,lit));
        }
        return b;
    }    public static Bitmap Log(){
        var b=New();string[] bark={"#3a120c","#521c11","#6b2817","#85361d","#9e4625","#b5582e"};
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            double ridge=VN(x*2.0,y*0.5,4,71)*0.6+H(x,y,73)*0.25+0.1;
            double furrow=VN(x*2.0,y*0.35,4,79);
            Color c=furrow<0.28?Ramp(bark,0.05):Ramp(bark,ridge);
            // broken ember cracks deep in the furrows
            if(furrow<0.24&&VN(x,y,8,83)>0.5)c=ColorTranslator.FromHtml(furrow<0.14?"#ffb347":"#e2621e");
            b.SetPixel(x,y,c);
        }
        return b;
    }
    public static Bitmap LogTop(){
        var b=New();string[] wood={"#5a2214","#73301b","#8c3f22","#a6502b","#bd6534"};string[] bark={"#2b0d0b","#3d150f","#521f13"};
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            double dx=x-7.5,dy=y-7.5,r=Math.Sqrt(dx*dx+dy*dy);
            Color c;
            if(Math.Max(Math.Abs(dx),Math.Abs(dy))>6.6)c=Ramp(bark,H(x,y,91));
            else{double ring=(r*0.62+VN(x,y,4,97)*0.6)%1.0;c=Ramp(wood,ring<0.22?0.1:0.45+ring*0.5+(H(x,y,101)-0.5)*0.15);}
            if(r<1.3)c=ColorTranslator.FromHtml(r<0.8?"#ffd27a":"#f08a30");   // the ember heart
            b.SetPixel(x,y,c);
        }
        return b;
    }
    public static Bitmap Shell(){
        var b=New();string[] plate={"#0b141b","#111e28","#172836","#1f3445","#294257","#35526b"};
        // an offset grid that tiles on 16: its Voronoi cells read as hexagonal plates
        double[,] cs={{0,0},{8,0},{4,8},{12,8},{16,0},{0,16},{16,16},{8,16},{-4,8},{20,8},{4,-8},{12,-8},{4,24},{12,24},{-8,0},{24,0},{-4,-8},{20,-8},{-4,24},{20,24},{-8,16},{24,16}};
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            double d1=1e9,d2=1e9,ox=0,oy=0;
            for(int i=0;i<cs.GetLength(0);i++){double dx=x+0.5-cs[i,0],dy=y+0.5-cs[i,1],d=Math.Sqrt(dx*dx+dy*dy*1.25);
                if(d<d1){d2=d1;d1=d;ox=dx;oy=dy;}else if(d<d2)d2=d;}
            Color c;
            if(d2-d1<1.3)c=ColorTranslator.FromHtml(H(x,y,107)>0.75?"#5cc7bf":"#2b6670");   // the seam, here and there aglow
            else{
                // a raised plate: lit at its upper left, its growth ring a shade lighter
                double lit=0.55-(ox+oy)*0.11+(d1>2.4&&d1<3.2?0.12:0)+(H(x,y,109)-0.5)*0.12;
                c=Ramp(plate,lit);
            }
            if(H(x,y,113)>0.988)c=ColorTranslator.FromHtml("#c9e3ec");   // frost
            b.SetPixel(x,y,c);
        }
        return b;
    }
}
'@
$made=[ordered]@{coppervein_cloudrock=[Tex]::Cloudrock();ironvein_bladestone=[Tex]::Bladestone();sunset_wutong_leaves=[Tex]::Leaves();sunset_wutong_log=[Tex]::Log();sunset_wutong_log_top=[Tex]::LogTop();xuanwu_shell_rock=[Tex]::Shell()}
if($OutDir){foreach($k in $made.Keys){$made[$k].Save((Join-Path $OutDir "$k.png"),[Drawing.Imaging.ImageFormat]::Png)}}
# a preview: each texture large, and as a 3x3 tiled patch
$s=10;$w=[int]($made.Count*(48*$s/3+16*$s+24));$canvas=New-Object System.Drawing.Bitmap -ArgumentList $w,([int](16*$s))
$g=[Drawing.Graphics]::FromImage($canvas);$g.Clear([Drawing.Color]::FromArgb(60,60,60));$g.InterpolationMode='NearestNeighbor';$g.PixelOffsetMode='Half'
$x=0;foreach($k in $made.Keys){$g.DrawImage($made[$k],$x,0,16*$s,16*$s);$x+=16*$s+8;$t=16*$s/3;for($i=0;$i -lt 3;$i++){for($j=0;$j -lt 3;$j++){$g.DrawImage($made[$k],[int]($x+$i*$t),[int]($j*$t),[int]$t,[int]$t)}};$x+=48*$s/3+16}
$canvas.Save($Sheet)
"done"
