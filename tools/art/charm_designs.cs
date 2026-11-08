using System;
using System.Drawing;

public static partial class Designs {
    static string[] Bronze={"#4e2c12","#6f4219","#935a24","#b77634","#d6984f","#efc483"};
    static string[] Steel={"#343c4a","#4f5a6b","#728097","#9aa7b8","#c7d0dc","#eef2f7"};
    static string[] Gold={"#5e3a0c","#875917","#b38223","#d9aa37","#f2d062","#fff1ad"};
    static string[] Crimson={"#3e0a10","#66121c","#951f29","#c33536","#e46550","#f8a07a"};
    static string[] Violet={"#22103e","#3a1a64","#562a8c","#7a40b8","#a26cdf","#d0aaff"};
    static string[] Cyan={"#0b3343","#145a70","#1f88a3","#38b9d1","#7fe2ef","#d4fbff"};
    static string[] Wood={"#32190b","#4f2c14","#6d411e","#8c582a","#ab7139"};
    static string[] Leaf={"#173d17","#22592a","#2f7a38","#45a04a","#77c766"};
    static string[] Paper={"#8f7a58","#b49f78","#d4c39c","#eee2c0","#fff8e4"};
    static string[] Night={"#0c1230","#16204e","#22326e","#334a92"};
    static string[] Iron={"#26282e","#3a3d45","#535761","#6f7480","#9096a3"};
    static string[] Ice={"#3a5f86","#4f80ad","#6fa6d1","#9ccbec","#cde8fb","#f4fcff"};
    static Color C(string h){return Cv.C(h);}
    /** lit from the upper left round a centre: a ramp shade by position */
    static Func<double,double,Color> Lit(string[] r,double cx,double cy,double size,double bias=0){return (x,y)=>Cv.Ramp(r,0.62+bias-((x-cx)+(y-cy))/(2.6*size));}
    static Func<double,double,Color> Flat(string h){var c=C(h);return (x,y)=>c;}
    static double Clamp(double v){return Math.Max(0,Math.Min(1,v));}

