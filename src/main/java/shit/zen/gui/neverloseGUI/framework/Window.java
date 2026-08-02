package shit.zen.gui.neverloseGUI.framework;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ZenClient;
import shit.zen.config.ConfigData;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.layout.*;
import shit.zen.gui.neverloseGUI.page.ModulePage;
import shit.zen.gui.neverloseGUI.popup.SettingsPopup;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.modules.Module;
import shit.zen.render.GlHelper;
import shit.zen.render.Renderer;

/**
 * Root compositor.
 * <pre>
 * ╭──────────────────────────────────────────────╮
 * │ NeverZen │ Save  Config name ▼      Search │
 * ├──────────┼───────────────────────────────────┤
 * │ Sidebar  │      Two-column function area      │
 * │          │                                   │
 * ╰──────────────────────────────────────────────╯
 * </pre>
 */
public class Window {

    public static final float W = 650, H = 420;
    public static final float SIDEBAR_W = 134, TOOLBAR_H = 44;
    private float cx, cy, width = W, height = H, sidebarWidth = SIDEBAR_W;
    private float dragOffsetX, dragOffsetY;
    private int viewportWidth, viewportHeight;
    private boolean positionInitialized, dragging;
    private final Animation openAnim = new Animation().easing(Animation.Easing.EASE_OUT_CUBIC);
    private final Sidebar sidebar = new Sidebar();
    private final TitleBar titleBar = new TitleBar();
    private final SettingsPopup settingsPopup = new SettingsPopup();
    private final List<Page> pages = new ArrayList<>();
    private final List<String> configNames = new ArrayList<>();
    private int currentTab = -1;
    private String selectedConfig = "default";
    private long configRefreshAt, toastAt;
    private String toastMessage = "";

    public Window() { openAnim.force(0f); }

    public void centre(int sw, int sh) {
        viewportWidth = sw;
        viewportHeight = sh;
        width = Math.min(W, Math.max(1, sw - 24));
        height = Math.min(H, Math.max(1, sh - 24));
        sidebarWidth = width < 560 ? 132 : SIDEBAR_W;
        if (!positionInitialized) {
            cx = (sw - width) / 2f;
            cy = (sh - height) / 2f;
            positionInitialized = true;
        } else {
            cx = clamp(cx, 0f, Math.max(0f, sw - width));
            cy = clamp(cy, 0f, Math.max(0f, sh - height));
        }
        layoutCurrentPage();
    }
    public void open() { openAnim.animate(1f); }
    public boolean isAnimating() { return openAnim.peek() < 0.99f; }

    public void addPage(Page p) {
        pages.add(p);
    }
    public int currentTabIndex() { return currentTab; }
    public Page currentPage() { return currentTab >= 0 && currentTab < pages.size() ? pages.get(currentTab) : null; }

    private void layoutCurrentPage() {
        Page page = currentPage();
        if (page != null) page.setBounds(cx + sidebarWidth + 14, cy + TOOLBAR_H + 12,
            Math.max(1, width - sidebarWidth - 28), Math.max(1, height - TOOLBAR_H - 24));
    }

    public void switchTab(int tab) {
        if (tab < 0 || tab >= pages.size()) return;
        if (tab == currentTab) { layoutCurrentPage(); return; }
        Page prev = currentPage(); if (prev != null) { prev.closePopups(); prev.onHide(); }
        titleBar.closeDropdown();
        currentTab = tab;
        Page next = currentPage();
        if (next != null) { layoutCurrentPage(); next.onShow(); }
    }

    public void render(GuiGraphics g, int mx, int my) {
        refreshConfigs(false);
        openAnim.animate(1f);
        float a = Animation.easeOutCubic(Animation.clamp01(openAnim.update(Animation.SPEED_OPEN)));
        if (a < 0.01f) return;

        g.fill(0, 0, g.guiWidth(), g.guiHeight(), Render2D.alpha(Colors.OVERLAY, 0.6f * a));

        Renderer.renderConsumer(dc -> {
            PoseStack ps = g.pose();

            // Window bg
            Render2D.drawShadow(ps, cx, cy, width, height, 16f, 18f, Render2D.alpha(0xFF000000, a * 0.55f));
            Render2D.drawRoundRect(ps, cx, cy, width, height, 16f, Render2D.alpha(Colors.BACKGROUND, a));

            // Sidebar
            sidebar.render(ps, g, cx, cy, sidebarWidth, height, mx, my, a, currentTab);

            // Context toolbar
            titleBar.render(ps, g, cx + sidebarWidth, cy, width - sidebarWidth, mx, my, a, selectedConfig);

            // Page
            Page page = currentPage();
            if (page != null && a > 0.3f) page.render(ps, g, mx, my, a);

            titleBar.renderOverlay(ps, g, mx, my, a, configNames(), selectedConfigIndex());
            renderToast(ps, a);

            // Settings popup (always on top)
            settingsPopup.render(ps, g, mx, my, a);
        });
    }

    // ── Input ───────────────────────────────────────────

