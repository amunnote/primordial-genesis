package local.primordial.mixin;

import local.primordial.alchemy.MysteryEnchanting;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The enchanting table opening a Mystery Recipe (see MysteryEnchanting); with neither of its things in it, the table is as it was. */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMysteryMixin {
    // no shadowed fields: the annotation processor left them out of the refmap, and outside the dev run they were not
    // found (the game would not start). The private ones come through EnchantmentMenuAccessor, the public ones as they are.
    @Unique private Player angelCharm$player;

    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at=@At("TAIL"))
    private void angelCharm$remember(int id,Inventory inventory,ContainerLevelAccess access,CallbackInfo info) {angelCharm$player=inventory.player;}

    @Inject(method="slotsChanged",at=@At("HEAD"),cancellable=true)
    private void angelCharm$offers(Container container,CallbackInfo info) {
        var menu=(EnchantmentMenu)(Object)this;var enchantSlots=((EnchantmentMenuAccessor)menu).primordial$enchantSlots();
        if(container!=enchantSlots||angelCharm$player==null||!MysteryEnchanting.active(enchantSlots))return;
        // the server deals the rows; the client keeps what it is sent rather than clearing it
        if(!angelCharm$player.level().isClientSide){
            MysteryEnchanting.fill(angelCharm$player.level(),angelCharm$player,enchantSlots,menu.costs,menu.enchantClue,menu.levelClue);
            ((EnchantmentMenuAccessor)menu).primordial$enchantmentSeed().set(angelCharm$player.getEnchantmentSeed());
        }
        info.cancel();
    }

    @Inject(method="clickMenuButton",at=@At("HEAD"),cancellable=true)
    private void angelCharm$choose(Player player,int row,CallbackInfoReturnable<Boolean> info) {
        var menu=(EnchantmentMenu)(Object)this;var enchantSlots=((EnchantmentMenuAccessor)menu).primordial$enchantSlots();
        if(!MysteryEnchanting.active(enchantSlots))return;
        boolean chosen=MysteryEnchanting.choose(player,enchantSlots,menu.enchantClue,menu.levelClue,row);
        if(chosen&&!player.level().isClientSide)menu.slotsChanged(enchantSlots);
        info.setReturnValue(chosen);
    }
}
