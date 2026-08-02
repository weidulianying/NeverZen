package shit.zen.gui.neverloseGUI.popup;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.factory.WidgetFactory;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.model.ModuleViewModel;
import shit.zen.gui.neverloseGUI.model.SettingViewModel;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.gui.neverloseGUI.widget.KeyBind;
import shit.zen.gui.neverloseGUI.widget.LineSlider;
import shit.zen.gui.neverloseGUI.widget.OverlayWidget;
import shit.zen.gui.neverloseGUI.widget.Toggle;
import shit.zen.render.GlHelper;

/** Floating block opened by right-clicking a module row. */
public final class ModuleSettingsBlock {
    private static final float WIDTH = 286f;
    private static final float HEADER_H = 42f;
    private static final float ROW_H = 34f;

    private final ModuleViewModel module;
    private final List<Row> rows = new ArrayList<>();
    private final Animation transition = new Animation().easing(Animation.Easing.EASE_OUT_CUBIC);
    private float anchorX, anchorY, anchorW;
    private float x, y, h, scrollSmooth, scrollTarget;
    private int visibilitySignature;
    private boolean closing;

    private static final class Row {
        final String label;
        final SettingViewModel setting;
        final Component control;
        final float offsetX, offsetY;

        Row(String label, SettingViewModel setting, Component control, float offsetX, float offsetY) {
            this.label = label;
            this.setting = setting;
            this.control = control;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    public ModuleSettingsBlock(ModuleViewModel module, float anchorX, float anchorY, float anchorW) {
        this.module = module;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorW = anchorW;
        rebuild();
        transition.force(0f);
        transition.animate(1f);
    }

    public ModuleViewModel module() { return module; }
    public void close() {
        closing = true;
        closeControlOverlays();
        transition.animate(0f);
    }
    public boolean isClosing() { return closing; }
    public boolean isClosed() { return closing && transition.isDone() && transition.peek() < 0.01f; }

    public void setAnchor(float anchorX, float anchorY, float anchorW) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorW = anchorW;
    }

    private int signature() {
        int result = 1;
        for (SettingViewModel setting : module.settings()) {
            result = 31 * result + System.identityHashCode(setting.raw());
            result = 31 * result + (setting.isVisible() ? 1 : 0);
        }
        return result;
    }

    private void rebuild() {
        rows.clear();
        float baseRowY = HEADER_H;
        Component bind = new KeyBind(WIDTH - 66, baseRowY + 8, 54, 18,
            module.keyCode(), module::setKeyCode);
        rows.add(new Row("Key bind", null, bind, bind.x(), bind.y() - baseRowY));

        int index = 1;
        for (SettingViewModel setting : module.settings()) {
            if (!setting.isVisible()) continue;
            float rowY = HEADER_H + index++ * ROW_H;
            Component control = WidgetFactory.create(setting, 12, rowY + 7, WIDTH - 24);
            if (control != null) rows.add(new Row(setting.name(), setting, control,
                control.x(), control.y() - rowY));
        }
        visibilitySignature = signature();
        scrollSmooth = scrollTarget = 0;
    }

