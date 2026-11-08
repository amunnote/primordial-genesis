using System;
using System.Drawing;
using System.Drawing.Imaging;

// One boss bar sheet a boss, 256x64, at the game's own pixels (client/BossBars):
//   frame   224x26 at (0,0)    the bar's surround, the boss's own things about it; its window 182x5 at (21,11)
//   fill    214x5  at (0,28)   the health; its pattern repeats every 32, so the game slides it along
//   empty   182x5  at (0,34)   what is lost
//   ring    26x26  at (0,40)   the portrait's frame at the top middle, clear inside (radius 9)
//   ground  26x26  at (28,40)  behind the portrait
public static class BossBars {
    public const int W=256,H=64,TILE=32;
    const int WX=21,WY=11,WW=182,WH=5;
    static Bitmap b;
    static Color C(int rgb,int a=255){return Color.FromArgb(a,(rgb>>16)&255,(rgb>>8)&255,rgb&255);}
    static int Mix(int a,int c,double t){t=Math.Max(0,Math.Min(1,t));int r=(int)(((a>>16)&255)*(1-t)+((c>>16)&255)*t),g=(int)(((a>>8)&255)*(1-t)+((c>>8)&255)*t),bl=(int)((a&255)*(1-t)+(c&255)*t);return (r<<16)|(g<<8)|bl;}
    static void P(int x,int y,int rgb){if(x>=0&&y>=0&&x<W&&y<H)b.SetPixel(x,y,C(rgb));}
    static bool Set(int x,int y){return x>=0&&y>=0&&x<W&&y<H&&b.GetPixel(x,y).A>0;}
    static void Rect(int x,int y,int w,int h,int c){for(int i=0;i<w;i++)for(int j=0;j<h;j++)P(x+i,y+j,c);}
    static void Line(int x0,int y0,int x1,int y1,int c){int dx=Math.Abs(x1-x0),dy=-Math.Abs(y1-y0),sx=x0<x1?1:-1,sy=y0<y1?1:-1,e=dx+dy;while(true){P(x0,y0,c);if(x0==x1&&y0==y1)break;int e2=2*e;if(e2>=dy){e+=dy;x0+=sx;}if(e2<=dx){e+=dx;y0+=sy;}}}
    static void Disc(double cx,double cy,double r,int c){for(int x=(int)(cx-r-1);x<=cx+r+1;x++)for(int y=(int)(cy-r-1);y<=cy+r+1;y++)if((x+0.5-cx)*(x+0.5-cx)+(y+0.5-cy)*(y+0.5-cy)<=r*r)P(x,y,c);}
    /** a little drawing: rows of letters, '.' clear, others from the key ("a=ffffff b=..."), mirrored when asked */
    static void Stamp(int x0,int y0,string key,bool mirror,params string[] rows){
        var map=new System.Collections.Generic.Dictionary<char,int>();foreach(var part in key.Split(' '))map[part[0]]=Convert.ToInt32(part.Substring(2),16);
        int w=0;foreach(var r in rows)w=Math.Max(w,r.Length);
        for(int y=0;y<rows.Length;y++)for(int x=0;x<rows[y].Length;x++){char ch=rows[y][x];if(ch=='.')continue;P(x0+(mirror?w-1-x:x),y0+y,map[ch]);}
    }
    /** a dark edge round everything drawn in the frame, but never into the bar's window */
    static void Outline(int ink){
        var mark=new System.Collections.Generic.List<int[]>();
        for(int x=0;x<224;x++)for(int y=0;y<26;y++){
            if(Set(x,y))continue;if(x>=WX&&x<WX+WW&&y>=WY&&y<WY+WH)continue;
            if(Set(x+1,y)&&x+1<224||Set(x-1,y)||Set(x,y+1)&&y+1<26||Set(x,y-1))mark.Add(new[]{x,y});
        }
        foreach(var m in mark)P(m[0],m[1],ink);
    }

