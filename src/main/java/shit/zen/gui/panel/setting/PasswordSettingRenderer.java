package shit.zen.gui.panel.setting;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.TextGlow;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.PasswordSetting;
import shit.zen.utils.render.RenderUtil;

/** Password editor that never exposes or persists its contents. */
public final class PasswordSettingRenderer extends ClientBase implements SettingRenderer {
    private static PasswordSetting editing;
    private static final StringBuilder text = new StringBuilder();

    @Override
    public int render(GuiGraphics graphics, Setting<?> setting, int x, int y, int width, int mouseX, int mouseY, float alpha, float scale) {
        PasswordSetting value = (PasswordSetting) setting;
        int rowHeight = Math.round(24.0f * scale), widgetHeight = Math.round(16.0f * scale);
        int widgetWidth = Math.round(220.0f * scale), widgetX = x + width - widgetWidth;
        int widgetY = y + (rowHeight - widgetHeight) / 2;
        FontRenderer nameFont = FontPresets.axiformaRegular(14.0f * scale);
        TextGlow.drawGlowText(value.getName(), x, y + rowHeight / 2.0f - nameFont.getMetrics().capHeight() / 2.0f,
                nameFont, applyAlpha(-1, alpha), applyAlpha(new Color(255, 255, 255, 120).getRGB(), alpha), 6.0f * scale);
        RenderUtil.drawRoundedRect(graphics.pose(), widgetX, widgetY, widgetWidth, widgetHeight, 4.0f * scale, applyAlpha(0x30101010, alpha));
        int count = value == editing ? text.length() : value.getValue().length;
        String display = count == 0 ? "..." : "•".repeat(Math.min(count, 24));
        FontRenderer font = FontPresets.axiformaRegular(12.0f * scale);
        float textWidth = GlHelper.getStringWidth(display, font);
        GlHelper.drawText(display, widgetX + widgetWidth - 6.0f * scale - textWidth,
                widgetY + widgetHeight / 2.0f - font.getMetrics().capHeight() / 2.0f, font, applyAlpha(-1, alpha));
        return rowHeight;
    }

    @Override
    public boolean onClick(Setting<?> setting, int x, int y, int width, int mouseX, int mouseY, int button, float scale) {
        if (button != 0) return false;
        int rowHeight = Math.round(24.0f * scale), widgetHeight = Math.round(16.0f * scale);
        int widgetWidth = Math.round(220.0f * scale), widgetX = x + width - widgetWidth;
        int widgetY = y + (rowHeight - widgetHeight) / 2;
        if (mouseX < widgetX || mouseX > widgetX + widgetWidth || mouseY < widgetY || mouseY > widgetY + widgetHeight) return false;
        editing = (PasswordSetting) setting;
        text.setLength(0);
        return true;
    }

    public static boolean onKeyPress(int keyCode) {
        if (editing == null) return false;
        if (keyCode == 257 || keyCode == 335) {
            editing.clear();
            editing.setValue(text.toString().toCharArray());
            finish();
            return true;
        }
        if (keyCode == 256) { finish(); return true; }
        if (keyCode == 259 && !text.isEmpty()) { text.deleteCharAt(text.length() - 1); return true; }
        return false;
    }

    public static boolean onCharTyped(char character) {
        if (editing == null || Character.isISOControl(character) || text.length() >= 256) return false;
        text.append(character);
        return true;
    }

    private static void finish() { editing = null; text.setLength(0); }
    private static int applyAlpha(int color, float alpha) {
        return (int) (((color >>> 24) & 0xff) * alpha) << 24 | color & 0xffffff;
    }
    @Override public boolean supports(Setting<?> setting) { return setting instanceof PasswordSetting; }
    @Override public int getHeight(Setting<?> setting, float scale) { return Math.round(24.0f * scale); }
    @Override public void onMouseRelease(double mouseX, double mouseY, int button) { }
}
