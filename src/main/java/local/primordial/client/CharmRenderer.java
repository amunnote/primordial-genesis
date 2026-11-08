package local.primordial.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import local.primordial.Primordial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class CharmRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation SOURCE = new ResourceLocation(Primordial.ID, "textures/item/ascension_charm.png");
    private static CharmRenderer instance;
    private static CharmRenderer crystalInstance;
    private final ResourceLocation source;
    private final String texturePrefix;
    private SpriteFrames.Frame[] frames;
    private final ResourceLocation[] textures = new ResourceLocation[SpriteFrames.FRAME_COUNT];
    private int displaySize;
    private int lastFrame=-1;
    private final long origin = System.nanoTime();
    private CharmRenderer(ResourceLocation source, String texturePrefix) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.source = source;this.texturePrefix = texturePrefix;
    }
    public static CharmRenderer instance() {
        if (instance == null) instance = new CharmRenderer(SOURCE, "ascension");
        return instance;
    }
    public static CharmRenderer crystalInstance() {
        if (crystalInstance == null) crystalInstance = new CharmRenderer(
                new ResourceLocation(Primordial.ID, "textures/item/purification_crystal_sheet.png"), "purification_crystal");
        return crystalInstance;
    }
    public void invalidate() {
        for (int i = 0; i < textures.length; i++) if (textures[i] != null) {
            Minecraft.getInstance().getTextureManager().release(textures[i]);
            textures[i] = null;
        }
        frames = null;lastFrame=-1;
    }
    private void load(ResourceManager manager) {
        try (var stream = manager.open(source); NativeImage image = NativeImage.read(stream)) {
            int sourceWidth = image.getWidth(),sourceHeight = image.getHeight();
            int[] pixels = new int[sourceWidth * sourceHeight];
            for (int y = 0; y < sourceHeight; y++) for (int x = 0; x < sourceWidth; x++)
                pixels[y * sourceWidth + x] = image.getPixelRGBA(x, y);
            frames = SpriteFrames.split(sourceWidth, sourceHeight, pixels);
            displaySize=SpriteFrames.displaySize(frames);
            for (int i = 0; i < frames.length; i++) {
                var frame = frames[i];
                NativeImage texture = new NativeImage(frame.width(), frame.height(), true);
                for (int y = 0; y < frame.height(); y++) for (int x = 0; x < frame.width(); x++)
                    texture.setPixelRGBA(x, y, frame.pixels()[y * frame.width() + x]);
                textures[i] = new ResourceLocation(Primordial.ID, "dynamic/" + texturePrefix + "_frame_" + i);
                Minecraft.getInstance().getTextureManager().register(textures[i], new FrameTexture(texture));
                // only its place is kept: the pixels now live on the graphics card alone
                frames[i] = new SpriteFrames.Frame(frame.width(), frame.height(), null, frame.originX(), frame.originY(),
                        frame.centerX(), frame.centerY(), frame.visibleSize());
            }
        } catch (IOException e) { throw new IllegalStateException("Cannot read sprite sheet " + source, e); }
    }
    public int lastRenderedFrame() {return lastFrame;}
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        if (frames == null) load(Minecraft.getInstance().getResourceManager());
        int index=SpriteFrames.frameIndex(System.nanoTime()-origin);
        var frame=frames[index];lastFrame=index;
        double scale=1.0/displaySize;
        pose.pushPose();pose.translate(0.5,0.5,0);
        float left=(float)(-frame.centerX()*scale),right=left+(float)(frame.width()*scale);
        float top=(float)(frame.centerY()*scale),bottom=top-(float)(frame.height()*scale);
        VertexConsumer vertices=buffers.getBuffer(RenderType.entityTranslucentEmissive(textures[index]));
        quad(vertices,pose.last().pose(),pose.last().normal(),left,right,bottom,top,light,overlay,false);
        // No extra rotation: the animation comes only from the eight supplied frames.
        pose.popPose();
    }
    /** a frame uploaded once and let go of, where DynamicTexture would keep a copy of its pixels in memory */
    private static final class FrameTexture extends AbstractTexture {
        FrameTexture(NativeImage image) {
            TextureUtil.prepareImage(getId(), image.getWidth(), image.getHeight());
            image.upload(0, 0, 0, true);
        }
        @Override public void load(ResourceManager manager) {}
    }
    private static void quad(VertexConsumer v, Matrix4f m, Matrix3f n, float left, float right,
                             float bottom, float top, int light, int overlay, boolean back) {
        if (back) {
            vertex(v,m,n,left,bottom,0,1,light,overlay,-1);
            vertex(v,m,n,left,top,0,0,light,overlay,-1);
            vertex(v,m,n,right,top,1,0,light,overlay,-1);
            vertex(v,m,n,right,bottom,1,1,light,overlay,-1);
        } else {
            vertex(v,m,n,left,bottom,0,1,light,overlay,1);
            vertex(v,m,n,right,bottom,1,1,light,overlay,1);
            vertex(v,m,n,right,top,1,0,light,overlay,1);
            vertex(v,m,n,left,top,0,0,light,overlay,1);
        }
    }
    private static void vertex(VertexConsumer v, Matrix4f m, Matrix3f n, float x, float y,
                                float u, float vv, int light, int overlay, int z) {
        v.vertex(m,x,y,0).color(255,255,255,255).uv(u,vv).overlayCoords(overlay).uv2(light).normal(n,0,0,z).endVertex();
    }
}
