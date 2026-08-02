package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

public class SearchBox extends Component {
    private String text = "";
    private boolean focused;
    private final String placeholder;

    public SearchBox(float x, float y, float w, float h, String placeholder) { super(x, y, w, h); this.placeholder = placeholder; }
    public String getText() { return text; }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        Render2D.drawRoundRect(ps, x, y, w, h, 6f, Render2D.alpha(focused ? Colors.CARD : Colors.PANEL, alpha * 0.6f));
        if (text.isEmpty() && !focused)
            GlHelper.drawText(placeholder, x + 6, y + (h - Typography.SMALL.getMetrics().capHeight()) / 2f, Typography.SMALL, Render2D.alpha(Colors.TEXT_DISABLED, alpha * 0.5f));
        else {
            GlHelper.drawText(text, x + 6, y + (h - Typography.SMALL.getMetrics().capHeight()) / 2f, Typography.SMALL, Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
            if (focused && (System.currentTimeMillis() / 500) % 2 == 0)
                Render2D.drawRect(ps, x + 6 + GlHelper.getStringWidth(text, Typography.SMALL) + 1, y + 3, 1f, h - 6, Render2D.alpha(Colors.TEXT_PRIMARY, alpha * 0.5f));
        }
    }
    @Override public boolean mouseClicked(double mx, double my, int btn) { focused = true; return true; }
    @Override public boolean keyPressed(int key, int scan, int mods) { if (!focused) return false; if (key == 259 && !text.isEmpty()) { text = text.substring(0, text.length() - 1); return true; } return false; }
    @Override public boolean charTyped(char c, int mods) { if (!focused) return false; if (c >= 32 && c < 127 && text.length() < 40) { text += c; return true; } return false; }
}
