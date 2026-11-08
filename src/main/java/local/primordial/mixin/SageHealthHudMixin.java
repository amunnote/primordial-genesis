package local.primordial.mixin;
import local.primordial.client.SageHud;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=ForgeGui.class,remap=false)
public abstract class SageHealthHudMixin {
    // Forge includes the remembered pre-damage health when allocating slots.
    // Bound that visual history to the current cursed health ceiling.
    @Redirect(method="renderHealth",at=@At(value="INVOKE",target="Ljava/lang/Math;max(FF)F"))
    private float sageVisibleHealth(float maximum,float remembered){return SageHud.healthSlots(Math.max(maximum,remembered),Minecraft.getInstance().player);}
}
