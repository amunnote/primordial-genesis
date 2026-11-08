package local.primordial.item;

import java.util.List;
import java.util.function.Consumer;
import local.primordial.Primordial;
import local.primordial.effect.HarmfulEffectProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 净化水晶: worn in an alchemical accessory slot, nothing harmful takes hold of its wearer, neither a harmful effect nor
 * one of the mod's negative statuses (malice, desaturation, feebleness, blood pact, befuddlement): what is on them is
 * cleared, and nothing new is put on them (HarmfulEffectProtection, AffixStatuses).
 */
public final class PurificationCrystalItem extends Item implements ICurioItem {
    public PurificationCrystalItem() { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return Primordial.ALCHEMICAL_ACCESSORY_SLOT.equals(context.identifier()) && !context.cosmetic();
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return true; }
    @Override public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) {
        if (canEquip(context, stack)) HarmfulEffectProtection.cleanse(context.entity());
    }
    @Override public void curioTick(SlotContext context, ItemStack stack) {
        if (canEquip(context, stack)) HarmfulEffectProtection.cleanse(context.entity());
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.primordial.crystal.immunity").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.primordial.crystal.slot").withStyle(ChatFormatting.GRAY));
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return local.primordial.client.CharmRenderer.crystalInstance();
            }
        });
    }
}
