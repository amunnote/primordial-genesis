package local.primordial.altar;

import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Performs an altar recipe for a player: empty-hand right-click on an altar with items on it. */
public final class AltarCrafting {
    public enum Result {CRAFTED,NO_RECIPE,UNKNOWN,TIER_TOO_LOW}
    /** on the player: what their last random transmutation gave */
    public static final String LAST_RANDOM="primordial:last_transmutation";
    /** on the player: what the current round of random transmutations has given so far */
    public static final String ROUND="primordial:transmutation_round";
    private static net.minecraft.world.item.Item item(String id) {
        var key=net.minecraft.resources.ResourceLocation.tryParse(id);
        return key==null?net.minecraft.world.item.Items.AIR:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(key);
    }
    private AltarCrafting() {}

    public static Result tryCraft(ServerLevel level,BlockPos pos,Player player) {
        if(!(level.getBlockEntity(pos) instanceof AltarBlockEntity altar)||!(level.getBlockState(pos).getBlock() instanceof AlchemyAltarBlock block))
            return Result.NO_RECIPE;
        var container=new SimpleContainer(altar.items().toArray(ItemStack[]::new));
        // nothing but alchemy recipes on the table: learned ones break down into fragments
        var fragments=fragments(level,block,altar,player);
        if(fragments!=null)return fragments;
        AltarRecipe found=null;
        for(AltarRecipe recipe:level.getRecipeManager().getAllRecipesFor(Primordial.ALTAR_RECIPE_TYPE.get()))
            if(recipe.matches(container,level)){found=recipe;break;}
        if(found==null){
            player.displayClientMessage(Component.translatable("message.primordial.altar.no_recipe").withStyle(ChatFormatting.GRAY),true);
            return Result.NO_RECIPE;
        }
        if(found.knowledge()!=null&&!AlchemyKnowledge.knows(player,found.knowledge())){
            player.displayClientMessage(Component.translatable("message.primordial.altar.unknown").withStyle(ChatFormatting.RED),true);
            return Result.UNKNOWN;
        }
        if(block.tier()<found.tier()){
            player.displayClientMessage(Component.translatable("message.primordial.altar.tier",tierName(found.tier())).withStyle(ChatFormatting.RED),true);
            return Result.TIER_TOO_LOW;
        }
        // random transmutations go round: nothing comes out twice until everything has come out once
        var data=player.getPersistentData();
        var used=new java.util.HashSet<net.minecraft.world.item.Item>();
        for(var entry:data.getList(ROUND,net.minecraft.nbt.Tag.TAG_STRING))used.add(item(entry.getAsString()));
        var last=item(data.getString(LAST_RANDOM));
        ItemStack result=found.craft(container,level.random,used,last==net.minecraft.world.item.Items.AIR?null:last);
        if(found.randomizes()){
            var id=String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(result.getItem()));
            var round=found.roundEnded()?new net.minecraft.nbt.ListTag():data.getList(ROUND,net.minecraft.nbt.Tag.TAG_STRING);
            round.add(net.minecraft.nbt.StringTag.valueOf(id));
            data.put(ROUND,round);data.putString(LAST_RANDOM,id);
        }
        altar.clear();
        // one item per place on the table, as when placing by hand, so a pile never counts as a single ingredient later
        ItemStack product=result.copy();
        while(!product.isEmpty()&&!altar.isFull())altar.add(product.split(1));
        if(!product.isEmpty())net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player,product);
        double x=pos.getX()+0.5,y=pos.getY()+block.tableTop()+0.3,z=pos.getZ()+0.5;
        level.sendParticles(ParticleTypes.ENCHANT,x,y+0.4,z,60,0.4,0.4,0.4,0.6);
        level.sendParticles(ParticleTypes.END_ROD,x,y,z,16,0.2,0.3,0.2,0.04);
        level.playSound(null,pos,SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.BLOCKS,1F,0.8F);
        level.playSound(null,pos,SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,1.2F,1F);
        player.displayClientMessage(Component.translatable("message.primordial.altar.crafted",result.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE),true);
        local.primordial.achievement.Achievements.altarCrafted(player,block.tier(),result);
        return Result.CRAFTED;
    }
    /**
     * Learned alchemy recipes (of transmutation tier and below, and no higher than the altar) broken down into a
     * fragment each, of the same tier. Null when the table holds anything but recipes, so the usual recipes are tried.
     */
    static Result fragments(ServerLevel level,AlchemyAltarBlock block,AltarBlockEntity altar,Player player) {
        var items=altar.items();
        if(items.isEmpty())return null;
        for(ItemStack stack:items)if(!stack.is(Primordial.ALCHEMY_RECIPE.get())||local.primordial.item.AlchemyRecipeItem.recipe(stack)==null)return null;
        var tiers=new java.util.ArrayList<Integer>();
        for(ItemStack stack:items){
            var recipe=local.primordial.item.AlchemyRecipeItem.recipe(stack);
            if(!AlchemyKnowledge.knows(player,recipe)){
                player.displayClientMessage(Component.translatable("message.primordial.altar.fragment_unknown").withStyle(ChatFormatting.RED),true);
                return Result.UNKNOWN;
            }
            int tier=local.primordial.alchemy.RecipeTiers.tier(level,recipe);
            if(tier<=0||tier>local.primordial.alchemy.RecipeTiers.FRAGMENT_MAX){
                player.displayClientMessage(Component.translatable("message.primordial.altar.fragment_tier").withStyle(ChatFormatting.RED),true);
                return Result.NO_RECIPE;
            }
            if(block.tier()<tier){
                player.displayClientMessage(Component.translatable("message.primordial.altar.tier",tierName(tier)).withStyle(ChatFormatting.RED),true);
                return Result.TIER_TOO_LOW;
            }
            for(int n=0;n<stack.getCount();n++)tiers.add(tier);
        }
        altar.clear();
        for(int tier:tiers){var fragment=local.primordial.item.RecipeFragmentItem.create(tier);
            if(!altar.isFull())altar.add(fragment);else net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player,fragment);}
        var pos=altar.getBlockPos();
        double x=pos.getX()+0.5,y=pos.getY()+block.tableTop()+0.3,z=pos.getZ()+0.5;
        level.sendParticles(ParticleTypes.ENCHANT,x,y+0.4,z,40,0.4,0.4,0.4,0.6);
        level.playSound(null,pos,SoundEvents.BOOK_PAGE_TURN,SoundSource.BLOCKS,1.2F,0.8F);
        level.playSound(null,pos,SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,1F,1.2F);
        player.displayClientMessage(Component.translatable("message.primordial.altar.fragmented",tiers.size()).withStyle(ChatFormatting.LIGHT_PURPLE),true);
        local.primordial.achievement.Achievements.fragmented(player);
        return Result.CRAFTED;
    }
    public static Component tierName(int tier) {
        return Component.translatable("altar.primordial.tier."+Math.max(1,Math.min(4,tier)));
    }
}