    public class Theme {public string Id,Pattern;public int Ink,Dark,Mid,Light,Top,Main,Bottom,Mark;}
    static Theme T(string id,string pattern,int ink,int dark,int mid,int light,int top,int main,int bottom,int mark){return new Theme{Id=id,Pattern=pattern,Ink=ink,Dark=dark,Mid=mid,Light=light,Top=top,Main=main,Bottom=bottom,Mark=mark};}
    // the rim's dark/mid/light; the fill's top/main/bottom; its pattern's colour
    public static Theme[] Themes={
        T("pride_lord","shine",    0x2a1a04,0x8a6014,0xd8a830,0xffe890,0xfff4b0,0xf2c040,0xb07a0c,0xffffff),
        T("wrath_lord","cracks",   0x140404,0x3a3a40,0x6a6a72,0xa8a8b0,0xff8a5a,0xd8281e,0x780a0a,0xffd070),
        T("envy_lord","smoke",     0x020a05,0x0e2a1a,0x1f5a38,0x3f9a68,0x8ae8b0,0x2fae64,0x0e5a30,0x0a2a18),
        T("greed_lord","coins",    0x221404,0x8a5a0a,0xd8a020,0xffe070,0xffe890,0xe8a820,0x9a5c08,0x7a4a06),
        T("lust_lord","petals",    0x200416,0x6a1048,0xb0307e,0xf47ac0,0xffb8e0,0xe8509e,0x902060,0xffe8f4),
        T("gluttony_lord","bubbles",0x140804,0x3a2210,0x6a4424,0x9a6a40,0xd88a8a,0xa83a3a,0x601818,0xf2c2a2),
        T("sloth_lord","drift",    0x0a0a16,0x2a2c50,0x6a6ea6,0xb8bce0,0xe8ecff,0xb0b4dc,0x6a6ea6,0xffffff),
        T("primordial_lord","prism",0x1a1a24,0x6a6c80,0xb8bcd0,0xf4f6ff,0xffffff,0xe0e4f4,0x9aa0bc,0xffd860),
        T("first_sage","runes",    0x0c0418,0x2a1048,0x5a2a98,0x9a6ad8,0xd0b0ff,0x8a5ad8,0x4a2088,0xf2c94c),
        T("qinglong","scales",     0x041812,0x0c4a36,0x1e8a64,0x52c8a0,0x9af0d0,0x30b888,0x106a4a,0xd8fff0),
        T("baihu","stripes",       0x0e0e12,0x4a4a54,0x9a9aa4,0xe8e8ec,0xffffff,0xe4e4e8,0xa8a8b0,0x30303a),
        T("zhuque","flames",       0x240402,0x7a1408,0xd04020,0xffa040,0xffe070,0xf06a20,0xb81c10,0xfff0a0),
        T("xuanwu","hex",          0x02080c,0x0e2a3a,0x1e5a74,0x3ab8d8,0x8ae8f8,0x2aa8c8,0x0e4a64,0x0a2a38),
    };

    public static Bitmap Sheet(Theme t){
        b=new Bitmap(W,H,PixelFormat.Format32bppArgb);
        Things(t,false);Rim(t);Things(t,true);Outline(t.Ink);
        Fill(t,0,28,214,false);Fill(t,0,34,182,true);
        Ring(t,0,40);Ground(t,28,40);
        return b;
    }

    // ------------------------------------------------------------------ the rim round the bar: two pixels of the boss's metal
    static void Rim(Theme t){
        for(int x=WX-2;x<WX+WW+2;x++)for(int y=WY-2;y<WY+WH+2;y++){
            if(x>=WX&&x<WX+WW&&y>=WY&&y<WY+WH)continue;
            bool outer=x==WX-2||x==WX+WW+1||y==WY-2||y==WY+WH+1;
            P(x,y,outer?(y==WY-2?t.Light:y==WY+WH+1?t.Dark:t.Mid):(y==WY-1?t.Mid:y==WY+WH?t.Dark:t.Mid));
        }
    }

