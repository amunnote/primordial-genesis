package local.primordial.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Hold Ctrl over an item in any inventory screen to open its recipe map: what it is made from on the left, what it
 * goes into on the right, each recipe a node joined to the item in the middle. While Ctrl is held the map stays open:
 * the wheel pages a side, a left click on any item recentres the map on it, a right click goes back.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class RecipeMapOverlay {
    static final long HOLD_MS=400;
    private static final int SLOT=18,PER_ROW=5,GAP=4,CENTER_GAP=64;
    private static final int LINE=0xFF7A64C0,BORDER=0xFF6E5AA8,LOCKED_BORDER=0xFF4A4A55,NODE=0xF01C1830,SLOT_BG=0xFF2E2A40;
    private static long holdStart;
    private static ItemStack pending=ItemStack.EMPTY;
    private static RecipeMap.Result map;
    private static final Deque<ItemStack> HISTORY=new ArrayDeque<>();
    private static int leftPage,rightPage;
    private record Hit(int x,int y,int w,int h,ItemStack stack) {}
    private static final List<Hit> HITS=new ArrayList<>();
    private RecipeMapOverlay() {}

    public static boolean open() {return map!=null;}
    static void close() {map=null;HISTORY.clear();holdStart=0;pending=ItemStack.EMPTY;HITS.clear();}
    static void focus(ItemStack stack) {
        var mc=Minecraft.getInstance();
        if(mc.level==null)return;
        map=RecipeMap.of(stack,mc.level.getRecipeManager(),mc.level.registryAccess(),AlchemyKnowledge::clientKnows);
        leftPage=rightPage=0;
    }

    @SubscribeEvent public static void recipeMapRender(ScreenEvent.Render.Post event) {
        if(!(event.getScreen() instanceof AbstractContainerScreen<?> screen)||!Screen.hasControlDown()){if(map!=null||holdStart!=0)close();return;}
        if(map==null){
            var slot=screen.getSlotUnderMouse();
            if(slot==null||!slot.hasItem()){holdStart=0;return;}
            var stack=slot.getItem();
            if(holdStart==0||!ItemStack.isSameItemSameTags(stack,pending)){holdStart=Util.getMillis();pending=stack.copy();return;}
            if(Util.getMillis()-holdStart<HOLD_MS)return;
            focus(stack);
            if(map==null)return;
        }
        draw(event.getGuiGraphics(),screen.width,screen.height,event.getMouseX(),event.getMouseY());
    }
    @SubscribeEvent public static void recipeMapClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if(map==null)return;
        event.setCanceled(true);
        if(event.getButton()==GLFW.GLFW_MOUSE_BUTTON_RIGHT){if(!HISTORY.isEmpty())focus(HISTORY.pop());return;}
        if(event.getButton()!=GLFW.GLFW_MOUSE_BUTTON_LEFT)return;
        for(var hit:HITS)if(event.getMouseX()>=hit.x&&event.getMouseX()<hit.x+hit.w&&event.getMouseY()>=hit.y&&event.getMouseY()<hit.y+hit.h){
            if(!hit.stack.isEmpty()&&!ItemStack.isSameItem(hit.stack,map.focus())){HISTORY.push(map.focus());focus(hit.stack);}
            return;
        }
    }
    @SubscribeEvent public static void recipeMapRelease(ScreenEvent.MouseButtonReleased.Pre event) {if(map!=null)event.setCanceled(true);}
    @SubscribeEvent public static void recipeMapScroll(ScreenEvent.MouseScrolled.Pre event) {
        if(map==null)return;
        event.setCanceled(true);
        int step=event.getScrollDelta()>0?-1:1;
        if(event.getMouseX()<event.getScreen().width/2.0)leftPage+=step;else rightPage+=step;
    }
    /** keys other than Escape do nothing while the map is open, so Ctrl+Q cannot throw the stack away under it */
    @SubscribeEvent public static void recipeMapKeys(ScreenEvent.KeyPressed.Pre event) {
        if(map!=null&&event.getKeyCode()!=GLFW.GLFW_KEY_ESCAPE)event.setCanceled(true);
    }
    @SubscribeEvent public static void recipeMapScreenClosed(ScreenEvent.Closing event) {close();}
    /** the last tooltip line of anything that has a recipe tree */
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void recipeMapHint(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        var mc=Minecraft.getInstance();
        if(mc.level==null||event.getItemStack().isEmpty()||map!=null)return;
        if(!RecipeMap.hasAny(event.getItemStack(),mc.level.getRecipeManager(),mc.level.registryAccess()))return;
        event.getToolTip().add(Component.translatable("recipe_map.primordial.tooltip").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    // ------------------------------------------------------------------ drawing
    private static void draw(GuiGraphics g,int width,int height,int mouseX,int mouseY) {
        var font=Minecraft.getInstance().font;
        HITS.clear();
        g.pose().pushPose();
        g.pose().translate(0,0,500);
        g.fill(0,0,width,height,0xE40C0A14);
        int cx=width/2,cy=height/2+4;
        var focus=map.focus();
        g.drawCenteredString(font,Component.translatable("recipe_map.primordial.title",focus.getHoverName()),cx,6,0xFFE8D8FF);
        leftPage=side(g,font,map.madeFrom(),true,leftPage,width,height,cx,cy);
        rightPage=side(g,font,map.usedIn(),false,rightPage,width,height,cx,cy);
        // the item itself, in the middle
        g.fill(cx-15,cy-15,cx+15,cy+15,0xFFB89CFF);g.fill(cx-14,cy-14,cx+14,cy+14,0xFF2A2240);
        g.pose().pushPose();g.pose().translate(cx-12,cy-12,0);g.pose().scale(1.5F,1.5F,1);g.renderItem(focus,0,0);g.pose().popPose();
        HITS.add(new Hit(cx-15,cy-15,30,30,focus));
        g.drawCenteredString(font,focus.getHoverName(),cx,cy+19,0xFFFFFFFF);
        Component hint=Component.translatable(HISTORY.isEmpty()?"recipe_map.primordial.hint":"recipe_map.primordial.hint_back");
        g.drawCenteredString(font,hint,cx,height-11,0xFF8C84A8);
        for(var hit:HITS)if(!hit.stack.isEmpty()&&mouseX>=hit.x&&mouseX<hit.x+hit.w&&mouseY>=hit.y&&mouseY<hit.y+hit.h){
            g.renderTooltip(font,hit.stack,mouseX,mouseY);break;
        }
        g.pose().popPose();
    }
    private static int columns(RecipeMap.Link link) {return Math.max(1,Math.min(PER_ROW,link.inputs().size()));}
    private static int rows(RecipeMap.Link link) {return (link.inputs().size()+PER_ROW-1)/PER_ROW;}
    /** inputs | station | arrow | output */
    private static int nodeWidth(RecipeMap.Link link) {return 3+columns(link)*SLOT+6+SLOT+12+SLOT+3;}
    private static int nodeHeight(RecipeMap.Link link) {return 4+Math.max(1,rows(link))*SLOT+2;}
    /** draws one side's current page; returns the page actually shown */
    private static int side(GuiGraphics g,Font font,List<RecipeMap.Link> links,boolean left,int page,int width,int height,int cx,int cy) {
        int top=32,bottom=height-26;
        int columnEdge=left?cx-CENTER_GAP:cx+CENTER_GAP;
        Component header=Component.translatable(left?"recipe_map.primordial.made_from":"recipe_map.primordial.used_in",links.size());
        int headerX=left?columnEdge-font.width(header):columnEdge;
        g.drawString(font,header,headerX,20,left?0xFFFFC870:0xFF8FE0FF,false);
        if(links.isEmpty()){
            Component none=Component.translatable("recipe_map.primordial.none");
            g.drawString(font,none,left?columnEdge-font.width(none):columnEdge,cy-4,0xFF6C6880,false);
            return 0;
        }
        // split into pages that fit the height
        List<List<RecipeMap.Link>> pages=new ArrayList<>();List<RecipeMap.Link> current=new ArrayList<>();int used=0;
        for(var link:links){
            int h=nodeHeight(link)+GAP;
            if(!current.isEmpty()&&used+h>bottom-top){pages.add(current);current=new ArrayList<>();used=0;}
            current.add(link);used+=h;
        }
        pages.add(current);
        page=Math.floorMod(page,pages.size());
        var shown=pages.get(page);
        int total=shown.stream().mapToInt(link->nodeHeight(link)+GAP).sum()-GAP;
        int y=Math.max(top,Math.min(bottom-total,cy-total/2));
        int trunk=left?cx-38:cx+38;
        for(var link:shown){
            int w=nodeWidth(link),h=nodeHeight(link);
            int x=left?Math.max(2,columnEdge-w):Math.min(width-2-w,columnEdge);
            int joinY=y+h/2;
            // elbow line: node, trunk, centre
            int nodeEdge=left?x+w:x;
            g.fill(Math.min(nodeEdge,trunk),joinY,Math.max(nodeEdge,trunk)+1,joinY+1,LINE);
            g.fill(trunk,Math.min(joinY,cy),trunk+1,Math.max(joinY,cy)+1,LINE);
            g.fill(Math.min(trunk,left?cx-15:cx+15),cy,Math.max(trunk,left?cx-15:cx+15),cy+1,LINE);
            node(g,font,link,x,y,w,h);
            y+=h+GAP;
        }
        if(pages.size()>1){
            Component pager=Component.translatable("recipe_map.primordial.page",page+1,pages.size());
            g.drawString(font,pager,left?columnEdge-font.width(pager):columnEdge,bottom+2,0xFFB8B0D0,false);
        }
        return page;
    }
    private static net.minecraft.resources.ResourceLocation sourceId(RecipeMap.Link link) {
        var level=Minecraft.getInstance().level;
        if(level!=null){var recipe=level.getRecipeManager().byKey(link.id()).orElse(null);
            if(recipe instanceof local.primordial.altar.AltarRecipe altar&&altar.knowledge()!=null)return altar.knowledge();
            if(recipe instanceof local.primordial.alchemy.KnowledgeShapedRecipe shaped&&shaped.knowledge()!=null)return shaped.knowledge();
        }
        for(var input:link.inputs())for(var stack:input.choices()){
            var id=local.primordial.item.AlchemyRecipeItem.recipe(stack);if(id!=null)return id;
        }
        return link.id();
    }
    private static void node(GuiGraphics g,Font font,RecipeMap.Link link,int x,int y,int w,int h) {
        if(link.locked())HITS.add(new Hit(x,y,w,h,local.primordial.item.AlchemyRecipeItem.create(sourceId(link))));
        g.fill(x,y,x+w,y+h,link.locked()?LOCKED_BORDER:BORDER);
        g.fill(x+1,y+1,x+w-1,y+h-1,NODE);
        long tick=Util.getMillis()/1000;
        int i=0;
        for(var input:link.inputs()){
            int sx=x+3+(i%PER_ROW)*SLOT,sy=y+4+(i/PER_ROW)*SLOT;
            var stack=input.choices().get((int)(tick%input.choices().size())).copyWithCount(input.count());
            slot(g,font,sx,sy,link.locked()?ItemStack.EMPTY:stack);
            i++;
        }
        int midY=y+4+(Math.max(1,rows(link))-1)*SLOT/2;
        int stationX=x+3+columns(link)*SLOT+6;
        g.renderItem(link.station(),stationX,midY);
        HITS.add(new Hit(stationX,midY,16,16,link.station()));
        g.drawString(font,"→",stationX+SLOT+1,midY+4,0xFFB8A8E8,false);
        var outputs=link.outputs();
        var output=outputs.get((int)(tick%outputs.size()));
        slot(g,font,stationX+SLOT+12,midY,link.locked()?ItemStack.EMPTY:output);
    }
    private static void slot(GuiGraphics g,Font font,int x,int y,ItemStack stack) {
        g.fill(x-1,y-1,x+17,y+17,SLOT_BG);
        if(stack.isEmpty()){g.drawString(font,"?",x+5,y+4,0xFF8A8A96,false);return;}
        g.renderItem(stack,x,y);g.renderItemDecorations(font,stack,x,y);
        HITS.add(new Hit(x-1,y-1,18,18,stack));
    }
}
