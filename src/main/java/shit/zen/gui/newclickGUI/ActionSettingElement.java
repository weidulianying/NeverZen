package shit.zen.gui.newclickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.NewClickGui;
import shit.zen.render.FontStore;
import shit.zen.settings.impl.ActionSetting;
import shit.zen.ui.neverlose.NeverloseTheme;
import shit.zen.utils.misc.CursorUtil;
import shit.zen.utils.math.Easings;
import shit.zen.utils.render.ColorUtil;
import shit.zen.utils.render.RenderUtil;

public final class ActionSettingElement extends SettingElement<ActionSetting> {
    private boolean hovered;
    public ActionSettingElement(CategoryPanel panel, ActionSetting setting) { super(panel, setting); }

    @Override
    public void render(NewClickGui gui, GuiGraphics graphics, PoseStack pose, int mouseX, int mouseY, float alpha, float partialTicks) {
        this.visibilityTimer.animate(this.setting.getVisibility() == null || this.setting.getVisibility().displayable() ? 1.0 : 0.0, 0.2, Easings.EASE_OUT_POW2);
        this.visibilityTimer.tick();
        alpha *= this.visibilityTimer.getValueF();
        if (alpha <= 0.0f) return;
        hovered = CursorUtil.isInBounds(mouseX, mouseY, x + 6, y + 2, 108, 16);
        RenderUtil.drawRoundedRect(pose, x + 6, y + 2, 108, 16, 3,
                ColorUtil.withAlpha(hovered ? NeverloseTheme.ACCENT : NeverloseTheme.BG_ELEMENT, alpha * (hovered ? .7f : 1)));
        FontStore.AXIFORMA_BOLD_13.drawStringCentered(pose, setting.getName(), x + 60, y + 5, ColorUtil.withAlpha(-1, alpha * .9f));
    }

    @Override public float getHeight() { return 20; }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered && button == 0) { setting.invoke(); return true; }
        return false;
    }
}
