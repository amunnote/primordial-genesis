using System;
using System.Drawing;

// Every charm one shape: a capsule laid on the diagonal, three bands of colour, a small emblem on its middle band.
// Colours by kind (red-orange attack, violet arcana, steel/gold guard, cyan-blue spell ward, silver speed, green
// projectiles); the seams between the bands by tier (none basic, dark pacts, gold laws, shining primordial, white-gold
// supreme); the higher tiers alive with a glint running down them.
public static partial class Designs {
    // the capsule's axis, from its lower-left end to its upper-right, and its half width
    const double AX=8.5,AY=24.5,BX=23.5,BY=7.5,HALF=6.6;
    static double Along(double x,double y){double dx=BX-AX,dy=BY-AY,l=dx*dx+dy*dy;return ((x-AX)*dx+(y-AY)*dy)/l;}
    static double Across(double x,double y){double dx=BX-AX,dy=BY-AY,l=Math.Sqrt(dx*dx+dy*dy);return ((x-AX)*dy-(y-AY)*dx)/l;}
    static bool InCapsule(double x,double y){double s=Math.Max(0,Math.Min(1,Along(x,y)));double px=AX+(BX-AX)*s,py=AY+(BY-AY)*s;return (x-px)*(x-px)+(y-py)*(y-py)<=HALF*HALF;}

    class Kind {public string[] Top,Mid,Low;public string Ink;public int Tier;public Action<Cv,string> Emblem;}

    public static Cv Capsule(string name,double t){
        var k=KindOf(name);var c=new Cv();
        // the three bands, lit from the upper left across the capsule
        c.Fill(InCapsule,(x,y)=>{double s=Along(x,y),a=Across(x,y);string[] ramp=s>0.64?k.Top:s>0.3?k.Mid:k.Low;return Cv.Ramp(ramp,0.62-a/(HALF*2.6));});
        k.Emblem(c,k.Ink);
        c.Outline(k.Tier>=5?0.42:0.32);
        // the upper end's shine, as on a glazed bead
        c.Px(23,7,"#ffffff");c.Px(25,9,"#e8f6ff");
        // the higher tiers: a glint runs down the capsule
        if(k.Tier>=3){double g=1.3-t*1.6;string glint=k.Tier==3?"#fff1ad":k.Tier==4?"#ffffff":"#ffffff";
            c.Fill((x,y)=>InCapsule(x,y)&&Math.Abs(Along(x,y)-g)<0.06,(x,y)=>{var b=C(glint);return Color.FromArgb(k.Tier==3?150:200,b.R,b.G,b.B);});}
        if(k.Tier>=4){string[] rb={"#ff6a5a","#ffd24a","#6ade7a","#5aaaff","#c27aff"};int i=(int)(t*5)%5;c.Px(6,16,rb[i]);c.Px(17,27,rb[(i+2)%5]);}
        if(k.Tier>=5){int i=(int)(t*4)%4;int[][] sp={new[]{27,4},new[]{4,27},new[]{29,14},new[]{14,29}};c.Px(sp[i][0],sp[i][1],"#ffffff");}
        return c;
    }

