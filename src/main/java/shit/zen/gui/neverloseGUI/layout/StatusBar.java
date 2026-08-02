package shit.zen.gui.neverloseGUI.layout;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

public class StatusBar {
    public void render(PoseStack ps, GuiGraphics g, float cx, float cy, float w, int mx, int my, float a) {
        Render2D.drawRect(ps, cx + 100, cy - 1, w - 100, 1f, Render2D.alpha(Colors.BORDER, a * 0.5f));
        long mem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        String info = "Build 1.0  |  " + mem + "MB  |  " + Minecraft.getInstance().getUser().getName();
        GlHelper.drawText(info, cx + 12, cy + 4, Typography.SMALL, Render2D.alpha(Colors.TEXT_DISABLED, a * 0.5f));
    }
}
