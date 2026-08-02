package shit.zen.gui.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import shit.zen.render.FontStore;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.ActionSetting;
import shit.zen.utils.render.RenderUtil;

public final class ActionComponent extends SettingComponent {
    public ActionComponent(Setting<?> setting, ModuleButton parent, int yOffset) { super(setting, parent, yOffset); }

    @Override
    public void renderWithAlpha(PoseStack poseStack, int mouseX, int mouseY, float partialTicks, float alpha) {
        int rowY = parentButton.panel.y + parentButton.yOffset + parentButton.panel.rowHeight + yOffset;
        int x = parentButton.panel.x + 7, width = parentButton.panel.width - 14;
        RenderUtil.drawFilledRect(poseStack, x, rowY + 3, width, parentButton.panel.rowHeight - 6,
                new Color(70, 110, 190, (int) (170 * alpha)).getRGB());
        String name = setting.getName();
        float tw = FontStore.OPENSANS_16.getStringWidth(name);
        FontStore.OPENSANS_16.drawStringWithShadow(poseStack, name, x + (width - tw) / 2f, rowY + 4, -1);
    }

    @Override public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) ((ActionSetting) setting).invoke();
    }
}
