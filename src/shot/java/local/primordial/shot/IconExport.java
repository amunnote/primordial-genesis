package local.primordial.shot;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

/**
 * An item's icon as the inventory draws it (a block in its three-quarter view, an item flat), drawn alone at a given
 * size onto a clear background and saved as a PNG: icons/&lt;size&gt;/&lt;id&gt;.png in the game directory. For wiki pages,
 * which want the game's own icons at 32 and 128 pixels.
 */
final class IconExport {
    private IconExport() {}

    static void export(Minecraft mc,String id,int size) throws Exception {
        var key=new ResourceLocation(id.contains(":")?id:"primordial:"+id);
        var item=ForgeRegistries.ITEMS.getValue(key);
        if(item==null||!ForgeRegistries.ITEMS.containsKey(key))throw new IllegalArgumentException("no item "+key);
        var stack=new ItemStack(item);
        var target=new TextureTarget(size,size,true,Minecraft.ON_OSX);
        target.setClearColor(0,0,0,0);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,size,size,0,1000,3000),VertexSorting.ORTHOGRAPHIC_Z);
        var view=RenderSystem.getModelViewStack();view.pushPose();view.setIdentity();view.translate(0,0,-2000);RenderSystem.applyModelViewMatrix();
        try{
            // as GuiGraphics.renderItem places a 16-pixel item, grown to the whole image
            var pose=new PoseStack();
            pose.translate(size/2F,size/2F,150);
            pose.mulPoseMatrix(new Matrix4f().scaling(1,-1,1));
            pose.scale(size,size,size);
            var model=mc.getItemRenderer().getModel(stack,mc.level,null,0);
            if(model.usesBlockLight())Lighting.setupFor3DItems();else Lighting.setupForFlatItems();
            RenderSystem.enableDepthTest();
            var buffers=mc.renderBuffers().bufferSource();
            mc.getItemRenderer().render(stack,ItemDisplayContext.GUI,false,pose,buffers,0xF000F0,OverlayTexture.NO_OVERLAY,model);
            buffers.endBatch();
            // read it back with its alpha (the game's screenshot makes every pixel opaque)
            var image=new NativeImage(size,size,false);
            RenderSystem.bindTexture(target.getColorTextureId());
            image.downloadTexture(0,false);image.flipY();
            var out=mc.gameDirectory.toPath().resolve("icons").resolve(Integer.toString(size));
            Files.createDirectories(out);
            image.writeToFile(out.resolve(key.getPath()+".png"));
            image.close();
        }finally{
            view.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.restoreProjectionMatrix();Lighting.setupFor3DItems();
            target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);
        }
    }
}
