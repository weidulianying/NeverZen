package shit.zen.gui.neverloseGUI.framework;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;

/** Every visible element is a Component. */
public abstract class Component {
    protected float x, y, w, h;
    protected boolean visible = true, enabled = true, hovered;
    public final Animation hoverAnim = new Animation();

    public Component(float x, float y, float w, float h) { this.x = x; this.y = y; this.w = w; this.h = h; }

    public void setPos(float x, float y) { this.x = x; this.y = y; }
    public void setSize(float w, float h) { this.w = w; this.h = h; }
    public float x() { return x; } public float y() { return y; }
    public float w() { return w; } public float h() { return h; }
    public boolean isVisible() { return visible; } public void setVisible(boolean v) { this.visible = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean e) { this.enabled = e; }
    public boolean contains(double mx, double my) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }

    public void render(GuiGraphics g, int mx, int my, float alpha) {
        if (!visible) return;
        hovered = contains(mx, my);
        hoverAnim.animate(hovered ? 1f : 0f);
        float ha = hoverAnim.update(hovered ? Animation.SPEED_HOVER : 0.16f);
        PoseStack ps = g.pose();
        draw(ps, g, mx, my, alpha, ha);
    }

    public abstract void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha);

    public boolean mouseClicked(double mx, double my, int btn) { return false; }
    public void mouseReleased(double mx, double my, int btn) {}
    public void mouseDragged(double mx, double my) {}
    public boolean mouseScrolled(double mx, double my, double delta) { return false; }
    public boolean keyPressed(int key, int scan, int mods) { return false; }
    public boolean charTyped(char c, int mods) { return false; }
}
