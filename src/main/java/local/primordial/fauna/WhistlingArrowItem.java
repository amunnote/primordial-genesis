package local.primordial.fauna;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 鸣镝 fletched with golden-bell feathers: a bow or crossbow shoots it as a WhistlingArrow. */
public class WhistlingArrowItem extends ArrowItem {
    public WhistlingArrowItem(Properties properties) {super(properties);}
    @Override public AbstractArrow createArrow(Level level,ItemStack stack,LivingEntity shooter) {return new WhistlingArrow(level,shooter);}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.whistling_arrow").withStyle(ChatFormatting.YELLOW));
    }
}
