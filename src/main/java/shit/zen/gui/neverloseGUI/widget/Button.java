package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

public class Button extends Component {
    private final String label;
    private final Runnable action;

    public Button(float x, float y, float w, float h, String label, Runnable action) {
        super(x, y, w, h); this.label = label; this.action = action;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        int bg;
        if (ha > 0.5f) bg = Render2D.alpha(Colors.ACCENT, alpha * 0.45f);
        else if (ha > 0.01f) bg = Render2D.alpha(Colors.CARD, alpha * 0.6f);
        else bg = Render2D.alpha(Colors.PANEL, alpha * 0.5f);
        Render2D.drawRoundRect(ps, x, y, w, h, 6f, bg);
        int tc = ha > 0.3f ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY;
        GlHelper.drawText(label, x + (w - GlHelper.getStringWidth(label, Typography.TINY)) / 2f, y + (h - Typography.TINY.getMetrics().capHeight()) / 2f, Typography.TINY, Render2D.alpha(tc, alpha));
    }
    @Override public boolean mouseClicked(double mx, double my, int btn) { if (btn == 0 && action != null) { action.run(); return true; } return false; }
}
