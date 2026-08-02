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
import shit.zen.settings.impl.ActionSetting;
import shit.zen.utils.render.RenderUtil;

public final class ActionSettingRenderer extends ClientBase implements SettingRenderer {
    @Override
    public int render(GuiGraphics graphics, Setting<?> setting, int x, int y, int width,
                      int mouseX, int mouseY, float alpha, float scale) {
        if (!(setting instanceof ActionSetting action)) return 0;
        int rowHeight = Math.round(28.0f * scale);
        int buttonWidth = Math.round(96.0f * scale);
        int buttonHeight = Math.round(19.0f * scale);
        int buttonX = x + width - buttonWidth;
        int buttonY = y + (rowHeight - buttonHeight) / 2;
        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        int background = hovered ? 0x604CAF50 : 0x404CAF50;
        RenderUtil.drawRoundedRect(graphics.pose(), buttonX, buttonY, buttonWidth, buttonHeight,
                4.0f * scale, applyAlpha(background, alpha));
        FontRenderer iconFont = FontPresets.materialIcons(14.0f * scale);
        FontRenderer textFont = FontPresets.axiformaBold(12.0f * scale);
        String icon = "\uE5D5";
        float iconWidth = GlHelper.getStringWidth(icon, iconFont);
        float textWidth = GlHelper.getStringWidth(action.getName(), textFont);
        float startX = buttonX + (buttonWidth - iconWidth - textWidth - 4.0f * scale) / 2.0f;
        float centerY = buttonY + buttonHeight / 2.0f;
        GlHelper.drawText(icon, startX, centerY - iconFont.getMetrics().capHeight() / 2.0f,
                iconFont, applyAlpha(-1, alpha));
        TextGlow.drawGlowText(action.getName(), startX + iconWidth + 4.0f * scale,
                centerY - textFont.getMetrics().capHeight() / 2.0f, textFont,
                applyAlpha(-1, alpha), applyAlpha(new Color(255, 255, 255, 100).getRGB(), alpha), 5.0f * scale);
        return rowHeight;
    }

    @Override
    public boolean onClick(Setting<?> setting, int x, int y, int width, int mouseX, int mouseY, int button, float scale) {
        if (!(setting instanceof ActionSetting action) || button != 0) return false;
        int rowHeight = Math.round(28.0f * scale);
        int buttonWidth = Math.round(96.0f * scale);
        int buttonHeight = Math.round(19.0f * scale);
        int buttonX = x + width - buttonWidth;
        int buttonY = y + (rowHeight - buttonHeight) / 2;
        if (mouseX < buttonX || mouseX > buttonX + buttonWidth || mouseY < buttonY || mouseY > buttonY + buttonHeight) return false;
        action.invoke();
        PanelClickGui.panelClickGui.addToast(action.getName() + " requested");
        return true;
    }

    private static int applyAlpha(int color, float alpha) {
        return (int)(((color >>> 24) & 0xFF) * alpha) << 24 | color & 0xFFFFFF;
    }

    @Override public boolean supports(Setting<?> setting) { return setting instanceof ActionSetting; }
    @Override public int getHeight(Setting<?> setting, float scale) { return Math.round(28.0f * scale); }
    @Override public void onMouseRelease(double mouseX, double mouseY, int button) { }
}