    // ------------------------------------------------------------------ emblems, drawn on the middle band (about 16,16)
    static void EClover(Cv c,string ink){c.Circle(14,14,2,(x,y)=>C(ink));c.Circle(18,14,2,(x,y)=>C(ink));c.Circle(14,18,2,(x,y)=>C(ink));c.Circle(18,18,2,(x,y)=>C(ink));c.Line(17,19,20,22,0.9,C(ink));}
    static void ESword(Cv c,string ink){c.Line(12,20,20,12,1.6,C(ink));c.Line(12,15,17,20,1,C(ink));}
    static void ECross(Cv c,string ink){c.Line(12,12,20,20,1.4,C(ink));c.Line(12,20,20,12,1.4,C(ink));}
    static void EBolt(Cv c,string ink){c.Line(17,11,14,16,1.2,C(ink));c.Line(14,16,18,16,1.2,C(ink));c.Line(18,16,15,21,1.2,C(ink));}
    static void EStar(Cv c,string ink){c.Line(16,11,16,21,1.2,C(ink));c.Line(11,16,21,16,1.2,C(ink));c.Px(16,16,"#ffffff");}
    static void EShield(Cv c,string ink){c.Poly(new double[]{12,12, 20,12, 20,17, 16,21, 12,17},(x,y)=>C(ink));}
    static void ETower(Cv c,string ink){c.Poly(new double[]{12,11, 20,11, 20,18, 16,21, 12,18},(x,y)=>C(ink));c.Line(16,12,16,19,1,C("#ffffff"));}
    static void ECrystal(Cv c,string ink){c.Poly(new double[]{16,11, 20,16, 16,21, 12,16},(x,y)=>C(ink));c.Px(15,14,"#ffffff");}
    static void EMoon(Cv c,string ink){c.Fill((x,y)=>(x-16)*(x-16)+(y-16)*(y-16)<=22&&(x-18)*(x-18)+(y-14.5)*(y-14.5)>14,(x,y)=>C(ink));}
    static void EFan(Cv c,string ink){c.Line(16,20,11,12,1,C(ink));c.Line(16,20,16,11,1,C(ink));c.Line(16,20,21,12,1,C(ink));}
    static void EWings(Cv c,string ink){c.Line(11,13,15,16,1.2,C(ink));c.Line(15,16,11,19,1.2,C(ink));c.Line(16,13,20,16,1.2,C(ink));c.Line(20,16,16,19,1.2,C(ink));}
    static void EArrow(Cv c,string ink){c.Line(12,20,20,12,1.1,C(ink));c.Poly(new double[]{21,11, 16.5,12.5, 19.5,15.5},(x,y)=>C(ink));}
    static void EBow(Cv c,string ink){c.Fill((x,y)=>{double d=Math.Sqrt((x-19)*(x-19)+(y-19)*(y-19));return d>=6.5&&d<=8&&x<19.5&&y<19.5;},(x,y)=>C(ink));c.Line(12,19,19,12,0.9,C(ink));}
    static void EEye(Cv c,string ink){c.Fill((x,y)=>{double a=Math.Abs(x-16)/5.5;return a<=1&&Math.Abs(y-16)<=3*(1-a*a);},(x,y)=>C("#fff6dc"));c.Circle(16,16,1.8,(x,y)=>C(ink));}
    static void ESpiral(Cv c,string ink){c.Fill((x,y)=>{double dx=x-16,dy=y-16,r=Math.Sqrt(dx*dx+dy*dy),a=Math.Atan2(dy,dx);return r<5.5&&Math.Abs(Math.IEEERemainder(a-r*0.9,Math.PI))<0.5;},(x,y)=>C(ink));}
    static void EHeart(Cv c,string ink){c.Fill((x,y)=>{double u=(x-16)/5.5,v=(17-y)/5.5;return Math.Pow(u*u+v*v-0.5,3)-u*u*v*v*v<=0;},(x,y)=>C(ink));}
    static void EYinYang(Cv c,string ink){c.Circle(16,16,5,(x,y)=>x<16?C(ink):C("#fff6dc"));c.Circle(16,13.5,1.4,(x,y)=>C("#fff6dc"));c.Circle(16,18.5,1.4,(x,y)=>C(ink));}
    static void EPrism(Cv c,string ink){c.Poly(new double[]{16,11, 21,20, 11,20},(x,y)=>C("#e6f4ff"));c.Line(13,18,19,18,0.9,C("#ff6a5a"));c.Line(14,16,18,16,0.9,C("#5aaaff"));}
    static void EHourglass(Cv c,string ink){c.Poly(new double[]{12,11, 20,11, 16,16, 20,21, 12,21, 16,16},(x,y)=>C(ink));c.Px(16,19,"#fff1ad");}
    static void EGem(Cv c,string ink){c.Poly(new double[]{16,10, 21,15, 16,22, 11,15},(x,y)=>C(ink));c.Line(11,15,21,15,0.8,C("#ffffff"));}
    static void EVoid(Cv c,string ink){c.Circle(16,16,5,(x,y)=>C(ink));c.Circle(16,16,3,(x,y)=>C("#05030a"));}
    static void EWorld(Cv c,string ink){c.Fill((x,y)=>{double a=Math.Abs(x-16)/6;return a<=1&&Math.Abs(y-16)<=3.6*(1-a*a);},(x,y)=>C("#fffaf0"));c.Circle(16,16,2.4,(x,y)=>x<16?C("#3cc0d6"):C("#45a04a"));}
    static void EHalo(Cv c,string ink){c.Poly(new double[]{12,12, 20,12, 20,17, 16,21, 12,17},(x,y)=>C(ink));c.Line(16,13,16,19,1,C("#fffaf0"));c.Line(13,15,19,15,1,C("#fffaf0"));}
    static void EDot(Cv c,string ink){c.Circle(16,16,2.6,(x,y)=>C(ink));}

