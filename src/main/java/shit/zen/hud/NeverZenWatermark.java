package shit.zen.hud;

import shit.zen.ClientBase;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.gui.neverloseGUI.hud.watermark.Watermark;

/**
 * NeverZen watermark bar — thin wrapper that delegates to the modular
 * {@link Watermark} renderer in {@code gui.neverloseGUI.hud.watermark}.
 * <p>
 * Glassmorphism background, dual-outline NZ logo, gradient FPS, icon-coloured
 * separators, and periodic shine scan are all handled by the watermark package.
 */
public class NeverZenWatermark {

    private final Watermark watermark = new Watermark();

    public void onRender2D(Render2DEvent e) {
        if (ClientBase.mc == null || ClientBase.mc.player == null) return;
        if (ClientBase.mc.options.renderDebug) return;

        watermark.render(ClientBase.mc);
    }
}
