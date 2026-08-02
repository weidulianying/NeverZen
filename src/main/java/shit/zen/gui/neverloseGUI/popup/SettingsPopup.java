package shit.zen.gui.neverloseGUI.popup;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.gui.neverloseGUI.theme.ThemeManager;
import shit.zen.gui.neverloseGUI.widget.ComboBox;
import shit.zen.gui.neverloseGUI.widget.Slider;
import shit.zen.render.GlHelper;

/** ⚙ Client Settings popup — categorized into Appearance / Effects / Interface / Advanced. */
public class SettingsPopup {

    private boolean open;
    private final Animation openAnim = new Animation();
    private static final float W = 260, H = 340;
    private float cx, cy;

    // Widgets
    private ComboBox themeCombo, langCombo, fontCombo;
    private Slider blurSlider, animSlider, scaleSlider;

    public SettingsPopup() { initWidgets(); }

    private void initWidgets() {
        themeCombo  = new ComboBox(0, 0, 120, ThemeManager.accentNames(), 0, idx -> ThemeManager.accentColor(ThemeManager.accentPresets()[idx]));
        langCombo   = new ComboBox(0, 0, 100, ThemeManager.languages(), 0, idx -> ThemeManager.language(ThemeManager.languages()[idx]));
        fontCombo   = new ComboBox(0, 0, 120, ThemeManager.fonts(), 0, idx -> ThemeManager.fontFamily(ThemeManager.fonts()[idx]));
        blurSlider  = new Slider(0, 0, 140, "Blur", 0, 24, 2, ThemeManager.blurRadius(), ThemeManager::blurRadius);
        animSlider  = new Slider(0, 0, 140, "Speed", 0.05f, 0.5f, 0.01f, ThemeManager.animationSpeed(), ThemeManager::animationSpeed);
        scaleSlider = new Slider(0, 0, 140, "Scale", 0.5f, 2f, 0.05f, ThemeManager.uiScale(), ThemeManager::uiScale);
    }

    public boolean isOpen() { return open; }
    public void toggle() { open = !open; openAnim.animate(open ? 1f : 0f); }
    public void close() { open = false; openAnim.animate(0f); }

    public void render(PoseStack ps, GuiGraphics g, int mx, int my, float a) {
        openAnim.animate(open ? 1f : 0f);
        float ea = openAnim.update(Animation.SPEED_EXPAND);
        if (ea < 0.01f) return;

        // Backdrop
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), Render2D.alpha(Colors.OVERLAY, 0.3f * ea));

        // Position: center of screen
        cx = (g.guiWidth() - W) / 2f; cy = (g.guiHeight() - H) / 2f;

        // Panel
        Render2D.drawRoundRect(ps, cx, cy, W, H * ea, 10f, Render2D.alpha(Colors.BACKGROUND, a * ea));
        Render2D.drawRoundRect(ps, cx, cy, W, H * ea, 10f, Render2D.alpha(Colors.BORDER, a * 0.3f * ea));

        if (ea < 0.8f) return; // don't render contents until mostly open

        float y = cy + 10;

        // ── Header ──
        GlHelper.drawText("⚙ Client Settings", cx + 12, y, Typography.H2, Render2D.alpha(Colors.TEXT_PRIMARY, a));
        y += 24;
        Render2D.drawRect(ps, cx + 12, y, W - 24, 1f, Render2D.alpha(Colors.BORDER, a * 0.3f));
        y += 8;

        // ── Section: Appearance ──
        GlHelper.drawText("Appearance", cx + 12, y, Typography.SMALL, Render2D.alpha(Colors.ACCENT, a));
        y += 18;
        GlHelper.drawText("Accent", cx + 16, y + 2, Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, a));
        themeCombo.setPos(cx + W - 136, y); themeCombo.render(g, (int)mx, (int)my, a);
        y += 24;
        GlHelper.drawText("UI Scale", cx + 16, y + 2, Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, a));
        scaleSlider.setPos(cx + W - 156, y); scaleSlider.render(g, mx, my, a);
        y += 26;

        // ── Section: Effects ──
        GlHelper.drawText("Effects", cx + 12, y, Typography.SMALL, Render2D.alpha(Colors.ACCENT, a));
        y += 18;
        GlHelper.drawText("Blur", cx + 16, y + 2, Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, a));
        blurSlider.setPos(cx + W - 156, y); blurSlider.render(g, mx, my, a);
        y += 26;
        GlHelper.drawText("Anim Speed", cx + 16, y + 2, Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, a));
        animSlider.setPos(cx + W - 156, y); animSlider.render(g, mx, my, a);
        y += 26;

        // ── Section: Interface ──
        GlHelper.drawText("Interface", cx + 12, y, Typography.SMALL, Render2D.alpha(Colors.ACCENT, a));
        y += 18;
        GlHelper.drawText("Language", cx + 16, y + 2, Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, a));
        langCombo.setPos(cx + W - 116, y); langCombo.render(g, (int)mx, (int)my, a);
        y += 24;
        GlHelper.drawText("Font", cx + 16, y + 2, Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, a));
        fontCombo.setPos(cx + W - 136, y); fontCombo.render(g, (int)mx, (int)my, a);
        y += 30;

        // ── Section: Advanced ──
        Render2D.drawRect(ps, cx + 12, y, W - 24, 1f, Render2D.alpha(Colors.BORDER, a * 0.3f));
        y += 8;
        GlHelper.drawText("Reset Layout", cx + 12, y, Typography.BODY, Render2D.alpha(Colors.DANGER, a));
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (!open) return false;
        if (mx < cx || mx > cx + W || my < cy || my > cy + H) { close(); return true; }
        if (themeCombo.contains(mx, my)) return themeCombo.mouseClicked(mx, my, btn);
        if (langCombo.contains(mx, my)) return langCombo.mouseClicked(mx, my, btn);
        if (fontCombo.contains(mx, my)) return fontCombo.mouseClicked(mx, my, btn);
        if (blurSlider.contains(mx, my)) return blurSlider.mouseClicked(mx, my, btn);
        if (animSlider.contains(mx, my)) return animSlider.mouseClicked(mx, my, btn);
        if (scaleSlider.contains(mx, my)) return scaleSlider.mouseClicked(mx, my, btn);
        return true; // consume click inside popup
    }
    public void mouseReleased(double mx, double my, int btn) { blurSlider.mouseReleased(mx, my, btn); animSlider.mouseReleased(mx, my, btn); scaleSlider.mouseReleased(mx, my, btn); }
    public void mouseDragged(double mx, double my) { blurSlider.mouseDragged(mx, my); animSlider.mouseDragged(mx, my); scaleSlider.mouseDragged(mx, my); }
}
