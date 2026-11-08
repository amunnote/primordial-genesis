using System;
using System.Drawing;

// Alchemy recipes, recipe fragments and mystery recipes, one look a tier: 1 basic, 2 transmutation, 3 truth, 4 primordial.
public static partial class Designs {
    static string[] Vellum={"#5e6e8a","#7f92b0","#a4b6d0","#c8d6ea","#e6eef8","#ffffff"};
    static string[] Dusk={"#120a24","#1e1238","#2c1a50","#3d266a","#523486"};
    static string[] PaperOf(int t){return t==3?Vellum:t==4?Dusk:Paper;}
    static string[] RollerOf(int t){return t==1?Wood:t==2?Copper:Gold;}
    static string InkOf(int t){return t==1?"#5a3416":t==2?"#17788a":t==3?"#2b4fa8":"#fff1ad";}
    static string RibbonOf(int t){return t==1?"#8c582a":t==2?"#36a065":t==3?"#3a6fd8":"#a26cdf";}

    /** the sigil of a tier: a triangle, a ringed triangle, a ringed star, an eye of light */
    static void Sigil(Cv c,double cx,double cy,double r,int t,double phase){
        var ink=C(InkOf(t));
        if(t>=2)c.Ring(cx,cy,r-0.6,r+0.6,(x,y)=>ink);
        if(t<=2){c.Line(cx,cy-r*0.75,cx-r*0.68,cy+r*0.45,1,ink);c.Line(cx,cy-r*0.75,cx+r*0.68,cy+r*0.45,1,ink);c.Line(cx-r*0.68,cy+r*0.45,cx+r*0.68,cy+r*0.45,1,ink);}
        else if(t==3){for(int k=0;k<5;k++){double a=-Math.PI/2+k*Math.PI*4/5,b=-Math.PI/2+(k+1)*Math.PI*4/5;c.Line(cx+Math.Cos(a)*r*0.8,cy+Math.Sin(a)*r*0.8,cx+Math.Cos(b)*r*0.8,cy+Math.Sin(b)*r*0.8,1,C(k%2==0?"#d9aa37":"#2b4fa8"));}}
        else{double glow=0.5+0.5*Math.Sin(phase*Math.PI*2);
            c.Poly(new double[]{cx,cy-r*0.85, cx+r*0.6,cy, cx,cy+r*0.85, cx-r*0.6,cy},(x,y)=>Cv.Mix(C("#f2d062"),C("#ffffff"),glow*0.7+((x<cx)?0.2:0)));}
    }
    static void DrawRecipe(Cv c,string name,double phase){
        int t=name[name.Length-1]-'0';
        var paper=PaperOf(t);var roller=RollerOf(t);
        if(name.StartsWith("alchemy_recipe")){
            // a single sheet, its lower-right corner turned up, a seal at its upper left: no rollers, so never a charm
            double[] sheet={6,4, 26,4, 26,22, 20,28, 6,28};
            c.Poly(sheet,(x,y)=>{var col=Cv.Ramp(paper,0.68-((x-16)+(y-16))/70);if(x<8||y<6)col=Cv.Ramp(paper,0.8);return col;});
            if(t>=3)c.Fill((x,y)=>Cv.InPoly(sheet,x,y)&&(x<7.6||y<5.6||x>24.4||y>26.4),(x,y)=>C(t==3?"#d9aa37":"#f2d062"));
            c.Poly(new double[]{26,22, 20,28, 20,22},(x,y)=>Cv.Ramp(paper,0.25));
            Sigil(c,16,15,6,t,phase);
            c.Line(9,24,17,24,0.9,C(InkOf(t)));c.Line(9,21,14,21,0.9,C(InkOf(t)));
            c.Circle(8,6,3.6,Lit(t==1?Crimson:t==2?Jade:t==3?Cyan:Violet,8,6,3,0.1));
            c.Outline();
            if(t==4){int k=(int)(phase*4)%4;int[][] sp={new[]{11,10},new[]{21,11},new[]{20,19},new[]{12,19}};c.Px(sp[k][0],sp[k][1],"#ffffff");}        }else if(name.StartsWith("recipe_fragment")){
            // a torn scrap of one, a piece of its sigil still on it
            double[] scrap={6,7, 13,5, 17,7, 22,5, 26,9, 23,13, 26,17, 22,21, 24,26, 17,24, 12,27, 9,22, 5,23, 7,16, 4,12};
            c.Poly(scrap,Lit(paper,15,15,11,0.05));
            Sigil(c,13,13,7,t,phase);
            c.Line(15,21,21,20,0.9,C(InkOf(t)));
            c.Outline();
        }else{
            // a mystery: a scroll rolled shut, tied, sealed with a question
            c.Line(6,25,25,6,8.4,Lit(paper,15,15,12,0.1));
            c.Fill((x,y)=>{double along=x-y,across=Math.Abs(x+y-31)/1.414;return across<=4.3&&(Math.Abs(along+12)<1.6||Math.Abs(along-12)<1.6);},(x,y)=>Cv.Ramp(roller,0.55));
            c.Line(4.5,26.5,8,23,4.2,Lit(roller,6,25,3));c.Line(23,8,26.5,4.5,4.2,Lit(roller,25,6,3,-0.1));
            c.Line(11,12,20,21,2.4,C(RibbonOf(t)));
            c.Circle(16.5,15.5,4.4,Lit(t==1?Crimson:t==2?Jade:t==3?Cyan:Violet,16,15,4,0.1));
            c.Outline();
            // the question in the wax
            foreach(var p in new[]{new[]{15,13},new[]{17,13},new[]{18,14},new[]{17,15},new[]{16,16},new[]{16,19}})c.Px(p[0],p[1],"#fff6dc");
        }
    }
}
