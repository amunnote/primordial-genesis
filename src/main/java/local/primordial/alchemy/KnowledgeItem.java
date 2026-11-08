package local.primordial.alchemy;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Plain item made by an alchemy that needs a learned recipe; says so in its tooltip. */
public final class KnowledgeItem extends Item {
    private final ResourceLocation knowledge;
    public KnowledgeItem(Properties properties,ResourceLocation knowledge) {super(properties);this.knowledge=knowledge;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.knowledge_required",
            Component.translatable("alchemy_recipe."+knowledge.getNamespace()+"."+knowledge.getPath())).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
