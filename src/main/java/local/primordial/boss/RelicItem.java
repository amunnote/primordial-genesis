package local.primordial.boss;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A guardian's relic: what it drops (a fang, a scute), or its sigil, which calls it back from its statue. Two lines of lore. */
public class RelicItem extends Item {
    private final String id;private final int colour;private final boolean foil;
    public RelicItem(String id,int colour,Rarity rarity,boolean foil) {super(new Properties().stacksTo(16).rarity(rarity).fireResistant());this.id=id;this.colour=colour;this.foil=foil;}
    @Override public boolean isFoil(ItemStack stack) {return foil;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.primordial."+id+".use").withStyle(s->s.withColor(colour)));
        tooltip.add(Component.translatable("tooltip.primordial."+id+".source").withStyle(ChatFormatting.DARK_GRAY));
    }
}
