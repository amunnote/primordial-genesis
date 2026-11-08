package local.primordial.alchemy;

import java.util.ArrayList;
import java.util.List;
import local.primordial.Primordial;
import local.primordial.item.AlchemyRecipeItem;
import local.primordial.item.MysteryRecipeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * A Mystery Recipe opened at an enchanting table: the recipe in the item slot, a block of amethyst where the lapis
 * goes, and the three rows offer three alchemy recipes of its tier its holder has not learned. Choosing one spends the
 * amethyst block and turns the mystery into that recipe. The three are drawn from the player's enchantment seed, as the
 * table's own offers are, so closing and opening the table never deals a new hand; only a choice does.
 * <p>
 * The offers go to the client in the table's own synced numbers: a row's level clue is {@link #OFFER} and its
 * enchantment clue the recipe's index among the alchemy recipes (registered in the same order on both sides); the
 * first row's level clue is {@link #ALL_LEARNED} when nothing of the tier is left to learn. Costs stay 0, so the table
 * itself shows nothing of its own on those rows.
 */
public final class MysteryEnchanting {
    public static final int OFFER=-4207,ALL_LEARNED=-4208;
    private MysteryEnchanting() {}

    public static boolean mystery(ItemStack stack) {return stack.is(Primordial.MYSTERY_RECIPE.get())&&MysteryRecipeItem.tier(stack)>0;}
    public static boolean fuel(ItemStack stack) {return stack.is(Items.AMETHYST_BLOCK);}
    /** whether the table is in this use: a mystery recipe in it, or an amethyst block (which may not enchant anything else) */
    public static boolean active(Container slots) {return mystery(slots.getItem(0))||fuel(slots.getItem(1));}

    /** the three on offer, fewer when fewer are left */
    public static List<ResourceLocation> offers(Level level,Player player,int tier) {
        var all=MysteryRecipeItem.choices(level,player,tier);
        var random=new java.util.Random(player.getEnchantmentSeed()*31L+tier);
        java.util.Collections.shuffle(all,random);
        return new ArrayList<>(all.subList(0,Math.min(3,all.size())));
    }

    /** the server fills the rows (the client keeps what the server sends) */
    public static void fill(Level level,Player player,Container slots,int[] costs,int[] enchantClue,int[] levelClue) {
        for(int i=0;i<3;i++){costs[i]=0;enchantClue[i]=-1;levelClue[i]=-1;}
        var stack=slots.getItem(0);
        if(!mystery(stack)||!fuel(slots.getItem(1)))return;
        var offers=offers(level,player,MysteryRecipeItem.tier(stack));
        if(offers.isEmpty()){levelClue[0]=ALL_LEARNED;return;}
        var all=AlchemyKnowledge.recipes();
        for(int i=0;i<offers.size();i++){enchantClue[i]=all.indexOf(offers.get(i));levelClue[i]=OFFER;}
    }

    /** the recipe a row offers, as the client reads it from the synced numbers; null for none */
    public static ResourceLocation offered(int[] enchantClue,int[] levelClue,int row) {
        if(row<0||row>2||levelClue[row]!=OFFER)return null;
        var all=AlchemyKnowledge.recipes();int i=enchantClue[row];
        return i>=0&&i<all.size()?all.get(i):null;
    }

    /** a row chosen: on the server the mystery becomes the recipe; false if the row offers nothing */
    public static boolean choose(Player player,Container slots,int[] enchantClue,int[] levelClue,int row) {
        var recipe=offered(enchantClue,levelClue,row);
        var stack=slots.getItem(0);var fuel=slots.getItem(1);
        if(recipe==null||!mystery(stack)||!fuel(fuel))return false;
        if(player.level().isClientSide)return true;
        // offered from the same draw, so still of its tier and still unknown; checked again all the same
        int tier=MysteryRecipeItem.tier(stack);
        if(!offers(player.level(),player,tier).contains(recipe))return false;
        if(!player.getAbilities().instabuild)fuel.shrink(1);
        var result=AlchemyRecipeItem.create(recipe);
        if(stack.getCount()<=1)slots.setItem(0,result);
        else{stack.shrink(1);ItemHandlerHelper.giveItemToPlayer(player,result);}
        if(fuel.isEmpty())slots.setItem(1,ItemStack.EMPTY);
        // a new seed, as an enchantment brings: the next mystery gets a fresh hand
        player.onEnchantmentPerformed(result,0);
        player.level().playSound(null,player.blockPosition(),SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.BLOCKS,1F,0.9F);
        player.level().playSound(null,player.blockPosition(),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.BLOCKS,1F,1.2F);
        local.primordial.achievement.Achievements.mysteryOpened(player);
        slots.setChanged();
        return true;
    }
}
