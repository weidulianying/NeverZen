package shit.zen.gui.neverloseGUI.hud.watermark;

import net.minecraft.client.Minecraft;
import shit.zen.hud.WatermarkModel;

/**
 * Neverlose-style watermark — top-right corner bar.
 * <p>
 * Uses Neverlose's dark-background construction method and Material Icons.
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────┐
 * │ NZ ▮  446 FPS   0 MS   Local World   lianying   18:34 │
 * └──────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class Watermark {

    private float x, y;
    private final float height = 28;
    private final float width = 260;

    private final WatermarkModel model = new WatermarkModel();

    private static final float MARGIN = 15f;

    public void render(Minecraft mc) {
        if (mc == null) return;

        model.tick(mc);

        float screenWidth = mc.getWindow().getGuiScaledWidth();

        x = screenWidth - width - MARGIN;
        y = MARGIN;

        WatermarkRenderer.render(x, y, width, height, model);
    }

    // ── Accessors ──

    public float x() { return x; }
    public float y() { return y; }
    public float width() { return width; }
    public float height() { return height; }

    public boolean contains(float mx, float my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }
}
