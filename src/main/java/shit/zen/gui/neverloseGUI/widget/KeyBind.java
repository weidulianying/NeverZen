package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

/** Key-bind button — click to listen, press key to bind, Right-click to unbind. */
public class KeyBind extends Component {
    private int key;
    private boolean listening;
    private final Animation pulse = new Animation();
    private final IntConsumer onBind;

    public KeyBind(float x, float y, float w, float h, int initial, IntConsumer onBind) {
        super(x, y, w, h);
        this.key = initial; this.onBind = onBind;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        pulse.animate(listening ? 1f : 0f);
        float pa = pulse.update(0.25f);
        int bg = listening ? Render2D.lerpColor(Render2D.alpha(Colors.PANEL, alpha), Render2D.alpha(Colors.ACCENT, alpha * 0.3f), pa)
                : Render2D.alpha(ha > 0.1f ? Colors.CARD : Colors.PANEL, alpha * 0.6f);
        Render2D.drawRoundRect(ps, x, y, w, h, 6f, bg);
        String label = listening ? "..." + (System.currentTimeMillis() / 300 % 2 == 0 ? "_" : "") : name(key);
        int tc = listening ? Colors.ACCENT : (ha > 0.1f ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY);
        GlHelper.drawText(label, x + 6, y + (h - Typography.SMALL.getMetrics().capHeight()) / 2f, Typography.SMALL, Render2D.alpha(tc, alpha));
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) { listening = !listening; return true; }
        if (btn == 1) { key = 0; if (onBind != null) onBind.accept(0); return true; }
        return false;
    }

    @Override public boolean keyPressed(int k, int scan, int mods) {
        if (!listening) return false;
        key = (k == GLFW.GLFW_KEY_ESCAPE) ? 0 : k; listening = false;
        if (onBind != null) onBind.accept(key);
        return true;
    }

    private static String name(int code) {
        return switch (code) {
            case 0 -> "None"; case GLFW.GLFW_KEY_ESCAPE -> "Esc"; case GLFW.GLFW_KEY_SPACE -> "Spc";
            case GLFW.GLFW_KEY_ENTER -> "Ent"; case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE -> "Bks"; case GLFW.GLFW_KEY_DELETE -> "Del";
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "Sft";
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctl";
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "Alt";
            default -> { String n = GLFW.glfwGetKeyName(code, 0); yield n != null ? n.toUpperCase() : "K" + code; }
        };
    }
}
