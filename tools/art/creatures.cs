using System;
using System.Collections.Generic;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;

// A small creature builder: bones of boxes with materials -> a GeckoLib geo model, a box-UV texture painted by
// material (lit top, shaded sides, dark bottom, grain), an emissive glowmask, and looping animations.
public class Mat {
    public string[] Ramp;public bool Glow;public string Pattern;public double Alpha=1;
    public Mat(string pattern,bool glow,params string[] ramp){Pattern=pattern;Glow=glow;Ramp=ramp;}
}
public class Box {public string Mat;public int X,Y,Z,W,H,D;public int U,V;public double Inflate;}
public class Bone {public string Name,Parent;public double PX,PY,PZ;public List<Box> Boxes=new List<Box>();}
public class Track {public string Bone,Channel;public Func<double,double[]> F;}
public class Anim {public string Name;public double Length;public bool Loop=true;public List<Track> Tracks=new List<Track>();}

public class Creature {
    public string Id;public int TexW,TexH;public List<Bone> Bones=new List<Bone>();public Dictionary<string,Mat> Mats=new Dictionary<string,Mat>();public List<Anim> Anims=new List<Anim>();
    public double BoundsW=2,BoundsH=2;
    Bone cur;
    public Creature(string id,int w,int h){Id=id;TexW=w;TexH=h;}
    public Creature B(string name,string parent,double px,double py,double pz){cur=new Bone{Name=name,Parent=parent,PX=px,PY=py,PZ=pz};Bones.Add(cur);return this;}
    public Creature C(string mat,int x,int y,int z,int w,int h,int d){cur.Boxes.Add(new Box{Mat=mat,X=x,Y=y,Z=z,W=Math.Max(1,w),H=Math.Max(1,h),D=Math.Max(1,d)});return this;}
    public Creature M(string name,Mat m){Mats[name]=m;return this;}
    public Anim A(string name,double length){var a=new Anim{Name=name,Length=length};Anims.Add(a);return a;}

    static double Hs(int x,int y,int s){unchecked{uint h=(uint)(x*374761393+y*668265263+s*1442695041);h=(h^(h>>13))*1274126177u;h^=h>>16;return (h&0xFFFF)/65535.0;}}
    static Color Ramp(string[] r,double t){t=Math.Max(0,Math.Min(0.9999,t));return ColorTranslator.FromHtml(r[(int)(t*r.Length)]);}

