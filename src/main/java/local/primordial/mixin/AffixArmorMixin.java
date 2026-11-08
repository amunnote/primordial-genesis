package local.primordial.mixin;

import local.primordial.affix.AffixCombat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class AffixArmorMixin {
    @ModifyVariable(method="hurt",at=@At("HEAD"),argsOnly=true)
    private DamageSource affixDamageType(DamageSource source) {
        var victim=(LivingEntity)(Object)this;var attacker=AffixCombat.source(source);
        // magic damage passes shields; an attack the victim's raised shield would stop stays as it was, so it is blocked
        if(!victim.level().isClientSide&&victim.isDamageSourceBlocked(source))return source;
        if(!victim.level().isClientSide&&!AffixCombat.secondary()&&attacker!=null
            &&local.primordial.affix.AffixAccessories.acts(attacker,local.primordial.affix.MobAffix.MAGIC,victim)&&!source.is(AffixCombat.MAGIC))return AffixCombat.magic(source,victim);
        // arcane charms: the wearer's own melee and projectile attacks become magic damage
        if(!victim.level().isClientSide&&source.getEntity() instanceof net.minecraft.world.entity.player.Player player&&!source.is(AffixCombat.MAGIC)
            &&(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE))
            &&local.primordial.item.AlchemyCharmItem.magicAttacks(player))source=AffixCombat.magic(source,victim);
        // Causality: the wearer's damage is adaptive, settled against the target's weaker side, hit by hit
        return local.primordial.affix.AffixDamage.adapt(source,victim);
    }
    @Inject(method="getDamageAfterArmorAbsorb",at=@At("HEAD"),cancellable=true)
    private void affixArmor(DamageSource source,float amount,CallbackInfoReturnable<Float> callback) {
        Float changed=AffixCombat.piercedArmor((LivingEntity)(Object)this,source,amount);
        if(changed!=null)callback.setReturnValue(changed);
    }
}
