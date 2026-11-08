package local.primordial.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import local.primordial.affix.AffixGrowth;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws Growth mobs at their size; right after the renderer's own scale step, so every mob type gets it. */
@Mixin(LivingEntityRenderer.class)
public abstract class AffixGrowthRenderMixin {
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;scale(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",shift=At.Shift.AFTER))
    private void angelGrowthRender(LivingEntity entity,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci) {
        float growth=AffixGrowth.growth(entity);
        if(growth>1)pose.scale(growth,growth,growth);
    }
}
