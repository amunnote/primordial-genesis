package local.primordial.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.MobAffix;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * An affix accessory, worn in an alchemical accessory slot with at least 20 luck. Its own effect lives in
 * {@link local.primordial.effect.AffixAccessoryEvents} (and attribute modifiers here); its ward against the same
 * affix on monsters is checked by the affix code through {@link AffixAccessories#blocks}.
 */
public final class AffixAccessoryItem extends Item implements ICurioItem {
    private final MobAffix affix;
    public AffixAccessoryItem(MobAffix affix) {super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.affix=affix;}
    public MobAffix affix() {return affix;}

    @Override public boolean canEquip(SlotContext context,ItemStack stack) {
        return !context.cosmetic()&&Primordial.ALCHEMICAL_ACCESSORY_SLOT.equals(context.identifier())
            &&context.entity().getAttributeValue(Attributes.LUCK)>=AffixAccessories.LUCK_REQUIRED;
    }
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack) {return canEquip(context,stack);}
    @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(SlotContext context,UUID uuid,ItemStack stack) {
        Multimap<Attribute,AttributeModifier> result=HashMultimap.create();
        if(context.cosmetic())return result;
        String name="primordial."+AffixAccessories.id(affix);
        switch(affix){
            case STRENGTH->{
                result.put(Attributes.ATTACK_DAMAGE,new AttributeModifier(uuid,name+"_attack",4,AttributeModifier.Operation.ADDITION));
                result.put(Attributes.ATTACK_DAMAGE,new AttributeModifier(uuid,name+"_attack_percent",0.1,AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            case PHYSICAL_RESISTANCE->result.put(Attributes.KNOCKBACK_RESISTANCE,new AttributeModifier(uuid,name+"_knockback",0.5,AttributeModifier.Operation.ADDITION));
            case ENLIGHTENMENT->result.put(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get(),new AttributeModifier(uuid,name+"_reach",1.5,AttributeModifier.Operation.ADDITION));
            default->{}
        }
        return result;
    }
    @Override public List<Component> getAttributesTooltip(List<Component> tooltips,ItemStack stack) {return List.of();}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.affix_accessory.requirement",(int)AffixAccessories.LUCK_REQUIRED).withStyle(ChatFormatting.DARK_GREEN));
        lines.add(Component.translatable("tooltip.primordial.affix_accessory."+affix.id()).withStyle(style->style.withColor(affix.color())));
        lines.add(Component.translatable("tooltip.primordial.affix_accessory.ward",affix.tag()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.primordial.affix_accessory.slot").withStyle(ChatFormatting.DARK_GRAY));
    }
}
