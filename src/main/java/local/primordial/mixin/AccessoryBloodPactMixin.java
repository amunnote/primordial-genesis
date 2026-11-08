package local.primordial.mixin;

import java.util.ArrayDeque;
import local.primordial.affix.AffixCombat;
import local.primordial.effect.AffixAccessoryEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=LivingEntity.class,priority=1100)
public abstract class AccessoryBloodPactMixin {
    @Unique private final ArrayDeque<DamageSource> angelBloodSources=new ArrayDeque<>();
    @Unique private final ArrayDeque<Boolean> angelBloodSecondary=new ArrayDeque<>();
    @Unique private final ArrayDeque<Float> angelBloodHealth=new ArrayDeque<>();
    @Inject(method="hurt",at=@At("HEAD"))
    private void angelBloodStart(DamageSource source,float amount,CallbackInfoReturnable<Boolean> callback) {
        angelBloodSources.push(source);angelBloodSecondary.push(AffixCombat.secondary());
    }
    @Inject(method="hurt",at=@At("RETURN"))
    private void angelBloodEnd(DamageSource source,float amount,CallbackInfoReturnable<Boolean> callback) {
        angelBloodSources.pop();angelBloodSecondary.pop();
    }
    @Inject(method="setHealth",at=@At("HEAD"))
    private void angelBloodBefore(float health,CallbackInfo callback) {
        angelBloodHealth.push(((LivingEntity)(Object)this).getHealth());
    }
    @Inject(method="setHealth",at=@At("RETURN"))
    private void angelBloodAfter(float health,CallbackInfo callback) {
        var victim=(LivingEntity)(Object)this;
        float lost=angelBloodHealth.pop()-victim.getHealth();
        if(victim.level().isClientSide||lost<=0||angelBloodSources.isEmpty()||angelBloodSecondary.peek())return;
        if(angelBloodSources.peek().getEntity() instanceof Player player)AffixAccessoryEvents.bloodPact(player,victim,lost);
    }
}
