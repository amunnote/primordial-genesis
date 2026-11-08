package local.primordial.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A part of the Gem Sword: the irregular crystal its blade is grown from, or the primordial hilt that holds it. */
public final class GemPartItem extends Item {
    private final String id;
    public GemPartItem(String id,Rarity rarity) {super(new Properties().rarity(rarity).fireResistant());this.id=id;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial."+id).withStyle(ChatFormatting.GRAY,ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.primordial.gem_part").withStyle(ChatFormatting.AQUA));
    }
    /** the blade is only ever cut on a stonecutter, which calls this as it is taken out */
    @Override public void onCraftedBy(ItemStack stack,Level level,net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack,level,player);
        if(!level.isClientSide&&"gem_sword_blade".equals(id)&&player.containerMenu instanceof net.minecraft.world.inventory.StonecutterMenu)
            local.primordial.achievement.Achievements.bladeCut(player);
    }
}
