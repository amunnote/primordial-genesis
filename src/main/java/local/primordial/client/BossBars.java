package local.primordial.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import local.primordial.Primordial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

/**
 * Each boss's own bar, at the game's own pixels: the boss's own things all about it (the Vermilion Bird's flames, the
 * Black Tortoise's waves, Pride's treasury, Gluttony's teeth, Sloth's sword...), its portrait in a ring over the bar's
 * middle (its own model, drawn live), the bar in its colour, a trail where health was just lost, a red pulse once it is enraged, and under it what its fight turns on (MenuDropNetwork
 * BossBarState, from SinBoss.barLook):
 * <ul>
 * <li>pips: the White Tiger's killing intent, Wrath's rage;</li>
 * <li>a meter: the Azure Dragon's wind (or how long it lies spent), the Black Tortoise's cracking shell, the Vermilion
 *     Bird's egg;</li>
 * <li>stages: the First Sage's four, the Primordial Lord's three (and the sin it is borrowing, in its colour);</li>
 * <li>a count: Greed's hoard;</li>
 * <li>a state named: Envy unseen, Lust charming, Gluttony gorging, Sloth dozing.</li>
 * </ul>
 * Sheets: textures/gui/bossbar/&lt;boss&gt;.png (tools/art/bossbars.ps1).
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class BossBars {
    public record State(String boss,int entity,int flags,int count,float meter,int accent) {
        public boolean enraged() {return (flags&1)!=0;}
        boolean a() {return (flags&2)!=0;}
        boolean b() {return (flags&4)!=0;}
    }
    /** how each boss is framed in its portrait: the height of its head above its feet and how much of it shows, in blocks; how far it turns */
    record Framing(float head,float span,float turn) {}
    // from each model's head bone (its cubes' middle and size, grown by the renderer's model scale), with a quarter more round it
    private static final Map<String,Framing> FRAMING=Map.ofEntries(
        Map.entry("pride_lord",new Framing(2.78F,1.17F,25)),Map.entry("wrath_lord",new Framing(3.15F,1.3F,25)),
        Map.entry("envy_lord",new Framing(1.99F,0.85F,25)),Map.entry("greed_lord",new Framing(2.55F,0.8F,25)),
        Map.entry("lust_lord",new Framing(1.84F,0.8F,25)),Map.entry("gluttony_lord",new Framing(2.47F,0.85F,25)),
        Map.entry("sloth_lord",new Framing(2.4F,0.95F,25)),Map.entry("primordial_lord",new Framing(4.06F,1.3F,25)),
        Map.entry("first_sage",new Framing(2.5F,2.0F,25)),Map.entry("qinglong",new Framing(4.1F,5.2F,0)),
        Map.entry("baihu",new Framing(4.2F,4.0F,0)),Map.entry("zhuque",new Framing(5.05F,2.9F,20)),
        Map.entry("xuanwu",new Framing(2.4F,3.2F,0)));
    private static final Map<UUID,State> STATES=new HashMap<>();
    /** the health each bar showed a moment ago, falling after it: the trail of what was just lost */
    private static final Map<UUID,Float> TRAIL=new HashMap<>();
    /** stand-ins for a boss too far off to be in the client's world, drawn in its portrait */
    private static final Map<String,LivingEntity> STAND_INS=new HashMap<>();
    // on screen, at the game's own pixels: the bar 182x5 at (x,y), its surround 224x26 round it, the portrait's ring 26 across over its middle
    private static final int BAR_W=182,BAR_H=5,ROW=10,TEX_W=256,TEX_H=64,ABOVE=26;
    private BossBars() {}

    public static void update(UUID bar,String boss,int entity,int flags,int count,float meter,int accent) {STATES.put(bar,new State(boss,entity,flags,count,meter,accent));}
    public static State state(UUID bar) {return STATES.get(bar);}
    @SubscribeEvent public static void leave(ClientPlayerNetworkEvent.LoggingOut event) {STATES.clear();TRAIL.clear();STAND_INS.clear();}

    @SubscribeEvent public static void draw(CustomizeGuiOverlayEvent.BossEventProgress event) {
        var bar=event.getBossEvent();var state=STATES.get(bar.getId());
        if(state==null)return;
        event.setCanceled(true);
        var row=row(state);
        event.setIncrement(ABOVE+24+(row!=null?ROW:0));
        render(event.getGuiGraphics(),event.getX(),event.getY()+ABOVE-9,bar.getName(),bar.getProgress(),bar.getId(),state,row);
    }

    static void render(GuiGraphics g,int x,int y,Component name,float progress,UUID id,State state,Component row) {
        var sheet=new ResourceLocation(Primordial.ID,"textures/gui/bossbar/"+state.boss()+".png");
        var mc=Minecraft.getInstance();float time=(mc.level!=null?mc.level.getGameTime():0)+mc.getFrameTime();
        var boss=boss(state);
        RenderSystem.enableBlend();
        // the surround, the boss's own things about the bar
        g.blit(sheet,x-21,y-11,0,0,224,26,TEX_W,TEX_H);
        // the bar: what is lost, the trail of what was just lost, the health
        g.blit(sheet,x,y,0,34,BAR_W,BAR_H,TEX_W,TEX_H);
        progress=Mth.clamp(progress,0,1);
        float trail=Math.max(progress,TRAIL.getOrDefault(id,progress));
        trail=progress+(trail-progress)*0.96F;TRAIL.put(id,trail);
        int filled=Math.round(progress*BAR_W),trailing=Math.round(trail*BAR_W);
        if(trailing>filled)g.fill(x+filled,y,x+trailing,y+BAR_H,0xC0FFF4E0);
        float fade=state.boss().equals("envy_lord")&&state.a()?0.35F+0.15F*Mth.sin(time*0.6F):1F;   // Envy unseen: its health hard to read
        RenderSystem.setShaderColor(1F,1F,1F,fade);
        if(filled>0)g.blit(sheet,x,y,(int)(time*flow(state.boss()))%32,28,filled,BAR_H,TEX_W,TEX_H);   // its pattern sliding along
        RenderSystem.setShaderColor(1F,1F,1F,1F);
        if(filled>4){int sweep=(int)((time*1.6F)%(filled+16))-8;   // a gleam running along it
            for(int i=0;i<4;i++){int sx=x+sweep+i;if(sx>=x&&sx<x+filled)g.fill(sx,y,sx+1,y+BAR_H,((i==1||i==2?60:30)<<24)|0xFFFFFF);}}
        if(state.boss().equals("xuanwu")&&state.a())for(int i=0;i<BAR_W;i+=4)g.fill(x+i,y,x+i+1,y+BAR_H,0x7014303C);   // a shell over it while it hides
        if(state.boss().equals("first_sage")&&state.a()){int a=(int)(90+50*Mth.sin(time*0.3F));g.fill(x,y,x+BAR_W,y+1,(a<<24)|0xF2C94C);g.fill(x,y+BAR_H-1,x+BAR_W,y+BAR_H,(a<<24)|0xF2C94C);}
        // the portrait over the bar's middle: what lies behind it, the boss itself, its ring
        float cx=x+BAR_W/2F,cy=y-13;
        if(state.boss().equals("gluttony_lord")&&state.a()){cx+=Mth.sin(time*2.1F)*1.2F;cy+=Mth.cos(time*2.7F)*1F;}
        int rx=Math.round(cx-13),ry=Math.round(cy-13);
        g.blit(sheet,rx,ry,28,40,26,26,TEX_W,TEX_H);
        if(boss!=null)portrait(g,boss,state.boss(),cx,cy);
        g.blit(sheet,rx,ry,0,40,26,26,TEX_W,TEX_H);
        if(state.boss().equals("lust_lord")&&state.a()){RenderSystem.setShaderColor(1F,0.5F,0.8F,0.35F+0.3F*Mth.sin(time*0.5F));g.blit(sheet,rx,ry,0,40,26,26,TEX_W,TEX_H);RenderSystem.setShaderColor(1F,1F,1F,1F);}
        if(state.boss().equals("sloth_lord")&&state.a()){int bob=(int)(time*0.15F)%3;g.drawString(mc.font,"z",rx+22,ry-4-bob,0xD4D8F4,true);}
        if(state.accent()!=0)g.fill(rx+8,ry+24,rx+18,ry+25,0xFF000000|state.accent());   // the sin the Primordial Lord is borrowing
        // enraged: the surround and the ring pulse red
        if(state.enraged()){
            RenderSystem.setShaderColor(1F,0.2F,0.15F,0.3F+0.25F*Mth.sin(time*0.25F));
            g.blit(sheet,x-21,y-11,0,0,224,26,TEX_W,TEX_H);g.blit(sheet,rx,ry,0,40,26,26,TEX_W,TEX_H);
            RenderSystem.setShaderColor(1F,1F,1F,1F);
        }
        // the name under it
        g.drawString(mc.font,name,x+BAR_W/2-mc.font.width(name)/2,y+13,0xFFFFFF,true);
        // what its fight turns on
        if(row!=null)gauge(g,x,y+24,state,row,time);
        RenderSystem.disableBlend();
    }

    /** the boss this bar is for: itself when the client has it, else a stand-in of its kind */
    static LivingEntity boss(State state) {
        var level=Minecraft.getInstance().level;if(level==null)return null;
        if(level.getEntity(state.entity()) instanceof LivingEntity living)return living;
        var standIn=STAND_INS.get(state.boss());
        if(standIn!=null&&standIn.level()==level)return standIn;
        var type=EntityType.byString(Primordial.ID+":"+state.boss()).orElse(null);
        var entity=type==null?null:type.create(level);
        if(entity instanceof local.primordial.boss.SinBoss boss)boss.portrait();
        if(!(entity instanceof LivingEntity living))return null;
        STAND_INS.put(state.boss(),living);
        return living;
    }
    /** the boss's head in its ring, turned toward the bar, cut to the ring's opening */
    static void portrait(GuiGraphics g,LivingEntity boss,String id,float cx,float cy) {
        var framing=FRAMING.getOrDefault(id,new Framing(boss.getBbHeight()*0.85F,boss.getBbHeight()*0.5F,25));
        int scale=Math.max(1,Math.round(15/framing.span()));
        g.enableScissor(Math.round(cx)-7,Math.round(cy)-7,Math.round(cx)+7,Math.round(cy)+7);
        float body=boss.yBodyRot,bodyO=boss.yBodyRotO,yaw=boss.getYRot(),yawO=boss.yRotO,pitch=boss.getXRot(),pitchO=boss.xRotO,head=boss.yHeadRot,headO=boss.yHeadRotO;
        float facing=180+framing.turn();
        boss.yBodyRot=facing;boss.yBodyRotO=facing;boss.setYRot(facing);boss.yRotO=facing;boss.setXRot(0);boss.xRotO=0;boss.yHeadRot=facing;boss.yHeadRotO=facing;
        var pose=new Quaternionf().rotateZ((float)Math.PI);var camera=new Quaternionf().rotateX((float)Math.toRadians(-8));pose.mul(camera);
        try{InventoryScreen.renderEntityInInventory(g,Math.round(cx),Math.round(cy+framing.head()*scale),scale,pose,camera,boss);}
        catch(RuntimeException e){/* a boss that will not draw out of its world: the ring stays empty */}
        finally{boss.yBodyRot=body;boss.yBodyRotO=bodyO;boss.setYRot(yaw);boss.yRotO=yawO;boss.setXRot(pitch);boss.xRotO=pitchO;boss.yHeadRot=head;boss.yHeadRotO=headO;}
        g.disableScissor();
    }

    /** how fast each boss's fill slides along, in the sheet's pixels a tick: fire races, a shell barely moves */
    static float flow(String boss) {
        return switch(boss){case "zhuque"->1.1F;case "wrath_lord"->0.6F;case "primordial_lord"->0.7F;case "qinglong","pride_lord"->0.45F;
            case "baihu","gluttony_lord"->0.35F;case "lust_lord","first_sage","greed_lord","envy_lord"->0.3F;case "sloth_lord"->0.2F;default->0.15F;};
    }

    /** the words under the bar, or null for none */
    static Component row(State state) {
        String key="bossbar.primordial.";
        switch(state.boss()){
            case "baihu":return Component.translatable(key+(state.a()?"panting":"killing_intent"));
            case "wrath_lord":return Component.translatable(key+"rage");
            case "greed_lord":return Component.translatable(key+"hoard",state.count());
            case "primordial_lord":return Component.translatable(key+(state.accent()!=0?"phase_borrowing":"phase"),state.count());
            case "first_sage":return Component.translatable(key+(state.a()?"coagula":"stage_"+Mth.clamp(state.count(),1,4)));
            case "qinglong":return Component.translatable(key+(state.a()?"spent":"wind"));
            case "xuanwu":return Component.translatable(key+(state.b()?"broken":state.a()?"withdrawn":"cracks"));
            case "zhuque":return state.a()?Component.translatable(key+"rebirth"):null;
            case "envy_lord":return state.a()?Component.translatable(key+"vanished"):null;
            case "lust_lord":return state.a()?Component.translatable(key+"charming"):null;
            case "gluttony_lord":return state.a()?Component.translatable(key+"gorging"):null;
            case "sloth_lord":return state.a()?Component.translatable(key+"dozing"):null;
            default:return null;
        }
    }
    static void gauge(GuiGraphics g,int x,int y,State state,Component label,float time) {
        var font=Minecraft.getInstance().font;
        int colour=switch(state.boss()){
            case "baihu","wrath_lord"->0xFFE63A2E;case "qinglong"->state.a()?0xFFFFA040:0xFFBFF0FF;case "xuanwu"->state.b()?0xFFFF5040:0xFF9AE8F8;
            case "zhuque"->0xFFFFC84A;case "greed_lord"->0xFFF5C040;case "first_sage"->0xFFF2C94C;case "primordial_lord"->0xFFFFD860;default->0xFFE0E0E0;};
        // the label small, three quarters of the game's letters; a gauge three pixels tall beside it
        float scale=0.75F;int labelW=Math.round(font.width(label)*scale);
        int gaugeW=switch(state.boss()){case "baihu","wrath_lord","qinglong","xuanwu","zhuque","first_sage","primordial_lord"->90;default->0;};
        int total=labelW+(gaugeW>0?4+gaugeW:0),lx=x+BAR_W/2-total/2;
        g.pose().pushPose();g.pose().translate(lx,y,0);g.pose().scale(scale,scale,1);
        g.drawString(font,label,0,0,colour&0xFFFFFF,true);
        g.pose().popPose();
        int gx=lx+labelW+4,gy=y+2;
        switch(state.boss()){
            case "baihu","wrath_lord"->{   // pips
                int max=state.boss().equals("baihu")?(state.enraged()?20:10):20,n=Math.min(max,state.count());
                int w=Math.max(2,(gaugeW-(max-1))/max);
                boolean flash=state.boss().equals("baihu")&&state.a()&&((int)(time*0.4F)&1)==0;
                for(int i=0;i<max;i++){int px=gx+i*(w+1);g.fill(px,gy-1,px+w,gy+3,0xFF000000);g.fill(px,gy-1,px+w,gy+2,i<n?(flash?0xFFFFFFFF:colour):0xFF3A2020);}
            }
            case "first_sage","primordial_lord"->{   // stages
                int stages=state.boss().equals("first_sage")?4:3,w=(gaugeW-(stages-1)*2)/stages;
                int[] tints=state.boss().equals("first_sage")?new int[]{0xFF6A6A7A,0xFFF4F4F0,0xFFF2C94C,0xFFD8281E}:new int[]{0xFFEEF0FF,0xFFFFD860,0xFFFFFFFF};
                for(int i=0;i<stages;i++){int px=gx+i*(w+2);g.fill(px-1,gy-2,px+w+1,gy+3,0xFF000000);g.fill(px,gy-1,px+w,gy+2,i<state.count()?tints[i]:0xFF303038);}
            }
            default->{ if(gaugeW>0&&state.meter()>=0){   // a meter
                g.fill(gx-1,gy-2,gx+gaugeW+1,gy+3,0xFF000000);g.fill(gx,gy-1,gx+gaugeW,gy+2,0xFF202428);
                int w=Math.round(Mth.clamp(state.meter(),0,1)*gaugeW);g.fill(gx,gy-1,gx+w,gy+2,colour);g.fill(gx,gy-1,gx+w,gy,0x60FFFFFF|colour);
            }}
        }
    }
}
