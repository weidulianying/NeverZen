package shit.zen.gui.neverloseGUI.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.render.Render2D;

/** Draggable HUD element base. */
public abstract class HUDComponent {

    protected float x, y, w, h;
    protected boolean dragging;
    protected float dragOffX, dragOffY;
    protected boolean editing;

    public HUDComponent(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public void setPos(float x, float y) { this.x = x; this.y = y; }
    public float x() { return x; } public float y() { return y; }

    public void setEditing(boolean e) { this.editing = e; }
    public boolean isEditing() { return editing; }

    public boolean contains(float mx, float my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ── Drag ──

    public boolean mouseClicked(double mx, double my, int btn) {
        if (!editing || btn != 0) return false;
        if (contains((float) mx, (float) my)) {
            dragging = true;
            dragOffX = (float) mx - x;
            dragOffY = (float) my - y;
            return true;
        }
        return false;
    }

    public void mouseDragged(double mx, double my) {
        if (dragging) {
            x = (float) mx - dragOffX;
            y = (float) my - dragOffY;
        }
    }

    public void mouseReleased() { dragging = false; }

    // ── Render ──

    public abstract void draw(PoseStack ps, GuiGraphics g, float alpha);
}
