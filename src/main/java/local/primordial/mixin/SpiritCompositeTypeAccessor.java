package local.primordial.mixin;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** a composite render type's state, to find the texture it draws (spirits are drawn see-through with it) */
@Mixin(targets="net.minecraft.client.renderer.RenderType$CompositeRenderType")
public interface SpiritCompositeTypeAccessor {
    @Invoker("state") RenderType.CompositeState angel$state();
}