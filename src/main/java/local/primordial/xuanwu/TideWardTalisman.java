package local.primordial.xuanwu;
import net.minecraft.world.item.*;
public final class TideWardTalisman extends Item {
    public TideWardTalisman(){super(new Properties().stacksTo(16));}
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(!l.isClientSide){p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING,3600));p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,3600));p.getCooldowns().addCooldown(this,20);if(!p.getAbilities().instabuild)stack.shrink(1);}
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
    }
}
