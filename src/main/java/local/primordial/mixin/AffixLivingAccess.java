package local.primordial.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface AffixLivingAccess {
    @Invoker("hurtArmor") void angelAffixHurtArmor(DamageSource source,float amount);
}
