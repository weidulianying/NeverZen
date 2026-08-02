package shit.zen.gui.neverloseGUI.card;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.factory.WidgetFactory;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.model.ModuleViewModel;
import shit.zen.gui.neverloseGUI.model.SettingViewModel;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.gui.neverloseGUI.widget.KeyBind;
import shit.zen.gui.neverloseGUI.widget.Toggle;
import shit.zen.render.GlHelper;

/**
 * ModuleCard = Header(Toggle + KeyBind + name) + expanded SettingWidgets.
 * Each widget stores its own x-offset from card left; only y is updated on scroll.
 */
public class ModuleCard extends Component {

    private final ModuleViewModel vm;
    private boolean expanded;
    private final Animation expandAnim = new Animation();
    private final List<Component> widgets = new ArrayList<>();
    private final float[] widgetOffX; // x offset from card.x for each widget
    private final Toggle toggle;
    private final KeyBind keyBind;
    private final Consumer<ModuleViewModel> onSelect;
    private static final float HEADER_H = 36, ROW_H = 22;

    public ModuleCard(float x, float y, float w, ModuleViewModel vm, Consumer<ModuleViewModel> onSelect) {
        super(x, y, w, HEADER_H);
        this.vm = vm;
        this.onSelect = onSelect;
        this.toggle = new Toggle(x + w - 40, y + (HEADER_H - 18) / 2f, vm.isEnabled(), vm::setEnabled);
        this.keyBind = new KeyBind(x + w - 78, y + (HEADER_H - 18) / 2f, 40, 18, vm.keyCode(), vm::setKeyCode);
        this.widgetOffX = new float[vm.settings().size()];
        buildWidgets();
    }

    private void buildWidgets() {
        widgets.clear();
        List<SettingViewModel> svms = vm.settings();
        for (int i = 0; i < svms.size(); i++) {
            SettingViewModel svm = svms.get(i);
            Component w = WidgetFactory.create(svm, x + 10, y + HEADER_H + 6 + i * ROW_H, this.w - 20);
            if (w != null) {
                widgets.add(w);
                widgetOffX[i] = w.x() - this.x; // store relative x offset
            }
        }
    }

    @Override public float h() { return expanded ? HEADER_H + widgets.size() * ROW_H + 8 : HEADER_H; }

    @Override public boolean contains(double mx, double my) {
        if (super.contains(mx, my)) return true;
        if (expanded) for (Component w : widgets) if (w.contains(mx, my)) return true;
        return false;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        float cardH = h();
        int bg = Render2D.alpha(Colors.CARD, alpha);
        if (ha > 0.01f) bg = Render2D.lerpColor(bg, Render2D.alpha(0xFF252830, alpha), ha * 0.4f);
        Render2D.drawRoundRect(ps, x, y, w, cardH, 8f, bg);

        // Header: name + keybind + toggle
        toggle.setOn(vm.isEnabled());
        int nc = vm.isEnabled() ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY;
        GlHelper.drawText(vm.name(), x + 12, y + (HEADER_H - Typography.H2.getMetrics().capHeight()) / 2f,
                Typography.H2, Render2D.alpha(nc, alpha));

        toggle.setPos(x + this.w - 40, y + (HEADER_H - 18) / 2f);
        toggle.render(g, mx, my, alpha);
        keyBind.setPos(x + this.w - 78, y + (HEADER_H - 18) / 2f);
        keyBind.render(g, mx, my, alpha);

        // Expanded settings
        expandAnim.animate(expanded ? 1f : 0f);
        if (expanded || expandAnim.peek() > 0.01f) {
            Render2D.drawRect(ps, x + 10, y + HEADER_H + 2, this.w - 20, 1f,
                    Render2D.alpha(Colors.BORDER, alpha * 0.3f));
            if (expanded) {
                List<SettingViewModel> svms = vm.settings();
                for (int i = 0; i < widgets.size(); i++) {
                    Component w = widgets.get(i);
                    // Restore x from stored offset, only update y for scroll
                    w.setPos(x + widgetOffX[i], y + HEADER_H + 6 + i * ROW_H);
                    // Setting name (left)
                    GlHelper.drawText(svms.get(i).name(), x + 12, w.y() + 3,
                            Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, alpha));
                    w.render(g, mx, my, alpha);
                }
            }
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (toggle.contains(mx, my)) return toggle.mouseClicked(mx, my, btn);
        if (keyBind.contains(mx, my)) return keyBind.mouseClicked(mx, my, btn);
        if (my <= y + HEADER_H) {
            expanded = !expanded; expandAnim.animate(expanded ? 1f : 0f);
            if (expanded) buildWidgets();
            if (onSelect != null) onSelect.accept(vm);
            return true;
        }
        if (expanded) for (Component w : widgets) if (w.contains(mx, my)) return w.mouseClicked(mx, my, btn);
        return false;
    }
    @Override public void mouseReleased(double mx, double my, int btn) { toggle.mouseReleased(mx, my, btn); keyBind.mouseReleased(mx, my, btn); for (Component w : widgets) w.mouseReleased(mx, my, btn); }
    @Override public void mouseDragged(double mx, double my) { for (Component w : widgets) w.mouseDragged(mx, my); }
    @Override public boolean keyPressed(int k, int s, int m) { if (keyBind.keyPressed(k, s, m)) return true; for (Component w : widgets) if (w.keyPressed(k, s, m)) return true; return false; }
    @Override public boolean charTyped(char c, int mods) { return keyBind.charTyped(c, mods); }
}
