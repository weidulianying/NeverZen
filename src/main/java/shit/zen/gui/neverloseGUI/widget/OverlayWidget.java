package shit.zen.gui.neverloseGUI.widget;

import net.minecraft.client.gui.GuiGraphics;

/** A control whose popup must be rendered after the page scissor is removed. */
public interface OverlayWidget {
    boolean isOverlayOpen();
    void renderOverlay(GuiGraphics g, int mx, int my, float alpha);
    boolean overlayMouseClicked(double mx, double my, int btn);
    default boolean overlayMouseScrolled(double delta) { return false; }
    void closeOverlay();
}
