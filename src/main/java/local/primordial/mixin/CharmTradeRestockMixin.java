package local.primordial.mixin;

import local.primordial.effect.AlchemistEvents;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Librarian charm offers are one of a kind: restocking never brings them back. Keyed on the result, so it survives saving. */
@Mixin(MerchantOffer.class)
public abstract class CharmTradeRestockMixin {
    @Inject(method="resetUses",at=@At("HEAD"),cancellable=true)
    private void angelCharmNoRestock(CallbackInfo callback) {
        if(AlchemistEvents.CharmOrBook.isCharmOffer((MerchantOffer)(Object)this))callback.cancel();
    }
}
