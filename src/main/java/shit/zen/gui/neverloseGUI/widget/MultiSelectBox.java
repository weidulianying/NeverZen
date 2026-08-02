package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

/** Multi-select control with a row-anchored floating panel. */
public final class MultiSelectBox extends Component implements OverlayWidget {
    private final String title;
    private final String[] options;
    private final Supplier<List<String>> selected;
    private final Consumer<String> toggle;
    private boolean open;
    private int hovered = -1;
    private int optionStart, visibleOptions;
    private float popupX, popupY, popupW, popupH;

    public MultiSelectBox(float x, float y, float w, String title, String[] options,
                          Supplier<List<String>> selected, Consumer<String> toggle) {
        super(x, y, w, 22);
        this.title = title;
        this.options = options;
        this.selected = selected;
        this.toggle = toggle;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        boolean hover = contains(mx, my);
        if (hover || open) Render2D.drawRoundRect(ps, x, y, w, h, 5f,
            Render2D.alpha(open ? Colors.CARD : Colors.INPUT_BG, alpha * 0.9f));
        List<String> values = selected.get();
        String value = values.isEmpty() ? "None" : values.size() == 1 ? values.get(0) : values.size() + " selected";
        value = fit(value, w - 24);
        GlHelper.drawText(value, x + 7, y + 7, Typography.TINY,
            Render2D.alpha(values.isEmpty() ? Colors.TEXT_DISABLED : Colors.TEXT_PRIMARY, alpha));
        GlHelper.drawText("\uE5CC", x + w - 19, y + 4, Typography.ICON,
            Render2D.alpha(open ? Colors.ACCENT : Colors.TEXT_SECONDARY, alpha));
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0 || !contains(mx, my)) return false;
        open = !open;
        return true;
    }

    @Override public boolean isOverlayOpen() { return open; }

    @Override
    public void renderOverlay(GuiGraphics g, int mx, int my, float alpha) {
        if (!open) return;
        PoseStack ps = g.pose();
        float itemH = 26;
        popupW = Math.max(190, w + 48);
        visibleOptions = Math.min(12, Math.max(1, Math.min(options.length,
            (int) ((g.guiHeight() - 72) / itemH))));
        optionStart = Math.max(0, Math.min(optionStart, options.length - visibleOptions));
        popupH = 44 + visibleOptions * itemH + 8;
        popupX = x + w + 10;
        popupY = y - 10;
        if (popupX + popupW > g.guiWidth() - 8) popupX = x - popupW - 10;
        if (popupY + popupH > g.guiHeight() - 8) popupY = Math.max(8, g.guiHeight() - popupH - 8);

        Render2D.drawShadow(ps, popupX, popupY, popupW, popupH, 14f, 14f,
            Render2D.alpha(0xFF000000, alpha * 0.6f));
        Render2D.drawRoundRect(ps, popupX, popupY, popupW, popupH, 14f, Render2D.alpha(Colors.CARD, alpha));
        GlHelper.drawText(title, popupX + 16, popupY + 14, Typography.H2, Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
        Render2D.drawRect(ps, popupX + 12, popupY + 42, popupW - 24, 1,
            Render2D.alpha(Colors.BORDER, alpha * 0.75f));

        hovered = -1;
        List<String> values = selected.get();
        for (int slot = 0; slot < visibleOptions; slot++) {
            int i = optionStart + slot;
            float iy = popupY + 48 + slot * itemH;
            boolean hover = Render2D.contains(popupX + 8, iy, popupW - 16, itemH, mx, my);
            if (hover) hovered = i;
            if (hover) Render2D.drawRoundRect(ps, popupX + 8, iy, popupW - 16, itemH, 5,
                Render2D.alpha(Colors.NAV_SELECTED, alpha));
            boolean active = values.contains(options[i]);
            GlHelper.drawText(options[i], popupX + 16, iy + 8, Typography.BODY,
                Render2D.alpha(active ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY, alpha));
            if (active) GlHelper.drawText("\uE5CA", popupX + popupW - 29, iy + 5, Typography.ICON,
                Render2D.alpha(Colors.ACCENT, alpha));
        }
    }

    @Override
    public boolean overlayMouseClicked(double mx, double my, int btn) {
        if (!open || btn != 0) return false;
        if (contains(mx, my)) { open = false; return true; }
        if (hovered >= 0) {
            toggle.accept(options[hovered]);
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

    private static String fit(String value, float maxWidth) {
        if (GlHelper.getStringWidth(value, Typography.TINY) <= maxWidth) return value;
        String out = value;
        while (out.length() > 1 && GlHelper.getStringWidth(out + "...", Typography.TINY) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "...";
    }
}
