package shit.zen.gui.neverloseGUI.layout;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.GlHelper;

/** Right-side panel — shows info about the selected module. */
public class InspectorPanel {

    private String moduleName = "";
    private String category = "";
    private int keyCode;
    private String description = "Select a module to view details";

    public void setModule(String name, String cat, int key, String desc) {
        this.moduleName = name; this.category = cat; this.keyCode = key; this.description = desc;
    }

    public void render(PoseStack ps, GuiGraphics g, float ox, float oy, float w, float h, int mx, int my, float a) {
        // Panel bg
        Render2D.drawRoundRect(ps, ox, oy, w, h, 8f, Render2D.alpha(Colors.PANEL, a * 0.6f));

        if (moduleName.isEmpty()) {
            GlHelper.drawText("Inspector", ox + 10, oy + 8, Typography.H2, Render2D.alpha(Colors.TEXT_PRIMARY, a));
            GlHelper.drawText(description, ox + 10, oy + 32, Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, a));
            return;
        }

        float y = oy + 8;
        GlHelper.drawText(moduleName, ox + 10, y, Typography.H2, Render2D.alpha(Colors.TEXT_PRIMARY, a));
        y += 20;
        GlHelper.drawText(category, ox + 10, y, Typography.SMALL, Render2D.alpha(Colors.ACCENT, a));
        y += 20;
        GlHelper.drawText("Key: " + (keyCode == 0 ? "None" : "Key"+keyCode), ox + 10, y, Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, a));
        y += 18;
        Render2D.drawRect(ps, ox + 10, y, w - 20, 1f, Render2D.alpha(Colors.BORDER, a * 0.3f));
        y += 8;
        GlHelper.drawText(description, ox + 10, y, Typography.SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, a));
    }
}