    // ------------------------------------------------------------------ the boss's own things about the bar (behind the rim, then in front)
    static void Things(Theme t,bool front){
        int L=t.Light,M=t.Mid,D=t.Dark;
        int top=WY-2,bottom=WY+WH+1,left=WX-2,right=WX+WW+1;
        switch(t.Id){
        case "qinglong":
            if(!front){
                // clouds curling along the top and the bottom
                foreach(int cx in new[]{40,80,140,180}){Stamp(cx,top-4,"w=e8fff6 l=a8e8d0 m=52c8a0",false,"..ww....",".wwlww..","wwlllww.",".mmmmm..");}
                foreach(int cx in new[]{60,120,165}){Stamp(cx,bottom+1,"w=e8fff6 l=a8e8d0 m=52c8a0",false,".mmmmm..","wwlllww.",".wwlww..","..ww....");}
                // a lightning bolt or two
                Stamp(100,top-6,"y=fff6a0 w=ffffff",false,"..yw",".yw.","ywww","..wy",".wy.","wy..");
            }else{
                // the dragon's pearl held at the left end, its tail fin at the right
                Stamp(left-9,WY-6,"o=0a2018 d=106a4a m=30b888 l=9af0d0 a=e8c25a w=ffffff",false,"....aa....","...a..a...","..oooooo..",".olllllmo.",".olwllmmo.",".ollllmmo.",".ommmmmdo.","..oddddo..","...oooo...","...a..a...","....aa....");
                Stamp(right-1,WY-5,"o=0a2018 d=106a4a m=30b888 l=9af0d0",false,"......oo..",".....olo..","oo..olmo..","mdooolmo..","lmmmmmlo..","mdooolmo..","oo..olmo..",".....olo..","......oo..");
            }
            break;
        case "baihu":
            if(!front){
                // blades jutting from the rim, up and down by turns
                for(int k=0;k<11;k++){int x=26+k*17;bool up=k%2==0;if(up&&x>WX+76&&x<WX+106)continue;
                    if(up)Stamp(x,top-8,"w=ffffff l=d8d8e0 d=6a6a74 k=2a2a30",false,"..w..",".wl..",".wld.",".wld.",".wld.",".wld.","kkkkk");
                    else Stamp(x,bottom+1,"w=ffffff l=d8d8e0 d=6a6a74 k=2a2a30",false,"kkkkk",".wld.",".wld.",".wld.",".wl..","..w..");}
            }else{
                // black stripes across the rim, as on the tiger's hide
                for(int x=WX+6;x<WX+WW;x+=11){P(x,top,0x1c1c22);P(x+1,top,0x1c1c22);P(x+3,bottom,0x1c1c22);}
                // claw rakes over each end, red
                foreach(int x0 in new[]{left-5,right-4}){for(int k=0;k<3;k++)Line(x0+k*3,WY-5,x0+k*3+4,WY+WH+4,0xd8463a);}
                Stamp(left-8,WY-2,"w=ffffff l=e8e8ec d=8a8a94 k=1c1c22",false,".wwk..","wlkll.","wllkd.","wlkll.",".dddd.");
            }
            break;
        case "zhuque":
            if(!front){
                // flames licking up from the rim's top, taller and shorter
                for(int x=WX;x<WX+WW;x+=6){int h=2+(int)(Math.Abs(Math.Sin(x*0.37))*4);
                    for(int i=0;i<h;i++){int c=i<h/3?0xffe070:i<2*h/3?0xff8a2a:0xd8281e;P(x+1,top-1-i,c);if(i<h-1){P(x,top-1-i,i<h/2?0xff8a2a:0xd8281e);P(x+2,top-1-i,i<h/2?0xff8a2a:0xd8281e);}}}
            }else{
                // the phoenix's plumes sweeping out from each end
                foreach(bool m in new[]{false,true}){int x0=m?right-1:left-11;
                    Stamp(x0,WY-6,"y=ffe070 o=ff8a2a r=d8281e",m,"y..........","yo.........","yoo........",".roooo.....","..rrrooooo.","....rrrrrrr","..rrrooooo.",".roooo.....","yoo........","yo.........","y..........");}
            }
            break;
        case "xuanwu":
            if(!front){
                // waves along the bottom, their crests white
                for(int x=WX-4;x<WX+WW+4;x+=8){Stamp(x,bottom+1,"w=e8fbff l=8ae8f8 m=2aa8c8",false,".ww.....","wllw....","lmmlw..w","mmmmmlwl");}
            }else{
                // shell plates at each end, the serpent coiled round the right
                foreach(int x0 in new[]{left-8,right-1}){Stamp(x0,WY-5,"o=02080c d=0e2a3a m=1e5a74 l=3ab8d8 h=8ae8f8",false,"..oooo...",".ohhllo..","ohlmmlmo.","olmddmlmo","olmddmlmo","ohlmmlmo.",".ollmmo..","..oooo...");}
                Stamp(right+4,WY-8,"o=0a1a10 g=3a8a3a l=8ad08a r=d8281e",false,"..ooo.","..ogro",".ogglo",".ogo..","oggo..","ogo...");
            }
            break;
        case "pride_lord":
            if(!front){
                // weapons from his treasury rising from behind the bar, points up: swords and spears by turns
                for(int k=0;k<11;k++){int x=30+k*16;if(x>WX+80&&x<WX+104)continue;int h=6+(k%3);
                    if(k%2==0)Stamp(x,top-h-2,"w=ffffff s=d8d8e8 d=8a8aa0 g=ffd84a o=8a6014",false,
                        h==6?new[]{".w.",".ws","wsd","wsd","wsd","gog",".o."}:h==7?new[]{".w.",".ws","wsd","wsd","wsd","wsd","gog",".o."}:new[]{".w.",".ws","wsd","wsd","wsd","wsd","wsd","gog",".o."});
                    else Stamp(x,top-h-1,"w=ffffff g=ffd84a o=8a6014",false,
                        h==7?new[]{".w.","www",".g.",".o.",".o.",".o.",".o."}:new[]{".w.","www",".g.",".o.",".o.",".o."});}
            }else{
                // golden gates rippling open at each end
                foreach(int cx in new[]{left-4,right+4}){Disc(cx+0.5,WY+2.5,6,0xb07a0c);Disc(cx+0.5,WY+2.5,5,0xffe070);Disc(cx+0.5,WY+2.5,3.5,0xb07a0c);Disc(cx+0.5,WY+2.5,2.5,0xfff4b0);}
            }
            break;
        case "wrath_lord":
            if(!front){
                // iron spikes along the top and bottom
                for(int x=WX+4;x<WX+WW-4;x+=10){Stamp(x,top-4,"l=a8a8b0 d=4a4a52",false,"..l..",".ld..",".ldd.","lddd.");Stamp(x+5,bottom+1,"l=a8a8b0 d=4a4a52",false,"lddd.",".ldd.",".ld..","..l..");}
            }else{
                // broken chains hanging from each end, a red glow at the breaks
                foreach(bool m in new[]{false,true}){int x0=m?right-1:left-9;
                    Stamp(x0,WY-1,"l=a8a8b0 d=4a4a52 r=ff4a1a",m,"dd.......","ldd......",".dld.....","..ldldd..","...dl.ld.","....dlld.","......d.r","........r");}
            }
            break;
        case "envy_lord":
            if(!front){
                // shadow dripping from the bottom of the bar
                for(int x=WX+2;x<WX+WW;x+=7){int h=2+(int)((x*13)%5);for(int i=0;i<h;i++){P(x,bottom+1+i,i==h-1?0x1f5a38:0x0e2a1a);if(i<h-2)P(x+1,bottom+1+i,0x0e2a1a);}}
                // wisps rising from the top
                for(int x=WX+10;x<WX+WW;x+=23){Stamp(x,top-4,"d=0e2a1a m=1f5a38",false,".m..","d.m.",".dd.","dd..");}
            }else{
                // eyes peering from the shadow at each end
                foreach(int x0 in new[]{left-9,right-1}){Stamp(x0,WY-3,"k=020a05 d=0e2a1a g=6aff9a w=d8ffe8",false,"..dddd...",".dkkkkd..","dkkkkkkd.","dkgwkgwkd","dkkkkkkd.",".dkkkkd..","..dddd...");}
            }
            break;
        case "greed_lord":
            if(!front){
                // coins spilling over the rim's top, and gems set in it
                for(int x=WX+4;x<WX+WW-6;x+=9){if(x>WX+78&&x<WX+104)continue;int up=(x*7)%3;
                    Stamp(x,top-4-up,"o=4a2c08 y=ffe070 g=d8a020",false,".ooo.","oyyyo","oggyo",".ooo.");}
                for(int x=WX+16;x<WX+WW-8;x+=36){Stamp(x,bottom,"r=e83a3a w=ffb0b0 g=8a5a0a",false,".gwg.","grrrg",".grg.");}
            }else{
                // piles of coins at each end
                foreach(bool m in new[]{false,true}){int x0=m?right-1:left-11;
                    Stamp(x0,WY-6,"o=4a2c08 y=ffe070 g=d8a020 d=9a5c08",m,"....ooo.....","...oyyyo....","...ogggo....","..ooodoooo..",".oyyyoyyyyo.",".ogggoggggo.","ooooooooodoo","oyyyoyyyyoyo","ogggogggggoo","oddddddddddo","oooooooooooo");}
            }
            break;
        case "lust_lord":
            if(!front){
                // small butterflies fluttering over the bar, petals drifting under it, a vine of roses along it
                for(int x=WX+8;x<WX+WW-8;x+=30){if(x>WX+70&&x<WX+108)continue;bool m=(x/30)%2==0;
                    Stamp(x,top-6-(x%2),"p=f47ac0 l=ffd0ec d=6a1048",m,"pp.pp","plplp",".pdp.","pp.pp");}
                for(int x=WX+4;x<WX+WW;x+=13){Stamp(x,bottom+1+(x%2),"p=ffb8e0 q=f47ac0",false,"pq",".q");}
                for(int x=WX+14;x<WX+WW-10;x+=22){Stamp(x,top-2,"r=e8406a d=902040 g=3a8a3a",false,".rr.","rdrg","grr.");}
            }else{
                // butterflies at each end, and one alighting on the middle
                foreach(bool m in new[]{false,true}){int x0=m?right:left-9;
                    Stamp(x0,WY-5,"p=f47ac0 l=ffd0ec y=ffe060 d=6a1048",m,"pp...pp..","plp.plp..","ppppppp..",".ppdpp...","..ldl....","..pdp....",".pp.pp...",".yp.py...");}
            }
            break;
        case "gluttony_lord":
            if(!front){
                // teeth along the top and the bottom, the bar held in a maw
                for(int x=WX;x<WX+WW;x+=5){Stamp(x,top-3,"w=f2ece0 s=c8bca8",false,"wwws",".ws.","..s.");Stamp(x+2,bottom+1,"w=f2ece0 s=c8bca8",false,"..s.",".ws.","wwws");}
            }else{
                // the corners of the jaw at each end
                foreach(bool m in new[]{false,true}){int x0=m?right-2:left-6;
                    Stamp(x0,WY-6,"r=a83a3a d=601818 w=f2ece0",m,"....dd.","..ddrd.",".drrrd.","drrrd..","drrd...","drrd...","drrd...","drrrd..",".drrrd.","..ddrd.","....dd.");}
            }
            break;
        case "sloth_lord":
            if(front){
                // the bar is a sword: its hilt and guard at the left, its point at the right
                Stamp(left-12,WY-6,"o=0a0a16 g=8a6a3a l=c8a060 s=b8bce0 d=2a2c50",false,
                    "..........oo...",".........osdo..",".........osdo..",".........osdo..","ooooooooosdoooo","gllglgllgsdslgg","ggggggggggsdggg","ooooooooosdoooo",".........osdo..",".........osdo..",".........osdo..","..........oo...");
                Stamp(right-1,WY-2,"o=0a0a16 w=ffffff s=e8ecff m=b8bce0",false,"oo.....","mswoo..","msswwoo","mmmssoo","oooo...");
                // a crescent moon or two, it is half asleep
                Stamp(70,WY-9,"m=e8ecff d=9294ba",false,".mm.","m...","m...",".mm.");Stamp(150,WY+WH+3,"m=e8ecff d=9294ba",false,".mm.","m...","m...",".mm.");
            }
            break;
        case "primordial_lord":
            if(!front){
                // seven shards, one of each sin's colour, floating over the bar
                int[] sins={0xFFDF7E,0xF5A623,0x9294BA,0x43C982,0xE63248,0xBA9162,0xF66ABC};
                for(int k=0;k<7;k++){int x=k<4?28+k*20:WX+116+(k-4)*22,y=top-8-(k%2);int c=sins[k],l=Mix(c,0xffffff,0.55),d=Mix(c,0x000000,0.35);
                    // a crystal shard, pointed at both ends, lit on its left
                    P(x+2,y,0xffffff);P(x+1,y+1,l);P(x+2,y+1,c);P(x+3,y+1,d);
                    for(int j=2;j<5;j++){P(x,y+j,l);P(x+1,y+j,l);P(x+2,y+j,c);P(x+3,y+j,d);P(x+4,y+j,d);}
                    P(x+1,y+5,c);P(x+2,y+5,d);P(x+3,y+5,d);P(x+2,y+6,d);}
                // a band of gold under the bar
                for(int x=WX;x<WX+WW;x++)if(x%6!=0)P(x,bottom+1,0xffd860);
            }else{
                // wings of light at each end
                foreach(bool m in new[]{false,true}){int x0=m?right:left-10;
                    Stamp(x0,WY-6,"w=ffffff l=e0e4f4 m=b8bcd0 y=ffd860",m,"w.........","wl........","wll.......",".wlll.....","..wllll...","...ymmmmy.","..wllll...",".wlll.....","wll.......","wl........","w.........");}
            }
            break;
        case "first_sage":
            if(!front){
                // runes along the rim, above and below
                for(int x=WX+6;x<WX+WW-4;x+=13){int r=(x/13)%3;
                    if(r==0)Stamp(x,top-4,"a=f2c94c",false,"a.a",".a.","a.a");else if(r==1)Stamp(x,top-4,"a=f2c94c",false,"aaa","a..","aa.");else Stamp(x,top-4,"a=f2c94c",false,".a.","aaa",".a.");}
            }else{
                // a transmutation circle at each end
                foreach(int cx in new[]{left-4,right+4}){Disc(cx+0.5,WY+2.5,6.2,0xf2c94c);Disc(cx+0.5,WY+2.5,5.2,0x2a1048);
                    Line(cx,WY-2,cx-4,WY+5,0xf2c94c);Line(cx+1,WY-2,cx+5,WY+5,0xf2c94c);Line(cx-4,WY+5,cx+5,WY+5,0xf2c94c);P(cx,WY+2,0xd8281e);P(cx+1,WY+2,0xd8281e);}
            }
            break;
        }
    }