    public static Cv Draw(string name,double t){
        var c=new Cv();
        switch(name){
        case "basic_charm":{
            // a bronze pendant on a cord, an alchemical sigil cut in it and a small green stone
            c.Ring(16,7.5,1.3,2.6,Lit(Bronze,16,7.5,3));
            c.Circle(16,18.5,9,Lit(Bronze,16,18.5,9));
            c.Ring(16,18.5,7,7.9,Lit(Bronze,16,18.5,9,-0.25));
            c.Line(16,12.5,10.8,21.5,0.9,C("#5e3a18"));c.Line(16,12.5,21.2,21.5,0.9,C("#5e3a18"));c.Line(10.8,21.5,21.2,21.5,0.9,C("#5e3a18"));
            c.Circle(16,18.6,2.2,Lit(Leaf,16,18.6,2.2,0.15));c.Px(15,17,"#c9f2a8");
            c.Outline();break;}
        case "breaking_edge_charm":{
            // the snapped point of a great blade, its edge still red-hot
            c.Poly(new double[]{27,4, 20.5,9, 7,22, 9,24.5, 5.5,26.5, 8,28.5, 11,26, 13,27, 24.5,12},Lit(Steel,16,16,11));
            c.Line(27,4,10,25,1.1,(x,y)=>Cv.Ramp(Crimson,0.55+0.4*Clamp((y-4)/24)));   // the fuller burns
            c.Line(26.5,4.5,9,23,0.8,C("#ffffff"));
            c.Line(14,19,16.5,21.5,0.8,C("#2a2f38"));c.Line(16.5,21.5,15.5,23.5,0.8,C("#2a2f38"));   // a crack
            c.Outline();c.Px(28,3,"#ff9a6a");c.Px(25,2,"#ffd1a8");c.Px(29,6,"#ff7a4a");break;}
        case "conquest_charm":{
            // a war banner on a spear: crimson with a gold chevron
            c.Line(8,30,23,4,1.6,Lit(Wood,15,17,13));
            c.Poly(new double[]{23,1, 26,5, 22.5,7.5, 20.5,5},Lit(Gold,23,4,3));
            c.Poly(new double[]{20,8.5, 9,10, 5,22, 9.5,19.5, 12,24, 15.5,13},Lit(Crimson,12,15,8));
            c.Line(9,12.5,13.5,14.5,1.3,C("#f2d062"));c.Line(13.5,14.5,10.5,19,1.3,C("#f2d062"));
            c.Line(20,8.5,9,10,0.9,C("#d9aa37"));
            c.Outline();break;}
        case "spellbreaker_charm":{
            // a rune tablet split by a bolt of light
            c.Poly(new double[]{6,5, 15,4, 13.5,11, 16,16, 12.5,22, 14,28, 5,27},Lit(Violet,10,16,11));
            c.Poly(new double[]{18,4, 27,5, 27,27, 17,28, 15.5,22, 19,16, 16.5,11},Lit(Violet,22,16,11,-0.05));
            c.Line(8,9,12,9,0.9,C("#d0aaff"));c.Line(9,13,9,19,0.9,C("#d0aaff"));c.Line(7,23,11,23,0.9,C("#d0aaff"));
            c.Line(20,9,25,9,0.9,C("#d0aaff"));c.Line(23,13,23,20,0.9,C("#d0aaff"));c.Line(20,24,24,22,0.9,C("#d0aaff"));
            c.Outline();
            c.Line(16,3,14.5,11,1.2,C("#d4fbff"));c.Line(14.5,11,17.5,16,1.2,C("#d4fbff"));c.Line(17.5,16,14,22,1.2,C("#7fe2ef"));c.Line(14,22,15.8,29,1.2,C("#38b9d1"));
            c.Glow(C("#7fe2ef"),1.5,0.35);break;}
        case "arcane_pact_charm":{
            // an open grimoire, its pages written in burning violet
            c.Poly(new double[]{3,12, 16,15, 29,12, 29,25, 16,28, 3,25},Lit(Violet,16,20,13,-0.2));
            c.Poly(new double[]{4.5,11, 15.5,13.5, 15.5,25.5, 4.5,23},Lit(Paper,10,18,8));
            c.Poly(new double[]{16.5,13.5, 27.5,11, 27.5,23, 16.5,25.5},Lit(Paper,22,18,8,-0.1));
            for(int i=0;i<4;i++){c.Line(6.5,14+i*2.5,13.5,15.5+i*2.5,0.8,C("#7a40b8"));c.Line(18.5,15.5+i*2.5,25.5,14+i*2.5,0.8,C("#7a40b8"));}
            c.Line(16,13,16,30,1,C("#c33536"));
            c.Outline();
            c.Circle(16,6,2,Flat("#d0aaff"));c.Px(16,6,"#ffffff");c.Px(11,3,"#a26cdf");c.Px(21,4,"#a26cdf");c.Px(13,8,"#d0aaff");c.Px(19,8,"#d0aaff");
            c.Glow(C("#a26cdf"),1.5,0.3);break;}
        case "hundred_forged_charm":{
            // a golden cuirass of a hundred forged scales
            double[] body={4,7, 10,4, 13,6, 19,6, 22,4, 28,7, 27,12, 23,13, 22,20, 24,27, 16,29, 8,27, 10,20, 9,13, 5,12};
            c.Poly(body,Lit(Gold,16,15,12));
            c.Fill((x,y)=>Cv.InPoly(body,x,y)&&y>8&&((int)y%3==0)&&((int)(x+((int)y/3%2)*1.5)%3==0),Flat("#875917"));
            c.Fill((x,y)=>Cv.InPoly(body,x,y)&&y>8&&((int)y%3==1)&&((int)(x+((int)y/3%2)*1.5)%3==1),Flat("#fff1ad"));
            c.Line(16,7,16,28,1,C("#fff1ad"));c.Line(10,20,22,20,1.2,Lit(Bronze,16,20,6));
            c.Poly(new double[]{10,4, 13,6, 19,6, 22,4, 20,8.5, 16,10, 12,8.5},Lit(Bronze,16,6,4));
            c.Outline();break;}        case "bulwark_pact_charm":{
            // a tower shield: a wall of stone bound in iron
            c.Poly(new double[]{6,3, 26,3, 27,16, 24,25, 16,30, 8,25, 5,16},Lit(Iron,16,15,13));
            c.Poly(new double[]{8.5,5.5, 23.5,5.5, 24.5,16, 22,23.5, 16,27.5, 10,23.5, 7.5,16},Lit(Steel,16,15,11,-0.1));
            for(int r=0;r<5;r++){double y=8+r*4;c.Line(8,y,24,y,0.7,C("#4f5a6b"));for(double x=(r%2==0?11:13);x<23;x+=5)c.Line(x,y,x,y+4,0.7,C("#4f5a6b"));}
            c.Line(16,4,16,29,1.6,Lit(Iron,16,16,12,0.1));c.Line(6,14,26,14,1.6,Lit(Iron,16,16,12,0.1));
            foreach(var p in new[]{new[]{8,5},new[]{24,5},new[]{16,14},new[]{8,22},new[]{24,22}})c.Px(p[0],p[1],"#eef2f7");
            c.Outline();break;}
        case "crystal_vestment_charm":{
            // a hooded mantle cut from crystal
            c.Poly(new double[]{16,2, 21,6, 22,11, 27,28, 16,30, 5,28, 10,11, 11,6},(x,y)=>Cv.Ramp(Ice,0.7-((x-16)*0.04+(y-8)*0.015)+((int)(x+y)%5==0?0.15:0)));
            c.Poly(new double[]{16,4.5, 19,7, 19.5,11, 16,12.5, 12.5,11, 13,7},Flat("#22326e"));
            c.Line(16,13,16,29,0.7,C("#cde8fb"));c.Line(11,14,8,27,0.7,C("#9ccbec"));c.Line(21,14,24,27,0.7,C("#4f80ad"));
            c.Outline();c.Px(9,20,"#ffffff");c.Px(23,17,"#ffffff");c.Px(16,9,"#9ccbec");c.Px(25,24,"#f4fcff");
            c.Glow(C("#cde8fb"),1.4,0.25);break;}
        case "astral_pact_charm":{
            // a crescent moon on its orbit, three stars hung from it
            c.Fill((x,y)=>(x-15)*(x-15)+(y-14)*(y-14)<=100&&(x-19.5)*(x-19.5)+(y-11.5)*(y-11.5)>64,Lit(Gold,13,14,10,0.1));
            c.Fill((x,y)=>{double d=Math.Sqrt((x-16)*(x-16)/1.0+(y-17)*(y-17)/0.16);return d>=12.5&&d<=13.6;},Flat("#7fa0ff"));
            foreach(var s in new[]{new[]{24,20},new[]{20,27},new[]{27,10}}){c.Line(s[0]-2,s[1],s[0]+2,s[1],0.9,C("#fff1ad"));c.Line(s[0],s[1]-2,s[0],s[1]+2,0.9,C("#fff1ad"));c.Px(s[0],s[1],"#ffffff");}
            c.Line(24,17.5,24,18,0.5,C("#f2d062"));c.Line(20,23,20,25,0.5,C("#b38223"));
            c.Outline();c.Glow(C("#7fa0ff"),1.3,0.25);break;}
        case "thousand_blade_charm":{
            // a fan of blades from one hub
            for(int i=0;i<7;i++){double a=Math.PI*(1.08+i*0.14),cx=16+Math.Cos(a)*17,cy=27+Math.Sin(a)*17,n=a+Math.PI/2;
                double bx=16+Math.Cos(a)*4.5,by=27+Math.Sin(a)*4.5,w=2.2;
                c.Poly(new double[]{bx+Math.Cos(n)*w,by+Math.Sin(n)*w, cx,cy, bx-Math.Cos(n)*w,by-Math.Sin(n)*w},(x,y)=>{double side=(x-16)*Math.Cos(n)+(y-27)*Math.Sin(n);return Cv.Ramp(Steel,side>0?0.8:0.45);});}
            c.Circle(16,27,4.6,Lit(Bronze,16,27,5));c.Circle(16,27,2,Lit(Crimson,16,27,2,0.1));
            c.Outline();break;}        case "swift_pact_charm":{
            // a dagger with wings at its guard
            c.Poly(new double[]{26,3, 28,5, 17,17, 15,15},Lit(Steel,22,10,7));c.Line(26.8,4.2,16.5,15.5,0.5,C("#ffffff"));
            c.Poly(new double[]{16,14, 4,8, 8,13, 5,15, 10,17, 8,20, 14,18},Lit(Ice,9,14,7,0.1));
            c.Poly(new double[]{18,16, 24,28, 19,24, 17,27, 15,22, 12,24, 14,18},Lit(Ice,18,22,7,-0.05));
            c.Line(15,17,9,23,2.2,Lit(Cyan,12,20,4));c.Circle(8,24,1.8,Lit(Gold,8,24,2));
            c.Outline();break;}
        case "quickstring_charm":{
            // an arrow through a rune ring
            c.Line(6,26,25,7,1.2,Lit(Wood,15,16,12));
            c.Poly(new double[]{28,4, 22,6.5, 25.5,10},Lit(Steel,25,7,3));
            c.Poly(new double[]{8,24, 3,25, 5,28.5},Flat("#eef2f7"));c.Poly(new double[]{8,24, 7,29, 3.5,27},Flat("#9ccbec"));
            c.Outline();
            c.Fill((x,y)=>{double dx=x-15.5,dy=y-16.5,u=(dx+dy)/1.41,v=(dx-dy)/1.41,d=Math.Sqrt(u*u/0.3+v*v);return d>=5.2&&d<=6.4;},(x,y)=>((int)(x*3+y)%4==0)?C("#fff1ad"):C("#38b9d1"));
            c.Glow(C("#38b9d1"),1.2,0.3);break;}
        case "swiftstring_pact_charm":{
            // a green longbow, drawn, an arrow nocked
            c.Fill((x,y)=>{double d=Math.Sqrt((x-21)*(x-21)+(y-21)*(y-21));return d>=14.2&&d<=16.2&&x+y<34&&x<20.5&&y<20.5||false;},Lit(Leaf,10,10,10));
            c.Fill((x,y)=>{double d=Math.Sqrt((x-21)*(x-21)+(y-21)*(y-21));return d>=14.2&&d<=16.2&&!(x+y<34&&x<20.5&&y<20.5)&&(x<21||y<21)&&(x<8||y<8);},Lit(Gold,10,10,10));
            c.Line(5.5,20,20,5.5,0.6,C("#f4fcff"));
            c.Line(12.8,12.8,26,26,1.1,Lit(Wood,20,20,8));c.Poly(new double[]{29,29, 24,27, 27,24},Lit(Steel,27,27,3));
            c.Poly(new double[]{12.8,12.8, 9,14, 10.5,11},Flat("#c33536"));
            c.Outline();break;}

        // ---------------------------------------------------------------- truth: gold, and alive
        case "law_of_arms_charm":{
            // a golden sword, a ring of law turning round it, a glint running down the blade
            DrawRingBack(c,16,15,11,4,t,"#f2d062",4);
            c.Poly(new double[]{16,1, 18.3,5, 18.3,20, 13.7,20, 13.7,5},Lit(Gold,16,10,8,0.1));
            c.Line(16,3,16,19,0.7,C("#fff1ad"));
            c.Poly(new double[]{8,20, 24,20, 23,22.5, 9,22.5},Lit(Gold,16,21,6,-0.1));
            c.Line(16,22.5,16,28,2,Lit(Crimson,16,25,4));c.Circle(16,29.3,1.7,Lit(Gold,16,29,2));
            double g=t*24;c.Fill((x,y)=>y>=g-1&&y<=g+1&&y<20&&Math.Abs(x-16)<2.3-(y<5?(5-y)*0.5:0),Flat("#ffffff"));
            c.Outline();DrawRingFront(c,16,15,11,4,t,"#f2d062",4);c.Glow(C("#f2d062"),1.2,0.25);break;}
        case "law_of_arcana_charm":{
            // an eye of law in a turning ring of runes, its pupil breathing
            DrawRingBack(c,16,16,13,5,t,"#d0aaff",5);
            c.Fill((x,y)=>{double a=Math.Abs(x-16)/11.0,b=Math.Abs(y-16)/6.5;return a*a+b*b*Math.Max(0.6,1-a)<=1&&Math.Abs(y-16)<=6.5*(1-a*a)+0.4;},Lit(Paper,16,14,10,0.1));
            double pr=3.2+0.8*Math.Sin(t*Math.PI*2);
            c.Circle(16,16,5,Lit(Violet,16,16,5,0.1));c.Circle(16,16,pr*0.55,Flat("#120a24"));c.Px(14,14,"#ffffff");
            c.Outline();DrawRingFront(c,16,16,13,5,t,"#d0aaff",5);c.Glow(C("#a26cdf"),1.5,0.3);break;}
        case "law_of_gale_charm":{
            // three wind-blades turning in a gale
            for(int k=0;k<3;k++){double a0=t*Math.PI*2/3+k*Math.PI*2/3;
                c.Fill((x,y)=>{double dx=x-16,dy=y-16,r=Math.Sqrt(dx*dx+dy*dy),a=Math.Atan2(dy,dx)-a0;a=Math.IEEERemainder(a-r*0.18,Math.PI*2);return r>3&&r<15&&a>0&&a<1.0*(1-r/17.5);},
                    (x,y)=>{double r=Math.Sqrt((x-16)*(x-16)+(y-16)*(y-16));return Cv.Ramp(Steel,0.35+r/22);});}
            c.Circle(16,16,3.2,Lit(Gold,16,16,3));c.Circle(16,16,1.2,Flat("#38b9d1"));
            c.Outline();
            for(int k=0;k<6;k++){double a=-t*Math.PI*2+k*Math.PI/3,r=11+2*Math.Sin(k*2.1);c.Px((int)(16+Math.Cos(a)*r),(int)(16+Math.Sin(a)*r),k%2==0?"#d4fbff":"#7fe2ef");}
            c.Glow(C("#7fe2ef"),1.2,0.2);break;}
        case "law_of_tailwind_charm":{
            // a golden arrow riding the wind, its trail streaming
            for(int k=0;k<3;k++){double off=(k-1)*4.5;
                for(int s=0;s<4;s++){double p=((s/4.0+t)%1.0);double x0=4+p*12,y0=28-p*12;c.Line(x0+off*0.7,y0+off*0.7,x0+off*0.7+3,y0+off*0.7-3,0.9,(x,y)=>Cv.Mix(C("#7fe2ef"),C("#d4fbff"),p));}}
            c.Line(9,23,26,6,1.4,Lit(Gold,17,15,9));
            c.Poly(new double[]{30,2, 20,4.5, 27.5,12},Lit(Gold,26,6,3,0.15));
            c.Poly(new double[]{10,22, 5,23, 7,27},Flat("#fff1ad"));c.Poly(new double[]{10,22, 9,27, 5.5,25.5},Flat("#d9aa37"));
            c.Outline();c.Glow(C("#d4fbff"),1.2,0.25);break;}
        case "unyielding_body_charm":{
            // an iron heart riveted in gold, its core beating red through the seams
            double beat=0.5+0.5*Math.Pow(Math.Max(0,Math.Sin(t*Math.PI*2)),3);
            Func<double,double,bool> heart=(x,y)=>{double u=(x-16)/12.5,v=(18-y)/12.5;return Math.Pow(u*u+v*v-0.5,3)-u*u*v*v*v<=0.0;};
            c.Fill(heart,Lit(Iron,14,13,12,0.05));
            var hot=Cv.Mix(C("#a2222c"),C("#ffb070"),beat);
            c.Fill((x,y)=>heart(x,y)&&(Math.Abs(x-16)<1.1&&y>7&&y<27||Math.Abs(y-14)<0.9&&x>6&&x<26||Math.Abs(y-20)<0.9&&x>9&&x<23),(x,y)=>hot);
            c.Circle(16,16,2.4,(x,y)=>Cv.Mix(hot,C("#ffffff"),beat*0.5));
            foreach(var p in new[]{new[]{9,10},new[]{23,10},new[]{11,23},new[]{21,23},new[]{8,16},new[]{24,16}})c.Px(p[0],p[1],"#f2d062");
            c.Outline();c.Glow(hot,1.3,0.15+0.3*beat);break;}        case "arcane_harmony_charm":{
            // violet and cyan, each in the other, turning
            double a0=t*Math.PI*2;
            c.Circle(16,16,12,(x,y)=>{double dx=x-16,dy=y-16,ca=Math.Cos(a0),sa=Math.Sin(a0),u=dx*ca+dy*sa,v=-dx*sa+dy*ca;
                double up=Math.Sqrt(u*u+(v+6)*(v+6)),dn=Math.Sqrt(u*u+(v-6)*(v-6));
                bool cyan=u>0;if(up<6)cyan=true;if(dn<6)cyan=false;if(up<2.6)cyan=false;if(dn<2.6)cyan=true;
                return cyan?Cv.Ramp(Cyan,0.6-(dx+dy)/36):Cv.Ramp(Violet,0.65-(dx+dy)/36);});
            c.Ring(16,16,12,13.2,Lit(Gold,16,16,13));
            c.Outline();c.Glow(C("#a26cdf"),1.5,0.25);break;}
        // ---------------------------------------------------------------- primordial: the light of the first things
        case "omni_amplify_charm":{
            // a prism: white light in, every colour out, the colours shimmering
            c.Line(1,17,13,15,1.6,C("#ffffff"));
            string[] rb={"#ff4a4a","#ff9a3a","#ffe24a","#5ad66a","#4aa8ff","#9a5aff"};
            for(int i=0;i<6;i++){double y1=8+i*3.6+Math.Sin(t*Math.PI*2+i)*0.6;int k=(i+(int)(t*6))%6;c.Line(19,15.5+i*0.4,31,y1,1.4,(x,y)=>Cv.Mix(C(rb[i]),C("#ffffff"),0.15+0.35*Math.Max(0,Math.Sin((x/31.0-t)*Math.PI*2))));}
            c.Poly(new double[]{16,4, 26,24, 6,24},(x,y)=>Cv.Mix(C("#cfe9ff"),C("#ffffff"),Clamp(0.3+((16-x)+(14-y))/30)));
            c.Line(16,5,8,23,0.6,C("#ffffff"));
            c.Fill((x,y)=>Cv.InPoly(new double[]{16,4, 26,24, 6,24},x,y)&&Math.Abs((x+y)-(10+t*30))<1.2,Flat("#ffffff"));
            c.Outline(0.45);break;}
        case "fleeting_eternity_charm":{
            // an hourglass before a turning dial, its sand running
            c.Ring(16,16,13,14.2,Flat("#875917"));
            for(int k=0;k<12;k++){double a=k*Math.PI/6;c.Px((int)(16+Math.Cos(a)*12.2),(int)(16+Math.Sin(a)*12.2),"#f2d062");}
            double ha=t*Math.PI*2-Math.PI/2;c.Line(16,16,16+Math.Cos(ha)*11,16+Math.Sin(ha)*11,0.9,C("#fff1ad"));
            c.Poly(new double[]{9,5, 23,5, 17.5,15.5, 17.5,16.5, 23,27, 9,27, 14.5,16.5, 14.5,15.5},(x,y)=>Cv.Mix(C("#cfe9ff"),C("#7fa0c8"),Clamp((x-9)/14)));
            double sand=1-t;   // the upper sand falls away as the lower heap grows
            c.Fill((x,y)=>Cv.InPoly(new double[]{9,5, 23,5, 17.5,15.5, 14.5,15.5},x,y)&&y>5+10*(1-sand)*0.9+1,Lit(Gold,16,10,6,0.1));
            c.Fill((x,y)=>Cv.InPoly(new double[]{14.5,16.5, 17.5,16.5, 23,27, 9,27},x,y)&&y>27-10*t*0.9-1&&Math.Abs(x-16)<(y-(27-10*t*0.9-1))*1.4+1,Lit(Gold,16,24,6,0.1));
            for(int s=0;s<3;s++){int yy=16+(int)(((t*12+s*3.5)%10));c.Px(15,yy,"#fff1ad");}
            c.Line(7,4,25,4,1.6,Lit(Wood,16,4,8));c.Line(7,28,25,28,1.6,Lit(Wood,16,28,8));
            c.Outline();c.Glow(C("#f2d062"),1.3,0.25);break;}
        case "unbreakable_charm":{
            // an adamant gem no blow can mark, light crossing its faces, shields of law circling it
            DrawShards(c,16,16,13,t,false);
            c.Poly(new double[]{16,4, 26,14, 16,28, 6,14},(x,y)=>Cv.Ramp(Ice,0.55+((x<16)?0.15:-0.1)+((y<14)?0.15:-0.1)));
            c.Poly(new double[]{16,4, 20,14, 16,28, 12,14},(x,y)=>Cv.Ramp(Ice,0.75+((x<16)?0.1:-0.05)));
            c.Line(6,14,26,14,0.7,C("#f4fcff"));
            double s=t*40-8;c.Fill((x,y)=>Cv.InPoly(new double[]{16,4, 26,14, 16,28, 6,14},x,y)&&Math.Abs(x-y*0.6-s)<1.3,Flat("#ffffff"));
            c.Outline(0.4);DrawShards(c,16,16,13,t,true);c.Glow(C("#cde8fb"),1.5,0.3);break;}
        case "spell_annul_charm":{
            // a hole in the world where spells go to die: sparks spiral into the dark
            c.Ring(16,16,8,12.5,(x,y)=>{double a=Math.Atan2(y-16,x-16)+t*Math.PI*2,r=Math.Sqrt((x-16)*(x-16)+(y-16)*(y-16));return Cv.Mix(Cv.Ramp(Violet,0.85-(r-8)/9),C("#120a24"),0.4+0.4*Math.Sin(a*3+r*0.7));});
            c.Circle(16,16,7.8,Flat("#05030a"));
            c.Outline(0.5);
            string[] sp={"#ff6a5a","#7fe2ef","#f2d062","#d0aaff","#77c766"};
            for(int k=0;k<5;k++){double p=(t+k/5.0)%1.0,r=15-p*9,a=k*1.26+p*4.2;c.Px((int)(16+Math.Cos(a)*r),(int)(16+Math.Sin(a)*r),sp[k]);if(r>9)c.Px((int)(16+Math.Cos(a-0.25)*(r+1)),(int)(16+Math.Sin(a-0.25)*(r+1)),"#4a3a6a");}
            c.Glow(C("#7a40b8"),1.6,0.3);break;}

        // ---------------------------------------------------------------- supreme: white gold, the whole world seen
        case "clear_world_charm":{
            // an eye that holds the world, its rays turning
            for(int k=0;k<12;k++){double a=k*Math.PI/6+t*Math.PI/3;c.Line(16+Math.Cos(a)*12.5,16+Math.Sin(a)*12.5,16+Math.Cos(a)*(k%2==0?15.5:14),16+Math.Sin(a)*(k%2==0?15.5:14),1,C(k%2==0?"#fff1ad":"#cde8fb"));}
            c.Fill((x,y)=>{double a=Math.Abs(x-16)/13.0;return a<=1&&Math.Abs(y-16)<=8.5*(1-a*a);},(x,y)=>Cv.Mix(C("#fffaf0"),C("#e8d8b0"),Clamp((y-10)/12)));
            c.Circle(16,16,7,(x,y)=>{double lon=Math.Asin(Math.Max(-1,Math.Min(1,(x-16)/7.0)))+t*Math.PI*2,lat=(y-16)/7.0;bool land=Math.Sin(lon*2)+Math.Cos(lat*5+lon)>0.6;return land?Cv.Ramp(Leaf,0.6-(x-16+y-16)/30):Cv.Ramp(Cyan,0.55-(x-16+y-16)/30);});
            c.Ring(16,16,7,7.8,Flat("#d9aa37"));c.Px(13,13,"#ffffff");c.Px(14,13,"#ffffff");
            c.Outline(0.4);c.Glow(C("#fff1ad"),1.8,0.35);break;}
        case "inviolable_charm":{
            // a sigil no law can touch: barriers turning both ways round a white-gold shield
            DrawPolyRing(c,16,16,14,6,t*Math.PI/3,"#d9aa37");
            DrawPolyRing(c,16,16,11,6,-t*Math.PI/3+Math.PI/6,"#cde8fb");
            c.Poly(new double[]{16,7, 23,9.5, 22,18, 16,25, 10,18, 9,9.5},Lit(new[]{"#8a7a5a","#b8a578","#e6d6a8","#fff6dc","#ffffff"},16,14,8));
            c.Line(16,10,16,21,1.4,Lit(Gold,16,15,6,0.1));c.Line(12,14,20,14,1.4,Lit(Gold,16,15,6,0.1));
            c.Outline(0.4);
            for(int k=0;k<4;k++){double a=t*Math.PI*2+k*Math.PI/2;int px=(int)(16+Math.Cos(a)*8.5),py=(int)(16+Math.Sin(a)*8.5);c.Px(px,py,"#ffffff");}
            c.Glow(C("#fff1ad"),1.8,0.35);break;}
        default:if(name.StartsWith("fauna_")){DrawFauna(c,name,t);break;}if(name.StartsWith("loot_")){DrawLoot(c,name,t);break;}if(name.StartsWith("alchemy_recipe")||name.StartsWith("recipe_fragment")||name.StartsWith("mystery_recipe")){DrawRecipe(c,name,t);break;}Cv.Soft=true;try{DrawAccessory(c,name,t);Trinket(c);}finally{Cv.Soft=false;}break;
        }
        return c;
    }

