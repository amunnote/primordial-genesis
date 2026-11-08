package local.primordial.mixin;

import local.primordial.item.AlchemyCharmItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Charms with resistance pierce weaken the target's Resistance effect; the rest of the vanilla method is unchanged. */
@Mixin(LivingEntity.class)
public abstract class CharmResistanceMixin {
    @Inject(method="getDamageAfterMagicAbsorb",at=@At("HEAD"),cancellable=true)
    private void angelCharmResistancePierce(DamageSource source,float amount,CallbackInfoReturnable<Float> callback) {
        var self=(LivingEntity)(Object)this;
        if(self.level().isClientSide||source.is(DamageTypeTags.BYPASSES_EFFECTS)||source.is(DamageTypeTags.BYPASSES_RESISTANCE)
            ||!self.hasEffect(MobEffects.DAMAGE_RESISTANCE)||!(source.getEntity() instanceof LivingEntity attacker))return;
        double pierce=AlchemyCharmItem.resistancePierce(attacker);
        if(pierce<=0)return;
        float resisted=(self.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()+1)*5*(float)(1-pierce);
        amount=Math.max(amount*(25-resisted)/25F,0);
        if(amount>0&&!source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            int protection=EnchantmentHelper.getDamageProtection(self.getArmorSlots(),source);
            if(protection>0)amount=CombatRules.getDamageAfterMagicAbsorb(amount,protection);
        }
        callback.setReturnValue(amount);
    }
}
