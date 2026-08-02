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

/**
 * Neverlose-style inline text mode selector — no background box.
 * <p>
 * Shows current mode as clickable text. Clicking expands a list of
 * options directly below. Selected option is highlighted in accent.
 */
public class TextModeSelector extends Component implements OverlayWidget {

    private final String[] options;
    private int selected;
    private boolean open;
    private final Animation expandAnim = new Animation();
    private int hoveredOpt = -1;
    private final IntConsumer onChange;
    private float popupX, popupY, popupW, popupH;
    private int optionStart, visibleOptions;

    private static final float OPT_H = 16f;

    public TextModeSelector(float x, float y, float w,
                            String[] options, int def, IntConsumer onChange) {
        super(x, y, w, 18);
        this.options = options;
        this.selected = Math.max(0, Math.min(options.length - 1, def));
        this.onChange = onChange;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        expandAnim.animate(open ? 1f : 0f);
        float pa = expandAnim.update(Animation.SPEED_EXPAND);

        float labelY = y + (h - Typography.SMALL.getMetrics().capHeight()) / 2f;
        Render2D.drawRoundRect(ps, x, y, w, h, 5f,
            Render2D.alpha(open ? Colors.CARD : Colors.INPUT_BG, alpha * (0.8f + ha * 0.15f)));

        // Current value text (accent)
        String cur = selected >= 0 && selected < options.length ? options[selected] : "";
        while (cur.length() > 1 && GlHelper.getStringWidth(cur, Typography.TINY) > w - 25) cur = cur.substring(0, cur.length() - 1);
        GlHelper.drawText(cur, x + 7, labelY, Typography.TINY,
            Render2D.alpha(Colors.TEXT_PRIMARY, alpha));

        // Arrow indicator
        String arrow = open ? "\uE5C7" : "\uE5C5";
        GlHelper.drawText(arrow, x + w - 20, y + 3, Typography.ICON,
            Render2D.alpha(Colors.TEXT_SECONDARY, alpha));
    }

    @Override
    public boolean contains(double mx, double my) {
        if (super.contains(mx, my)) return true;
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.contains(mx, my)) {
            open = !open;
            return true;
        }
        return false;
    }

    @Override public boolean isOverlayOpen() { return open; }

    @Override
    public void renderOverlay(GuiGraphics g, int mx, int my, float alpha) {
        if (!open) return;
        float pa = Math.max(0.01f, expandAnim.peek());
        popupW = Math.max(120, w);
        visibleOptions = Math.min(12, Math.max(1, Math.min(options.length,
            (int) ((g.guiHeight() - 24) / 24f))));
        optionStart = Math.max(0, Math.min(optionStart, options.length - visibleOptions));
        popupH = visibleOptions * 24f + 8;
        popupX = x;
        popupY = y + h + 4;
        if (popupX + popupW > g.guiWidth() - 8) popupX = g.guiWidth() - popupW - 8;
        if (popupY + popupH > g.guiHeight() - 8) popupY = y - popupH - 4;
        PoseStack ps = g.pose();
        Render2D.drawShadow(ps, popupX, popupY, popupW, popupH, 9f, 10f,
            Render2D.alpha(0xFF000000, alpha * pa * 0.55f));
        Render2D.drawRoundRect(ps, popupX, popupY, popupW, popupH, 9f,
            Render2D.alpha(Colors.CARD, alpha * pa));
        hoveredOpt = -1;
        for (int slot = 0; slot < visibleOptions; slot++) {
            int i = optionStart + slot;
            float oy = popupY + 4 + slot * 24f;
            boolean hover = Render2D.contains(popupX + 4, oy, popupW - 8, 24, mx, my);
            if (hover) hoveredOpt = i;
            if (hover) Render2D.drawRoundRect(ps, popupX + 4, oy, popupW - 8, 24, 5,
                Render2D.alpha(Colors.NAV_SELECTED, alpha * pa));
            int color = i == selected ? Colors.ACCENT : Colors.TEXT_SECONDARY;
            GlHelper.drawText(options[i], popupX + 10, oy + 7, Typography.SMALL,
                Render2D.alpha(hover ? Colors.TEXT_PRIMARY : color, alpha * pa));
        }
    }

    @Override
    public boolean overlayMouseClicked(double mx, double my, int btn) {
        if (!open || btn != 0) return false;
        if (contains(mx, my)) { open = false; return true; }
        if (hoveredOpt >= 0) {
            selected = hoveredOpt;
            open = false;
            if (onChange != null) onChange.accept(selected);
            return true;
        }
        return Render2D.contains(popupX, popupY, popupW, popupH, (float) mx, (float) my);
    }

    @Override public void closeOverlay() { open = false; }

    @Override public boolean overlayMouseScrolled(double delta) {
        if (!open || options.length <= visibleOptions) return false;
        optionStart = Math.max(0, Math.min(options.length - visibleOptions,
            optionStart - (int) Math.signum(delta)));
        return true;
    }
}
