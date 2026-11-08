package local.primordial.mixin;
import local.primordial.sage.SageHeart;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(TamableAnimal.class)
public abstract class SagePetMixin {
    @Inject(method="canAttack",at=@At("HEAD"),cancellable=true)
    private void sageAttack(LivingEntity target,CallbackInfoReturnable<Boolean> callback){if(SageHeart.enemyOf((Mob)(Object)this,target))callback.setReturnValue(true);}
    @Inject(method="isAlliedTo",at=@At("HEAD"),cancellable=true)
    private void sageAlliance(Entity target,CallbackInfoReturnable<Boolean> callback){if(SageHeart.enemyOf((Mob)(Object)this,target))callback.setReturnValue(false);}
}
