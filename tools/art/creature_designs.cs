using System;

public static class CreatureDesigns {
    static double S(double t,double k=0){return Math.Sin((t+k)*Math.PI*2);}
    static double[] V(double x,double y,double z){return new[]{x,y,z};}

    // 毕方: a one-legged crane, blue-green with red bars, a white beak, flame at crest and tail
    public static Creature Bifang(){
        var c=new Creature("bifang",64,64){BoundsW=2.5,BoundsH=3};
        c.M("teal",new Mat("feather",false,"#0f3a40","#16555c","#20707a","#2e8e96","#4cb0b4","#8fd8d4"))
         .M("barred",new Mat("barred",false,"#0f3a40","#16555c","#20707a","#2e8e96","#d8402c","#ff7a4a"))
         .M("leg",new Mat("scale",false,"#5a1410","#7e1e16","#a02a1c","#c44028","#e06a3a"))
         .M("beak",new Mat("plain",false,"#8f8a80","#bdb6a8","#e2dccd","#fffaf0"))
         .M("flame",new Mat("flame",true,"#a0200c","#e04a14","#ff8a1e","#ffc23c","#fff09a"))
         .M("eye",new Mat("eye",true,"#1a0a04","#ffd23c"));
        c.B("root",null,0,0,0);
        c.B("leg","root",0,13,0).C("leg",0,1,0,1,12,1).C("leg",-1,0,-2,3,1,4);
        c.B("body","root",0,13,0).C("barred",-3,12,-5,6,6,10).C("teal",-2,11,-4,4,1,8);
        c.B("tail","body",0,16,5).C("flame",-2,14,5,4,3,4).C("flame",-1,15,9,2,2,3);
        c.B("wing_left","body",3,17,-3).C("teal",3,13,-4,1,5,9);
        c.B("wing_right","body",-3,17,-3).C("teal",-4,13,-4,1,5,9);
        c.B("neck","body",0,17,-4).C("teal",-1,17,-5,2,8,2);
        c.B("head","neck",0,25,-4).C("teal",-2,24,-7,3,3,4).C("beak",-1,25,-12,1,1,5).C("flame",-1,27,-6,1,3,3).C("eye",1,25,-6,1,1,1).C("eye",-3,25,-6,1,1,1);
        c.A("idle",2.0).T("neck","rotation",t=>V(S(t)*4,0,0)).T("tail","rotation",t=>V(S(t,0.25)*6,S(t)*5,0)).T("wing_left","rotation",t=>V(0,0,-2-S(t)*2)).T("wing_right","rotation",t=>V(0,0,2+S(t)*2));
        c.A("walk",0.6).T("root","position",t=>V(0,Math.Abs(S(t*0.5))*2.5,0)).T("leg","rotation",t=>V(S(t)*18,0,0)).T("neck","rotation",t=>V(S(t)*8,0,0)).T("wing_left","rotation",t=>V(0,0,-10-Math.Abs(S(t))*14)).T("wing_right","rotation",t=>V(0,0,10+Math.Abs(S(t))*14));
        c.A("attack",0.5).T("neck","rotation",t=>V(t<0.4?-40*t/0.4:-40+40*(t-0.4)/0.6,0,0)).T("head","rotation",t=>V(t<0.4?-20*t/0.4:-20+20*(t-0.4)/0.6,0,0));
        c.A("breathe",1.0).T("neck","rotation",t=>V(-25+S(t)*4,0,0)).T("wing_left","rotation",t=>V(0,0,-30)).T("wing_right","rotation",t=>V(0,0,30));
        return c;
    }
    // 雷萤: a black firefly with a cyan, lightning-bright tail
    public static Creature Firefly(){
        var c=new Creature("thunder_firefly",32,32){BoundsW=0.6,BoundsH=0.6};
        c.M("shell",new Mat("plain",false,"#0a0c14","#141a28","#1e2a40","#2c3e5a"))
         .M("glow",new Mat("glass",true,"#1f88a3","#38b9d1","#7fe2ef","#d4fbff","#ffffff"))
         .M("wing",new Mat("plain",false,"#9ab8c8","#c8dce6","#eef6fa"){Alpha=0.65})
         .M("eye",new Mat("eye",true,"#0a0c14","#7fe2ef"));
        c.B("root",null,0,0,0);
        c.B("body","root",0,2,0).C("shell",-1,1,-2,2,2,3).C("eye",0,2,-3,1,1,1).C("eye",-1,2,-3,1,1,1);
        c.B("tail","body",0,2,1).C("glow",-1,1,1,2,2,2);
        c.B("wing_left","body",0,3,-1).C("wing",0,3,-1,3,1,2);
        c.B("wing_right","body",0,3,-1).C("wing",-3,3,-1,3,1,2);
        c.A("fly",0.2).T("wing_left","rotation",t=>V(0,0,-S(t)*45)).T("wing_right","rotation",t=>V(0,0,S(t)*45)).T("root","position",t=>V(0,S(t)*0.3,0));
        c.A("glow",1.6).T("tail","scale",t=>{double s=1+0.25*Math.Max(0,S(t));return V(s,s,s);});
        return c;
    }
    // 鸣金雀: a white-and-gold sparrow with a little bell at its throat
    public static Creature Sparrow(){
        var c=new Creature("golden_bell_sparrow",32,32){BoundsW=0.8,BoundsH=0.8};
        c.M("white",new Mat("feather",false,"#8f8a80","#bdb6a8","#e2dccd","#f6f1e4","#ffffff"))
         .M("gold",new Mat("feather",false,"#5e3a0c","#875917","#b38223","#d9aa37","#f2d062","#fff1ad"))
         .M("bell",new Mat("plain",true,"#875917","#d9aa37","#fff1ad"))
         .M("leg",new Mat("plain",false,"#5e3a0c","#875917","#b38223"))
         .M("eye",new Mat("eye",false,"#0a0a0a","#ffffff"));
        c.B("root",null,0,0,0);
        c.B("leg_left","root",1,2,0).C("leg",0,0,0,1,2,1);
        c.B("leg_right","root",-1,2,0).C("leg",-2,0,0,1,2,1);
        c.B("body","root",0,4,0).C("white",-2,2,-2,4,4,5);
        c.B("head","body",0,6,-2).C("white",-2,5,-5,3,3,3).C("gold",-1,6,-7,1,1,2).C("eye",1,6,-4,1,1,1).C("eye",-3,6,-4,1,1,1).C("bell",-1,4,-4,1,1,1);
        c.B("tail","body",0,5,3).C("gold",-1,5,3,3,1,4);
        c.B("wing_left","body",2,6,-1).C("gold",2,3,-2,1,3,5);
        c.B("wing_right","body",-2,6,-1).C("gold",-3,3,-2,1,3,5);
        c.A("idle",2.4).T("head","rotation",t=>V(0,S(t)*12,S(t,0.3)*8)).T("tail","rotation",t=>V(S(t*2)*8,0,0));
        c.A("walk",0.4).T("root","position",t=>V(0,Math.Abs(S(t*0.5))*1.2,0)).T("leg_left","rotation",t=>V(S(t)*25,0,0)).T("leg_right","rotation",t=>V(-S(t)*25,0,0));
        c.A("fly",0.3).T("wing_left","rotation",t=>V(0,0,-S(t)*60-20)).T("wing_right","rotation",t=>V(0,0,S(t)*60+20)).T("tail","rotation",t=>V(-10,0,0)).T("leg_left","rotation",t=>V(40,0,0)).T("leg_right","rotation",t=>V(40,0,0));
        return c;
    }
    // 冥灯鱼: a dark fish of the cold deep, a lamp hung before its mouth
    public static Creature Lampfish(){
        var c=new Creature("netherlamp_fish",32,32){BoundsW=1,BoundsH=1};
        c.M("skin",new Mat("scale",false,"#060a14","#0c1426","#14223a","#1e3254","#2c4670"))
         .M("fin",new Mat("plain",false,"#0c1426","#1e3254","#3a6fa0"){Alpha=0.85})
         .M("stalk",new Mat("plain",false,"#14223a","#1e3254","#2c4670"))
         .M("lamp",new Mat("glass",true,"#1f6a8a","#38b9d1","#7fe2ef","#d4fbff","#ffffff"))
         .M("eye",new Mat("eye",true,"#060a14","#7fe2ef"));
        c.B("root",null,0,0,0);
        c.B("body","root",0,2,0).C("skin",-2,0,-4,4,4,7).C("eye",2,2,-3,1,1,1).C("eye",-3,2,-3,1,1,1);
        c.B("tail","body",0,2,3).C("fin",-1,0,3,1,4,3);
        c.B("fin_top","body",0,4,0).C("fin",0,4,-1,1,2,3);
        c.B("lure","body",0,4,-3).C("stalk",0,4,-4,1,3,1).C("stalk",0,6,-6,1,1,2).C("lamp",-1,4,-8,2,2,2);
        c.A("swim",0.8).T("tail","rotation",t=>V(0,S(t)*30,0)).T("body","rotation",t=>V(0,S(t,0.25)*5,0)).T("lure","rotation",t=>V(S(t)*6,0,0));
        c.A("glow",2.0).T("lure","scale",t=>{double s=1+0.12*S(t);return V(s,s,s);});
        c.A("flop",0.5).T("body","rotation",t=>V(0,0,90)).T("tail","rotation",t=>V(0,S(t)*40,0));
        return c;
    }
}
