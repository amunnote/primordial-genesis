package local.primordial.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import local.primordial.Primordial;
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
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class AngelCharmItem extends Item implements ICurioItem {
    public AngelCharmItem() { super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)); }
    @Override public Component getName(ItemStack stack) { return RainbowText.animated(Component.translatable(getDescriptionId(stack))); }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return !context.cosmetic() && (local.primordial.effect.AscensionSlot.SLOT.equals(context.identifier())
            || local.primordial.effect.Protection.active(context.entity()));
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return true; }
    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext context, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> result = HashMultimap.create();
        if (context.cosmetic()) return result;
        // Fixed UUIDs prevent extra charm slots from stacking this unique artifact.
        put(result, Primordial.DAMAGE.get(), "01", 9.99, AttributeModifier.Operation.MULTIPLY_TOTAL);
        put(result, Primordial.REDUCTION.get(), "02", 9.99, AttributeModifier.Operation.ADDITION);
        // Luck starts at zero. (base + 1) * 10.99 - 1 gives 9.99 luck from zero.
        put(result, Attributes.LUCK, "03", 9.99 / 10.99, AttributeModifier.Operation.ADDITION);
        put(result, Attributes.LUCK, "04", 9.99, AttributeModifier.Operation.MULTIPLY_TOTAL);
        // the bonuses of Transparent World and Untouched by Any Law, whose every power it carries (the rest in AlchemyCharmItem)
        double attack = 0, speed = 0, armor = 0, knockback = 0;
        for (var charm : AlchemyCharmItem.ascended()) {
            attack = Math.max(attack, charm.attackPercent()); speed = Math.max(speed, charm.attackSpeedBonus());
            armor = Math.max(armor, charm.armorBonus()); knockback = Math.max(knockback, charm.knockbackBonus());
        }
        put(result, Attributes.ATTACK_DAMAGE, "05", attack, AttributeModifier.Operation.MULTIPLY_TOTAL);
        put(result, Attributes.ATTACK_SPEED, "06", speed, AttributeModifier.Operation.MULTIPLY_TOTAL);
        put(result, Attributes.ARMOR, "07", armor, AttributeModifier.Operation.ADDITION);
        put(result, Attributes.KNOCKBACK_RESISTANCE, "08", knockback, AttributeModifier.Operation.ADDITION);
        return result;
    }
    private static void put(Multimap<Attribute, AttributeModifier> map, Attribute attribute, String suffix,
                            double amount, AttributeModifier.Operation operation) {
        map.put(attribute, new AttributeModifier(UUID.fromString("39ed9b37-5e7d-4269-bb1d-ef00624900" + suffix),
                "primordial." + suffix, amount, operation));
    }
    @Override public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) { return List.of(); }
    @Override public List<Component> getTagsTooltip(List<Component> tooltips, ItemStack stack) {
        return List.of(Component.translatable("tooltip.primordial.slot").withStyle(ChatFormatting.GOLD));
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(RainbowText.animated(Component.translatable("tooltip.primordial.intro")));
        for (String key : List.of("damage", "reduction", "luck"))
            tooltip.add(Component.translatable("tooltip.primordial." + key).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.primordial.ascended").withStyle(ChatFormatting.AQUA));
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return local.primordial.client.CharmRenderer.instance();
            }
        });
    }
}
