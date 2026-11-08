package local.primordial.item;

import java.util.List;
import local.primordial.Primordial;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 萤石耳坠: worn in an alchemical accessory slot, night vision while it is on, and nothing else. Unlike the affix accessories
 * it asks no luck of its wearer: it is for those who have none yet.
 */
public final class GlowstoneEarringItem extends Item implements ICurioItem {
    public GlowstoneEarringItem() {super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public boolean canEquip(SlotContext context,ItemStack stack) {
        return !context.cosmetic()&&Primordial.ALCHEMICAL_ACCESSORY_SLOT.equals(context.identifier());
    }
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack) {return true;}
    @Override public void curioTick(SlotContext context,ItemStack stack) {
        var wearer=context.entity();
        if(context.cosmetic()||wearer.level().isClientSide)return;
        // kept topped up, past the flicker the game gives night vision in its last ten seconds
        var current=wearer.getEffect(MobEffects.NIGHT_VISION);
        if(current==null||current.getDuration()<220)wearer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,260,0,true,false,true));
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.primordial.glowstone_earring").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.primordial.glowstone_earring.slot").withStyle(ChatFormatting.GRAY));
    }
}