    static string[] R(params string[] r){return r;}
    static Kind KindOf(string n){
        // ramps, dark to light
        var red=R("#5a0e10","#8a1a18","#c0302a","#e8604a"); var orange=R("#6a2a08","#a04810","#d87420","#f7a840");
        var gold=R("#6b4410","#a0701c","#d9aa37","#f5d667"); var steel=R("#343c4a","#5a6578","#8a96a8","#c4ccd8");
        var violet=R("#2a1248","#4a2280","#7440b8","#a674e8"); var pink=R("#4a1636","#7a2a5a","#b0488a","#e07ab8");
        var cyan=R("#0d3a4a","#16708a","#2eaac4","#7fe2ef"); var blue=R("#0e1c4a","#1a3488","#2c58c0","#5a8ae8");
        var green=R("#123a14","#20602a","#34903c","#62c060"); var white=R("#7a8494","#a8b2c0","#d4dbe4","#f6f8fb");
        var night=R("#0c1230","#16204e","#22326e","#3a5292"); var black=R("#07060c","#141020","#241c3a","#3a2c5a");
        var rose=R("#6a1020","#a01c30","#d03448","#f06a7a"); var ivory=R("#8a7a5a","#b8a578","#e6d6a8","#fff6dc");
        var bronze=R("#4e2c12","#7a4a1e","#a8692c","#d6984f"); var lime=R("#2a4a10","#4a7a18","#78ae2a","#b0e050");
        switch(n){
            case "basic_charm":return new Kind{Top=bronze,Mid=orange,Low=bronze,Ink="#5a3416",Tier=1,Emblem=EDot};
            case "worn_traveler_charm":return new Kind{Top=bronze,Mid=ivory,Low=bronze,Ink="#6a4a22",Tier=1,Emblem=EClover};
            case "iron_vitality_charm":return new Kind{Top=steel,Mid=rose,Low=steel,Ink="#ffe0e0",Tier=1,Emblem=EHeart};
            case "breaking_edge_charm":return new Kind{Top=steel,Mid=red,Low=orange,Ink="#eef2f7",Tier=1,Emblem=ESword};
            case "conquest_charm":return new Kind{Top=rose,Mid=red,Low=bronze,Ink="#f2d062",Tier=2,Emblem=ECross};
            case "spellbreaker_charm":return new Kind{Top=cyan,Mid=violet,Low=violet,Ink="#d4fbff",Tier=1,Emblem=EBolt};
            case "arcane_pact_charm":return new Kind{Top=pink,Mid=violet,Low=pink,Ink="#f0d8ff",Tier=2,Emblem=EStar};
            case "hundred_forged_charm":return new Kind{Top=bronze,Mid=steel,Low=bronze,Ink="#fff1ad",Tier=1,Emblem=EShield};
            case "bulwark_pact_charm":return new Kind{Top=steel,Mid=steel,Low=night,Ink="#d4dbe4",Tier=2,Emblem=ETower};
            case "crystal_vestment_charm":return new Kind{Top=white,Mid=cyan,Low=cyan,Ink="#f4fcff",Tier=1,Emblem=ECrystal};
            case "astral_pact_charm":return new Kind{Top=night,Mid=blue,Low=night,Ink="#f5d667",Tier=2,Emblem=EMoon};
            case "thousand_blade_charm":return new Kind{Top=white,Mid=steel,Low=steel,Ink="#20242c",Tier=1,Emblem=EFan};
            case "swift_pact_charm":return new Kind{Top=cyan,Mid=white,Low=cyan,Ink="#16708a",Tier=2,Emblem=EWings};
            case "quickstring_charm":return new Kind{Top=steel,Mid=green,Low=green,Ink="#f4fcff",Tier=1,Emblem=EArrow};
            case "swiftstring_pact_charm":return new Kind{Top=lime,Mid=green,Low=lime,Ink="#f5d667",Tier=2,Emblem=EBow};
            case "law_of_arms_charm":return new Kind{Top=red,Mid=orange,Low=red,Ink="#fff1ad",Tier=3,Emblem=ESword};
            case "law_of_arcana_charm":return new Kind{Top=violet,Mid=violet,Low=pink,Ink="#fff6dc",Tier=3,Emblem=EEye};
            case "law_of_gale_charm":return new Kind{Top=white,Mid=cyan,Low=white,Ink="#f4fcff",Tier=3,Emblem=ESpiral};
            case "law_of_tailwind_charm":return new Kind{Top=green,Mid=lime,Low=cyan,Ink="#fff1ad",Tier=3,Emblem=EArrow};
            case "unyielding_body_charm":return new Kind{Top=steel,Mid=steel,Low=rose,Ink="#d03448",Tier=3,Emblem=EHeart};
            case "arcane_harmony_charm":return new Kind{Top=cyan,Mid=violet,Low=cyan,Ink="#7440b8",Tier=3,Emblem=EYinYang};
            case "omni_amplify_charm":return new Kind{Top=rose,Mid=violet,Low=gold,Ink="#fff1ad",Tier=4,Emblem=EPrism};
            case "fleeting_eternity_charm":return new Kind{Top=cyan,Mid=night,Low=night,Ink="#f5d667",Tier=4,Emblem=EHourglass};
            case "unbreakable_charm":return new Kind{Top=white,Mid=steel,Low=white,Ink="#7fe2ef",Tier=4,Emblem=EGem};
            case "spell_annul_charm":return new Kind{Top=violet,Mid=black,Low=violet,Ink="#a674e8",Tier=4,Emblem=EVoid};
            case "clear_world_charm":return new Kind{Top=ivory,Mid=cyan,Low=ivory,Ink="#d9aa37",Tier=5,Emblem=EWorld};
            case "inviolable_charm":return new Kind{Top=ivory,Mid=gold,Low=ivory,Ink="#8a5a14",Tier=5,Emblem=EHalo};
        }        throw new ArgumentException(n);
    }
}

