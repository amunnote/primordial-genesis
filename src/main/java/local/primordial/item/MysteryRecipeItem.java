package local.primordial.item;

import java.util.ArrayList;
import java.util.List;
import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.alchemy.RecipeTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 神秘配方 Mystery Recipe: seven fragments of one tier bound with a Transmutation Source. Laid in an enchanting table
 * with a block of amethyst where the lapis goes, it offers three alchemy recipes of its tier its user has not learned,
 * and becomes the one chosen (see MysteryEnchanting).
 */
public final class MysteryRecipeItem extends Item {
    public MysteryRecipeItem() {super(new Properties().stacksTo(16).rarity(Rarity.RARE));}
    public static ItemStack create(int tier) {
        var stack=new ItemStack(Primordial.MYSTERY_RECIPE.get());
        stack.getOrCreateTag().putInt(RecipeFragmentItem.TIER_KEY,tier);
        return stack;
    }
    public static int tier(ItemStack stack) {return stack.hasTag()?stack.getTag().getInt(RecipeFragmentItem.TIER_KEY):0;}
    /** the recipes it could become for this player: findable at random, of its tier, not yet learned */
    public static List<ResourceLocation> choices(Level level,Player player,int tier) {
        var out=new ArrayList<ResourceLocation>();
        for(var id:AlchemyKnowledge.recipes())
            if(AlchemyKnowledge.findable(id)&&!AlchemyKnowledge.knows(player,id)&&RecipeTiers.tier(level,id)==tier)out.add(id);
        return out;
    }
    @Override public Component getName(ItemStack stack) {
        int tier=tier(stack);
        return tier<=0?super.getName(stack):Component.translatable("item.primordial.mystery_recipe.named",RecipeTiers.name(tier));
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.mystery_recipe.use").withStyle(ChatFormatting.DARK_PURPLE));
    }

}
