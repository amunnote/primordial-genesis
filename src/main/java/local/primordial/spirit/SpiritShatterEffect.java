package local.primordial.spirit;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** 灵体破碎: thrown out of the Spirit Realm, every attribute is 20% lower for a minute. Milk does not mend it. */
public final class SpiritShatterEffect extends MobEffect {
    public SpiritShatterEffect() {
        super(MobEffectCategory.HARMFUL,0x7A6AB8);
        var down=AttributeModifier.Operation.MULTIPLY_TOTAL;
        addAttributeModifier(Attributes.MAX_HEALTH,"1d3c5e7a-2b4d-4f6a-8c1e-3a5b7c9d0e11",-0.2,down);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,"1d3c5e7a-2b4d-4f6a-8c1e-3a5b7c9d0e12",-0.2,down);
        addAttributeModifier(Attributes.ATTACK_SPEED,"1d3c5e7a-2b4d-4f6a-8c1e-3a5b7c9d0e13",-0.2,down);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,"1d3c5e7a-2b4d-4f6a-8c1e-3a5b7c9d0e14",-0.2,down);
        addAttributeModifier(Attributes.ARMOR,"1d3c5e7a-2b4d-4f6a-8c1e-3a5b7c9d0e15",-0.2,down);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS,"1d3c5e7a-2b4d-4f6a-8c1e-3a5b7c9d0e16",-0.2,down);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE,"1d3c5e7a-2b4d-4f6a-8c1e-3a5b7c9d0e17",-0.2,down);
        addAttributeModifier(Attributes.LUCK,"1d3c5e7a-2b4d-4f6a-8c1e-3a5b7c9d0e18",-0.2,down);
    }
    @Override public List<ItemStack> getCurativeItems() {return new ArrayList<>();}
}
