package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;

/** Horizontal divider line. */
public class Separator extends Component {
    public Separator(float x, float y, float w) { super(x, y, w, 1f); }
    @Override public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        Render2D.drawRect(ps, x, y, w, 1f, Render2D.alpha(Colors.BORDER, alpha * 0.4f));
    }
}
