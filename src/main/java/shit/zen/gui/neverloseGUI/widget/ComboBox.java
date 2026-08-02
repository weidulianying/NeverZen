package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

public class ComboBox extends Component {
    private final String[] options;
    private int selected;
    private boolean open;
    private final Animation popupAnim = new Animation();
    private int hoveredOpt = -1;
    private final IntConsumer onChange;
    private static final float OPT_H = 18;

    public ComboBox(float x, float y, float w, String[] options, int def, IntConsumer onChange) {
        super(x, y, w, 20); this.options = options; this.selected = def; this.onChange = onChange;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        popupAnim.animate(open ? 1f : 0f);
        float pa = popupAnim.update(Animation.SPEED_EXPAND);

        Render2D.drawRoundRect(ps, x, y, w, h, 6f, Render2D.alpha(open ? Colors.CARD : Colors.PANEL, alpha * 0.6f));
        GlHelper.drawText(options[selected], x + 6, y + (h - Typography.SMALL.getMetrics().capHeight()) / 2f, Typography.SMALL, Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
        GlHelper.drawText("▼", x + w - 14, y + 1, Typography.ICON, Render2D.alpha(Colors.TEXT_SECONDARY, alpha));

        if (pa > 0.01f) {
            float popY = y + h + 2; int vis = (int) (options.length * pa); float popH = vis * OPT_H;
            Render2D.drawRoundRect(ps, x, popY, w, popH, 8f, Render2D.alpha(Colors.CARD, alpha * pa));
            hoveredOpt = -1;
            for (int i = 0; i < Math.min(vis, options.length); i++) {
                float oy = popY + i * OPT_H;
                if (Render2D.contains(x, oy, w, OPT_H, mx, my)) hoveredOpt = i;
                if (hoveredOpt == i) Render2D.drawRoundRect(ps, x + 2, oy + 1, w - 4, OPT_H - 2, 3f, Render2D.alpha(Colors.ACCENT, alpha * 0.2f * pa));
                int tc = i == selected ? Colors.ACCENT : (hoveredOpt == i ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY);
                GlHelper.drawText(options[i], x + 8, oy + (OPT_H - Typography.SMALL.getMetrics().capHeight()) / 2f, Typography.SMALL, Render2D.alpha(tc, alpha * pa));
            }
        }
    }

    @Override public boolean contains(double mx, double my) {
        if (super.contains(mx, my)) return true;
        if (open) return mx >= x && mx <= x + w && my >= y && my <= y + h + 2 + options.length * OPT_H;
        return false;
    }
    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (super.contains(mx, my)) { open = !open; return true; }
        if (open && hoveredOpt >= 0) { selected = hoveredOpt; open = false; if (onChange != null) onChange.accept(selected); return true; }
        if (open) { open = false; return true; }
        return false;
    }
}