    public void render(GuiGraphics g, int mx, int my, float alpha) {
        if (signature() != visibilitySignature) rebuild();
        float progress = Animation.clamp01(transition.update(closing ? 0.26f : 0.18f));
        if (progress < 0.005f) return;
        float animatedAlpha = alpha * progress;
        float desiredH = HEADER_H + rows.size() * ROW_H + 8;
        h = Math.min(desiredH, Math.max(150, g.guiHeight() - 20));
        x = anchorX + anchorW + 10;
        y = anchorY - 8;
        if (x + WIDTH > g.guiWidth() - 8) x = anchorX - WIDTH - 10;
        x = Math.max(8, Math.min(x, g.guiWidth() - WIDTH - 8));
        y = Math.max(8, Math.min(y, g.guiHeight() - h - 8));

        float viewportH = h - HEADER_H - 8;
        float contentH = rows.size() * ROW_H;
        scrollTarget = Math.max(-Math.max(0, contentH - viewportH), Math.min(0, scrollTarget));
        scrollSmooth += (scrollTarget - scrollSmooth) * 0.18f;

        float direction = x >= anchorX ? 1f : -1f;
        float drawX = x + direction * (1f - progress) * 14f;
        float animatedH = HEADER_H + (h - HEADER_H) * progress;

        PoseStack ps = g.pose();
        Render2D.drawShadow(ps, drawX, y, WIDTH, animatedH, 13f, 14f,
            Render2D.alpha(0xFF000000, animatedAlpha * 0.6f));
        Render2D.drawRoundRect(ps, drawX, y, WIDTH, animatedH, 13f, Render2D.alpha(Colors.CARD, animatedAlpha));
        GlHelper.drawText(fit(module.name(), Typography.H2, WIDTH - 68), drawX + 14, y + 13, Typography.H2,
            Render2D.alpha(Colors.TEXT_PRIMARY, animatedAlpha));
        GlHelper.drawText(module.isEnabled() ? "ON" : "OFF", drawX + WIDTH - 38, y + 15,
            Typography.TINY, Render2D.alpha(module.isEnabled() ? Colors.ACCENT : Colors.TEXT_DISABLED, animatedAlpha));
        Render2D.drawRect(ps, drawX + 12, y + HEADER_H - 1, WIDTH - 24, 1,
            Render2D.alpha(Colors.BORDER, animatedAlpha * 0.75f));

        float revealH = Math.max(0f, animatedH - HEADER_H);
        Render2D.pushScissor((int) drawX, (int) (y + HEADER_H), (int) WIDTH, (int) revealH);
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            float rowProgress = Animation.clamp01(progress * 1.35f - i * 0.045f);
            float rowY = y + HEADER_H + i * ROW_H + scrollSmooth + (1f - rowProgress) * 7f;
            if (i > 0) Render2D.drawRect(ps, drawX + 12, rowY, WIDTH - 24, 1,
                Render2D.alpha(Colors.BORDER, animatedAlpha * rowProgress * 0.6f));
            GlHelper.drawText(fit(row.label, Typography.SMALL, WIDTH * 0.46f), drawX + 14, rowY + 12, Typography.SMALL,
                Render2D.alpha(Colors.TEXT_PRIMARY, animatedAlpha * rowProgress));
            row.control.setPos(drawX + row.offsetX, rowY + row.offsetY);
            if (row.control instanceof Toggle toggle && row.setting != null) toggle.setOn(row.setting.getBoolean());
            row.control.render(g, mx, my, animatedAlpha * rowProgress);
        }
        Render2D.popScissor();

        for (Row row : rows) {
            if (row.control instanceof OverlayWidget overlay && overlay.isOverlayOpen()) {
                overlay.renderOverlay(g, mx, my, animatedAlpha);
            }
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (closing || transition.peek() < 0.65f) return false;
        for (Row row : rows) {
            if (row.control instanceof LineSlider slider) slider.blurUnlessValueHit(mx, my);
        }
        for (int i = rows.size() - 1; i >= 0; i--) {
            Component control = rows.get(i).control;
            if (control instanceof OverlayWidget overlay && overlay.isOverlayOpen()
                && overlay.overlayMouseClicked(mx, my, btn)) return true;
        }
        closeControlOverlays();
        if (!contains(mx, my)) return false;
        for (int i = rows.size() - 1; i >= 0; i--) {
            Row row = rows.get(i);
            float rowY = y + HEADER_H + i * ROW_H + scrollSmooth;
            if (mx >= x + 8 && mx <= x + WIDTH - 8 && my >= rowY && my <= rowY + ROW_H
                    && row.control.mouseClicked(mx, my, btn)) return true;
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (closing) return false;
        for (Row row : rows) {
            if (row.control instanceof OverlayWidget overlay && overlay.isOverlayOpen()
                && overlay.overlayMouseScrolled(delta)) return true;
        }
        if (!contains(mx, my)) return false;
        scrollTarget += (float) delta * 24;
        return true;
    }

    public void mouseReleased(double mx, double my, int btn) {
        for (Row row : rows) row.control.mouseReleased(mx, my, btn);
    }

    public void mouseDragged(double mx, double my) {
        for (Row row : rows) row.control.mouseDragged(mx, my);
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && hasControlOverlay()) { closeControlOverlays(); return true; }
        for (Row row : rows) if (row.control.keyPressed(key, scan, mods)) return true;
        return false;
    }

    public boolean charTyped(char c, int mods) {
        for (Row row : rows) if (row.control.charTyped(c, mods)) return true;
        return false;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx <= x + WIDTH && my >= y && my <= y + h;
    }

    private boolean hasControlOverlay() {
        for (Row row : rows) if (row.control instanceof OverlayWidget overlay && overlay.isOverlayOpen()) return true;
        return false;
    }

    private void closeControlOverlays() {
        for (Row row : rows) if (row.control instanceof OverlayWidget overlay) overlay.closeOverlay();
    }

    private static String fit(String value, shit.zen.render.FontRenderer font, float maxWidth) {
        if (GlHelper.getStringWidth(value, font) <= maxWidth) return value;
        String out = value;
        while (out.length() > 1 && GlHelper.getStringWidth(out + "...", font) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "...";
    }
}
