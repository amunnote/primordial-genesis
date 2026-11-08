package local.primordial.item;

import java.util.List;
import local.primordial.Primordial;
import local.primordial.alchemy.RecipeTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 配方残片 Recipe Fragment: what is left of an alchemy recipe already learned, broken down on an altar (transmutation
 * tier and below). Its tier is kept in NBT. Seven of one tier with a Transmutation Source make a Mystery Recipe.
 */
public final class RecipeFragmentItem extends Item {
    public static final String TIER_KEY="primordial:tier";
    public RecipeFragmentItem() {super(new Properties().stacksTo(64).rarity(Rarity.UNCOMMON));}
    public static ItemStack create(int tier) {
        var stack=new ItemStack(Primordial.RECIPE_FRAGMENT.get());
        stack.getOrCreateTag().putInt(TIER_KEY,tier);
        return stack;
    }
    public static int tier(ItemStack stack) {return stack.hasTag()?stack.getTag().getInt(TIER_KEY):0;}
    @Override public Component getName(ItemStack stack) {
        int tier=tier(stack);
        return tier<=0?super.getName(stack):Component.translatable("item.primordial.recipe_fragment.named",RecipeTiers.name(tier));
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.recipe_fragment.source").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.primordial.recipe_fragment.use").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
