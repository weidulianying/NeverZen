package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;

public class Toggle extends Component {
    private boolean value;
    private final Animation anim = new Animation();
    private final Consumer<Boolean> onChange;

    public Toggle(float x, float y, boolean initial, Consumer<Boolean> onChange) {
        super(x, y, 32, 18);
        this.value = initial; this.onChange = onChange;
        anim.force(value ? 1f : 0f);
    }
    public boolean isOn() { return value; }
    public void setOn(boolean v) { this.value = v; }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        anim.animate(value ? 1f : 0f);
        float p = anim.update(Animation.SPEED_TOGGLE);
        Render2D.drawRoundRect(ps, x, y, w, h, h / 2f, Render2D.lerpColor(Render2D.alpha(Colors.TOGGLE_OFF, alpha * 0.6f), Render2D.alpha(Colors.TOGGLE_ON, alpha * 0.7f), p));
        float kr = h / 2f - 2, kx = x + 2 + (w - kr * 2 - 4) * p;
        Render2D.drawRoundRect(ps, kx, y + 2, kr * 2, kr * 2, kr, Render2D.alpha(Colors.TOGGLE_KNOB, alpha));
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) { if (btn == 0) { value = !value; if (onChange != null) onChange.accept(value); return true; } return false; }
}
