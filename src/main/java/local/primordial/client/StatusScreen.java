package local.primordial.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import local.primordial.Primordial;
import local.primordial.affix.AffixStatuses;
import local.primordial.network.MenuDropNetwork;
import local.primordial.network.StatusValuesCache;
import local.primordial.sage.SageHeart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Read-only status page opened from the inventory side tab: the player on the left with health and armor,
 * and on the right a scrolling list of key stats, other changed attributes and every active status.
 */
public final class StatusScreen extends Screen {
    private static final int W=300,H=206,ROW=11,STATUS_ROW=22;
    private static final int GOLD=0xFFE6B65A,TEXT=0xFFE8E8EB,DIM=0xFF9A9CA8;
    private static ResourceLocation icon(String name){return new ResourceLocation(Primordial.ID,"textures/gui/status/"+name+".png");}
    private final Screen parent;
    private int scroll,contentHeight;
    public StatusScreen(Screen parent){super(Component.translatable("screen.primordial.status"));this.parent=parent;}
    private int left(){return (width-W)/2;}
    private int top(){return (height-H)/2;}
    private int ticks;
    /** the tooltip under the mouse this frame, drawn last so it sits on top */
    private List<Component> tooltip;
    /** attribute values from the server (many attributes are not synced to the client); refreshed every second */
    private static double v(Player player,Attribute attribute){return StatusValuesCache.value(attribute,player.getAttributeValue(attribute));}
    @Override public void tick(){if(++ticks%20==0)MenuDropNetwork.requestStatus();}
    @Override protected void init(){
        if(ticks==0){StatusValuesCache.clear();MenuDropNetwork.requestStatus();}
        addRenderableWidget(Button.builder(Component.literal("×"),button->onClose()).bounds(left()+W-20,top()+5,14,14).build());
    }
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean mouseScrolled(double x,double y,double delta){
        scroll=Mth.clamp(scroll-(int)Math.signum(delta)*ROW*2,0,Math.max(0,contentHeight-listHeight()));
        return true;
    }
    private int listHeight(){return H-34;}

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);
        var player=Minecraft.getInstance().player;
        int x=left(),y=top();
        // panel: dark body, hairline frame, gold accents at the top
        g.fill(x-1,y-1,x+W+1,y+H+1,0xFF05060A);
        g.fillGradient(x,y,x+W,y+H,0xF41A1C26,0xF4101118);
        g.fill(x,y,x+W,y+1,0xFF3A3F55);g.fill(x,y+H-1,x+W,y+H,0xFF3A3F55);g.fill(x,y,x+1,y+H,0xFF3A3F55);g.fill(x+W-1,y,x+W,y+H,0xFF3A3F55);
        g.fill(x+6,y+22,x+W-6,y+23,0x806A5A30);g.fill(x+6,y+22,x+60,y+23,GOLD);
        g.drawString(font,title,x+9,y+8,GOLD,false);
        if(player!=null){
            var name=player.getName();g.drawString(font,name,x+W-26-font.width(name),y+8,DIM,false);
            tooltip=null;
            renderPlayerPane(g,player,x+7,y+28,mouseX,mouseY);
            renderList(g,player,x+98,y+28,W-98-7,listHeight(),mouseX,mouseY);
        }
        super.render(g,mouseX,mouseY,partial);
        if(tooltip!=null)g.renderComponentTooltip(font,tooltip,mouseX,mouseY);
    }

    private void renderPlayerPane(GuiGraphics g,Player player,int x,int y,int mouseX,int mouseY){
        int w=86,h=listHeight();
        g.fill(x,y,x+w,y+h,0xFF0B0C12);g.fill(x,y,x+w,y+1,0xFF2A2E3E);g.fill(x,y+h-1,x+w,y+h,0xFF2A2E3E);
        int cx=x+w/2,feet=y+104;
        g.enableScissor(x+1,y+1,x+w-1,y+h-1);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g,cx,feet,40,cx-mouseX,feet-60-mouseY,player);
        g.disableScissor();
        // health bar
        int barY=y+112;float health=player.getHealth(),max=Math.max(1,player.getMaxHealth());
        g.fill(x+6,barY,x+w-6,barY+7,0xFF2A0E12);
        g.fill(x+6,barY,x+6+(int)((w-12)*Mth.clamp(health/max,0,1)),barY+7,0xFFD03040);
        g.fill(x+6,barY,x+6+(int)((w-12)*Mth.clamp(health/max,0,1)),barY+2,0xFFF07080);
        center(g,number(health)+" / "+number(max),cx,barY+10,TEXT);
        if(mouseX>=x+4&&mouseX<x+w-4&&mouseY>=barY-1&&mouseY<barY+19)tooltip=attributeTip(player,Attributes.MAX_HEALTH);
        // armor and toughness
        center(g,t("armor",number(v(player,Attributes.ARMOR)),number(v(player,Attributes.ARMOR_TOUGHNESS))).getString(),cx,barY+24,DIM);
        if(mouseX>=x+4&&mouseX<x+w-4&&mouseY>=barY+22&&mouseY<barY+33){
            var tip=attributeTip(player,Attributes.ARMOR);tip.add(Component.empty());tip.addAll(attributeTip(player,Attributes.ARMOR_TOUGHNESS));tooltip=tip;}
        center(g,t("level",player.experienceLevel).getString(),cx,barY+36,0xFF8FD460);
    }
    private void center(GuiGraphics g,String text,int cx,int y,int color){g.drawString(font,text,cx-font.width(text)/2,y,color,false);}

    private interface Row{int height();void draw(GuiGraphics g,int x,int y,int w);default List<Component> tip(){return null;}}
    private void renderList(GuiGraphics g,Player player,int x,int y,int w,int h,int mouseX,int mouseY){
        var rows=rows(player);
        contentHeight=0;for(var r:rows)contentHeight+=r.height();
        scroll=Mth.clamp(scroll,0,Math.max(0,contentHeight-h));
        g.enableScissor(x,y,x+w,y+h);
        int cy=y-scroll;
        boolean inside=mouseX>=x&&mouseX<x+w-6&&mouseY>=y&&mouseY<y+h;
        for(var r:rows){
            if(cy+r.height()>=y&&cy<=y+h)r.draw(g,x,cy,w-6);
            if(inside&&mouseY>=cy&&mouseY<cy+r.height()&&r.tip()!=null)tooltip=r.tip();
            cy+=r.height();
        }
        g.disableScissor();
        if(contentHeight>h){  // scroll bar
            int knob=Math.max(14,h*h/contentHeight),ky=y+(h-knob)*scroll/Math.max(1,contentHeight-h);
            g.fill(x+w-3,y,x+w-1,y+h,0xFF20232E);g.fill(x+w-3,ky,x+w-1,ky+knob,0xFF6A6E85);
        }
    }

    /** status page text: screen.primordial.status.<key> */
    private static net.minecraft.network.chat.MutableComponent t(String key,Object... args){return Component.translatable("screen.primordial.status."+key,args);}
    private Row section(Component label){
        return new Row(){public int height(){return 15;}
            public void draw(GuiGraphics g,int x,int y,int w){
                g.drawString(font,label,x,y+4,GOLD,false);
                int lx=x+font.width(label)+5;g.fill(lx,y+8,x+w,y+9,0x506A5A30);}};
    }
    private Row stat(Component label,String value,int valueColor){return stat(label,value,valueColor,null);}
    private Row stat(Component label,String value,int valueColor,List<Component> tip){
        return new Row(){public int height(){return ROW;}
            public void draw(GuiGraphics g,int x,int y,int w){
                g.drawString(font,label,x+2,y+1,TEXT,false);
                g.drawString(font,value,x+w-font.width(value),y+1,valueColor,false);}
            public List<Component> tip(){return tip;}};
    }
    /** a stat row for an attribute: hovering it tells what it is made of */
    private Row stat(Player player,Attribute attribute,Component label,String value,int valueColor){return stat(label,value,valueColor,attributeTip(player,attribute));}
    /** the attribute's value, its base, and each modifier on it with its source */
    private List<Component> attributeTip(Player player,Attribute attribute){
        var lines=new ArrayList<Component>();
        lines.add(Component.translatable(attribute.getDescriptionId()).withStyle(s->s.withColor(GOLD)).append(Component.literal("  "+number(v(player,attribute))).withStyle(s->s.withColor(TEXT))));
        var entry=StatusValuesCache.entry(attribute);
        if(entry==null){lines.add(t("tip.loading").withStyle(s->s.withColor(DIM)));return lines;}
        lines.add(t("tip.base",number(entry.base())).withStyle(s->s.withColor(DIM)));
        if(entry.sources().isEmpty())lines.add(t("tip.no_sources").withStyle(s->s.withColor(DIM)));
        for(var source:entry.sources()){
            String amount=switch(source.operation()){
                case 1->signed(source.amount()*100)+"% ";
                case 2->"x"+String.format(Locale.ROOT,"%.2f",1+source.amount())+" ";
                default->signed(source.amount())+" ";
            };
            int color=source.amount()>=0?0xFF8FD460:0xFFF07070;
            var line=Component.literal(amount).withStyle(s->s.withColor(color)).append(source.label().copy().withStyle(s->s.withColor(TEXT)));
            if(source.operation()==1)line.append(t("tip.of_base").withStyle(s->s.withColor(DIM)));
            lines.add(line);
        }
        return lines;
    }
    /** a status's tooltip: its name, what it does, and whether it helps or harms */
    private List<Component> statusTip(Component name,int color,Component description,Boolean beneficial,int ticks){
        var lines=new ArrayList<Component>();
        lines.add(name.copy().withStyle(s->s.withColor(color)));
        lines.add(description.copy().withStyle(s->s.withColor(TEXT)));
        if(beneficial!=null)lines.add(t(beneficial?"tip.beneficial":"tip.harmful").withStyle(s->s.withColor(beneficial?0xFF8FD460:0xFFF07070)));
        if(ticks>0&&ticks!=Integer.MAX_VALUE)lines.add(t("tip.left",time(ticks)).withStyle(s->s.withColor(DIM)));
        return lines;
    }
    /** what an effect does: its description key (effect.&lt;ns&gt;.&lt;id&gt;.description), or a note that none is known */
    private static Component effectDescription(MobEffect effect){
        String key=effect.getDescriptionId()+".description";
        return net.minecraft.client.resources.language.I18n.exists(key)?Component.translatable(key):t("tip.no_description");
    }
    /** an icon (texture or mob effect sprite or item), its name, time left, and a bar when the full length is known */
    private Row status(Component name,int color,ResourceLocation texture,MobEffect effect,ItemStack item,int ticks,int full){return status(name,color,texture,effect,item,ticks,full,null);}
    private Row status(Component name,int color,ResourceLocation texture,MobEffect effect,ItemStack item,int ticks,int full,List<Component> tip){
        return new Row(){public int height(){return STATUS_ROW;}
            public List<Component> tip(){return tip;}
            public void draw(GuiGraphics g,int x,int y,int w){
                g.fill(x,y+1,x+w,y+STATUS_ROW-1,0x40000000);
                if(texture!=null)g.blit(texture,x+2,y+3,16,16,0,0,18,18,18,18);
                else if(effect!=null)g.blit(x+2,y+3,0,16,16,Minecraft.getInstance().getMobEffectTextures().get(effect));
                else if(item!=null)g.renderItem(item,x+2,y+3);
                g.drawString(font,name,x+22,y+3,color,false);
                String time=ticks<0?"∞":ticks==Integer.MAX_VALUE?t("active").getString():time(ticks);
                g.drawString(font,time,x+w-font.width(time),y+3,DIM,false);
                if(full>0){int bw=w-24;g.fill(x+22,y+15,x+22+bw,y+17,0xFF262A36);
                    g.fill(x+22,y+15,x+22+(int)(bw*Mth.clamp(ticks/(float)full,0,1)),y+17,color);}
            }};
    }

    private List<Row> rows(Player player){
        List<Row> rows=new ArrayList<>();
        rows.add(section(t("section.basic")));
        rows.add(stat(player,Attributes.ATTACK_DAMAGE,t("attack_damage"),number(v(player,Attributes.ATTACK_DAMAGE)),TEXT));
        rows.add(stat(player,Attributes.ATTACK_SPEED,t("attack_speed"),number(v(player,Attributes.ATTACK_SPEED)),TEXT));
        double damage=v(player,Primordial.DAMAGE.get()),reduction=v(player,Primordial.REDUCTION.get());
        rows.add(stat(player,Primordial.DAMAGE.get(),t("all_damage"),signed((damage-1)*100)+"%",damage>1?0xFF8FD460:TEXT));
        rows.add(stat(player,Primordial.REDUCTION.get(),t("damage_reduction"),number(reduction*100)+"%",reduction>0?0xFF8FD460:TEXT));
        rows.add(stat(player,Attributes.LUCK,t("luck"),number(v(player,Attributes.LUCK)),TEXT));
        rows.add(stat(player,Attributes.MOVEMENT_SPEED,t("movement_speed"),number(v(player,Attributes.MOVEMENT_SPEED)*100)+"%",TEXT));

        Set<Attribute> shown=Set.of(Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED,Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,
            Primordial.DAMAGE.get(),Primordial.REDUCTION.get(),Attributes.LUCK,Attributes.MOVEMENT_SPEED);
        record Changed(Attribute attribute,double value){}
        var modified=new ArrayList<Changed>();
        if(StatusValuesCache.loaded()){
            for(var e:StatusValuesCache.changed()){var attribute=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(e.id());
                if(attribute!=null&&!shown.contains(attribute))modified.add(new Changed(attribute,e.value()));}
        }else for(var instance:player.getAttributes().getSyncableAttributes()){
            if(shown.contains(instance.getAttribute()))continue;
            if(Math.abs(instance.getValue()-instance.getAttribute().getDefaultValue())>0.00001||!instance.getModifiers().isEmpty())
                modified.add(new Changed(instance.getAttribute(),instance.getValue()));
        }
        modified.sort(Comparator.comparing(m->m.attribute().getDescriptionId()));
        if(!modified.isEmpty()){
            rows.add(section(t("section.other")));
            for(var m:modified)rows.add(stat(player,m.attribute(),Component.translatable(m.attribute().getDescriptionId()),number(m.value()),TEXT));
        }

        // the bonds holding: each as a status of its own, under its first part's accessory
        var bonds=local.primordial.affix.AffixBonds.of(AffixStatuses.bonds(player));
        if(!bonds.isEmpty()){
            rows.add(section(t("section.bonds")));
            for(var bond:bonds){
                var part=bond.parts().iterator().next();
                var name=Component.translatable("tooltip.primordial.bond.name",bond.label());
                // the lifesaving bonds show their cooldown counting down here; ready reads as plain "active"
                int ticks=Integer.MAX_VALUE,full=0;
                if(bond==local.primordial.affix.AffixBonds.Bond.UNQUENCHED){int left=AffixStatuses.unquenchedLeft(player);if(left>0){ticks=left;full=6000;}}
                else if(bond==local.primordial.affix.AffixBonds.Bond.WHEEL){int left=AffixStatuses.wheelLeft(player);if(left>0){ticks=left;full=3600;}}
                rows.add(status(name,bond.color(),null,null,new ItemStack(local.primordial.affix.AffixAccessories.ITEMS.get(part).get()),ticks,full,
                    bondTip(bond)));
            }
        }

        rows.add(section(t("section.statuses")));
        int before=rows.size();
        // Growth's stacks, with the countdown to losing them all
        int growthAttack=AffixStatuses.growthAttack(player),growthHealth=AffixStatuses.growthHealth(player);
        if(growthAttack+growthHealth>0){
            var name=t("growth",growthAttack*4,growthHealth*4);int color=0xFF8BCB4A;
            rows.add(status(name,color,null,null,new ItemStack(local.primordial.affix.AffixAccessories.ITEMS.get(local.primordial.affix.MobAffix.GROWTH).get()),
                AffixStatuses.growthLeft(player),AffixStatuses.GROWTH_IDLE,statusTip(name,color,t("growth.desc"),true,AffixStatuses.growthLeft(player))));
        }
        if(AffixStatuses.malice(player)>0)rows.add(affixStatus("malice",0xFFC060F0,"malice",AffixStatuses.malice(player),AffixStatuses.MALICE_TICKS));
        if(AffixStatuses.desaturation(player)>0)rows.add(affixStatus("desaturation",0xFFB8B8C0,"desaturation",AffixStatuses.desaturation(player),AffixStatuses.DESATURATION_TICKS));
        if(AffixStatuses.feeble(player)>0)rows.add(affixStatus("feeble",0xFFC8C080,"feebleness",AffixStatuses.feeble(player),AffixStatuses.FEEBLE_TICKS));
        if(AffixStatuses.bloodPact(player)>0)rows.add(affixStatus("blood_pact",0xFFE04050,"blood_pact",AffixStatuses.bloodPact(player),AffixStatuses.BLOOD_PACT_TICKS));
        if(AffixStatuses.befuddled(player)>0)rows.add(affixStatus("befuddled",0xFFD8A0F0,"befuddlement",AffixStatuses.befuddled(player),AffixStatuses.BEFUDDLED_TICKS));
        for(var effect:player.getActiveEffects()){
            int color=effect.getEffect().isBeneficial()?0xFF8FD460:0xFFF07070;
            var name=Component.translatable(effect.getDescriptionId());
            if(effect.getAmplifier()>0)name=name.copy().append(" ").append(Component.translatable("enchantment.level."+(effect.getAmplifier()+1)));
            int left=effect.isInfiniteDuration()?-1:effect.getDuration();
            rows.add(status(name,color,null,effect.getEffect(),null,left,0,statusTip(name,color,effectDescription(effect.getEffect()),effect.getEffect().isBeneficial(),left)));
        }
        if(SageHeart.active(player)){
            var gift=new ItemStack(Primordial.SAGE_HEART.get());
            rows.add(status(t("sage_curse"),0xFFE6B65A,null,null,gift,Integer.MAX_VALUE,0,statusTip(t("sage_curse"),0xFFE6B65A,t("sage_curse.desc"),null,0)));
            if(SageHeart.netherUnlocked(player))rows.add(status(t("nether_curse"),0xFFE07050,null,null,new ItemStack(net.minecraft.world.item.Items.NETHERRACK),Integer.MAX_VALUE,0,
                statusTip(t("nether_curse"),0xFFE07050,t("nether_curse.desc"),null,0)));
            if(SageHeart.endUnlocked(player))rows.add(status(t("end_curse"),0xFFC090F0,null,null,new ItemStack(net.minecraft.world.item.Items.END_STONE),Integer.MAX_VALUE,0,
                statusTip(t("end_curse"),0xFFC090F0,t("end_curse.desc"),null,0)));
        }
        if(rows.size()==before)rows.add(stat(t("none"),"",DIM));
        return rows;
    }
    /** a bond's tooltip: its name, what it does, and its parts */
    private List<Component> bondTip(local.primordial.affix.AffixBonds.Bond bond){
        var name=Component.translatable("tooltip.primordial.bond.name",bond.label());
        var lines=statusTip(name,bond.color(),bond.description(),true,0);
        var parts=Component.empty();boolean first=true;
        for(var part:bond.parts()){if(!first)parts.append(" ");parts.append(part.tag());first=false;}
        lines.add(parts);
        return lines;
    }
    /** one of the affixes' own statuses: its texture, time left, and what it does */
    private Row affixStatus(String key,int color,String texture,int ticks,int full){
        return status(t(key),color,icon(texture),null,null,ticks,full,statusTip(t(key),color,t(key+".desc"),false,ticks));
    }
    private static String number(double value){return String.format(Locale.ROOT,"%.1f",value);}
    private static String signed(double value){return (value>=0?"+":"")+number(value);}
    private static String time(int ticks){return String.format(Locale.ROOT,"%d:%02d",ticks/1200,(ticks/20)%60);}
}
