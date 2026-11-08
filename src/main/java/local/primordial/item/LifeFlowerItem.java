package local.primordial.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import local.primordial.Primordial;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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

public final class LifeFlowerItem extends Item implements ICurioItem {
    private static final int MINT=0x6FE8C0,LEAF=0x58D878,PETAL=0xEAFBF4,CORE=0xFFF2A8;
    public LifeFlowerItem() {super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public boolean canEquip(SlotContext context,ItemStack stack) {
        return !context.cosmetic()&&Primordial.ALCHEMICAL_ACCESSORY_SLOT.equals(context.identifier())
            &&context.entity().getAttributeValue(Attributes.LUCK)>=10;
    }
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack) {return canEquip(context,stack);}
    @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(SlotContext context,UUID uuid,ItemStack stack) {
        Multimap<Attribute,AttributeModifier> result=HashMultimap.create();
        if(!context.cosmetic())result.put(Attributes.MAX_HEALTH,
            new AttributeModifier(uuid,"primordial.life_flower_health",0.6,AttributeModifier.Operation.MULTIPLY_TOTAL));
        return result;
    }
    @Override public void curioTick(SlotContext context,ItemStack stack) {
        var entity=context.entity();
        if(!context.cosmetic()&&!entity.level().isClientSide&&entity.tickCount%20==0&&entity.isAlive())
            entity.heal(entity.getMaxHealth()*0.1F);
    }
    @Override public void onUnequip(SlotContext context,ItemStack replacement,ItemStack stack) {
        // Curios removes the attribute modifier before this callback.
        var entity=context.entity();
        if(!context.cosmetic()&&entity.getHealth()>entity.getMaxHealth())entity.setHealth(entity.getMaxHealth());
    }
    @Override public Component getName(ItemStack stack) {
        return shimmer(Component.translatable(getDescriptionId(stack)).getString(),System.nanoTime()/1_000_000_000.0);
    }
    @Override public List<Component> getAttributesTooltip(List<Component> tooltips,ItemStack stack) {return List.of();}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(line("requirement",CORE));
        lines.add(line("health",LEAF));
        lines.add(line("regen",MINT));
        lines.add(line("slot",0x8FA39A));
    }
    private static Component line(String key,int color) {
        return Component.translatable("tooltip.primordial.life_flower."+key).withStyle(Style.EMPTY.withColor(color));
    }
    /** Mint-to-white wave that travels across the name, with a warm glint at each crest. */
    private static MutableComponent shimmer(String text,double seconds) {
        MutableComponent result=Component.empty();int index=0;
        for(int point:text.codePoints().toArray()) {
            double wave=0.5+0.5*Math.sin(seconds*2.4-index*0.9);
            int color=wave>0.92?CORE:mix(MINT,PETAL,wave);
            result.append(Component.literal(new String(Character.toChars(point)))
                .setStyle(Style.EMPTY.withColor(color).withItalic(false)));
            index++;
        }
        return result;
    }
    private static int mix(int from,int to,double amount) {
        int r=(int)Math.round(((from>>16)&255)+(((to>>16)&255)-((from>>16)&255))*amount);
        int g=(int)Math.round(((from>>8)&255)+(((to>>8)&255)-((from>>8)&255))*amount);
        int b=(int)Math.round((from&255)+((to&255)-(from&255))*amount);
        return r<<16|g<<8|b;
    }
}
