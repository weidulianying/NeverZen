package shit.zen.gui.neverloseGUI.hud.watermark;

import shit.zen.hud.WatermarkModel;
import shit.zen.render.*;
import shit.zen.ui.neverlose.NeverloseTheme;

/**
 * Static renderer for the NeverZen watermark bar.
 * <p>
 * Uses Neverlose-style background construction ({@link GlHelper#drawRoundedRect}
 * with {@link Paint}) and Neverlose Material Icons for each data section.
 *
 * <h3>Layout</h3>
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────┐
 * │ NZ     446 FPS     0 MS     Local World     lianying     18:34 │
 * └──────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Color table</h3>
 * <table>
 *   <tr><td>Background</td><td>NeverloseTheme.BG_PANEL</td><td>0xB3121212 ~70%</td></tr>
 *   <tr><td>NZ fill</td><td>#FFFFFF</td><td></td></tr>
 *   <tr><td>NZ outline</td><td>NeverloseTheme.ACCENT</td><td>#577FFF</td></tr>
 *   <tr><td>Icons</td><td>NeverloseTheme.ACCENT</td><td>#577FFF</td></tr>
 *   <tr><td>FPS gradient</td><td>#FFFFFF → #5A8CFF</td><td></td></tr>
 *   <tr><td>Normal text</td><td>#C8D1E0</td><td></td></tr>
 * </table>
 */
public final class WatermarkRenderer {

    // ── Design tokens ──
    private static final int BG              = NeverloseTheme.BG_PANEL;   // 0xB3121212
    private static final int NZ_OUTLINE      = NeverloseTheme.ACCENT;     // 0xFF577FFF
    private static final int NZ_FILL         = NeverloseTheme.TEXT;       // 0xFFFFFFFF
    private static final int ICON_COLOR      = NeverloseTheme.ACCENT;     // 0xFF577FFF
    private static final int TEXT_NORMAL     = 0xFFC8D1E0;
    private static final int GRADIENT_START  = 0xFFFFFFFF;
    private static final int GRADIENT_END    = 0xFF5A8CFF;

    // ── Neverlose Material Icons ──
    private static final String ICON_FPS    = "";   // monitor
    private static final String ICON_PING   = "";    // latency
    private static final String ICON_SERVER = "";  // globe
    private static final String ICON_USER   = "";  // location
    private static final String ICON_TIME   = "";    // clock
    private static final String ICON_SEP    = "▮";     // vertical bar (for after NZ)

    private static final float RADIUS       = NeverloseTheme.RADIUS;      // 8f
    private static final float PADDING      = NeverloseTheme.PADDING;     // 8f
    private static final float GAP          = 6f;
    private static final float BAR_HEIGHT   = 28f;

    private static final FontRenderer LOGO_FONT  = FontPresets.axiformaBold(14f);
    private static final FontRenderer TEXT_FONT  = FontPresets.axiformaRegular(13f);
    private static final FontRenderer ICON_FONT  = FontPresets.materialIcons(15f);

    private static final Paint BG_PAINT = new Paint().setColor(BG);

    private WatermarkRenderer() {}

    // ════════════════════════════════════════════════════════════════
    //  Public entry point
    // ════════════════════════════════════════════════════════════════

    public static void render(float x, float y, float w, float h,
                              WatermarkModel model) {

        Renderer.renderConsumer(dc -> {

            // ── Layer 1: Dark background (Neverlose method) ──
            GlHelper.drawRoundedRect(x, y, w, h, RADIUS, BG_PAINT);

            // ── Layer 2: Text content ──
            float capHeight = TEXT_FONT.getMetrics().capHeight();
            float ty = y + (BAR_HEIGHT - capHeight) / 2f;
            float tx = x + PADDING;

            // NZ logo
            tx = drawNZ(tx, ty);
            tx += GAP;

            // Separator
            tx = drawSeparator(tx, ty);
            tx += GAP;

            // Icon + FPS
            tx = drawIconText(tx, ty, ICON_FPS, model.fps() + " FPS");
            tx += GAP;

            // Icon + Ping  (icon: clock ← was time)
            tx = drawIconText(tx, ty, ICON_TIME, model.ping() + " MS");
            tx += GAP;

            // Icon + Server  (icon: latency ← was ping)
            tx = drawIconText(tx, ty, ICON_PING, model.server());
            tx += GAP;

            // Icon + Username  (icon: globe ← was server)
            tx = drawIconText(tx, ty, ICON_SERVER, model.username());
            tx += GAP;

            // Icon + Time  (icon: location ← was username)
            drawIconText(tx, ty, ICON_USER, model.time());
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  Drawing helpers
    // ════════════════════════════════════════════════════════════════

    /** Dual-outline NZ logo — accent outline + white interior. */
    static float drawNZ(float x, float y) {
        GlHelper.drawText("NZ", x - 1, y, LOGO_FONT, NZ_OUTLINE);
        GlHelper.drawText("NZ", x + 1, y, LOGO_FONT, NZ_OUTLINE);
        GlHelper.drawText("NZ", x, y - 1, LOGO_FONT, NZ_OUTLINE);
        GlHelper.drawText("NZ", x, y + 1, LOGO_FONT, NZ_OUTLINE);
        GlHelper.drawText("NZ", x, y, LOGO_FONT, NZ_FILL);
        return x + GlHelper.getStringWidth("NZ", LOGO_FONT);
    }

    /** Separator bar after NZ logo. */
    static float drawSeparator(float x, float y) {
        GlHelper.drawText(ICON_SEP, x, y, TEXT_FONT, ICON_COLOR);
        return x + GlHelper.getStringWidth(ICON_SEP, TEXT_FONT);
    }

    /**
     * Draws a Material Icon in accent colour followed by the label text.
     * The icon and label share the same baseline.
     */
    static float drawIconText(float x, float y, String icon, String text) {
        // Icon in accent colour
        GlHelper.drawText(icon, x, y, ICON_FONT, ICON_COLOR);
        float iconW = GlHelper.getStringWidth(icon, ICON_FONT);

        // Label text — FPS gets gradient, everything else normal
        float textX = x + iconW + 3f;

        if (text.endsWith("FPS")) {
            drawGradientText(text, textX, y, TEXT_FONT, GRADIENT_START, GRADIENT_END);
        } else {
            GlHelper.drawText(text, textX, y, TEXT_FONT, TEXT_NORMAL);
        }

        return textX + GlHelper.getStringWidth(text, TEXT_FONT);
    }

    // ════════════════════════════════════════════════════════════════
    //  Gradient text helper
    // ════════════════════════════════════════════════════════════════

    /** Per-character colour interpolation from startColor to endColor. */
    private static void drawGradientText(String text, float x, float y, FontRenderer font,
                                         int startColor, int endColor) {
        float cx = x;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            String ch = text.substring(i, i + 1);
            float t = len > 1 ? (float) i / (len - 1) : 0f;
            int color = lerpColor(startColor, endColor, t);
            GlHelper.drawText(ch, cx, y, font, color);
            cx += GlHelper.getStringWidth(ch, font);
        }
    }

    private static int lerpColor(int a, int b, float t) {
        return ((int)((a>>24&0xFF)+((b>>24&0xFF)-(a>>24&0xFF))*t))<<24
            | ((int)((a>>16&0xFF)+((b>>16&0xFF)-(a>>16&0xFF))*t))<<16
            | ((int)((a>>8&0xFF)+((b>>8&0xFF)-(a>>8&0xFF))*t))<<8
            |  (int)((a&0xFF)+((b&0xFF)-(a&0xFF))*t);
    }
}