    /** shelf-pack every box's UV rectangle (2(w+d) by h+d) */
    void Pack(){
        int x=0,y=0,row=0;
        foreach(var b in Bones.SelectMany(b=>b.Boxes).OrderByDescending(b=>b.H+b.D)){
            int w=2*(b.W+b.D),h=b.H+b.D;
            if(x+w>TexW){x=0;y+=row;row=0;}
            if(y+h>TexH)throw new Exception(Id+": texture too small");
            b.U=x;b.V=y;x+=w;row=Math.Max(row,h);
        }
    }
    /** one face, f: 0 top,1 bottom,2 side,3 front,4 back */
    void Face(Bitmap tex,Bitmap glow,Box b,Mat m,int fx,int fy,int fw,int fh,int f){
        for(int i=0;i<fw;i++)for(int j=0;j<fh;j++){
            int px=fx+i,py=fy+j;
            double light=f==0?0.78:f==1?0.25:f==3?0.62:0.5;
            double grain=(Hs(px,py,7)-0.5)*0.18;
            double t=light+grain;
            switch(m.Pattern){
                case "stripe":if((j+((int)(i/2)))%4==0)t-=0.35;break;                 // dark bands
                case "barred":if(j%3==0&&f>=2)t=0.95;break;                           // bright bars (red patterns on a crane)
                case "scale":if((i+(j%2))%2==0)t-=0.12;break;
                case "feather":if(j%2==0&&(i+j/2)%3==0)t-=0.2;if(f>=2&&j==0)t+=0.15;break;
                case "flame":t=0.25+0.75*(1-(double)j/Math.Max(1,fh-1))+grain;break; // hot at the top of a face
                case "glass":t=0.55+((i==0||j==0)?0.3:0)+grain*0.5;break;
                case "eye":t=(i==0&&j==0)?0.95:0.15;break;
                case "plain":break;
            }
            var c=Ramp(m.Ramp,t);
            int a=(int)(255*m.Alpha);
            tex.SetPixel(px,py,Color.FromArgb(a,c.R,c.G,c.B));
            if(m.Glow)glow.SetPixel(px,py,Color.FromArgb(255,c.R,c.G,c.B));
        }
    }
    public void Write(string assets){
        Pack();
        var tex=new Bitmap(TexW,TexH,System.Drawing.Imaging.PixelFormat.Format32bppArgb);
        var glow=new Bitmap(TexW,TexH,System.Drawing.Imaging.PixelFormat.Format32bppArgb);
        foreach(var b in Bones.SelectMany(b=>b.Boxes)){
            var m=Mats[b.Mat];int u=b.U,v=b.V,w=b.W,h=b.H,d=b.D;
            Face(tex,glow,b,m,u+d,v,w,d,0);Face(tex,glow,b,m,u+d+w,v,w,d,1);
            Face(tex,glow,b,m,u,v+d,d,h,2);Face(tex,glow,b,m,u+d,v+d,w,h,3);
            Face(tex,glow,b,m,u+d+w,v+d,d,h,2);Face(tex,glow,b,m,u+2*d+w,v+d,w,h,4);
        }
        Directory.CreateDirectory(Path.Combine(assets,"textures","entity"));
        tex.Save(Path.Combine(assets,"textures","entity",Id+".png"),System.Drawing.Imaging.ImageFormat.Png);
        glow.Save(Path.Combine(assets,"textures","entity",Id+"_glowmask.png"),System.Drawing.Imaging.ImageFormat.Png);
        var ci=CultureInfo.InvariantCulture;Func<double,string> n=x=>x.ToString("0.###",ci);
        var g=new StringBuilder();
        g.Append("{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"geometry."+Id+"\",\"texture_width\":"+TexW+",\"texture_height\":"+TexH+",\"visible_bounds_width\":"+n(BoundsW)+",\"visible_bounds_height\":"+n(BoundsH)+",\"visible_bounds_offset\":[0,"+n(BoundsH/2)+",0]},\"bones\":[");
        g.Append(string.Join(",",Bones.Select(b=>"{\"name\":\""+b.Name+"\""+(b.Parent!=null?",\"parent\":\""+b.Parent+"\"":"")+",\"pivot\":["+n(b.PX)+","+n(b.PY)+","+n(b.PZ)+"]"
            +(b.Boxes.Count>0?",\"cubes\":["+string.Join(",",b.Boxes.Select(c=>"{\"origin\":["+c.X+","+c.Y+","+c.Z+"],\"size\":["+c.W+","+c.H+","+c.D+"],\"uv\":["+c.U+","+c.V+"]}"))+"]":"")+"}")));
        g.Append("]}]}");
        Directory.CreateDirectory(Path.Combine(assets,"geo","entity"));
        File.WriteAllText(Path.Combine(assets,"geo","entity",Id+".geo.json"),g.ToString());
        var an=new StringBuilder("{\"format_version\":\"1.8.0\",\"animations\":{");
        an.Append(string.Join(",",Anims.Select(a=>{
            var bones=a.Tracks.GroupBy(t=>t.Bone).Select(bt=>"\""+bt.Key+"\":{"+string.Join(",",bt.Select(t=>{
                var keys=new List<string>();int steps=(int)Math.Round(a.Length/0.05);
                for(int i=0;i<=steps;i++){double tt=i*0.05;var v=t.F(tt/a.Length);keys.Add("\""+n(tt)+"\":["+n(v[0])+","+n(v[1])+","+n(v[2])+"]");}
                return "\""+t.Channel+"\":{"+string.Join(",",keys)+"}";}))+"}");
            return "\"animation."+Id+"."+a.Name+"\":{\"loop\":"+(a.Loop?"true":"false")+",\"animation_length\":"+n(a.Length)+",\"bones\":{"+string.Join(",",bones)+"}}";
        })));
        an.Append("}}");
        Directory.CreateDirectory(Path.Combine(assets,"animations","entity"));
        File.WriteAllText(Path.Combine(assets,"animations","entity",Id+".animation.json"),an.ToString());
        // a preview: the texture, large
        var prev=new Bitmap(TexW*6,TexH*6);using(var gr=Graphics.FromImage(prev)){gr.Clear(Color.FromArgb(50,50,56));gr.InterpolationMode=System.Drawing.Drawing2D.InterpolationMode.NearestNeighbor;gr.PixelOffsetMode=System.Drawing.Drawing2D.PixelOffsetMode.Half;gr.DrawImage(tex,0,0,TexW*6,TexH*6);}
        prev.Save(Path.Combine(Path.GetTempPath(),"creature_"+Id+".png"),System.Drawing.Imaging.ImageFormat.Png);
    }
}
public static class AnimExt {
    public static Anim T(this Anim a,string bone,string channel,Func<double,double[]> f){a.Tracks.Add(new Track{Bone=bone,Channel=channel,F=f});return a;}
}
