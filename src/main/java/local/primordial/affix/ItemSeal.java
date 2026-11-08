package local.primordial.affix;

import local.primordial.effect.Protection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ItemSeal {
    public static final String KEY="primordial:sealed_until";
    /** how long a seal lasts, in ticks */
    public static final int DURATION=100;
    private ItemSeal() {}
    public static void apply(LivingEntity target) {apply(target,DURATION);}
    /** a seal of another length (the bond 缚咒 lengthens the wearer's) */
    public static void apply(LivingEntity target,int ticks) {
        if(Protection.active(target))return;var stack=target.getMainHandItem();
        if(!stack.isEmpty()&&!unsealable(stack)){stack.getOrCreateTag().putLong(KEY,target.level().getGameTime()+ticks);target.stopUsingItem();}
    }
    public static boolean active(LivingEntity holder,ItemStack stack) {
        return !Protection.active(holder)&&!unsealable(stack)&&stack.hasTag()&&stack.getTag().getLong(KEY)>holder.level().getGameTime();
    }
    /** The Gem Sword ignores every affix, including Seal. */
    private static boolean unsealable(ItemStack stack) {return stack.is(local.primordial.Primordial.GEM_SWORD.get());}
    public static void expire(LivingEntity holder,ItemStack stack) {
        if(stack.hasTag()&&stack.getTag().contains(KEY)&&(!active(holder,stack)))stack.getTag().remove(KEY);
    }
}
