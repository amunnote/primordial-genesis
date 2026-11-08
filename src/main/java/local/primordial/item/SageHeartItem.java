package local.primordial.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.effect.Protection;
import local.primordial.sage.SageHeart;
import local.primordial.sage.SageProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class SageHeartItem extends Item implements ICurioItem {
    public SageHeartItem() { super(new Properties().stacksTo(1).fireResistant()); }
    @Override public Component getName(ItemStack stack) { return RainbowText.animated(Component.translatable(getDescriptionId(stack))); }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return !context.cosmetic() && Primordial.SAGE_SLOT.equals(context.identifier());
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return true; }
    @Override public boolean canUnequip(SlotContext context, ItemStack stack) {
        return Protection.active(context.entity())
            || context.entity() instanceof net.minecraft.world.entity.player.Player player && player.isCreative();
    }
    @Override public ICurio.DropRule getDropRule(SlotContext context, DamageSource source, int looting, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext context, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers=HashMultimap.create();
        if(context.cosmetic())return modifiers;
        modifiers.put(Attributes.LUCK,new AttributeModifier(uuid,"primordial.sage_luck",10,AttributeModifier.Operation.ADDITION));
        CuriosApi.addSlotModifier(modifiers,Primordial.ALCHEMICAL_SLOT,uuid,1,AttributeModifier.Operation.ADDITION);
        CuriosApi.addSlotModifier(modifiers,Primordial.ALCHEMICAL_ACCESSORY_SLOT,uuid,3,AttributeModifier.Operation.ADDITION);
        return modifiers;
    }
    @Override public void onUnequip(SlotContext context, ItemStack replacement, ItemStack stack) {
        if(context.entity() instanceof net.minecraft.world.entity.player.Player player) {
            // Curios calls this while iterating its slots. Resizing those slots here can
            // invalidate the active iteration; finish the change on the next player tick.
            SageHeart.scheduleSlotRelease(player);
        }
    }
    @Override public List<Component> getAttributesTooltip(List<Component> lines, ItemStack stack) { return List.of(); }
    @Override public List<Component> getTagsTooltip(List<Component> lines, ItemStack stack) {
        return List.of(Component.translatable("tooltip.primordial.sage.slot").withStyle(ChatFormatting.GOLD));
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.primordial.sage.intro").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.primordial.sage.progress").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("1%").withStyle(ChatFormatting.GREEN)
            .append(Component.translatable("tooltip.primordial.sage.damage_label").withStyle(ChatFormatting.GOLD)));
        lines.add(Component.literal("0.5%").withStyle(ChatFormatting.GREEN)
            .append(Component.translatable("tooltip.primordial.sage.reduction_label").withStyle(ChatFormatting.GOLD)));
        lines.add(Component.literal("1%").withStyle(ChatFormatting.GREEN)
            .append(Component.translatable("tooltip.primordial.sage.luck_growth_label").withStyle(ChatFormatting.GOLD)));
        lines.add(Component.translatable("tooltip.primordial.sage.max_prefix").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("33%").withStyle(ChatFormatting.GREEN)));
        lines.add(Component.literal("+10").withStyle(ChatFormatting.GREEN)
            .append(Component.translatable("tooltip.primordial.sage.luck_label").withStyle(ChatFormatting.GOLD)));
        lines.add(Component.literal("+1").withStyle(ChatFormatting.GREEN)
            .append(Component.translatable("tooltip.primordial.sage.charm_slots_label").withStyle(ChatFormatting.GOLD)));
        lines.add(Component.literal("+3").withStyle(ChatFormatting.GREEN)
            .append(Component.translatable("tooltip.primordial.sage.accessory_slots_label").withStyle(ChatFormatting.GOLD)));
        lines.add(Component.translatable("tooltip.primordial.sage.warning").withStyle(ChatFormatting.RED));
        boolean client=net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient();
        appendAdvancementBonus(lines,client&&net.minecraft.client.gui.screens.Screen.hasShiftDown());
        appendCurses(lines,client&&net.minecraft.client.gui.screens.Screen.hasAltDown(),SageProgress.clientUnlocks());
    }
    public static void appendAdvancementBonus(List<Component> lines,boolean shift) {
        if(shift){
            int count=SageProgress.clientCount();
            lines.add(Component.translatable("tooltip.primordial.sage.current_damage").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.format(java.util.Locale.ROOT,"+%.0f%%",SageProgress.damageBonus(count)*100)).withStyle(ChatFormatting.GREEN)));
            lines.add(Component.translatable("tooltip.primordial.sage.current_reduction").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.format(java.util.Locale.ROOT,"+%.1f%%",SageProgress.reductionBonus(count)*100)).withStyle(ChatFormatting.GREEN)));
            lines.add(Component.translatable("tooltip.primordial.sage.current_luck").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.format(java.util.Locale.ROOT,"+%.0f%%",SageProgress.luckBonus(count)*100)).withStyle(ChatFormatting.GREEN)));
        }else lines.add(Component.translatable("tooltip.primordial.sage.shift_prefix").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("Shift").withStyle(ChatFormatting.GREEN))
            .append(Component.translatable("tooltip.primordial.sage.shift_suffix").withStyle(ChatFormatting.GOLD)));
    }
    public static void appendCurses(List<Component> lines,boolean alt,int unlocks) {
        if(!alt){
            lines.add(Component.translatable("tooltip.primordial.sage.alt_prefix").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Alt").withStyle(ChatFormatting.GREEN))
                .append(Component.translatable("tooltip.primordial.sage.alt_suffix").withStyle(ChatFormatting.GOLD)));
            return;
        }
        for(int number=1;number<=7;number++){
            boolean unlocked=number<=3||(number<=5&&(unlocks&1)!=0)||(number>=6&&(unlocks&2)!=0);
            Component curse=Component.translatable("tooltip.primordial.sage.curse"+number)
                .withStyle(unlocked?new ChatFormatting[]{ChatFormatting.RED}:new ChatFormatting[]{ChatFormatting.GRAY,ChatFormatting.STRIKETHROUGH});
            var line=Component.translatable("tooltip.primordial.sage.curse_number",number).withStyle(unlocked?ChatFormatting.RED:ChatFormatting.GRAY).append(curse);
            if(number>=4)line.append(Component.translatable(number<=5?"tooltip.primordial.sage.nether_unlock":"tooltip.primordial.sage.end_unlock")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
            lines.add(line);
        }
    }
}
