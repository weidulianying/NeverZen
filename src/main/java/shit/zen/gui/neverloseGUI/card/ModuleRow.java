package shit.zen.gui.neverloseGUI.card;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.factory.WidgetFactory;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.model.ModuleViewModel;
import shit.zen.gui.neverloseGUI.model.SettingViewModel;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.gui.neverloseGUI.widget.DotToggle;
import shit.zen.render.GlHelper;

/**
 * Neverlose-style text-flow module row — no background card.
 * <p>
 * Layout:
 * <pre>
 * KillAura                             ●
 *   Range                           4.5
 *   ━━━━━━●━━━━━━
 *   Mode                        Single ▼
 * </pre>
 * Module name on the first line with a dot-toggle right-aligned.
 * Settings rendered inline below, indented 12px.
 * Clicking the name row expands/collapses settings.
 */
public class ModuleRow extends Component {

    private final ModuleViewModel vm;
    private boolean expanded;
    private final Animation expandAnim = new Animation();
    private final List<Component> widgets = new ArrayList<>();
    private final float[] widgetOffX;
    private final DotToggle toggle;

    private static final float HEADER_H = 22f;
    private static final float ROW_H    = 20f;
    private static final float INDENT   = 12f;

    public ModuleRow(float x, float y, float w, ModuleViewModel vm) {
        super(x, y, w, HEADER_H);
        this.vm = vm;
        this.toggle = new DotToggle(x + w - 18, y + (HEADER_H - 16) / 2f, vm.isEnabled(), vm::setEnabled);
        this.widgetOffX = new float[vm.settings().size()];
        buildWidgets();
    }

    private void buildWidgets() {
        widgets.clear();
        List<SettingViewModel> svms = vm.settings();
        for (int i = 0; i < svms.size(); i++) {
            SettingViewModel svm = svms.get(i);
            float sx = x + INDENT;
            float sy = y + HEADER_H + 2 + i * ROW_H;
            Component widget = WidgetFactory.create(svm, sx, sy, this.w - INDENT - 8);
            if (widget != null) {
                widgets.add(widget);
                widgetOffX[i] = widget.x() - this.x;
            }
        }
    }

    @Override
    public float h() {
        return expanded ? HEADER_H + widgets.size() * ROW_H + 4 : HEADER_H;
    }

    @Override
    public boolean contains(double mx, double my) {
        if (super.contains(mx, my)) return true;
        if (expanded) for (Component w : widgets) if (w.contains(mx, my)) return true;
        return false;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        toggle.setOn(vm.isEnabled());

        // ── Module name ──
        int nameColor = vm.isEnabled() ? Colors.ACCENT : Colors.TEXT_SECONDARY;
        GlHelper.drawText(vm.name(), x, y + (HEADER_H - Typography.H2.getMetrics().capHeight()) / 2f,
            Typography.H2, Render2D.alpha(nameColor, alpha));

        // ── Dot toggle (right-aligned) ──
        toggle.setPos(x + w - 18, y + (HEADER_H - 16) / 2f);
        toggle.render(g, mx, my, alpha);

        // ── Settings (expanded) ──
        expandAnim.animate(expanded ? 1f : 0f);
        if (expanded || expandAnim.peek() > 0.01f) {
            List<SettingViewModel> svms = vm.settings();
            for (int i = 0; i < widgets.size(); i++) {
                Component w = widgets.get(i);
                w.setPos(x + widgetOffX[i], y + HEADER_H + 2 + i * ROW_H);

                if (!(w instanceof DotToggle)) {
                    // Setting name on the left
                    GlHelper.drawText(svms.get(i).name(), x + INDENT,
                        w.y() + (ROW_H - Typography.SMALL.getMetrics().capHeight()) / 2f,
                        Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, alpha));
                }
                w.render(g, mx, my, alpha);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (toggle.contains(mx, my)) return toggle.mouseClicked(mx, my, btn);
        if (my <= y + HEADER_H && mx < x + w - 20) {
            expanded = !expanded;
            expandAnim.animate(expanded ? 1f : 0f);
            if (expanded) buildWidgets();
            return true;
        }
        if (expanded) for (Component w : widgets) if (w.contains(mx, my)) return w.mouseClicked(mx, my, btn);
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int btn) {
        toggle.mouseReleased(mx, my, btn);
        for (Component w : widgets) w.mouseReleased(mx, my, btn);
    }

    @Override
    public void mouseDragged(double mx, double my) {
        for (Component w : widgets) w.mouseDragged(mx, my);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        for (Component w : widgets) if (w.keyPressed(k, s, m)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        for (Component w : widgets) if (w.charTyped(c, mods)) return true;
        return false;
    }
}
