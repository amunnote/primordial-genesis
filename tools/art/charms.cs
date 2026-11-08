using System;
using System.Collections.Generic;
using System.Drawing;

// A tiny pixel-art canvas: shapes are filled per pixel centre, coloured by a function of position, then outlined.
public class Cv {
    /** the designs are drawn in a 32-unit space; S units to a pixel (2: a 16-pixel item) */
    public static int S=2;
    public static int N{get{return 32/S;}}
    public Color[,] P=new Color[N,N];
    public Cv(){for(int x=0;x<N;x++)for(int y=0;y<N;y++)P[x,y]=Color.Transparent;}
    public static Color C(string h){return ColorTranslator.FromHtml(h);}
    public static Color Mix(Color a,Color b,double t){t=Math.Max(0,Math.Min(1,t));return Color.FromArgb((int)(a.A+(b.A-a.A)*t),(int)(a.R+(b.R-a.R)*t),(int)(a.G+(b.G-a.G)*t),(int)(a.B+(b.B-a.B)*t));}
    public static Color Ramp(string[] r,double t){t=Math.Max(0,Math.Min(0.9999,t));return C(r[(int)(t*r.Length)]);}
    public static Color Dark(Color c,double k){return Color.FromArgb(c.A,(int)(c.R*k),(int)(c.G*k),(int)(c.B*k));}
    public static double H(int x,int y,int s){unchecked{uint h=(uint)(x*374761393+y*668265263+s*1442695041);h=(h^(h>>13))*1274126177u;h^=h>>16;return (h&0xFFFF)/65535.0;}}
    public void Set(int x,int y,Color c){if(x<0||y<0||x>=N||y>=N||c.A==0)return;if(c.A>=255)P[x,y]=c;else P[x,y]=Mix(P[x,y].A==0?Color.FromArgb(0,c.R,c.G,c.B):P[x,y],Color.FromArgb(255,c.R,c.G,c.B),c.A/255.0);}
    public void Fill(Func<double,double,bool> inside,Func<double,double,Color> col){for(int x=0;x<N;x++)for(int y=0;y<N;y++){double px=(x+0.5)*S,py=(y+0.5)*S;if(inside(px,py))Set(x,y,col(px,py));}}
    public void Circle(double cx,double cy,double r,Func<double,double,Color> col){Fill((x,y)=>(x-cx)*(x-cx)+(y-cy)*(y-cy)<=r*r,col);}
    public void Ring(double cx,double cy,double r1,double r2,Func<double,double,Color> col){Fill((x,y)=>{double d=Math.Sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy));return d>=r1&&d<=r2;},col);}
    public void Poly(double[] p,Func<double,double,Color> col){Fill((x,y)=>InPoly(p,x,y),col);}
    public static bool InPoly(double[] p,double x,double y){bool c=false;int n=p.Length/2;for(int i=0,j=n-1;i<n;j=i++){double xi=p[2*i],yi=p[2*i+1],xj=p[2*j],yj=p[2*j+1];if(((yi>y)!=(yj>y))&&(x<(xj-xi)*(y-yi)/(yj-yi)+xi))c=!c;}return c;}
    public void Line(double x0,double y0,double x1,double y1,double w,Func<double,double,Color> col){w=Math.Max(w,S*1.05);
        Fill((x,y)=>{double dx=x1-x0,dy=y1-y0,l=dx*dx+dy*dy,t=l==0?0:Math.Max(0,Math.Min(1,((x-x0)*dx+(y-y0)*dy)/l));double ex=x0+t*dx-x,ey=y0+t*dy-y;return ex*ex+ey*ey<=w*w/4;},col);}
    public void Line(double x0,double y0,double x1,double y1,double w,Color c){Line(x0,y0,x1,y1,w,(x,y)=>c);}
    public void Px(int x,int y,string h){Set(x/S,y/S,C(h));}
    /** a dark rim round every opaque shape: each outside pixel next to one takes a deep shade of it */
    /** accessories: no bevel, a lighter rim in their own colour, so they read lighter than the charms */
    public static bool Soft=false;
    public void Outline(double k=0.32){if(Soft)k=Math.Max(k,0.55);else Bevel();var add=new List<Tuple<int,int,Color>>();
        for(int x=0;x<N;x++)for(int y=0;y<N;y++){if(P[x,y].A!=0)continue;Color best=Color.Transparent;
            foreach(var d in new[]{new[]{1,0},new[]{-1,0},new[]{0,1},new[]{0,-1}}){int a=x+d[0],b=y+d[1];if(a<0||b<0||a>=N||b>=N||P[a,b].A<200)continue;best=P[a,b];}
            if(best.A!=0)add.Add(Tuple.Create(x,y,Color.FromArgb(255,(int)(best.R*k)+8,(int)(best.G*k)+6,(int)(best.B*k)+12)));}
        foreach(var t in add)P[t.Item1,t.Item2]=t.Item3;}
    bool Empty(int x,int y){return x<0||y<0||x>=N||y>=N||P[x,y].A<128;}
    public void Bevel(){var src=(Color[,])P.Clone();for(int x=0;x<N;x++)for(int y=0;y<N;y++){if(src[x,y].A<200)continue;
        if(Empty(x-1,y)||Empty(x,y-1))P[x,y]=Mix(src[x,y],Color.White,0.28);else if(Empty(x+1,y)||Empty(x,y+1))P[x,y]=Dark(src[x,y],0.72);}}
    /** soft glow: transparent pixels near bright ones take a faint tint (drawn after the outline) */
    public void Glow(Color c,double r,double a){if(S>1)return;   // crisp at item size: no soft halo
        r=Math.Max(1,r/S*1.4);var src=(Color[,])P.Clone();for(int x=0;x<N;x++)for(int y=0;y<N;y++){if(src[x,y].A!=0)continue;double best=0;
        for(int i=-3;i<=3;i++)for(int j=-3;j<=3;j++){int u=x+i,v=y+j;if(u<0||v<0||u>=N||v>=N||src[u,v].A==0)continue;double d=Math.Sqrt(i*i+j*j);if(d<=r)best=Math.Max(best,1-d/(r+0.5));}
        if(best>0)P[x,y]=Color.FromArgb((int)(255*a*best),c.R,c.G,c.B);}}
    public Bitmap Bmp(){var b=new Bitmap(N,N,System.Drawing.Imaging.PixelFormat.Format32bppArgb);for(int x=0;x<N;x++)for(int y=0;y<N;y++)b.SetPixel(x,y,P[x,y]);return b;}
}
