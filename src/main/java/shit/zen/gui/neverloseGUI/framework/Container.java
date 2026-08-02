package shit.zen.gui.neverloseGUI.framework;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/** A component that owns and arranges children. */
public abstract class Container extends Component {
    protected final List<Component> children = new ArrayList<>();

    public Container(float x, float y, float w, float h) { super(x, y, w, h); }

    public void add(Component c) { children.add(c); }
    public void remove(Component c) { children.remove(c); }
    public void clear() { children.clear(); }
    public List<Component> children() { return children; }

    protected abstract void layout(PoseStack ps, GuiGraphics g, float ox, float oy);

    @Override public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        layout(ps, g, x, y);
        for (Component c : children) { if (c.isVisible()) c.render(g, mx, my, alpha); }
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = children.size() - 1; i >= 0; i--) { Component c = children.get(i); if (c.isVisible() && c.isEnabled() && c.contains(mx, my) && c.mouseClicked(mx, my, btn)) return true; }
        return false;
    }
    @Override public void mouseReleased(double mx, double my, int btn) { for (Component c : children) c.mouseReleased(mx, my, btn); }
    @Override public void mouseDragged(double mx, double my) { for (Component c : children) c.mouseDragged(mx, my); }
    @Override public boolean mouseScrolled(double mx, double my, double d) { for (Component c : children) if (c.mouseScrolled(mx, my, d)) return true; return false; }
    @Override public boolean keyPressed(int k, int s, int m) { for (Component c : children) if (c.keyPressed(k, s, m)) return true; return false; }
    @Override public boolean charTyped(char ch, int m) { for (Component c : children) if (c.charTyped(ch, m)) return true; return false; }
}
