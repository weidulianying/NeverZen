package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

public final class TextInput extends Component {
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private final boolean secret;
    private final StringBuilder editing = new StringBuilder();
    private boolean focused;

    public TextInput(float x, float y, float w, Supplier<String> getter, Consumer<String> setter, boolean secret) {
        super(x, y, w, 18);
        this.getter = getter;
        this.setter = setter;
        this.secret = secret;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        Render2D.drawRoundRect(ps, x, y, w, h, 4, Render2D.alpha(Colors.INPUT_BG, alpha));
        String raw = focused ? editing.toString() : getter.get();
        if (raw == null) raw = "";
        String shown = secret && !raw.isEmpty() ? "•".repeat(Math.min(raw.length(), 24)) : raw;
        while (shown.length() > 1 && GlHelper.getStringWidth(shown, Typography.TINY) > w - 10) shown = shown.substring(1);
        if (shown.isEmpty()) shown = "...";
        float tw = GlHelper.getStringWidth(shown, Typography.TINY);
        GlHelper.drawText(shown, Math.max(x + 5, x + w - 5 - tw),
                y + (h - Typography.TINY.getMetrics().capHeight()) / 2f, Typography.TINY,
                Render2D.alpha(shown.equals("...") ? Colors.TEXT_DISABLED : Colors.TEXT_PRIMARY, alpha));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return false;
        focused = true;
        editing.setLength(0);
        if (!secret && getter.get() != null) editing.append(getter.get());
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!focused) return false;
        if (key == 257 || key == 335) { setter.accept(editing.toString()); focused = false; editing.setLength(0); return true; }
        if (key == 256) { focused = false; editing.setLength(0); return true; }
        if (key == 259 && !editing.isEmpty()) { editing.deleteCharAt(editing.length() - 1); return true; }
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (!focused || Character.isISOControl(c) || editing.length() >= 512) return false;
        editing.append(c);
        return true;
    }
}
