package local.primordial.item;

import java.util.List;
import local.primordial.alchemy.AlchemyKnowledge;
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

/** 创造之书：使用后直接习得全部炼金知识（不需要贤者的馈赠）。生存模式下用后消失。 */
public final class CreationBookItem extends Item {
    public CreationBookItem() {super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}

    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(!level.isClientSide){
            int added=AlchemyKnowledge.learnAll(player);
            if(added==0)player.sendSystemMessage(Component.translatable("message.primordial.creation_book.none").withStyle(ChatFormatting.GRAY));
            else{
                player.sendSystemMessage(Component.translatable("message.primordial.creation_book.learned",added).withStyle(ChatFormatting.GOLD));
                level.playSound(null,player.blockPosition(),SoundEvents.PLAYER_LEVELUP,SoundSource.PLAYERS,1F,0.8F);
                level.playSound(null,player.blockPosition(),SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.PLAYERS,1F,1F);
                if(!player.getAbilities().instabuild)stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.creation_book").withStyle(ChatFormatting.GOLD));
    }
    @Override public boolean isFoil(ItemStack stack) {return true;}
}
