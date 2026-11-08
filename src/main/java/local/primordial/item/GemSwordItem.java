package local.primordial.item;

import java.util.List;
import local.primordial.effect.GemSwordPower;
import local.primordial.effect.Protection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.entity.PartEntity;

public final class GemSwordItem extends Item {
    private static final String SLAUGHTER="Slaughter";
    public GemSwordItem() {super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());}
    public static boolean slaughter(ItemStack stack) {return stack.hasTag()&&stack.getTag().getBoolean(SLAUGHTER);}
    private static Component modeName(boolean slaughter) {
        return Component.translatable("mode.primordial.gem_sword."+(slaughter?"slaughter":"normal"))
            .withStyle(slaughter?ChatFormatting.RED:ChatFormatting.AQUA);
    }
    @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity entity) {
        Entity target=entity instanceof PartEntity<?> part?part.getParent():entity;
        if(!(target instanceof LivingEntity)||Protection.active(target))return false;
        if(!player.level().isClientSide)GemSwordPower.slay(player,target);
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()) {
            if(!level.isClientSide) {
                boolean next=!slaughter(stack);
                stack.getOrCreateTag().putBoolean(SLAUGHTER,next);
                player.displayClientMessage(Component.translatable("tooltip.primordial.gem_sword.mode",modeName(next)),true);
            }
        } else if(player instanceof ServerPlayer server) {
            GemSwordPower.burst(server,slaughter(stack));
            player.getCooldowns().addCooldown(this,10);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public boolean canAttackBlock(BlockState state,Level level,BlockPos pos,Player player) {return !player.isCreative();}
    @Override public Component getName(ItemStack stack) {return RainbowText.animated(Component.translatable(getDescriptionId(stack)));}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(RainbowText.animated(Component.translatable("tooltip.primordial.gem_sword.intro")));
        lines.add(Component.translatable("tooltip.primordial.gem_sword.damage").withStyle(ChatFormatting.DARK_GREEN));
        lines.add(Component.translatable("tooltip.primordial.gem_sword.left").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.primordial.gem_sword.right").withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("tooltip.primordial.gem_sword.pierce").withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("tooltip.primordial.gem_sword.affix").withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("tooltip.primordial.gem_sword.mode",modeName(slaughter(stack))).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.primordial.gem_sword.switch").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.primordial.gem_sword.spectator",
            Component.keybind("key.primordial.gem_spectator")).withStyle(ChatFormatting.DARK_GRAY));
    }
}
