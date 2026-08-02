package shit.zen.gui.neverloseGUI.framework;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

/** One full-content page inside the main window. */
public abstract class Page {
    protected float x, y, w, h;
    private boolean active;

    public void setBounds(float x, float y, float w, float h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    public float x() { return x; } public float y() { return y; } public float w() { return w; } public float h() { return h; }

    public void onShow() { active = true; } public void onHide() { active = false; } public boolean isActive() { return active; }

    public void closePopups() {}

    public abstract void render(PoseStack ps, GuiGraphics g, int mx, int my, float alpha);
    public boolean mouseClicked(double mx, double my, int btn) { return false; }
    public void mouseReleased(double mx, double my, int btn) {}
    public void mouseDragged(double mx, double my) {}
    public boolean mouseScrolled(double mx, double my, double delta) { return false; }
    public boolean keyPressed(int key, int scan, int mods) { return false; }
    public boolean charTyped(char c, int mods) { return false; }
}
