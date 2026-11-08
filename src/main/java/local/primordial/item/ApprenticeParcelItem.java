package local.primordial.item;

import java.util.List;
import local.primordial.Primordial;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 炼金学徒的包裹: opened, two tier 1 recipe fragments and an apprentice's start: alchemical essence, source ingots, source
 * string, and amethyst shards to fire more ingots with.
 */
public final class ApprenticeParcelItem extends Item {
    public ApprenticeParcelItem() {super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));}
    /** what one parcel holds */
    public static List<ItemStack> contents() {
        return List.of(RecipeFragmentItem.create(1),RecipeFragmentItem.create(1),
            new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get(),3),new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get(),2),
            new ItemStack(Primordial.ALCHEMICAL_SOURCE_STRING.get(),2),new ItemStack(Items.AMETHYST_SHARD,3));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        if(!player.getAbilities().instabuild)stack.shrink(1);
        for(var item:contents())ItemHandlerHelper.giveItemToPlayer(player,item);
        level.playSound(null,player.blockPosition(),SoundEvents.BUNDLE_DROP_CONTENTS,SoundSource.PLAYERS,1F,1F);
        return InteractionResultHolder.consume(stack);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.primordial.apprentice_parcel").withStyle(ChatFormatting.GOLD));
    }
}
