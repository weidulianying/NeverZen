package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

public class Slider extends Component {
    private float min, max, step, value;
    private boolean dragging;
    private final Animation dragAnim = new Animation();
    private final String label;
    private final Consumer<Float> onChange;

    public Slider(float x, float y, float w, String label, float min, float max, float step, float initial, Consumer<Float> onChange) {
        super(x, y, w, 18);
        this.label = label; this.min = min; this.max = max; this.step = step; this.value = initial; this.onChange = onChange;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        dragAnim.animate(dragging ? 1f : 0f);
        float da = dragAnim.update(Animation.SPEED_DRAG), trackH = 4 + da * 2, sy = y + (h - trackH) / 2f;
        float frac = (value - min) / (max - min);
        // Value label (right of track)
        String txt = String.format("%.1f", value);
        GlHelper.drawText(txt, x + w + 6, y + (h - Typography.SMALL.getMetrics().capHeight()) / 2f, Typography.SMALL, Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
        Render2D.drawRoundRect(ps, x, sy, w, trackH, 2f, Render2D.alpha(Colors.TOGGLE_OFF, alpha));
        Render2D.drawRoundRect(ps, x, sy, w * frac, trackH, 2f, Render2D.alpha(Colors.ACCENT, alpha));
        float thumbR = 3 + da * 2, tx = x + w * frac - thumbR;
        Render2D.drawRoundRect(ps, tx, y + (h - thumbR * 2) / 2f, thumbR * 2, thumbR * 2, thumbR, Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) { if (btn == 0) { dragging = true; update((float) mx); return true; } return false; }
    @Override public void mouseReleased(double mx, double my, int btn) { dragging = false; }
    @Override public void mouseDragged(double mx, double my) { if (dragging) update((float) mx); }
    private void update(float mx) {
        float frac = (mx - x) / w, raw = min + frac * (max - min);
        value = Math.max(min, Math.min(max, Math.round(raw / step) * step));
        if (onChange != null) onChange.accept(value);
    }
}
