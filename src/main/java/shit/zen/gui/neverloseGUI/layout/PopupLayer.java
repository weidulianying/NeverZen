package shit.zen.gui.neverloseGUI.layout;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.framework.Component;

/** Always-on-top layer for popups, toasts, dialogs. */
public class PopupLayer {
    private final List<Component> popups = new ArrayList<>();

    public void add(Component c) { popups.add(c); }
    public void remove(Component c) { popups.remove(c); }
    public void clear() { popups.clear(); }

    public void render(PoseStack ps, GuiGraphics g, int mx, int my, float a) {
        for (Component c : popups) c.render(g, mx, my, a);
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = popups.size() - 1; i >= 0; i--) if (popups.get(i).mouseClicked(mx, my, btn)) return true;
        return false;
    }
}
