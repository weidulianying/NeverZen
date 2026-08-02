package shit.zen.gui.neverloseGUI.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import shit.zen.render.RoundedRectangle;
import shit.zen.render.Renderer;
import shit.zen.utils.render.RenderUtil;

/** Unified 2D drawing — every shape passes through here. */
public final class Render2D {
    private Render2D() {}

    public static void drawRect(PoseStack ps, float x, float y, float w, float h, int color)       { RenderUtil.drawFilledRect(ps, x, y, w, h, color); }
    public static void drawRoundRect(PoseStack ps, float x, float y, float w, float h, float r, int c) { RenderUtil.drawRoundedRect(ps, x, y, w, h, r, c); }
    public static void drawGradientV(PoseStack ps, float x, float y, float w, float h, int top, int bot) { RenderUtil.drawGradientV(ps, x, y, w, h, top, bot); }
    public static void drawGradientH(PoseStack ps, float x, float y, float w, float h, int l, int r)  { RenderUtil.drawGradientH(ps, x, y, w, h, l, r); }

    public static void drawTexture(PoseStack ps, ResourceLocation texture, float x, float y,
                                   float w, float h, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderUtil.drawTexture(texture, ps, x, y, w, h, alpha, 0xFFFFFFFF);
        RenderSystem.disableBlend();
    }

    /** Draws an opaque-background logo without replacing the panel colour behind dark pixels. */
    public static void drawTextureAdditive(PoseStack ps, ResourceLocation texture, float x, float y,
                                           float w, float h, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderUtil.drawTexture(texture, ps, x, y, w, h, alpha, 0xFFFFFFFF);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public static void drawCircle(PoseStack ps, float cx, float cy, float radius, int color) {
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = ps.last().pose();
        float a = (color >> 24 & 0xFF) / 255f, rr = (color >> 16 & 0xFF) / 255f, g = (color >> 8 & 0xFF) / 255f, b = (color & 0xFF) / 255f;
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(m, cx, cy, 0f).color(rr, g, b, a).endVertex();
        for (int i = 0; i <= 64; i++) { double ang = (double)i/64*Math.PI*2.0; buf.vertex(m, cx+(float)Math.cos(ang)*radius, cy+(float)Math.sin(ang)*radius, 0f).color(rr, g, b, a).endVertex(); }
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.disableBlend();
    }

    public static void pushScissor(int x, int y, int w, int h) { RenderUtil.pushScissor(x, y, w, h); }
    public static void popScissor() { RenderUtil.popScissor(); }

    public static void drawShadow(PoseStack ps, float x, float y, float w, float h, float radius, float blur, int color) {
        Renderer.renderConsumer(dc -> dc.drawBlurredRoundedRect(RoundedRectangle.ofXYWHR(x, y, w, h, radius), 0, 0, blur, 0, color));
    }

    /** Frosted-glass background blur — blurs content behind the rect, then fills with the given color. */
    public static void drawBlur(PoseStack ps, float x, float y, float w, float h, float radius, float blur, int color) {
        RenderUtil.drawBlurredRect(ps, x, y, w, h, radius, blur, 1f, color);
    }

    public static int alpha(int c, float a) { return ((int)((c>>24&0xFF)*clamp(a)))<<24 | (c&0xFFFFFF); }
    public static int lerpColor(int a, int b, float t) {
        return ((int)((a>>24&0xFF)+((b>>24&0xFF)-(a>>24&0xFF))*t))<<24|((int)((a>>16&0xFF)+((b>>16&0xFF)-(a>>16&0xFF))*t))<<16|((int)((a>>8&0xFF)+((b>>8&0xFF)-(a>>8&0xFF))*t))<<8|(int)((a&0xFF)+((b&0xFF)-(a&0xFF))*t);
    }
    public static boolean contains(float x, float y, float w, float h, float px, float py) { return px>=x&&px<=x+w&&py>=y&&py<=y+h; }
    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }
}
