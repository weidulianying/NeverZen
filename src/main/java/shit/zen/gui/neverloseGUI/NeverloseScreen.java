package shit.zen.gui.neverloseGUI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import shit.zen.ZenClient;
import shit.zen.gui.neverloseGUI.framework.Window;
import shit.zen.gui.neverloseGUI.page.ConfigPage;
import shit.zen.gui.neverloseGUI.page.ModulePage;

public class NeverloseScreen extends Screen {

    private static int lastSelectedTab;
    private Window window;

    public NeverloseScreen() { super(Component.literal("NeverZen")); }

    @Override
    protected void init() {
        if (window != null && window.currentTabIndex() >= 0) {
            lastSelectedTab = window.currentTabIndex();
        }
        window = new Window();
        window.addPage(new ModulePage(0)); // Combat
        window.addPage(new ModulePage(1)); // Movement
        window.addPage(new ModulePage(2)); // Player
        window.addPage(new ModulePage(3)); // Visual (Render)
        window.addPage(new ModulePage(4)); // World (Exploit + World)
        window.addPage(new ModulePage(5)); // Misc
        window.addPage(new ConfigPage());  // Configs
        window.switchTab(lastSelectedTab);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) { renderBackground(g); if (window != null) { window.centre(width, height); window.render(g, mx, my); } super.render(g, mx, my, pt); }
    @Override public boolean mouseClicked(double mx, double my, int btn) { if (window != null && window.isAnimating()) return true; if (window != null && window.mouseClicked(mx, my, btn)) return true; if (window != null && !window.contains(mx, my)) { onClose(); return true; } return super.mouseClicked(mx, my, btn); }
    @Override public boolean mouseReleased(double mx, double my, int btn) { if (window != null) window.mouseReleased(mx, my, btn); return super.mouseReleased(mx, my, btn); }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { if (window != null) window.mouseDragged(mx, my); return super.mouseDragged(mx, my, btn, dx, dy); }
    @Override public boolean mouseScrolled(double mx, double my, double delta) { if (window != null && window.mouseScrolled(mx, my, delta)) return true; return super.mouseScrolled(mx, my, delta); }
    @Override public boolean keyPressed(int key, int scan, int mods) { if (window != null && window.keyPressed(key, scan, mods)) return true; if (key == 256) { onClose(); return true; } return super.keyPressed(key, scan, mods); }
    @Override public boolean charTyped(char c, int mods) { return window != null && window.charTyped(c, mods) || super.charTyped(c, mods); }
    @Override public void onClose() {
        if (window != null && window.currentTabIndex() >= 0) lastSelectedTab = window.currentTabIndex();
        if (ZenClient.isReady()) ZenClient.getInstance().getConfigManager().saveAll();
        super.onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
}
