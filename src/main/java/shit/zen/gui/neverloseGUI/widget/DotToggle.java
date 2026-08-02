package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;

/**
 * Neverlose-style dot toggle — ● (accent blue, filled) when ON, ○ (gray outline) when OFF.
 * <p>
 * Renders as an 8px-radius circle with a smooth alpha animation on state change.
 * No pill background, no knob — just a dot.
 */
public class DotToggle extends Component {

    private boolean value;
    private final Animation anim = new Animation();
    private final Consumer<Boolean> onChange;

    private static final float DOT_RADIUS = 5f;
    private static final float HIT_SIZE   = 16f;

    public DotToggle(float x, float y, boolean initial, Consumer<Boolean> onChange) {
        super(x, y, HIT_SIZE, HIT_SIZE);
        this.value = initial;
        this.onChange = onChange;
        anim.force(value ? 1f : 0f);
    }

    public boolean isOn() { return value; }
    public void setOn(boolean v) { this.value = v; }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        anim.animate(value ? 1f : 0f);
        float p = anim.update(Animation.SPEED_TOGGLE);

        float cx = x + w / 2f;
        float cy = y + h / 2f;

        if (p > 0.01f) {
            // Filled dot (accent blue)
            int dotColor = Render2D.lerpColor(
                Render2D.alpha(Colors.DOT_OFF, alpha),
                Render2D.alpha(Colors.DOT_ON, alpha),
                p
            );
            Render2D.drawCircle(ps, cx, cy, DOT_RADIUS, dotColor);
        }

        if (p < 0.99f) {
            // Outline ring (gray, fades out as filled dot fades in)
            float ringAlpha = alpha * (1f - p) * 0.6f;
            Render2D.drawCircle(ps, cx, cy, DOT_RADIUS, Render2D.alpha(Colors.DOT_OFF, ringAlpha));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && contains(mx, my)) {
            value = !value;
            if (onChange != null) onChange.accept(value);
            return true;
        }
        return false;
    }
}
