package local.primordial.mixin;
import local.primordial.sage.SageHeart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FoodData.class)
public abstract class SageFoodMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void sageNoHunger(Player player,CallbackInfo ci){if(SageHeart.active(player))ci.cancel();}
}
