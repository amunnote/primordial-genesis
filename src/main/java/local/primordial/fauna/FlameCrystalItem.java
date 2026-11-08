package local.primordial.fauna;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** 毕方炎晶 Bifang's flame crystal: burns twice as long as a bucket of lava, or is thrown to burst into flame (ThrownFlameCrystal). */
public class FlameCrystalItem extends Item {
    public static final int BURN_TICKS=40000;
    public FlameCrystalItem(Properties properties) {super(properties);}
    @Override public int getBurnTime(ItemStack stack,RecipeType<?> type) {return BURN_TICKS;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.FIRECHARGE_USE,SoundSource.PLAYERS,0.6F,1.2F);
        if(!level.isClientSide){
            var thrown=new ThrownFlameCrystal(level,player);thrown.setItem(stack);
            thrown.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.2F,1);level.addFreshEntity(thrown);
        }
        player.getCooldowns().addCooldown(this,10);
        if(!player.getAbilities().instabuild)stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.bifang_flame_crystal").withStyle(ChatFormatting.GOLD));
    }
}
