package local.primordial.boss;

import java.util.List;
import local.primordial.Primordial;
import local.primordial.affix.BossAffix;
import local.primordial.item.AlchemyCharmItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The Seven Sins (七宗罪): an alchemical accessory made on a primordial altar from the seven sigils. Its wearer holds all
 * seven sin powers that a weapon forged with the lords' materials gives, each stronger (see
 * {@link local.primordial.effect.ImbueEvents}), and takes less from the Sin Lords and their kin.
 */
public final class SevenSinsItem extends Item implements ICurioItem {
    public static final double LUCK_REQUIRED=80;
    /** damage taken from Sin Lords and their creatures */
    public static final float SIN_WARD=0.7F;
    public SevenSinsItem() {super(new Properties().stacksTo(1).rarity(AlchemyCharmItem.PRIMORDIAL).fireResistant());}

    public static boolean wears(LivingEntity entity) {
        if(!(entity instanceof Player player))return false;
        return CuriosApi.getCuriosInventory(player).map(inventory->{
            for(var found:inventory.findCurios(stack->stack.getItem() instanceof SevenSinsItem))
                if(!found.slotContext().cosmetic())return true;
            return false;
        }).orElse(false);
    }

    @Override public boolean canEquip(SlotContext context,ItemStack stack) {
        return !context.cosmetic()&&Primordial.ALCHEMICAL_ACCESSORY_SLOT.equals(context.identifier())
            &&context.entity().getAttributeValue(Attributes.LUCK)>=LUCK_REQUIRED;
    }
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack) {return canEquip(context,stack);}
    @Override public boolean isFoil(ItemStack stack) {return true;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.seven_sins.requirement",(int)LUCK_REQUIRED).withStyle(ChatFormatting.DARK_GREEN));
        lines.add(Component.translatable("tooltip.primordial.seven_sins.all").withStyle(ChatFormatting.GOLD));
        for(var sin:BossAffix.values())
            lines.add(Component.translatable("tooltip.primordial.seven_sins."+sin.id()).withStyle(s->s.withColor(sin.color())));
        lines.add(Component.translatable("tooltip.primordial.seven_sins.ward",Math.round((1-SIN_WARD)*100)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.primordial.affix_accessory.slot").withStyle(ChatFormatting.DARK_GRAY));
    }
}
