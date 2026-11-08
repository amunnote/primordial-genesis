package local.primordial.client;

import local.primordial.Primordial;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.AffixBonds;
import local.primordial.item.AffixAccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** An affix accessory's tooltip names each bond it is part of and how much of it the player wears: 羁绊·众生 3/4. */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class BondTooltips {
    private BondTooltips() {}
    @SubscribeEvent public static void bondProgress(ItemTooltipEvent event) {
        if(!(event.getItemStack().getItem() instanceof AffixAccessoryItem item))return;
        var bonds=AffixBonds.containing(item.affix());if(bonds.isEmpty())return;
        var worn=event.getEntity()!=null?AffixAccessories.worn(event.getEntity()):java.util.EnumSet.noneOf(local.primordial.affix.MobAffix.class);
        for(var bond:bonds){
            int have=bond.count(worn),need=bond.parts().size();
            var line=Component.translatable("tooltip.primordial.bond.progress",bond.label(),have,need);
            event.getToolTip().add(have>=need?line.withStyle(style->style.withColor(bond.color())):line.withStyle(ChatFormatting.DARK_GRAY));
            // held down: what the bond does and what it is made of
            if(net.minecraft.client.gui.screens.Screen.hasShiftDown()){
                event.getToolTip().add(Component.literal("  ").append(bond.description()).withStyle(ChatFormatting.GRAY));
                var parts=Component.literal("  ");boolean first=true;
                for(var part:bond.parts()){if(!first)parts.append(" ");parts.append(worn.contains(part)?part.tag():part.label().copy().withStyle(ChatFormatting.DARK_GRAY));first=false;}
                event.getToolTip().add(parts);
            }
        }
        if(!net.minecraft.client.gui.screens.Screen.hasShiftDown())event.getToolTip().add(Component.translatable("tooltip.primordial.bond.shift").withStyle(ChatFormatting.DARK_GRAY));
    }
}
