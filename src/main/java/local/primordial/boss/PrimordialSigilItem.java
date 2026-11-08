package local.primordial.boss;

import java.util.List;
import local.primordial.item.AlchemyCharmItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Primordial Lord's two relics. 原初之印 the Primordial Sigil, made on a primordial altar from its heart and the seven
 * sigils: used on its statue, it calls the lord back. 原初之心 the Primordial Heart, which it drops.
 */
public class PrimordialSigilItem extends Item {
    private final boolean heart;
    public PrimordialSigilItem(boolean heart) {super(new Properties().stacksTo(16).rarity(AlchemyCharmItem.PRIMORDIAL).fireResistant());this.heart=heart;}
    @Override public boolean isFoil(ItemStack stack) {return true;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        String id=heart?"primordial_heart":"primordial_sigil";
        tooltip.add(Component.translatable("tooltip.primordial."+id+".use").withStyle(s->s.withColor(PrimordialLord.COLOUR)));
        tooltip.add(Component.translatable("tooltip.primordial."+id+".source").withStyle(ChatFormatting.DARK_GRAY));
    }
}
