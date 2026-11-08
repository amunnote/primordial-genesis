package local.primordial.mixin;
import local.primordial.sage.SageHeart;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class SageLivingMixin {
    @ModifyVariable(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),argsOnly=true)
    private MobEffectInstance sageEffect(MobEffectInstance effect){return angelRescale((LivingEntity)(Object)this,effect);}
    @ModifyVariable(method="forceAddEffect",at=@At("HEAD"),argsOnly=true)
    private MobEffectInstance sageForceEffect(MobEffectInstance effect){return angelRescale((LivingEntity)(Object)this,effect);}
    /** Sage's Gift first (its doubling), then Time's accessory and the bonds shorten what harms (AffixBonds.effect) */
    private static MobEffectInstance angelRescale(LivingEntity owner,MobEffectInstance effect){
        return local.primordial.affix.AffixBonds.effect(owner,SageHeart.effect(owner,effect));
    }
    @Inject(method="canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",at=@At("HEAD"),cancellable=true)
    private void sageAttack(LivingEntity target,CallbackInfoReturnable<Boolean> callback){if((Object)this instanceof Mob mob&&SageHeart.enemyOf(mob,target))callback.setReturnValue(true);}
}