// The same charms as scrolls: two thick rollers, the sheet between them laid on the diagonal, flecked, an emblem at its
// heart. Top roller, sheet and bottom roller take the kind's three colours; from truth on the rollers' ends are gold
// and a glint runs down the sheet.
public static partial class Designs {
    const double RX=0.95,RY=-0.31,DX=0.33,DY=0.94;            // along the rollers; down the sheet
    const double TX=12.2,TY=6.2,BX2=19,BY2=25.4,ROLL=8.2,SHEET=6.2,THICK=3.0;
    static double Dist(double x,double y,double ax,double ay,double bx,double by){double dx=bx-ax,dy=by-ay,l=dx*dx+dy*dy,s=Math.Max(0,Math.Min(1,((x-ax)*dx+(y-ay)*dy)/l));double px=ax+dx*s-x,py=ay+dy*s-y;return Math.Sqrt(px*px+py*py);}
    static bool InSheet(double x,double y){
        double[] q={TX-RX*SHEET,TY-RY*SHEET, TX+RX*SHEET,TY+RY*SHEET, BX2+RX*SHEET,BY2+RY*SHEET, BX2-RX*SHEET,BY2-RY*SHEET};
        return Cv.InPoly(q,x,y);
    }
    static void Roller(Cv c,double cx,double cy,string[] ramp,string knob){
        double ax=cx-RX*ROLL,ay=cy-RY*ROLL,bx=cx+RX*ROLL,by=cy+RY*ROLL;
        // lit along its upper side: the across coordinate (signed distance from the axis) picks the shade
        c.Fill((x,y)=>Dist(x,y,ax,ay,bx,by)<=THICK,(x,y)=>{double across=(x-cx)*(-RY)+(y-cy)*RX;return Cv.Ramp(ramp,0.62-across/(THICK*2.2));});
        c.Circle(ax,ay,THICK*0.75,(x,y)=>C(knob));c.Circle(bx,by,THICK*0.75,(x,y)=>C(knob));
    }
    /** which effect a charm's kind carries: fire, arcane, guard, ward, wind, shot */
    static string FxOf(string n){
        switch(n){
            case "basic_charm":case "breaking_edge_charm":case "conquest_charm":case "law_of_arms_charm":return "fire";
            case "spellbreaker_charm":case "arcane_pact_charm":case "law_of_arcana_charm":case "arcane_harmony_charm":case "spell_annul_charm":case "omni_amplify_charm":return "arcane";
            case "hundred_forged_charm":case "bulwark_pact_charm":case "iron_vitality_charm":case "unyielding_body_charm":case "unbreakable_charm":case "inviolable_charm":return "guard";
            case "crystal_vestment_charm":case "astral_pact_charm":case "fleeting_eternity_charm":case "clear_world_charm":return "ward";
            case "thousand_blade_charm":case "swift_pact_charm":case "law_of_gale_charm":case "worn_traveler_charm":return "wind";
            default:return "shot";
        }
    }
    public static Cv Scroll(string name,double t){
        var k=KindOf(name);var c=new Cv();string fx=FxOf(name);
        // the sheet: shaded across, faint lines of writing, flecks, and the shadow where each roller holds it
        c.Fill(InSheet,(x,y)=>{double across=(x-TX)*RX+(y-TY)*RY,down=(x-TX)*DX+(y-TY)*DY;var col=Cv.Ramp(k.Mid,0.62-across/(SHEET*2.4));
            if(down<4.2||down>16.5)col=Cv.Dark(col,0.72);
            else if(((int)Math.Floor(down/2.2))%2==0&&Math.Abs(across)<SHEET-2&&Cv.H((int)x,(int)y,31)>0.45)col=Cv.Mix(col,C(k.Mid[0]),0.35);
            if(Cv.H((int)x,(int)y,77)>0.9)col=Cv.Mix(col,C(k.Ink),0.6);return col;});
        // the emblem, ringed dark so it stands out on the sheet
        var em=new Cv();k.Emblem(em,k.Ink);
        string inkNow=k.Ink;
        if(k.Tier>=4){string[] rb={"#ff7a6a","#ffd24a","#7ade8a","#6ab0ff","#c88aff"};inkNow=rb[(int)(t*5)%5];em=new Cv();k.Emblem(em,inkNow);}
        if(fx=="ward"){double pulse=0.5+0.5*Math.Sin(t*Math.PI*2);for(int x=0;x<Cv.N;x++)for(int y=0;y<Cv.N;y++)if(em.P[x,y].A>0)em.P[x,y]=Cv.Mix(em.P[x,y],Color.White,pulse*0.45);}
        for(int x=0;x<Cv.N;x++)for(int y=0;y<Cv.N;y++){
            if(em.P[x,y].A>0){c.P[x,y]=em.P[x,y];continue;}
            bool near=false;for(int i=-1;i<=1&&!near;i++)for(int j=-1;j<=1&&!near;j++){int u=x+i,v=y+j;if(u>=0&&v>=0&&u<Cv.N&&v<Cv.N&&em.P[u,v].A>0)near=true;}
            if(near&&c.P[x,y].A>0)c.P[x,y]=Cv.Dark(c.P[x,y],0.74);
        }
        // the effect of its kind, on the sheet
        if(fx=="guard"){double g=-6+t*16;c.Fill((x,y)=>InSheet(x,y)&&Math.Abs(((x-16)+(y-16))*0.7-g)<1,(x,y)=>Color.FromArgb(170,255,255,255));}
        if(fx=="wind"){for(int s=0;s<2;s++){double p=(t+s*0.5)%1.0;double along=-8+p*16,down=6+s*7;double cx=TX+RX*along+DX*down,cy=TY+RY*along+DY*down;c.Line(cx-RX*2.5,cy-RY*2.5,cx+RX*2.5,cy+RY*2.5,1,(x,y)=>Color.FromArgb(190,240,250,255));}}
        if(fx=="shot"){int i=(int)(t*8)%8;if(i<3){double sx=12+i*3,sy=20-i*3;c.Px((int)sx,(int)sy,"#ffffff");}}
        // the rollers: lit along the top, a bright knob at each end
        string knob=k.Tier>=5?"#fff6dc":k.Tier>=3?"#f2d062":k.Top[1];
        Roller(c,TX,TY,k.Top,knob);Roller(c,BX2,BY2,k.Low,k.Tier>=3?knob:k.Low[1]);
        foreach(var e in new[]{new[]{TX-RX*ROLL,TY-RY*ROLL},new[]{TX+RX*ROLL,TY+RY*ROLL},new[]{BX2-RX*ROLL,BY2-RY*ROLL},new[]{BX2+RX*ROLL,BY2+RY*ROLL}})c.Px((int)(e[0]-0.5),(int)(e[1]-1.5),"#ffffff");
        c.Fill((x,y)=>Dist(x,y,TX-RX*ROLL*0.7,TY-RY*ROLL*0.7,TX+RX*ROLL*0.7,TY+RY*ROLL*0.7)<=THICK&&((x-TX)*(-RY)+(y-TY)*RX)<-THICK*0.45,(x,y)=>Color.FromArgb(140,255,255,255));
        c.Outline(k.Tier>=5?0.42:0.32);
        // fire: embers rise off the top; arcane: motes twinkle round the sheet
        if(fx=="fire"){for(int s=0;s<2;s++){double p=(t+s*0.5)%1.0;int ex=(int)(10+s*12+Math.Sin(p*6+s)*2),ey=(int)(10-p*9);string col=p<0.5?"#ffd24a":"#e8604a";if(ey>=0)c.Px(ex,ey,col);}}
        if(fx=="arcane"){int[][] spots={new[]{4,14},new[]{27,9},new[]{29,22},new[]{6,26}};for(int s=0;s<4;s++){double ph=(t*2+s*0.25)%1.0;if(ph<0.4)c.Px(spots[s][0],spots[s][1],ph<0.2?"#ffffff":"#d0aaff");}}
        // truth on: a glint runs down the sheet; supreme: rays at the corners
        if(k.Tier>=3){double gx=TX+(BX2-TX)*t,gy=TY+(BY2-TY)*t;
            c.Fill((x,y)=>InSheet(x,y)&&Math.Abs((x-gx)*DX+(y-gy)*DY)<1.1,(x,y)=>Color.FromArgb(k.Tier==3?120:180,255,245,200));}
        if(k.Tier>=5){int i=(int)(t*4)%4;int[][] sp={new[]{3,3},new[]{28,4},new[]{29,28},new[]{3,28}};c.Px(sp[i][0],sp[i][1],"#ffffff");c.Px(sp[(i+2)%4][0],sp[(i+2)%4][1],"#fff1ad");}
        return c;
    }}