    // a tilted ring of n beads round (cx,cy): the far half drawn before the body, the near half after
    static void Bead(Cv c,double cx,double cy,double rx,double ry,double t,string col,int n,bool front){
        for(int k=0;k<n;k++){double a=t*Math.PI*2+k*Math.PI*2/n;double y=Math.Sin(a);if((y>0)!=front)continue;
            int x=(int)Math.Round(cx+Math.Cos(a)*rx-0.5),yy=(int)Math.Round(cy+y*ry-0.5);c.Px(x,yy,col);if(front)c.Px(x+1,yy,"#ffffff");}}
    static void DrawRingBack(Cv c,double cx,double cy,double rx,double ry,double t,string col,int n){
        c.Fill((x,y)=>{double u=(x-cx)/rx,v=(y-cy)/ry;double d=u*u+v*v;return d>0.86&&d<1.14&&y<cy;},Flat("#875917"));Bead(c,cx,cy,rx,ry,t,col,n,false);}
    static void DrawRingFront(Cv c,double cx,double cy,double rx,double ry,double t,string col,int n){
        c.Fill((x,y)=>{double u=(x-cx)/rx,v=(y-cy)/ry;double d=u*u+v*v;return d>0.86&&d<1.14&&y>=cy;},Flat("#d9aa37"));Bead(c,cx,cy,rx,ry,t,col,n,true);}
    static void DrawShards(Cv c,double cx,double cy,double r,double t,bool front){
        for(int k=0;k<3;k++){double a=t*Math.PI*2+k*Math.PI*2/3;double y=Math.Sin(a);if((y>0)!=front)continue;
            double x0=cx+Math.Cos(a)*r,y0=cy+y*r*0.35;c.Poly(new double[]{x0,y0-2.2, x0+1.8,y0-1, x0+1.8,y0+1.2, x0,y0+2.2, x0-1.8,y0+1.2, x0-1.8,y0-1},Lit(Gold,x0,y0,2,front?0.1:-0.15));}}
    static void DrawPolyRing(Cv c,double cx,double cy,double r,int sides,double rot,string col){
        for(int k=0;k<sides;k++){double a=rot+k*Math.PI*2/sides,b=rot+(k+1)*Math.PI*2/sides;c.Line(cx+Math.Cos(a)*r,cy+Math.Sin(a)*r,cx+Math.Cos(b)*r,cy+Math.Sin(b)*r,1.1,C(col));}}
}
