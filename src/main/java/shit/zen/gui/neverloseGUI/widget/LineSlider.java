package shit.zen.gui.neverloseGUI.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

/**
 * Neverlose-style inline line slider.
 * <p>
 * Layout: {@code Name                    4.5} with a {@code ━━━━●━━━━} line below.
 * No background track — just a thin line with an accent-filled portion and a draggable dot.
 */
public class LineSlider extends Component {

    private final float min, max, step;
    private float value;
    private boolean dragging;
    private boolean editing;
    private final StringBuilder input = new StringBuilder();
    private long editingStartedAt;
    private final Animation dragAnim = new Animation();
    private float displayFraction;
    private float springVelocity;
    private final String label;
    private final Consumer<Float> onChange;

    private static final float LINE_H  = 2f;
    private static final float DOT_R   = 4f;
    private static final float ROW_H   = 18f;
    private static final float VALUE_W = 48f;

    public LineSlider(float x, float y, float w, String label,
                      float min, float max, float step, float initial,
                      Consumer<Float> onChange) {
        super(x, y, w, ROW_H);
        this.label = label;
        this.min = min; this.max = max; this.step = step;
        this.value = initial;
        this.displayFraction = fraction(initial);
        this.onChange = onChange;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float ha) {
        dragAnim.animate(dragging ? 1f : 0f);
        float da = dragAnim.update(Animation.SPEED_DRAG);
        float targetFraction = fraction(value);
        springVelocity += (targetFraction - displayFraction) * (dragging ? 0.28f : 0.22f);
        springVelocity *= dragging ? 0.72f : 0.64f;
        displayFraction += springVelocity;
        if (Math.abs(targetFraction - displayFraction) < 0.0005f && Math.abs(springVelocity) < 0.0005f) {
            displayFraction = targetFraction;
            springVelocity = 0f;
        }
        float frac = Math.max(0f, Math.min(1f, displayFraction));

        float labelY = y + 2;
        float lineY = y + ROW_H - LINE_H - 2;
        float dotR = DOT_R + da * 1.5f;

        // The value doubles as a compact numeric input.
        String valText = editing ? input.toString() : formatValue();
        if (editing) {
            Render2D.drawRoundRect(ps, valueX(), y, VALUE_W, 11f, 3f,
                Render2D.alpha(Colors.INPUT_BG, alpha));
            Render2D.drawRoundRect(ps, valueX(), y + 10f, VALUE_W, 1f, 0.5f,
                Render2D.alpha(Colors.ACCENT, alpha));
            if ((System.currentTimeMillis() - editingStartedAt) % 1000L < 500L) valText += "|";
        }
        int valColor = dragging || editing ? Colors.ACCENT : Colors.TEXT_PRIMARY;
        float valW = GlHelper.getStringWidth(valText, Typography.SMALL);
        GlHelper.drawText(valText, x + w - valW - (editing ? 3f : 0f), labelY, Typography.SMALL,
            Render2D.alpha(valColor, alpha));

        // Background line (full width, muted)
        Render2D.drawRoundRect(ps, x, lineY, w, LINE_H, 1f,
            Render2D.alpha(Colors.TOGGLE_OFF, alpha * 0.5f));

        // Filled line (accent)
        float fillW = w * frac;
        if (fillW > 1f) {
            if (da > 0.01f) {
                Render2D.drawRoundRect(ps, x, lineY - 1f, fillW, LINE_H + 2f, 2f,
                    Render2D.alpha(Colors.ACCENT, alpha * da * 0.18f));
            }
            Render2D.drawRoundRect(ps, x, lineY, fillW, LINE_H, 1f,
                Render2D.alpha(Colors.ACCENT, alpha));
        }

        // Draggable dot
        float dotX = x + fillW - dotR;
        if (da > 0.01f) {
            Render2D.drawCircle(ps, dotX + dotR, lineY + LINE_H / 2f, dotR + 3f + da,
                Render2D.alpha(Colors.ACCENT, alpha * da * 0.14f));
        }
        Render2D.drawCircle(ps, dotX + dotR, lineY + LINE_H / 2f, dotR,
            Render2D.alpha(Colors.ACCENT, alpha * (0.8f + da * 0.2f)));
    }

    private float fraction(float currentValue) {
        float range = max - min;
        return range == 0f ? 0f : Math.max(0f, Math.min(1f, (currentValue - min) / range));
    }

    private String formatValue() {
        if (step >= 1f) return String.valueOf((int) value);
        if (step >= 0.1f) return String.format(Locale.ROOT, "%.1f", value);
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return false;
        if (isValueHit(mx, my)) {
            beginEditing();
            return true;
        }
        if (editing) commitInput();
        if (contains(mx, my)) { dragging = true; update((float) mx); return true; }
        return false;
    }
    @Override public void mouseReleased(double mx, double my, int btn) { dragging = false; }
    @Override public void mouseDragged(double mx, double my) { if (dragging) update((float) mx); }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!editing) return false;
        if (key == 257 || key == 335) { commitInput(); return true; }
        if (key == 256) { cancelInput(); return true; }
        if (key == 259 && !input.isEmpty()) { input.deleteCharAt(input.length() - 1); return true; }
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (!editing) return false;
        if (c == ',') c = '.';
        if (Character.isDigit(c) && input.length() < 16) { input.append(c); return true; }
        if (c == '.' && step < 1f && input.indexOf(".") < 0 && input.length() < 16) {
            if (input.isEmpty() || "-".contentEquals(input)) input.append('0');
            input.append('.');
            return true;
        }
        if (c == '-' && min < 0f && input.isEmpty()) { input.append(c); return true; }
        return true;
    }

    /** Commits an active edit when another control or the surrounding panel is clicked. */
    public void blurUnlessValueHit(double mx, double my) {
        if (editing && !isValueHit(mx, my)) commitInput();
    }

    private float valueX() { return x + w - VALUE_W; }

    private boolean isValueHit(double mx, double my) {
        return mx >= valueX() && mx <= x + w && my >= y && my <= y + 11f;
    }

    private void beginEditing() {
        if (editing) return;
        dragging = false;
        editing = true;
        editingStartedAt = System.currentTimeMillis();
        input.setLength(0);
        input.append(formatValue());
    }

    private void cancelInput() {
        editing = false;
        input.setLength(0);
    }

    private void commitInput() {
        if (!editing) return;
        try {
            String text = input.toString().trim();
            if (!text.isEmpty() && !"-".equals(text) && !".".equals(text) && !"-.".equals(text)) {
                setValue(Float.parseFloat(text));
            }
        } catch (NumberFormatException ignored) {
            // Invalid transient input keeps the previous slider value.
        }
        cancelInput();
    }

    private void update(float mx) {
        float frac = Math.max(0, Math.min(1, (mx - x) / w));
        float raw = min + frac * (max - min);
        setValue(raw);
    }

    private void setValue(float raw) {
        float snapped = step > 0f ? Math.round(raw / step) * step : raw;
        value = Math.max(min, Math.min(max, snapped));
        if (onChange != null) onChange.accept(value);
    }
}
