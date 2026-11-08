package local.primordial.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 嬗变之源。
 *  - 背包中拿着它右键可烧炼的物品：每烧一个消耗一个嬗变之源，直接得到烧炼产物，每个有 30% 概率翻倍。
 *  - 手持右键生物：见 {@link local.primordial.alchemy.Transmutation}。
 */
public final class TransmutationSourceItem extends Item {
    public static final float DOUBLE_CHANCE=0.3F;
    public TransmutationSourceItem() {super(new Properties().rarity(Rarity.UNCOMMON));}

    @Override public boolean overrideStackedOnOther(ItemStack sources,Slot slot,ClickAction action,Player player) {
        if(action!=ClickAction.SECONDARY||!slot.hasItem()||!slot.allowModification(player))return false;
        if(smeltingResult(player.level(),slot.getItem()).isEmpty())return false;
        if(!player.level().isClientSide)smelt(sources,slot,player);
        return true; // the server sends the resulting slots back
    }
    static ItemStack smeltingResult(Level level,ItemStack input) {
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(input),level)
            .map(recipe->recipe.getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
    }
    /** Smelt as many of the slot's items as there are sources in hand. @return how many were smelted */
    public static int smelt(ItemStack sources,Slot slot,Player player) {
        Level level=player.level();
        ItemStack input=slot.getItem(),result=smeltingResult(level,input);
        if(result.isEmpty())return 0;
        int count=Math.min(sources.getCount(),input.getCount());
        int produced=0;
        for(int i=0;i<count;i++)produced+=result.getCount()*(level.random.nextFloat()<DOUBLE_CHANCE?2:1);
        sources.shrink(count);
        slot.remove(count);
        // the products go back into the emptied slot first, the rest into the inventory
        ItemStack out=result.copyWithCount(produced);
        if(!slot.hasItem()&&slot.mayPlace(out)){
            ItemStack placed=out.split(Math.min(out.getCount(),slot.getMaxStackSize(out)));
            slot.set(placed);
        }
        while(!out.isEmpty())ItemHandlerHelper.giveItemToPlayer(player,out.split(Math.min(out.getCount(),out.getMaxStackSize())));
        level.playSound(null,player.blockPosition(),SoundEvents.FIRECHARGE_USE,SoundSource.PLAYERS,0.5F,1.4F);
        return count;
    }

    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.transmutation_source.smelt").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.primordial.transmutation_source.mob").withStyle(ChatFormatting.GRAY));
    }
}
