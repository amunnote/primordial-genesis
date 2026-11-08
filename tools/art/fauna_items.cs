using System;
using System.Drawing;

// The items the guardians' creatures leave, in the charms' chunky style; and the two lanterns' block textures.
public static partial class Designs {
    static string[] Ember={"#5a1206","#8a2208","#c0400e","#ea6a1c","#ffa53c","#ffe08a"};
    static string[] Tide={"#06101e","#0c1a32","#14284a","#1e3c66","#2c5688"};
    static string[] Lamp={"#1f6a8a","#38b9d1","#7fe2ef","#d4fbff","#ffffff"};
    static string[] Bell={"#5e3a0c","#875917","#b38223","#d9aa37","#f2d062","#fff1ad"};

    static void DrawFauna(Cv c,string name,double t){
        switch(name){
        case "fauna_bifang_flame_crystal":
            // a red-gold crystal with a flame caught inside it
            c.Poly(new double[]{16,2, 24,10, 22,24, 16,30, 10,24, 8,10},Lit(Ember,16,15,11,0.1));
            c.Poly(new double[]{16,6, 20,12, 16,24, 12,12},(x,y)=>Cv.Ramp(Ember,0.9-(y-6)/40));
            c.Line(16,4,16,28,1,C("#fff09a"));
            c.Outline();break;
        case "fauna_thunder_firefly_bottle":
            // a stoppered bottle, a cyan firefly glowing in it
            c.Poly(new double[]{13,3, 19,3, 19,9, 25,15, 25,27, 21,30, 11,30, 7,27, 7,15, 13,9},(x,y)=>Cv.Mix(C("#cde8fb"),C("#ffffff"),0.25));
            c.Poly(new double[]{12,1, 20,1, 20,4, 12,4},Lit(Wood,16,2,4));
            c.Circle(16,21,4.6,(x,y)=>Cv.Ramp(Lamp,0.85-Math.Sqrt((x-16)*(x-16)+(y-21)*(y-21))/9));
            c.Line(9,13,13,17,1,C("#9ab8c8"));c.Line(23,13,19,17,1,C("#9ab8c8"));
            c.Outline(0.45);c.Px(16,21,"#ffffff");break;
        case "fauna_golden_bell_feather":
            // a gold feather with a little bell hung at its quill
            c.Line(7,27,24,6,1.2,C("#f6f1e4"));
            c.Poly(new double[]{24,4, 27,8, 15,22, 8,25, 11,17},Lit(Bell,18,14,10,0.1));
            c.Line(23,7,11,22,0.9,C("#875917"));
            c.Circle(7,27,2.8,Lit(Gold,7,27,3,0.15));
            c.Outline();break;
        case "fauna_whistling_arrow":
            // an arrow fletched with gold, a bell-mouthed whistle behind its head
            c.Line(6,26,25,7,1.2,Lit(Wood,15,16,12));
            c.Poly(new double[]{29,3, 22,5.5, 26.5,10},Lit(Steel,25,7,3));
            c.Circle(21.5,10.5,2.2,Lit(Bell,21,10,2,0.1));
            c.Poly(new double[]{8,24, 3,25, 5,28.5},Flat("#f2d062"));c.Poly(new double[]{8,24, 7,29, 3.5,27},Flat("#d9aa37"));
            c.Outline();break;
        case "fauna_netherlamp_sac":
            // a glowing sac, the lure of a deep fish
            c.Line(16,3,16,10,1.4,C("#1e3c66"));
            c.Circle(16,19,9,(x,y)=>Cv.Ramp(Lamp,0.9-Math.Sqrt((x-14)*(x-14)+(y-16)*(y-16))/14));
            c.Ring(16,19,8,9.2,Flat("#14284a"));
            c.Outline(0.4);c.Px(13,15,"#ffffff");break;
        case "fauna_netherlamp":
            // a dark iron lantern round a lamp of the deep
            c.Line(16,1,16,5,1.2,C("#2c5688"));
            c.Poly(new double[]{9,6, 23,6, 22,9, 10,9},Lit(Iron,16,7,6));
            c.Poly(new double[]{9,9, 23,9, 23,26, 9,26},Lit(Iron,16,17,8,-0.1));
            c.Poly(new double[]{11,11, 21,11, 21,24, 11,24},(x,y)=>Cv.Ramp(Lamp,0.85-Math.Abs(y-17)/16));
            c.Poly(new double[]{8,26, 24,26, 24,29, 8,29},Lit(Iron,16,27,8));
            c.Outline();break;
        case "fauna_netherlamp_fish_bucket":
            // a water bucket, a dark fish and its lamp in it
            c.Poly(new double[]{5,9, 27,9, 24,29, 8,29},Lit(Steel,16,18,11));
            c.Poly(new double[]{6,9, 26,9, 25,13, 7,13},(x,y)=>Cv.Ramp(new[]{"#2a4fa8","#3a6fd8","#5a8fe8"},0.5));
            c.Poly(new double[]{9,12, 20,12, 22,15, 9,15},Lit(Tide,15,13,6,0.15));
            c.Circle(23,8,2,(x,y)=>C("#7fe2ef"));c.Line(20,12,23,9,0.8,C("#1e3c66"));
            c.Line(5,9,16,3,0.9,C("#9aa7b8"));c.Line(27,9,16,3,0.9,C("#9aa7b8"));
            c.Outline();break;
        }
    }

    /** a lantern-shaped block texture in the vanilla lantern's layout */
    public static Bitmap LanternTexture(bool fireflies){
        var b=new Bitmap(16,16,System.Drawing.Imaging.PixelFormat.Format32bppArgb);
        Func<int,int,int,Color> frame=(x,y,k)=>C(fireflies?(k==0?"#5a3416":k==1?"#8c582a":"#ab7139"):(k==0?"#141620":k==1?"#2a2e3a":"#454b5a"));
        Func<int,int,Color> inner=(x,y)=>{double d=Math.Abs(x-2.5)+Math.Abs(y-5.5);return Cv.Ramp(fireflies?new[]{"#cde8fb","#7fe2ef","#38b9d1"}:Lamp,fireflies?0.2+d/8:0.85-d/7);};
        // the body's sides (0,2)-(6,9): a frame round a lit pane
        for(int x=0;x<6;x++)for(int y=2;y<9;y++){bool edge=x==0||x==5||y==2||y==8;b.SetPixel(x,y,edge?frame(x,y,y==2?2:1):inner(x,y));}
        if(fireflies){b.SetPixel(2,5,C("#ffffff"));b.SetPixel(3,6,C("#7fe2ef"));}
        // top and bottom (0,9)-(6,15)
        for(int x=0;x<6;x++)for(int y=9;y<15;y++){bool edge=x==0||x==5||y==9||y==14;b.SetPixel(x,y,edge?frame(x,y,1):(fireflies?C("#9ab8c8"):inner(x,y-4)));}
        // the cap's sides (1,0)-(5,2) and its top (1,10)-(5,14) drawn over the top face's middle
        for(int x=1;x<5;x++)for(int y=0;y<2;y++)b.SetPixel(x,y,frame(x,y,y==0?2:1));
        // the handle (11,1)-(14,3) and (11,10)-(14,12)
        for(int x=11;x<14;x++){foreach(int y in new[]{1,10}){b.SetPixel(x,y,frame(x,y,2));}foreach(int y in new[]{2,11}){if(x!=12)b.SetPixel(x,y,frame(x,y,1));}}
        return b;
    }
}
