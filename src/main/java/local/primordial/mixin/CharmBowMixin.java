package local.primordial.mixin;

import local.primordial.item.AlchemyCharmItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pact of the Swift String: a bow treats every arrow as infinite, like the Infinity enchantment. */
@Mixin(BowItem.class)
public abstract class CharmBowMixin {
    @Redirect(method="releaseUsing",at=@At(value="INVOKE",remap=false,
        target="Lnet/minecraft/world/item/ArrowItem;isInfinite(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean angelCharmFreeArrows(ArrowItem arrow,ItemStack ammo,ItemStack bow,Player player) {
        return arrow.isInfinite(ammo,bow,player)||AlchemyCharmItem.infiniteAmmo(player);
    }
}
