package local.primordial.item;

import java.util.List;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.sage.SageHeart;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 残缺的配方页: used, it teaches a basic (tier 1) alchemy recipe its reader has not yet learned, at random; if they know
 * them all, it becomes a tier 1 recipe fragment instead. Like any recipe, only a sage (wearing Sage's Gift) learns from it.
 */
public final class TornRecipePageItem extends Item {
    public static final int TIER=1;
    public TornRecipePageItem() {super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        if(!SageHeart.active(player)){
            player.sendSystemMessage(Component.translatable("message.primordial.recipe.not_sage").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
        var choices=MysteryRecipeItem.choices(level,player,TIER);
        if(!player.getAbilities().instabuild)stack.shrink(1);
        if(choices.isEmpty()){
            ItemHandlerHelper.giveItemToPlayer(player,RecipeFragmentItem.create(TIER));
            player.sendSystemMessage(Component.translatable("message.primordial.torn_recipe_page.all_known").withStyle(ChatFormatting.GRAY));
        }else{
            var recipe=choices.get(player.getRandom().nextInt(choices.size()));
            AlchemyKnowledge.learn(player,recipe);
            player.sendSystemMessage(Component.translatable("message.primordial.torn_recipe_page.learned",AlchemyRecipeItem.recipeName(recipe)).withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null,player.blockPosition(),SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.PLAYERS,1F,1.1F);
        }
        return InteractionResultHolder.consume(stack);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.primordial.torn_recipe_page").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.primordial.torn_recipe_page.all_known").withStyle(ChatFormatting.GRAY));
    }
}
