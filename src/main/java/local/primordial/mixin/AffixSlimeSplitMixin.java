package local.primordial.mixin;

import local.primordial.affix.AffixState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A dying slime or magma cube splits into smaller ones that copy its name. They now take over its affixes instead of
 * rolling new ones on top of the old name; being pieces of a mob that already dropped its loot, they drop no essence or
 * materials (marked as split copies).
 */
@Mixin(Slime.class)
public abstract class AffixSlimeSplitMixin {
    @Redirect(method="remove",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean angelAffixSplit(Level level,Entity child) {
        if(!level.isClientSide&&child instanceof Mob piece)AffixState.splitPiece((Slime)(Object)this,piece);
        return level.addFreshEntity(child);
    }
}
