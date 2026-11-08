package local.primordial.mixin;

import local.primordial.item.AlchemyCharmItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Pact of the Swift String: loading a crossbow copies the arrow instead of using it up, as in creative mode. */
@Mixin(CrossbowItem.class)
public abstract class CharmCrossbowMixin {
    @ModifyArg(method="tryLoadProjectiles",index=4,at=@At(value="INVOKE",
        target="Lnet/minecraft/world/item/CrossbowItem;loadProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;ZZ)Z"))
    private static boolean angelCharmFreeBolts(LivingEntity shooter,ItemStack crossbow,ItemStack ammo,boolean hasAmmo,boolean creative) {
        return creative||AlchemyCharmItem.infiniteAmmo(shooter);
    }
}