    // ------------------------------------------------------------------ the fill, 5 tall, its marks repeating every 32
    static void Fill(Theme t,int x0,int y0,int width,bool empty){
        for(int x=0;x<width;x++)for(int y=0;y<5;y++){
            int c=y==0?t.Top:y==4?t.Bottom:t.Main;
            if(y==1)c=Mix(t.Top,t.Main,0.5);
            double m=Mark(t.Pattern,x%TILE,y);if(m>0)c=Mix(c,t.Mark,m);
            if(empty)c=Mix(Mix(c,0x26262c,0.75),0x000000,0.3);
            P(x0+x,y0+y,c);
        }
    }
    static double Mark(string pattern,int x,int y){
        switch(pattern){
            case "shine": return (x+y)%32<2?0.8:0;
            case "cracks": {int[] line={2,2,1,1,2,3,3,2,1,1,2,3,3,3,2,1,2,2,3,2,1,1,2,3,2,2,1,2,3,3,2,2};return y==line[x]?0.8:0;}
            case "smoke": return (x%8==(y*3)%8)?0.5:0;
            case "coins": {int cx=x%8;return (cx==3||cx==4)&&(y==1||y==3)||(cx==2||cx==5)&&y==2?0.6:0;}
            case "petals": {int cx=x%11;return cx==4&&y==2||cx==5&&(y==1||y==3)?0.8:0;}
            case "bubbles": {int cx=x%9;return cx==3&&y==1||cx==7&&y==3?0.7:0;}
            case "drift": return (x*5+y*3)%16==0?0.8:0;
            case "prism": {int[] sins={0xFFDF7E,0xF5A623,0x9294BA,0x43C982,0xE63248,0xBA9162,0xF66ABC};return x%16==0&&y==2?0.9:0;}
            case "runes": {int cx=x%16;return cx==4&&(y==1||y==3)||cx==5&&y==2||cx==11&&y==2?0.8:0;}
            case "scales": {int sx=(x+(y%2==0?0:2))%4;return sx==0&&y>0&&y<4?0.5:0;}
            case "stripes": {int s=(x+y)%8;return s<2?0.95:0;}
            case "flames": {int[] h={3,2,2,1,1,2,3,3,2,1,1,1,2,3,2,2,1,2,3,3,2,1,2,2,3,2,1,1,2,3,3,2};return y>=h[x]?0.35:0;}
            case "hex": {int hx=x%8;return hx==0&&y>0&&y<4||(hx==1||hx==7)&&(y==0||y==4)?0.6:0;}
        }
        return 0;
    }

