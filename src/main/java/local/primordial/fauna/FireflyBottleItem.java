package local.primordial.fauna;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * 萤雷瓶 a bottled thunder firefly. On a block it sets down as a lamp (FireflyJarBlock); thrown it shatters in a shock
 * (ThrownFireflyBottle); used while sneaking it lets the firefly go, and the bottle comes back.
 */
public class FireflyBottleItem extends BlockItem {
    public FireflyBottleItem(Block block,Properties properties) {super(block,properties);}

    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();
        if(player!=null&&player.isShiftKeyDown()){
            release(context.getLevel(),player,context.getItemInHand(),Vec3.atCenterOf(context.getClickedPos().relative(context.getClickedFace())));
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()){release(level,player,stack,player.getEyePosition().add(player.getLookAngle()));return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
        level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.SPLASH_POTION_THROW,SoundSource.PLAYERS,0.5F,1.2F);
        if(!level.isClientSide){
            var thrown=new ThrownFireflyBottle(level,player);thrown.setItem(stack);
            thrown.shootFromRotation(player,player.getXRot(),player.getYRot(),-10,0.9F,1);level.addFreshEntity(thrown);
        }
        if(!player.getAbilities().instabuild)stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    /** the firefly let go; the empty bottle back in hand */
    static void release(Level level,Player player,ItemStack stack,Vec3 at) {
        level.playSound(null,at.x,at.y,at.z,SoundEvents.BOTTLE_EMPTY,SoundSource.PLAYERS,1F,1.4F);
        if(!(level instanceof ServerLevel server))return;
        FaunaContent.FIREFLY.get().spawn(server,BlockPos.containing(at),MobSpawnType.BUCKET);
        if(!player.getAbilities().instabuild){
            stack.shrink(1);
            if(!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE)))player.drop(new ItemStack(Items.GLASS_BOTTLE),false);
        }
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.thunder_firefly_bottle").withStyle(ChatFormatting.AQUA));
    }
}
