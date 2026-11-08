using System;
using System.Drawing;

// The 32 affix accessories: each a thing that shows its affix, each alive in its own way (t: 0..1 over the loop).
public static partial class Designs {
    static string[] Silver={"#4a4f5c","#6b7282","#9199a8","#b8bfcc","#dde2ea","#ffffff"};
    static string[] Copper={"#4a2210","#6e3518","#9a4c22","#c26a33","#e08c50","#f5b47e"};
    static string[] Jade={"#0f3b2a","#17583d","#22784f","#36a065","#62c98a","#a8efc0"};
    static string[] Rose={"#4a1636","#6e2250","#98336e","#c44e90","#e47ab2","#ffb0d8"};
    static string[] Blood={"#2e0508","#550a10","#80121a","#b01f25","#e03a34","#ff7a6a"};
    static string[] Obsidian={"#07060c","#120f1c","#1e1a2e","#2d2744","#43395e"};
    static string[] Quartz={"#7d8796","#a4adba","#c8cfd8","#e6eaf0","#ffffff"};
    static string[] Leather={"#2c180c","#432614","#5e371e","#7a4a29","#985f36"};
    static double Wave(double t,double k){return Math.Sin((t+k)*Math.PI*2);}

    static void DrawAccessory(Cv c,string name,double t){
        switch(name){
        case "amplify_accessory":{
            // a silver tuning fork, its note spreading in rings
            c.Line(16,30,16,20,2.6,Lit(Silver,16,25,5));
            c.Fill((x,y)=>{double d=Math.Sqrt((x-16)*(x-16)+(y-20)*(y-20)*1.3);return d>=4.2&&d<=7&&y<=20.5;},Lit(Silver,16,16,7));
            c.Line(10,19,10,6,2.6,Lit(Silver,10,12,6));c.Line(22,19,22,6,2.6,Lit(Silver,22,12,6,-0.1));
            c.Outline();
            for(int k=0;k<2;k++){double r=4+((t+k*0.5)%1.0)*9;c.Fill((x,y)=>{double d=Math.Sqrt((x-16)*(x-16)+(y-11)*(y-11));return d>=r&&d<=r+1.6&&Math.Abs(x-16)>8.5;},Flat(r<9?"#cde8fb":"#7fa0c8"));}
            break;}
        case "armor_pierce_accessory":{
            // a steel ring set with thorns, a glint running round them
            for(int k=0;k<8;k++){double a=k*Math.PI/4;c.Poly(new double[]{16+Math.Cos(a-0.22)*9,16+Math.Sin(a-0.22)*9, 16+Math.Cos(a)*15,16+Math.Sin(a)*15, 16+Math.Cos(a+0.22)*9,16+Math.Sin(a+0.22)*9},Lit(Steel,16+Math.Cos(a)*12,16+Math.Sin(a)*12,4));}
            c.Ring(16,16,6,10,Lit(Steel,16,16,10,-0.05));c.Ring(16,16,7.5,8.5,Flat("#4f5a6b"));
            c.Outline();
            double g=t*Math.PI*2;c.Px((int)(16+Math.Cos(g)*14),(int)(16+Math.Sin(g)*14),"#ffffff");c.Px((int)(16+Math.Cos(g)*8),(int)(16+Math.Sin(g)*8),"#eef2f7");
            break;}
        case "befuddlement_accessory":{
            // a spiral shell that turns the head round with it
            c.Fill((x,y)=>{double dx=x-15,dy=y-16,r=Math.Sqrt(dx*dx+dy*dy);return r<=12.5&&!(dx>2&&dy>4&&r>9);},(x,y)=>{double dx=x-15,dy=y-16,r=Math.Sqrt(dx*dx+dy*dy),a=Math.Atan2(dy,dx)-t*Math.PI*2;
                double s=Math.IEEERemainder(a*2.0+r*0.6,Math.PI*2);return Cv.Ramp(Rose,0.4+0.45*Math.Abs(s)/Math.PI-(dx+dy)/60);});
            c.Poly(new double[]{17,21, 28,28, 21,28},Lit(Rose,22,26,4,-0.1));
            c.Outline();
            c.Px(27,5,"#fff1ad");c.Px(6,27,"#d0aaff");
            break;}
        case "blood_pact_accessory":{
            // a gold ring set with a red crystal that bleeds
            c.Ring(16,21,6,9,Lit(Gold,16,21,9));
            c.Poly(new double[]{16,2, 21,7, 20,13, 16,15, 12,13, 11,7},Lit(Blood,16,8,6,0.1));
            c.Poly(new double[]{12,13, 20,13, 18,15.5, 14,15.5},Lit(Gold,16,14,4));
            c.Outline();
            double fall=t*14;c.Circle(16,16+fall,1.4,Flat(fall<10?"#e03a34":"#80121a"));
            break;}
        case "causality_accessory":{
            // two chain links, cause and effect, a light running from one into the other
            Func<double,double,double,double,bool> link=(x,y,cx,cy)=>{double u=(x-cx+(y-cy))/1.414,v=(y-cy-(x-cx))/1.414,d=Math.Sqrt(u*u/2.6+v*v);return d>=3.4&&d<=6;};
            c.Fill((x,y)=>link(x,y,11,11),Lit(Gold,11,11,7));c.Fill((x,y)=>link(x,y,21,21)&&!(link(x,y,11,11)&&x+y<33),Lit(Silver,21,21,7));
            c.Outline();
            double p=t*2;var at=p<1?new[]{5+p*10,5+p*10}:new[]{15+(p-1)*12,15+(p-1)*12};c.Px((int)at[0]+1,(int)at[1]+1,"#ffffff");
            break;}
        case "curse_accessory":{
            // a violet crystal pendant breathing out a dark smoke
            c.Line(16,2,16,7,1,C("#43395e"));
            c.Poly(new double[]{16,7, 22,14, 19,27, 16,30, 13,27, 10,14},Lit(Violet,16,18,9));
            c.Line(16,9,16,28,1,C("#d0aaff"));
            c.Outline();
            for(int k=0;k<3;k++){double p=(t+k/3.0)%1.0;int x=(int)(7+k*8+Wave(p,k*0.3)*2),y=(int)(28-p*24);c.Px(x,y,p<0.5?"#5a3a7a":"#2d2744");}
            break;}
        case "damage_cap_accessory":{
            // a gold tablet barred with a line no blow can pass
            c.Poly(new double[]{8,4, 24,4, 26,8, 24,28, 8,28, 6,8},Lit(Gold,16,15,11));
            c.Poly(new double[]{10,7, 22,7, 22,25, 10,25},Lit(Gold,16,15,9,-0.25));
            for(int k=0;k<5;k++)c.Line(11,9+k*3.5,14,9+k*3.5,0.9,C("#875917"));
            c.Line(9,13,23,13,1.6,(x,y)=>Cv.Mix(C("#c33536"),C("#ff9a7a"),0.5+0.5*Wave(t,0)));
            c.Ring(18,19,1.6,3.2,Flat("#5e3a0c"));c.Poly(new double[]{15,20, 21,20, 21,25, 15,25},Flat("#875917"));
            c.Outline();break;}
        case "desaturation_accessory":{
            // a hand mirror that drains the colour from what it shows
            c.Line(22,22,29,29,3,Lit(Wood,25,25,5));
            c.Circle(14,14,10.5,Lit(Silver,14,14,11,-0.1));
            double edge=4+t*20;
            c.Circle(14,14,8.5,(x,y)=>x+y<edge*1.4?Cv.Ramp(new[]{"#e64a4a","#f0a03a","#f2e24a","#5ad66a","#4aa8ff","#9a5aff"},((x-5)/18.0+t)%1.0):Cv.Ramp(Quartz,0.3+(28-x-y)/40));
            c.Outline();break;}
        case "enlightenment_accessory":{
            // a crystal circlet, its stones lit one by one
            c.Fill((x,y)=>{double u=(x-16)/13.0,v=(y-20)/6.0,d=u*u+v*v;return d<=1&&d>=0.45&&y>=17;},Lit(Silver,16,20,12));
            double[] px={7,11.5,16,20.5,25},py={15,10,5,10,15};
            for(int k=0;k<5;k++){double h=k==2?7:5;c.Poly(new double[]{px[k],py[k], px[k]+2.2,py[k]+h*0.6, px[k],py[k]+h, px[k]-2.2,py[k]+h*0.6},(x,y)=>Cv.Ramp(Cyan,0.6-(x-16)/40));}
            c.Outline();
            int lit=(int)(t*5)%5;c.Px((int)px[lit],(int)(py[lit]+2),"#ffffff");c.Px((int)px[lit]-2,(int)py[lit]-2,"#d4fbff");
            break;}
        case "growth_accessory":{
            // a copper sprout putting out new leaves
            double sway=Wave(t,0)*1.2;
            c.Line(16,29,16+sway*0.5,12,2.2,Lit(Copper,16,20,8));
            c.Poly(new double[]{16+sway*0.5,18, 7+sway,13, 9+sway,19},Lit(Jade,10,16,5));
            c.Poly(new double[]{16+sway*0.6,14, 25+sway,9, 23+sway,15},Lit(Jade,22,12,5,-0.1));
            c.Circle(16+sway*0.5,9,3.2,Lit(Copper,16,9,3,0.15));
            c.Poly(new double[]{9,26, 23,26, 21,31, 11,31},Lit(Copper,16,28,6,-0.2));
            c.Outline();break;}
        case "healing_accessory":{
            // a bottle of green dew, bubbles rising through it
            c.Poly(new double[]{13,3, 19,3, 19,9, 25,15, 25,27, 21,30, 11,30, 7,27, 7,15, 13,9},(x,y)=>Cv.Mix(C("#cde8fb"),C("#ffffff"),0.3));
            c.Poly(new double[]{8,16, 24,16, 24,27, 21,29, 11,29, 8,27},Lit(Jade,16,22,9,0.1));
            c.Poly(new double[]{12,1, 20,1, 20,4, 12,4},Lit(Wood,16,2,4));
            c.Line(16,19,16,26,1.4,C("#ffffff"));c.Line(12.5,22.5,19.5,22.5,1.4,C("#ffffff"));
            c.Outline();
            for(int k=0;k<2;k++){double p=(t+k*0.5)%1.0;c.Px((int)(11+k*10),(int)(28-p*12),"#a8efc0");}
            break;}
        case "magic_accessory":{
            // a pendant holding an arcane star that turns
            c.Line(16,1,16,6,1,C("#9aa7b8"));
            c.Circle(16,17,11,Lit(Violet,16,17,11,-0.15));c.Ring(16,17,10,11.5,Lit(Silver,16,17,11));
            double a0=t*Math.PI*2/5;
            c.Fill((x,y)=>{double dx=x-16,dy=y-17,r=Math.Sqrt(dx*dx+dy*dy),a=Math.Atan2(dy,dx)-a0;double k=Math.Abs(Math.IEEERemainder(a,Math.PI*2/5))/(Math.PI/5);return r<=3+5.5*(1-k);},(x,y)=>Cv.Ramp(Violet,0.85));
            c.Outline();c.Px(16,17,"#ffffff");break;}
        case "magic_resistance_accessory":{
            // a quartz pendant on which the spells fall silent: a wave struck through
            c.Line(16,1,16,6,1,C("#9aa7b8"));
            c.Poly(new double[]{16,5, 24,12, 24,22, 16,30, 8,22, 8,12},Lit(Quartz,16,16,10));
            c.Fill((x,y)=>y>15&&y<19.5&&Math.Abs(y-17-Math.Sin(x*0.9)*1.5)<1,Flat("#7a40b8"));
            c.Line(9,24,23,10,1.4,C("#38b9d1"));
            c.Outline();
            double s=t*36-4;c.Fill((x,y)=>Cv.InPoly(new double[]{16,5, 24,12, 24,22, 16,30, 8,22, 8,12},x,y)&&Math.Abs(x+y*0.5-s)<1.2,Flat("#ffffff"));
            break;}
        case "malice_accessory":{
            // a black thorn, red malice beating in its veins
            c.Poly(new double[]{29,2, 21,17, 14,27, 7,31, 5,25, 11,18, 23,5},Lit(Obsidian,16,16,12,0.2));
            c.Poly(new double[]{18,12, 22,9, 21,14},Lit(Obsidian,20,12,3,0.2));c.Poly(new double[]{12,20, 8,18, 11,23},Lit(Obsidian,10,20,3,0.2));
            var hot=Cv.Mix(C("#80121a"),C("#ff5a3a"),0.5+0.5*Wave(t,0));
            c.Line(25,6,10,25,1.6,(x,y)=>hot);
            c.Outline(0.6);break;}
        case "mastery_accessory":{
            // a badge of two crossed blades, a shine crossing it
            c.Poly(new double[]{16,3, 27,7, 26,19, 16,29, 6,19, 5,7},Lit(Night,16,15,12,0.2));
            c.Line(8,24,24,7,2,Lit(Silver,16,16,8));c.Line(24,24,8,7,2,Lit(Silver,16,16,8,-0.1));
            c.Line(7,21,11,25,1.2,C("#d9aa37"));c.Line(25,21,21,25,1.2,C("#d9aa37"));
            c.Ring(16,16,0,2.2,Lit(Gold,16,16,2));
            c.Outline();
            double s=t*40-6;c.Fill((x,y)=>Cv.InPoly(new double[]{16,3, 27,7, 26,19, 16,29, 6,19, 5,7},x,y)&&Math.Abs(x-y*0.4-s)<1,Flat("#ffffff"));
            break;}
        case "physical_resistance_accessory":{
            // a bracer padded against blows, a shock dying out across it
            c.Poly(new double[]{6,8, 26,8, 28,24, 4,24},Lit(Leather,16,16,11));
            for(int k=0;k<3;k++)c.Line(5+k*0.4,11+k*5,27-k*0.4,11+k*5,1.6,Lit(Steel,16,11+k*5,10));
            c.Outline();
            double r=((t*14)%14);c.Fill((x,y)=>{double d=Math.Sqrt((x-16)*(x-16)+(y-16)*(y-16)*2);return d>=r&&d<=r+1.2&&Cv.InPoly(new double[]{6,8, 26,8, 28,24, 4,24},x,y);},Flat("#d4fbff"));
            break;}
        case "reflection_accessory":{
            // a gold mirror on a stand, a beam struck back off it
            c.Line(16,22,16,28,2,Lit(Gold,16,25,4));c.Poly(new double[]{9,28, 23,28, 24,31, 8,31},Lit(Gold,16,29,6,-0.1));
            c.Circle(16,12,10,Lit(Gold,16,12,10));c.Circle(16,12,7.6,(x,y)=>Cv.Mix(C("#fff6dc"),C("#f2d062"),Clamp((x+y-14)/22)));
            c.Outline();
            double p=t;var a=p<0.5?new[]{2+p*2*12,2+p*2*8}:new[]{14+(p-0.5)*2*14,10-(p-0.5)*2*8};c.Px((int)a[0],(int)a[1],"#ffffff");
            break;}
        case "regeneration_accessory":{
            // a ring of living vine, new green running round it
            c.Ring(16,16,7,11,Lit(Jade,16,16,11,-0.1));
            for(int k=0;k<6;k++){double a=k*Math.PI/3+0.3;c.Poly(new double[]{16+Math.Cos(a)*10,16+Math.Sin(a)*10, 16+Math.Cos(a+0.35)*14.5,16+Math.Sin(a+0.35)*14.5, 16+Math.Cos(a+0.55)*10.5,16+Math.Sin(a+0.55)*10.5},Lit(Jade,16+Math.Cos(a)*12,16+Math.Sin(a)*12,4,0.15));}
            c.Outline();
            double g=t*Math.PI*2;c.Px((int)(16+Math.Cos(g)*9),(int)(16+Math.Sin(g)*9),"#d8ffd0");c.Px((int)(16+Math.Cos(g-0.4)*9),(int)(16+Math.Sin(g-0.4)*9),"#a8efc0");
            break;}
        case "resistance_accessory":{
            // an amulet of layered alloys, its plates lit in turn
            for(int k=0;k<3;k++){double r=13-k*3.6;c.Poly(Hex(16,16,r,Math.PI/6),Lit(k==1?Gold:Steel,16,16,r,-0.1*k));}
            c.Outline();
            int lit=(int)(t*6)%6;double a=Math.PI/6+lit*Math.PI/3;c.Line(16+Math.Cos(a)*12,16+Math.Sin(a)*12,16+Math.Cos(a+Math.PI/3)*12,16+Math.Sin(a+Math.PI/3)*12,1.2,C("#ffffff"));
            break;}
        case "ruin_accessory":{
            // a cracked hourglass, its sand bursting out of the break
            c.Line(7,4,25,4,2,Lit(Wood,16,4,8));c.Line(7,28,25,28,2,Lit(Wood,16,28,8));
            c.Poly(new double[]{9,5, 23,5, 17.5,15.5, 23,27, 9,27, 14.5,16.5},(x,y)=>Cv.Mix(C("#cfe9ff"),C("#7fa0c8"),Clamp((x-9)/14)));
            c.Poly(new double[]{10,27, 22,27, 18,21, 14,21},Lit(Copper,16,24,6,0.15));
            c.Line(19,8,22,13,1,C("#3a2a2a"));c.Line(22,13,20,17,1,C("#3a2a2a"));
            c.Outline();
            for(int k=0;k<4;k++){double p=(t+k/4.0)%1.0;c.Px((int)(23+p*7),(int)(13+p*p*14-k),"#e08c50");}
            break;}
        case "seal_accessory":{
            // a crystal ring shut with a lock
            c.Ring(16,13,6.5,10,(x,y)=>Cv.Ramp(Cyan,0.65-(x+y-26)/40));
            c.Poly(new double[]{9,17, 23,17, 23,29, 9,29},Lit(Gold,16,23,7));
            c.Ring(16,17,3,5,Lit(Gold,16,15,5,-0.1));
            c.Circle(16,22,1.6,Flat("#3a2410"));c.Line(16,22,16,26,1,C("#3a2410"));
            c.Outline();
            double s=t*28;c.Fill((x,y)=>{double d=Math.Sqrt((x-16)*(x-16)+(y-13)*(y-13));return d>=6.5&&d<=10&&y<17&&Math.Abs(x-(s-4))<1;},Flat("#ffffff"));
            break;}
        case "shared_accessory":{
            // a knot of red thread tying two hearts' fates together, a pulse running the thread
            Func<double,double,double,double,bool> heart=(x,y,cx,cy)=>{double u=(x-cx)/6.0,v=(cy-y)/6.0;return Math.Pow(u*u+v*v-0.5,3)-u*u*v*v*v<=0;};
            c.Fill((x,y)=>heart(x,y,8,13),Lit(Rose,8,12,6,0.1));c.Fill((x,y)=>heart(x,y,24,13),Lit(Rose,24,12,6,0.05));
            c.Fill((x,y)=>{double d=Math.Abs(y-22-Math.Sin((x-8)*0.4)*3);return x>6&&x<26&&d<1.1;},Flat("#c33536"));
            c.Circle(16,23,2.2,Flat("#e46550"));
            c.Outline();
            double px=7+t*18;c.Px((int)px,(int)(22+Math.Sin((px-8)*0.4)*3),"#ffd0c0");
            break;}
        case "shatter_accessory":{
            // a wedge driven into a shield, the crack spreading
            c.Poly(new double[]{6,6, 26,6, 26,18, 16,29, 6,18},Lit(Steel,16,15,11,-0.05));
            double crack=0.3+0.7*t;
            c.Line(16,9,16,9+15*crack,1,C("#2a2f38"));c.Line(16,16,16-6*crack,16+5*crack,1,C("#2a2f38"));c.Line(16,13,16+7*crack,13+6*crack,1,C("#2a2f38"));
            c.Poly(new double[]{12,1, 20,1, 17.5,12, 14.5,12},Lit(Copper,16,6,5,0.1));
            c.Outline();break;}
        case "slowness_accessory":{
            // an ice bell, its slow tolling spreading in rings
            c.Poly(new double[]{16,3, 21,6, 23,17, 27,22, 5,22, 9,17, 11,6},Lit(Ice,16,13,10));
            c.Circle(16,24,2.2,Lit(Ice,16,24,2,-0.2));c.Ring(16,3,0,1.6,Flat("#9ccbec"));
            c.Outline();
            double r=4+t*11;c.Fill((x,y)=>{double d=Math.Sqrt((x-16)*(x-16)+(y-27)*(y-27)*4);return d>=r&&d<=r+1.3&&y>24;},Flat(t<0.6?"#cde8fb":"#6fa6d1"));
            break;}
        case "space_accessory":{
            // a prism with a rift torn through it, the other side showing
            c.Poly(new double[]{16,2, 28,24, 4,24},(x,y)=>Cv.Ramp(Violet,0.75-(x+y-26)/50));
            c.Poly(new double[]{16,2, 28,24, 16,30, 4,24},(x,y)=>y>24?Cv.Ramp(Violet,0.3):Color.Transparent);
            c.Fill((x,y)=>Cv.InPoly(new double[]{16,2, 28,24, 4,24},x,y)&&Math.Abs(x-16-Math.Sin(y*0.8)*1.6)<1.4&&y>7,(x,y)=>Cv.Mix(C("#05030a"),C("#7fe2ef"),0.5+0.5*Wave(t,y/20)));
            c.Outline();
            double a=t*Math.PI*2;c.Px((int)(16+Math.Cos(a)*12),(int)(17+Math.Sin(a)*11),"#d0aaff");
            break;}
        case "split_accessory":{
            // twin crystals, one of light and one of shadow, drawing apart and back
            double gap=1.5+1.5*Wave(t,0);
            c.Poly(new double[]{12-gap,3, 16-gap,12, 14-gap,29, 7-gap,24, 6-gap,12},Lit(Cyan,11,15,8,0.1));
            c.Poly(new double[]{20+gap,3, 26+gap,12, 25+gap,24, 18+gap,29, 16+gap,12},Lit(Obsidian,21,15,8,0.3));
            c.Outline(0.45);break;}
        case "strength_accessory":{
            // a heavy copper bangle, its red stone throbbing with force
            c.Fill((x,y)=>{double u=(x-16)/13.0,v=(y-16)/9.5,d=u*u+v*v;return d<=1&&d>=0.42;},Lit(Copper,16,16,12));
            for(int k=0;k<5;k++){double a=Math.PI*(0.15+k*0.175);c.Px((int)(16+Math.Cos(a)*11),(int)(16+Math.Sin(a)*7.8),"#f5b47e");}
            double pulse=0.5+0.5*Wave(t,0);
            c.Circle(16,6.8,3.4,(x,y)=>Cv.Mix(Cv.Ramp(Blood,0.5-(x+y-23)/14),C("#ffb0a0"),pulse*0.35));
            c.Outline();break;}
        case "summon_accessory":{
            // a bronze bell with spirits circling it
            c.Poly(new double[]{16,5, 21,8, 23,19, 27,24, 5,24, 9,19, 11,8},Lit(Bronze,16,15,10));
            c.Circle(16,26,2.2,Lit(Bronze,16,26,2,-0.2));c.Ring(16,4,1,2.6,Lit(Bronze,16,4,3));
            c.Outline();
            for(int k=0;k<3;k++){double a=t*Math.PI*2+k*Math.PI*2/3;int x=(int)(16+Math.Cos(a)*13),y=(int)(15+Math.Sin(a)*7);c.Px(x,y,"#d4fbff");c.Px(x,y+2,"#7fe2ef");}
            break;}
        case "thunder_accessory":{
            // a glass core with lightning trapped in it, never still
            c.Circle(16,16,12,(x,y)=>Cv.Mix(C("#1a2a4a"),C("#3a5f86"),Clamp((x+y-12)/30)));
            c.Ring(16,16,11,13,Lit(Silver,16,16,13));
            int f=(int)(t*4);double[][] bolts={new double[]{14,6, 18,13, 13,17, 19,26},new double[]{18,6, 13,12, 19,16, 14,26},new double[]{16,5, 20,11, 14,18, 17,27},new double[]{13,7, 17,12, 12,19, 18,25}};
            var b=bolts[f%4];for(int i=0;i<3;i++)c.Line(b[i*2],b[i*2+1],b[i*2+2],b[i*2+3],1.4,C("#f2f06a"));
            c.Outline();c.Px(11,10,"#ffffff");break;}
        case "time_accessory":{
            // an hourglass whose sand runs back up, a dial turning the wrong way behind it
            c.Ring(16,16,13,14.4,Flat("#5a6b8a"));
            double ha=-t*Math.PI*2-Math.PI/2;c.Line(16,16,16+Math.Cos(ha)*11,16+Math.Sin(ha)*11,1,C("#cde8fb"));
            c.Poly(new double[]{9,5, 23,5, 17.5,15.5, 17.5,16.5, 23,27, 9,27, 14.5,16.5, 14.5,15.5},(x,y)=>Cv.Mix(C("#cfe9ff"),C("#7fa0c8"),Clamp((x-9)/14)));
            c.Fill((x,y)=>Cv.InPoly(new double[]{9,5, 23,5, 17.5,15.5, 14.5,15.5},x,y)&&y<5+10*t+1&&Math.Abs(x-16)<(5+10*t+1-y)*1.4+1,Lit(Cyan,16,9,6,0.1));
            c.Fill((x,y)=>Cv.InPoly(new double[]{14.5,16.5, 17.5,16.5, 23,27, 9,27},x,y)&&y<27-10*t,Lit(Cyan,16,22,6,0.1));
            c.Line(7,4,25,4,2,Lit(Silver,16,4,8));c.Line(7,28,25,28,2,Lit(Silver,16,28,8));
            c.Outline();c.Px(16,(int)(14-t*8)*2/2,"#d4fbff");break;}
        case "undying_accessory":{
            // a golden flower whose calyx never fades, light passing over its petals
            c.Line(16,18,16,30,1.6,Lit(Jade,16,24,6));c.Poly(new double[]{16,26, 22,22, 18,27},Lit(Jade,19,25,3));
            for(int k=0;k<6;k++){double a=k*Math.PI/3-Math.PI/2;c.Circle(16+Math.Cos(a)*6,12+Math.Sin(a)*6,4.2,Lit(Gold,16+Math.Cos(a)*6,12+Math.Sin(a)*6,4,0.05));}
            c.Circle(16,12,3.4,Lit(Bronze,16,12,3,0.1));
            c.Outline();
            int lit=(int)(t*6)%6;double la=lit*Math.PI/3-Math.PI/2;c.Px((int)(16+Math.Cos(la)*7),(int)(12+Math.Sin(la)*7),"#ffffff");
            break;}
        case "void_accessory":{
            // a cluster of lightless crystal drinking in the sparks round it
            c.Poly(new double[]{16,2, 21,12, 20,29, 12,29, 11,12},Lit(Obsidian,16,15,10,0.25));
            c.Poly(new double[]{8,12, 12,19, 11,29, 5,29, 4,20},Lit(Obsidian,8,22,6,0.2));
            c.Poly(new double[]{25,10, 28,19, 27,29, 21,29, 21,18},Lit(Obsidian,24,20,6,0.15));
            c.Outline(0.9);
            for(int k=0;k<3;k++){double p=(t+k/3.0)%1.0,a=k*2.1+p*2,r=15-p*11;c.Px((int)(16+Math.Cos(a)*r),(int)(17+Math.Sin(a)*r),"#a26cdf");}
            break;}
        }
    }
    /** the mark of an accessory: a silver jump ring hung in the top-left corner (in item pixels) */
    static void Trinket(Cv c){int s=Cv.S;
        foreach(var p in new[]{new[]{1,0},new[]{2,0},new[]{0,1},new[]{3,1},new[]{0,2},new[]{3,2},new[]{1,3},new[]{2,3}})c.Set(p[0],p[1],C(p[1]<2?"#eef2f7":"#9aa7b8"));
        for(int x=1;x<=2;x++)for(int y=1;y<=2;y++)c.P[x,y]=Color.Transparent;c.Set(4,4,C("#c7d0dc"));}
    static double[] Hex(double cx,double cy,double r,double rot){var p=new double[12];for(int k=0;k<6;k++){p[2*k]=cx+Math.Cos(rot+k*Math.PI/3)*r;p[2*k+1]=cy+Math.Sin(rot+k*Math.PI/3)*r;}return p;}
}
