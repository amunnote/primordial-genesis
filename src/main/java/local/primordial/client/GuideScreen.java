package local.primordial.client;
import java.util.*;
import local.primordial.guide.*;
import local.primordial.alchemy.AlchemyKnowledge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class GuideScreen extends Screen {
    private static List<Component> progress=List.of();
    private int tab,scroll,ticks;
    public GuideScreen(){super(NextStep.t("title"));}
    public static void receive(GuideNetwork.Snapshot packet){progress=List.copyOf(packet.lines());if(packet.open())Minecraft.getInstance().setScreen(new GuideScreen());}
    @Override protected void init(){for(int i=0;i<3;i++){final int page=i;addRenderableWidget(Button.builder(NextStep.t("tab."+i),b->{tab=page;scroll=0;}).bounds(12+i*((width-24)/3),12,(width-30)/3,20).build());}}
    @Override public void tick(){if(++ticks%20==0)local.primordial.network.MenuDropNetwork.CHANNEL.sendToServer(new GuideNetwork.Request(-1));}
    @Override public boolean isPauseScreen(){return false;}
    private List<Component> lines(){
        if(tab==0)return progress;
        var out=new ArrayList<Component>();
        if(tab==1)for(var id:AlchemyKnowledge.recipes()){
            out.add(Component.literal(AlchemyKnowledge.clientKnows(id)?"✓ ":"○ ").append(Component.translatable("alchemy_recipe."+id.getNamespace()+"."+id.getPath())));
            out.add(RecipeSource.describe(id));
        }else for(var place:NextStep.places()){
            var p=place.pos();out.add(place.name().copy().append(NextStep.sideQuest(place.id())?"":(" · "+p.getX()+", "+p.getY()+", "+p.getZ())));
            out.add(Component.literal(place.dimension().location().toString()).append(" · ").append(place.condition()).append(" · ").append(NextStep.t("track")));
        }
        return out;
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(tab==2&&button==0&&x>=12&&x<width-12&&y>=42&&y<height-12){
            int cy=44-scroll;var lines=lines();
            for(int i=0;i<lines.size();i++){int h=font.split(lines.get(i),width-40).size()*13;
                if(y>=cy&&y<cy+h){local.primordial.network.MenuDropNetwork.CHANNEL.sendToServer(new GuideNetwork.Request(i/2));return true;}cy+=h;
            }
        }return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){scroll=Math.max(0,scroll-(int)Math.signum(delta)*24);return true;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);g.fill(8,38,width-8,height-10,0xDB10121D);
        var wrapped=new ArrayList<net.minecraft.util.FormattedCharSequence>();for(var line:lines())wrapped.addAll(font.split(line,width-40));
        int h=height-58;scroll=Math.min(scroll,Math.max(0,wrapped.size()*13-h));g.enableScissor(12,42,width-12,height-12);
        int y=44-scroll;for(var line:wrapped){g.drawString(font,line,18,y,0xFFE8E8EB);y+=13;}g.disableScissor();super.render(g,mx,my,partial);
    }
}
