package local.primordial.alchemy;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Block item whose crafting recipe needs a learned alchemy recipe; says so in its tooltip. */
public final class KnowledgeBlockItem extends BlockItem {
    private final ResourceLocation knowledge;
    public KnowledgeBlockItem(Block block,Properties properties,ResourceLocation knowledge) {super(block,properties);this.knowledge=knowledge;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.knowledge_required",
            Component.translatable("alchemy_recipe."+knowledge.getNamespace()+"."+knowledge.getPath())).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
