package shit.zen.gui.newclickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.NewClickGui;
import shit.zen.render.FontStore;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.PasswordSetting;
import shit.zen.settings.impl.StringSetting;
import shit.zen.ui.neverlose.NeverloseTheme;
import shit.zen.utils.misc.CursorUtil;
import shit.zen.utils.math.Easings;
import shit.zen.utils.render.ColorUtil;
import shit.zen.utils.render.RenderUtil;

public final class TextSettingElement extends SettingElement<Setting<?>> {
    private final StringBuilder text = new StringBuilder();
    private boolean editing;
    private boolean inputHovered;

    public TextSettingElement(CategoryPanel panel, Setting<?> setting) { super(panel, setting); }

    @Override
    public void render(NewClickGui gui, GuiGraphics graphics, PoseStack pose, int mouseX, int mouseY, float alpha, float partialTicks) {
        this.visibilityTimer.animate(this.setting.getVisibility() == null || this.setting.getVisibility().displayable() ? 1.0 : 0.0, 0.2, Easings.EASE_OUT_POW2);
        this.visibilityTimer.tick();
        alpha *= this.visibilityTimer.getValueF();
        if (alpha <= 0.0f) return;
        float inputX = x + 6, inputY = y + 16, inputW = 108, inputH = 15;
        inputHovered = CursorUtil.isInBounds(mouseX, mouseY, inputX, inputY, inputW, inputH);
        FontStore.AXIFORMA_REGULAR_14.drawString(pose, setting.getName(), x + 6, y + 3, ColorUtil.withAlpha(-1, alpha * .8f));
        RenderUtil.drawRoundedRect(pose, inputX, inputY, inputW, inputH, 3,
                ColorUtil.withAlpha(editing ? NeverloseTheme.BG_PANEL : NeverloseTheme.BG_ELEMENT, alpha));
        String raw = editing ? text.toString() : value();
        String shown = setting instanceof PasswordSetting && !raw.isEmpty() ? "•".repeat(Math.min(raw.length(), 24)) : raw;
        if (shown.isEmpty()) shown = "...";
        while (shown.length() > 1 && FontStore.AXIFORMA_REGULAR_14.getStringWidth(shown) > inputW - 8) shown = shown.substring(1);
        float tw = FontStore.AXIFORMA_REGULAR_14.getStringWidth(shown);
        FontStore.AXIFORMA_REGULAR_14.drawString(pose, shown, inputX + inputW - 4 - tw, inputY + 2,
                ColorUtil.withAlpha(-1, alpha * (shown.equals("...") ? .35f : .8f)));
    }

    @Override public float getHeight() { return 35; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (setting.getVisibility() != null && !setting.getVisibility().displayable()) return false;
        if (!inputHovered || button != 0) return false;
        editing = true;
        text.setLength(0);
        if (setting instanceof StringSetting) text.append(value());
        return true;
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
