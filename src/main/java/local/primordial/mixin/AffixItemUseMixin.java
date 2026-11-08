package local.primordial.mixin;

import local.primordial.affix.ItemSeal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class AffixItemUseMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void sealedUse(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        var stack=(ItemStack)(Object)this;if(ItemSeal.active(player,stack))callback.setReturnValue(InteractionResultHolder.fail(stack));
    }
    @Inject(method="useOn",at=@At("HEAD"),cancellable=true)
    private void sealedUseOn(UseOnContext context,CallbackInfoReturnable<InteractionResult> callback) {
        if(context.getPlayer()!=null&&ItemSeal.active(context.getPlayer(),(ItemStack)(Object)this))callback.setReturnValue(InteractionResult.FAIL);
    }
}
