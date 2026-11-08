package local.primordial.item;

import java.util.List;
import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.sage.SageHeart;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 炼金配方：NBT 记录配方 id。佩戴贤者的馈赠时右键学习，学会后消耗。 */
public final class AlchemyRecipeItem extends Item {
    public static final String RECIPE_KEY="primordial:recipe";
    public AlchemyRecipeItem() {super(new Properties().stacksTo(16).rarity(Rarity.RARE));}

    public static ItemStack create(ResourceLocation recipe) {
        ItemStack stack=new ItemStack(Primordial.ALCHEMY_RECIPE.get());
        stack.getOrCreateTag().putString(RECIPE_KEY,recipe.toString());
        return stack;
    }
    public static ResourceLocation recipe(ItemStack stack) {
        return stack.hasTag()?ResourceLocation.tryParse(stack.getTag().getString(RECIPE_KEY)):null;
    }
    public static Component recipeName(ResourceLocation recipe) {
        return Component.translatable("alchemy_recipe."+recipe.getNamespace()+"."+recipe.getPath());
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        ResourceLocation recipe=recipe(stack);
        if(recipe==null)return InteractionResultHolder.pass(stack);
        if(!level.isClientSide){
            if(!SageHeart.active(player))
                player.sendSystemMessage(Component.translatable("message.primordial.recipe.not_sage").withStyle(ChatFormatting.RED));
            else if(!AlchemyKnowledge.learn(player,recipe))
                player.sendSystemMessage(Component.translatable("message.primordial.recipe.known").withStyle(ChatFormatting.GRAY));
            else{
                player.sendSystemMessage(Component.translatable("message.primordial.recipe.learned").withStyle(ChatFormatting.LIGHT_PURPLE));
                level.playSound(null,player.blockPosition(),SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.PLAYERS,1F,1F);
                if(!player.getAbilities().instabuild){
                    // its essence spent, the recipe is left as a plain sheet of paper
                    stack.shrink(1);
                    if(stack.isEmpty())return InteractionResultHolder.success(new ItemStack(Items.PAPER));
                    ItemHandlerHelper.giveItemToPlayer(player,new ItemStack(Items.PAPER));
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }

    @Override public Component getName(ItemStack stack) {
        ResourceLocation recipe=recipe(stack);
        if(recipe==null)return super.getName(stack);
        var name=Component.translatable("item.primordial.alchemy_recipe.named",recipeName(recipe));
        // on the client, mark recipes the local player has already learned
        if(net.minecraftforge.fml.loading.FMLEnvironment.dist==net.minecraftforge.api.distmarker.Dist.CLIENT
            &&AlchemyKnowledge.clientKnows(recipe))
            return name.append(Component.literal("（已习得）").withStyle(ChatFormatting.GREEN));
        return name;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        if(recipe(stack)!=null)lines.add(local.primordial.guide.RecipeSource.describe(recipe(stack)));
        lines.add(Component.translatable("tooltip.primordial.recipe.use").withStyle(ChatFormatting.GRAY));
    }

}
