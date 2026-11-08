package local.primordial.client;

import java.util.ArrayList;
import java.util.List;
import local.primordial.Primordial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class AffixNameplateClient {
    private static final int MAX_WIDTH=160;
    private AffixNameplateClient() {}

    @SubscribeEvent public static void render(RenderNameTagEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||!mob.isCustomNameVisible())return;
        var content=event.getContent();
        if(!affixed(content))return;
        content=strike(content,mob);
        event.setContent(content);
        var minecraft=Minecraft.getInstance();var font=minecraft.font;
        if(font.width(content)<=MAX_WIDTH)return;
        List<Component> lines=wrap(content,font);
        if(lines.size()<2)return;
        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        var pose=event.getPoseStack();pose.pushPose();
        pose.translate(0,mob.getBbHeight()+0.5D,0);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F,-0.025F,0.025F);
        Matrix4f matrix=pose.last().pose();
        int background=(int)(minecraft.options.getBackgroundOpacity(0.25F)*255F)<<24;
        for(int i=0;i<lines.size();i++){
            Component line=lines.get(i);float x=-font.width(line)/2F;
            float y=-10F-(lines.size()-1-i)*10F;
            font.drawInBatch(line,x,y,0x20FFFFFF,false,matrix,event.getMultiBufferSource(),
                Font.DisplayMode.SEE_THROUGH,background,event.getPackedLight());
            font.drawInBatch(line,x,y,0xFFFFFFFF,false,matrix,event.getMultiBufferSource(),
                Font.DisplayMode.NORMAL,0,event.getPackedLight());
        }
        pose.popPose();
    }

    /**
     * Strikes out the affixes that do nothing to the local player: those its accessories (or forged weapon) ward off,
     * Amplify loans when it wears Amplify's accessory, and all of them while the mob is sealed.
     */
    static Component strike(Component content,Mob mob) {
        var player=Minecraft.getInstance().player;
        if(player==null)return content;
        var worn=local.primordial.affix.AffixAccessories.worn(player);
        boolean sealed=AffixClient.sealed(mob);
        if(worn.isEmpty()&&!sealed)return content;
        boolean changed=false;
        MutableComponent out=content.plainCopy().withStyle(content.getStyle());
        for(var part:content.getSiblings()){
            Component piece=part;
            String marker=part.getStyle().getInsertion();
            if(marker!=null&&marker.startsWith(local.primordial.affix.AffixState.MARKER)){
                String rest=marker.substring(local.primordial.affix.AffixState.MARKER.length());
                boolean lent=rest.endsWith(local.primordial.affix.AffixState.LENT);
                var affix=local.primordial.affix.MobAffix.byId(lent?rest.substring(0,rest.length()-local.primordial.affix.AffixState.LENT.length()):rest);
                if(affix!=null&&(sealed||worn.contains(affix)||lent&&worn.contains(local.primordial.affix.MobAffix.AMPLIFY))){
                    piece=part.copy().withStyle(style->style.withStrikethrough(true));changed=true;
                }
            }
            out.append(piece);
        }
        return changed?out:content;
    }

    /** an affix name: carries the affix tag component (any language), or 【 from names saved before affixes were translatable */
    static boolean affixed(Component content) {
        for(var part:content.getSiblings())
            if(part.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("affix.primordial.tag"))return true;
        return content.getString().contains("【");
    }
    static List<Component> wrap(Component content,Font font) {
        List<Component> lines=new ArrayList<>();MutableComponent line=Component.empty();
        var parts=content.getSiblings();
        if(parts.isEmpty())return List.of(content);
        for(var part:parts){
            MutableComponent candidate=Component.empty().append(line).append(part);
            if(font.width(candidate)>MAX_WIDTH&&!line.getString().isEmpty()){
                lines.add(line);line=Component.empty().append(part);
            }else line=candidate;
        }
        if(!line.getString().isEmpty())lines.add(line);
        return lines;
    }
}
