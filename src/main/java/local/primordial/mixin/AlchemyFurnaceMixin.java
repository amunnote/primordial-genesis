package local.primordial.mixin;

import local.primordial.Primordial;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AlchemyFurnaceMixin {
    @Unique private boolean angel_charm$amethystFlame;

    @Inject(method = "getBurnDuration", at = @At("HEAD"))
    private void angel_charm$rememberFuel(ItemStack fuel, CallbackInfoReturnable<Integer> callback) {
        angel_charm$amethystFlame = fuel.is(Items.AMETHYST_SHARD);
    }

    @Inject(method = "canBurn", at = @At("HEAD"), cancellable = true)
    private void angel_charm$requireAmethyst(RegistryAccess registryAccess, Recipe<?> recipe,
            NonNullList<ItemStack> contents, int maxStackSize, CallbackInfoReturnable<Boolean> callback) {
        if (recipe == null || !angel_charm$isSourceRecipe(recipe.getId())) return;
        if (((AlchemyFurnaceAccessor)this).angel_charm$getLitTime() > 0 ? !angel_charm$amethystFlame : !contents.get(1).is(Items.AMETHYST_SHARD)) {
            callback.setReturnValue(false);
        }
    }

    @Unique
    private static boolean angel_charm$isSourceRecipe(ResourceLocation id) {
        return id.getNamespace().equals(Primordial.ID) &&
                (id.getPath().equals("alchemical_source_ingot_from_smelting")
                        || id.getPath().equals("alchemical_source_ingot_from_blasting"));
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void angel_charm$loadFuel(CompoundTag tag, CallbackInfo callback) {
        angel_charm$amethystFlame = tag.getBoolean("primordial:amethyst_flame");
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void angel_charm$saveFuel(CompoundTag tag, CallbackInfo callback) {
        tag.putBoolean("primordial:amethyst_flame", angel_charm$amethystFlame);
    }
}