    public boolean mouseClicked(double mx, double my, int btn) {
        // SettingsPopup gets priority
        if (settingsPopup.isOpen()) {
            if (settingsPopup.mouseClicked(mx, my, btn)) return true;
        }
        refreshConfigs(false);
        TitleBar.Action action = titleBar.mouseClicked(mx, my, btn, configNames(), selectedConfigIndex());
        if (action != TitleBar.Action.NONE) {
            Page page = currentPage();
            if (page != null) page.closePopups();
        }
        if (action == TitleBar.Action.SAVE) {
            if (ZenClient.isReady()) {
                ZenClient.getInstance().getConfigManager().saveConfig(selectedConfig);
                showToast("Saved config: " + selectedConfig);
                refreshConfigs(true);
            }
            return true;
        }
        if (action == TitleBar.Action.SELECT_CONFIG) {
            int index = titleBar.consumeSelectedOption();
            if (index >= 0 && index < configNames.size()) {
                selectedConfig = configNames.get(index);
                if (configExists(selectedConfig)) ZenClient.getInstance().getConfigManager().loadConfig(selectedConfig);
            }
            return true;
        }
        if (action == TitleBar.Action.SELECT_MODULE) {
            focusModule(titleBar.consumeSelectedModule());
            return true;
        }
        if (action == TitleBar.Action.SETTINGS) { settingsPopup.toggle(); return true; }
        if (action == TitleBar.Action.CONSUMED) return true;
        if (btn == 0 && Render2D.contains(cx, cy, width, TOOLBAR_H, (float) mx, (float) my)) {
            dragging = true;
            dragOffsetX = (float) mx - cx;
            dragOffsetY = (float) my - cy;
            return true;
        }
        // Sidebar
        if (sidebar.mouseClicked(mx, my, btn)) { switchTab(sidebar.selected()); return true; }
        // Page
        Page p = currentPage(); return p != null && p.mouseClicked(mx, my, btn);
    }

    public void mouseReleased(double mx, double my, int btn) {
        dragging = false;
        if (settingsPopup.isOpen()) { settingsPopup.mouseReleased(mx, my, btn); return; }
        Page p = currentPage(); if (p != null) p.mouseReleased(mx, my, btn);
    }

    public void mouseDragged(double mx, double my) {
        if (dragging) {
            cx = clamp((float) mx - dragOffsetX, 0f, Math.max(0f, viewportWidth - width));
            cy = clamp((float) my - dragOffsetY, 0f, Math.max(0f, viewportHeight - height));
            layoutCurrentPage();
            return;
        }
        if (settingsPopup.isOpen()) { settingsPopup.mouseDragged(mx, my); return; }
        Page p = currentPage(); if (p != null) p.mouseDragged(mx, my);
    }

    public boolean mouseScrolled(double mx, double my, double d) {
        if (titleBar.mouseScrolled(d, configNames.size())) return true;
        Page p = currentPage(); return p != null && p.mouseScrolled(mx, my, d);
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (titleBar.keyPressed(key)) {
            focusModule(titleBar.consumeSelectedModule());
            return true;
        }
        Page p = currentPage(); return p != null && p.keyPressed(key, scan, mods);
    }

    public boolean charTyped(char c, int mods) {
        if (titleBar.charTyped(c)) return true;
        Page p = currentPage(); return p != null && p.charTyped(c, mods);
    }

    private void focusModule(Module module) {
        if (module == null) return;
        int tab = switch (module.getCategory()) {
            case COMBAT -> 0;
            case MOVEMENT -> 1;
            case PLAYER -> 2;
            case RENDER -> 3;
            case EXPLOIT, WORLD -> 4;
            case MISC -> 5;
        };
        switchTab(tab);
        Page page = currentPage();
        if (page instanceof ModulePage modulePage) {
            modulePage.focusModule(module);
        }
    }

    public boolean contains(double mx, double my) {
        return mx >= cx && mx <= cx + width && my >= cy && my <= cy + height;
    }

    private void refreshConfigs(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - configRefreshAt < 1000) return;
        configRefreshAt = now;
        if (!ZenClient.isReady()) return;
        configNames.clear();
        for (ConfigData config : ZenClient.getInstance().getConfigManager().getConfigs()) {
            configNames.add(config.getName());
        }
        if (configNames.isEmpty()) configNames.add("default");
        if (!configNames.contains(selectedConfig)) selectedConfig = configNames.get(0);
    }

    private boolean configExists(String name) {
        if (!ZenClient.isReady()) return false;
        for (ConfigData config : ZenClient.getInstance().getConfigManager().getConfigs()) {
            if (config.getName().equals(name)) return true;
        }
        return false;
    }

    private String[] configNames() { return configNames.toArray(new String[0]); }
    private int selectedConfigIndex() { return Math.max(0, configNames.indexOf(selectedConfig)); }

    private void showToast(String message) {
        toastMessage = message;
        toastAt = System.currentTimeMillis();
    }

    private void renderToast(PoseStack ps, float alpha) {
        long elapsed = System.currentTimeMillis() - toastAt;
        if (toastMessage.isEmpty() || elapsed >= 2400) return;
        float fade = elapsed < 180 ? elapsed / 180f : elapsed > 2100 ? (2400 - elapsed) / 300f : 1f;
        float textW = GlHelper.getStringWidth(toastMessage, Typography.SMALL);
        float toastW = textW + 30;
        float tx = cx + width - toastW - 12;
        float ty = cy + TOOLBAR_H + 10;
        Render2D.drawShadow(ps, tx, ty, toastW, 28, 7f, 8f,
            Render2D.alpha(0xFF000000, alpha * fade * 0.45f));
        Render2D.drawRoundRect(ps, tx, ty, toastW, 28, 7f,
            Render2D.alpha(Colors.CARD, alpha * fade));
        Render2D.drawCircle(ps, tx + 12, ty + 14, 3, Render2D.alpha(Colors.ACCENT, alpha * fade));
        GlHelper.drawText(toastMessage, tx + 21, ty + 9, Typography.SMALL,
            Render2D.alpha(Colors.TEXT_PRIMARY, alpha * fade));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
