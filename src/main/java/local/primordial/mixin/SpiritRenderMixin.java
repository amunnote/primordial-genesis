package local.primordial.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import local.primordial.client.SpiritRealmClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** every creature in the Spirit Realm is drawn see-through, whatever renderer it has (vanilla, GeckoLib, other mods) */
@Mixin(EntityRenderDispatcher.class)
public abstract class SpiritRenderMixin {
    @ModifyArg(method="render",index=4,at=@At(value="INVOKE",
        target="Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private MultiBufferSource angel$spiritBuffers(Entity entity,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
        return SpiritRealmClient.spiritBuffers(entity,buffers);
    }
}