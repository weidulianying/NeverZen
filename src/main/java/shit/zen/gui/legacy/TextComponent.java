package shit.zen.gui.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import shit.zen.render.FontStore;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.PasswordSetting;
import shit.zen.settings.impl.StringSetting;
import shit.zen.utils.render.RenderUtil;

public final class TextComponent extends SettingComponent {
    private final StringBuilder text = new StringBuilder();
    private boolean editing;

    public TextComponent(Setting<?> setting, ModuleButton parent, int yOffset) {
        super(setting, parent, yOffset);
    }

    @Override
    public void renderWithAlpha(PoseStack poseStack, int mouseX, int mouseY, float partialTicks, float alpha) {
        int rowY = parentButton.panel.y + parentButton.yOffset + parentButton.panel.rowHeight + yOffset;
        int x = parentButton.panel.x, width = parentButton.panel.width, padding = 7;
        int color = new Color(255, 255, 255, (int) (255 * alpha)).getRGB();
        FontStore.OPENSANS_16.drawStringWithShadow(poseStack, setting.getName(), x + padding, rowY + 4, color);
        String raw = editing ? text.toString() : value();
        String shown = setting instanceof PasswordSetting && !raw.isEmpty() ? "•".repeat(Math.min(raw.length(), 20)) : raw;
        if (shown.isEmpty()) shown = "...";
        float max = width * 0.54f;
        while (shown.length() > 1 && FontStore.OPENSANS_16.getStringWidth(shown) > max) shown = shown.substring(1);
        float tw = FontStore.OPENSANS_16.getStringWidth(shown);
        RenderUtil.drawFilledRect(poseStack, x + width - max - padding, rowY + 3, max, parentButton.panel.rowHeight - 6,
                new Color(30, 30, 35, (int) (210 * alpha)).getRGB());
        FontStore.OPENSANS_16.drawStringWithShadow(poseStack, shown, x + width - padding - tw, rowY + 4, color);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        editing = true;
        text.setLength(0);
        if (setting instanceof StringSetting) text.append(value());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editing) return false;
        if (keyCode == 257 || keyCode == 335) { commit(); return true; }
        if (keyCode == 256) { editing = false; text.setLength(0); return true; }
        if (keyCode == 259 && !text.isEmpty()) text.deleteCharAt(text.length() - 1);
        return true;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (!editing || Character.isISOControl(character) || text.length() >= 512) return false;
        text.append(character);
        return true;
    }

    private String value() {
        if (setting instanceof StringSetting string) return string.getValue() == null ? "" : string.getValue();
        return "•".repeat(((PasswordSetting) setting).getValue().length);
    }

    private void commit() {
        if (setting instanceof StringSetting string) string.setValue(text.toString());
        else {
            PasswordSetting password = (PasswordSetting) setting;
            password.clear();
            password.setValue(text.toString().toCharArray());
        }
        editing = false;
        text.setLength(0);
    }
}
