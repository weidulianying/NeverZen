package shit.zen.gui.panel.setting;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.gui.PanelClickGui;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.TextGlow;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.StringSetting;
import shit.zen.utils.render.RenderUtil;

/** Compact text input used by SelfSkin's UUID, path, and URL settings. */
public class StringSettingRenderer extends ClientBase implements SettingRenderer {
    private static StringSetting editing;
    private static String text;

    @Override
    public int render(GuiGraphics graphics, Setting<?> setting, int x, int y, int width, int mouseX, int mouseY, float alpha, float scale) {
        if (!(setting instanceof StringSetting value)) return 0;
        int rowHeight = Math.round(24.0f * scale);
        int widgetHeight = Math.round(16.0f * scale);
        int widgetWidth = Math.round(220.0f * scale);
        int widgetX = x + width - widgetWidth;
        int widgetY = y + (rowHeight - widgetHeight) / 2;
        FontRenderer nameFont = FontPresets.axiformaRegular(14.0f * scale);
        float nameY = y + rowHeight / 2.0f - nameFont.getMetrics().capHeight() / 2.0f;
        TextGlow.drawGlowText(value.getName(), x, nameY, nameFont, applyAlpha(-1, alpha), applyAlpha(new Color(255, 255, 255, 120).getRGB(), alpha), 6.0f * scale);
        RenderUtil.drawRoundedRect(graphics.pose(), widgetX, widgetY, widgetWidth, widgetHeight, 4.0f * scale, applyAlpha(0x30101010, alpha));
        FontRenderer valueFont = FontPresets.axiformaRegular(12.0f * scale);
        String display = value == editing ? text : value.getValue();
        if (display == null || display.isEmpty()) display = "...";
        display = fitText(display, valueFont, widgetWidth - Math.round(12.0f * scale));
        float textWidth = GlHelper.getStringWidth(display, valueFont);
        float textX = Math.max(widgetX + 6.0f * scale, widgetX + widgetWidth - 6.0f * scale - textWidth);
        GlHelper.drawText(display, textX, widgetY + widgetHeight / 2.0f - valueFont.getMetrics().capHeight() / 2.0f, valueFont, applyAlpha(-1, alpha));
        return rowHeight;
    }

    @Override
    public boolean onClick(Setting<?> setting, int x, int y, int width, int mouseX, int mouseY, int button, float scale) {
        if (!(setting instanceof StringSetting value) || button != 0) return false;
        int rowHeight = Math.round(24.0f * scale);
        int widgetHeight = Math.round(16.0f * scale);
        int widgetWidth = Math.round(220.0f * scale);
        int widgetX = x + width - widgetWidth;
        int widgetY = y + (rowHeight - widgetHeight) / 2;
        if (mouseX < widgetX || mouseX > widgetX + widgetWidth || mouseY < widgetY || mouseY > widgetY + widgetHeight) return false;
        editing = value;
        text = value.getValue() == null ? "" : value.getValue();
        return true;
    }

    public static boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (editing == null) return false;
        if (keyCode == 257 || keyCode == 335) {
            editing.setValue(text);
            PanelClickGui.panelClickGui.addToast(editing.getName() + " updated");
            editing = null;
            text = "";
            return true;
        }
        if (keyCode == 256) {
            editing = null;
            text = "";
            return true;
        }
        if (keyCode == 259 && !text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
            return true;
        }
        return false;
    }

    public static boolean onCharTyped(char character) {
        if (editing == null || Character.isISOControl(character) || text.length() >= 512) return false;
        text += character;
        return true;
    }

    private static int applyAlpha(int color, float alpha) {
        return (int)(((color >>> 24) & 0xFF) * alpha) << 24 | color & 0xFFFFFF;
    }

    private static String fitText(String value, FontRenderer font, float maxWidth) {
        if (GlHelper.getStringWidth(value, font) <= maxWidth) return value;
        String suffix = "...";
        while (value.length() > 1 && GlHelper.getStringWidth(value, font) > maxWidth) {
            value = value.substring(1);
            if (GlHelper.getStringWidth(suffix + value, font) <= maxWidth) return suffix + value;
        }
        return suffix;
    }

    @Override public boolean supports(Setting<?> setting) { return setting instanceof StringSetting; }
    @Override public int getHeight(Setting<?> setting, float scale) { return Math.round(24.0f * scale); }
    @Override public void onMouseRelease(double mouseX, double mouseY, int button) { }
}