    // ------------------------------------------------------------------ the portrait's ring, at the top middle, and what lies behind it
    static void Ring(Theme t,int x0,int y0){
        double c=13;
        for(int x=0;x<26;x++)for(int y=0;y<26;y++){
            double dx=x+0.5-c,dy=y+0.5-c,d=Math.Sqrt(dx*dx+dy*dy);
            if(d>13||d<9)continue;
            int col=d>12.2||d<9.8?t.Ink:(dx+dy<-3?t.Light:dx+dy>3?t.Dark:t.Mid);
            P(x0+x,y0+y,col);
        }
        // its crest at the top, its own for each boss
        int L=t.Light,M=t.Mid;
        switch(t.Id){
            case "pride_lord":Stamp(x0+9,y0-1,"y=ffe070 o=2a1a04 r=e83a3a",false,"y.y.y.y",".yyryy.");break;
            case "wrath_lord":Stamp(x0+3,y0,"l=e8e0d4 o=140404",false,"l................l",".l..............l.");break;
            case "qinglong":Stamp(x0+7,y0,"a=e8c25a",false,"a..........a",".a........a.");break;
            case "zhuque":Stamp(x0+11,y0-2,"y=ffe070 o=ff8a2a",false,".y..",".oy.","yoo.");break;
            default:break;
        }
    }
    static void Ground(Theme t,int x0,int y0){
        for(int x=0;x<26;x++)for(int y=0;y<26;y++){double dx=x+0.5-13,dy=y+0.5-13,d=Math.Sqrt(dx*dx+dy*dy);if(d>9.8)continue;
            P(x0+x,y0+y,Mix(Mix(t.Main,0x000000,0.55),Mix(t.Bottom,0x000000,0.85),d/10));}
    }
}
