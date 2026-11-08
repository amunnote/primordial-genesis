package local.primordial.mixin;

import local.primordial.effect.CooldownBridge;
import local.primordial.effect.Protection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerGuardMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void angelDrop(net.minecraft.world.item.ItemStack stack, boolean random, boolean trace,
                           CallbackInfoReturnable<net.minecraft.world.entity.item.ItemEntity> cir) {
        if (Protection.active((Entity)(Object)this) && !local.primordial.effect.InventoryProtection.allowsDrop()) cir.setReturnValue(null);
    }
    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void angelDisarm(net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
        Player player = (Player)(Object)this;
        if (local.primordial.effect.InventoryProtection.locked(player)
            && local.primordial.effect.InventoryProtection.removes(player.getItemBySlot(slot), stack)) ci.cancel();
    }
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void angelHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Protection.active((Entity)(Object)this)) cir.setReturnValue(false);
    }
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void angelDie(DamageSource source, CallbackInfo ci) { if (Protection.active((Entity)(Object)this)) ci.cancel(); }
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void angelRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (Protection.lethalRemoval(reason) && Protection.active((Entity)(Object)this)) ci.cancel();
    }
    @Inject(method = "getCooldowns", at = @At("RETURN"))
    private void angelOwner(CallbackInfoReturnable<ItemCooldowns> cir) {
        CooldownBridge.bind(cir.getReturnValue(), (Player)(Object)this);
    }
}
