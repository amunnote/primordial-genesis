package local.primordial.mixin;
import local.primordial.sage.SageHeart;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class SageAllianceMixin {
    @Inject(method="isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
    private void sageAlliance(Entity target,CallbackInfoReturnable<Boolean> callback){if((Object)this instanceof Mob mob&&SageHeart.enemyOf(mob,target))callback.setReturnValue(false);}
}
