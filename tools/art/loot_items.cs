using System;
using System.Drawing;

// The overworld's chest finds (ItemLootModifier), in the charms' chunky style: a straw substitute doll, a torn recipe
// page, a glowstone earring and an apprentice's parcel. (The two charms among the finds are scrolls, in capsule_designs.cs.)
public static partial class Designs {
    static string[] Straw={"#5a3e12","#8a6a24","#b8963a","#dcc060","#f2e090"};
    static string[] Kraft={"#4a2e14","#6e4622","#946234","#b88250","#d6a878"};
    static string[] Glowstone={"#7a4a10","#b87418","#e8a830","#ffd060","#fff0a8","#ffffff"};

    static void DrawLoot(Cv c,string name,double t){
        switch(name){
        case "loot_substitute_doll":
            // a doll of bound straw: a round head with stitched eyes, arms out, red cord at its neck and waist
            c.Circle(16,8,5.4,Lit(Straw,16,8,5));
            c.Poly(new double[]{12,13, 20,13, 22,23, 10,23},Lit(Straw,16,18,6));
            c.Poly(new double[]{5,14, 12,14, 12,17, 5,17},Lit(Straw,9,15,4));c.Poly(new double[]{20,14, 27,14, 27,17, 20,17},Lit(Straw,24,15,4));
            c.Poly(new double[]{11,23, 15,23, 14,30, 10,30},Lit(Straw,12,26,4));c.Poly(new double[]{17,23, 21,23, 22,30, 18,30},Lit(Straw,20,26,4));
            for(int i=0;i<4;i++){c.Line(7+i*1.5,14,7+i*1.5,17,0.6,C("#8a6a24"));c.Line(22+i*1.5,14,22+i*1.5,17,0.6,C("#8a6a24"));}
            c.Line(11,13.5,21,13.5,1.1,C("#c0302a"));c.Line(10.5,21,21.5,21,1.1,C("#c0302a"));c.Line(20,21,23,24,0.9,C("#c0302a"));
            c.Line(13,6.5,15,8.5,0.7,C("#3a2408"));c.Line(15,6.5,13,8.5,0.7,C("#3a2408"));c.Line(17,6.5,19,8.5,0.7,C("#3a2408"));c.Line(19,6.5,17,8.5,0.7,C("#3a2408"));
            c.Outline();break;
        case "loot_torn_recipe_page":
            // a sheet torn across, the bronze triangle of a basic recipe on it, its lower corner burnt
            double[] page={6,4, 25,4, 26,10, 23,13, 26,17, 24,21, 26,26, 20,28, 16,25, 12,29, 7,27, 5,20, 7,14, 5,9};
            c.Poly(page,(x,y)=>Cv.Ramp(Paper,0.75-((x-16)+(y-16))/70));
            c.Line(16,8,11,17,1,C("#5a3416"));c.Line(16,8,21,17,1,C("#5a3416"));c.Line(11,17,21,17,1,C("#5a3416"));
            c.Line(9,21,18,21,0.8,C("#7a5a3a"));c.Line(9,24,15,24,0.8,C("#7a5a3a"));
            c.Fill((x,y)=>Cv.InPoly(page,x,y)&&(x-22)*(x-22)+(y-26)*(y-26)<20,(x,y)=>Cv.Mix(C("#3a2410"),C("#8a5a2a"),((x-22)*(x-22)+(y-26)*(y-26))/20.0));
            c.Outline();break;
        case "loot_glowstone_earring":
            // a gold hook, a short chain, and a faceted drop of glowstone
            Cv.Soft=true;
            try{
                c.Ring(16,6,3.6,4.8,Lit(Gold,15,5,3));
                c.Line(16,10,16,13,1.2,C("#b38223"));c.Circle(16,13.5,1.4,Lit(Gold,16,13,1));
                c.Poly(new double[]{16,15, 22,20, 20,27, 16,30, 12,27, 10,20},(x,y)=>Cv.Ramp(Glowstone,0.95-((x-12)+(y-17))/26));
                c.Line(16,15,16,30,0.7,C("#ffe08a"));c.Line(10,20,22,20,0.7,C("#b87418"));
                c.Outline(0.4);
            }finally{Cv.Soft=false;}
            c.Px(14,19,"#ffffff");c.Px(13,21,"#fff0a8");break;
        case "loot_apprentice_parcel":
            // a parcel of brown paper seen from above and in front, tied with twine in a bow, a wax seal of the triangle
            c.Poly(new double[]{5,11, 16,6, 27,11, 16,16},Lit(Kraft,16,10,8,0.15));
            c.Poly(new double[]{5,11, 16,16, 16,28, 5,23},Lit(Kraft,10,19,8,-0.05));
            c.Poly(new double[]{16,16, 27,11, 27,23, 16,28},Lit(Kraft,22,19,8,-0.25));
            c.Line(10.5,8.5,21.5,13.5,1,C("#efe2c0"));c.Line(21.5,8.5,10.5,13.5,1,C("#efe2c0"));
            c.Line(10.5,13.5,10.5,25.5,1,C("#efe2c0"));c.Line(21.5,13.5,21.5,25.5,1,C("#efe2c0"));
            c.Circle(14,9.5,1.8,(x,y)=>C("#efe2c0"));c.Circle(18,9.5,1.8,(x,y)=>C("#efe2c0"));
            c.Circle(21.5,19,3,Lit(Crimson,21,18,3));c.Line(21.5,17,20,20,0.6,C("#f8a07a"));c.Line(21.5,17,23,20,0.6,C("#f8a07a"));c.Line(20,20,23,20,0.6,C("#f8a07a"));
            c.Outline();break;
        }
    }
}
