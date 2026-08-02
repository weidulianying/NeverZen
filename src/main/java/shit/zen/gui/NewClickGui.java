package shit.zen.gui;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import shit.zen.ui.neverlose.NeverloseTheme;
import shit.zen.gui.newclickgui.CategoryPanel;
import shit.zen.modules.Category;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.utils.animation.SmoothAnimationTimer;
import shit.zen.utils.math.Easings;
import shit.zen.utils.render.ColorUtil;
import shit.zen.utils.render.RenderUtil;

public class NewClickGui
extends Screen {
    private static final float NAV_WIDTH = 80.0f;
    private static final float PANEL_WIDTH = 120.0f;
    private static final float GAP = 8.0f;
    private static final List<CategoryPanel> categoryPanels;
    public static CategoryPanel focusedPanel;
    @Getter
    private boolean closing = false;
    @Getter
    private final SmoothAnimationTimer closeAnim = new SmoothAnimationTimer();

    public NewClickGui() {
        super(Component.literal("ClickGui"));
    }

    private float getNavX() {
        float totalWidth = NAV_WIDTH + GAP + categoryPanels.size() * PANEL_WIDTH;
        return Math.max(0.0f, ((float)this.width - totalWidth) / 2.0f);
    }

    private float getPanelStartX() {
        return getNavX() + NAV_WIDTH + GAP;
    }

    protected void init() {
        focusedPanel = categoryPanels.get(0);
        float panelX = getPanelStartX();
        for (CategoryPanel categoryPanel : categoryPanels) {
            categoryPanel.setX(panelX);
            categoryPanel.setY(36.0f);
            panelX += PANEL_WIDTH;
        }
    }

    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.closeAnim.animate(this.closing ? 0.0 : 1.0, 0.22, Easings.EASE_OUT_EXPO);
        this.closeAnim.tick();
        float closeProgress = this.closeAnim.getValueF();
        if (Mth.equal(closeProgress, 0.0f) && this.closing) {
            this.closing = false;
            super.onClose();
            categoryPanels.forEach(CategoryPanel::reset);
            return;
        }

        // Dark overlay with blur
        RenderUtil.drawFilledRect(guiGraphics.pose(), 0.0f, 0.0f, this.width, this.height,
            ColorUtil.withAlpha(NeverloseTheme.BG_SOLID, 0.6f * closeProgress));

        // Left-side navigation bar
        float navWidth = NAV_WIDTH;
        float navX = getNavX();
        float navY = 36.0f;

        // Nav bar background
        RenderUtil.drawRoundedRect(guiGraphics.pose(), navX, navY, navWidth,
            categoryPanels.size() * 32.0f + 8.0f,
            NeverloseTheme.RADIUS, ColorUtil.withAlpha(NeverloseTheme.BG_PANEL, closeProgress));

        // Category tabs on left nav
        float tabY = navY + 4.0f;
        for (CategoryPanel categoryPanel : categoryPanels) {
            boolean isFocused = focusedPanel == categoryPanel;
            Category cat = categoryPanel.getCategory();
            float tabHeight = 28.0f;

            // Focus indicator
            if (isFocused) {
                RenderUtil.drawRoundedRect(guiGraphics.pose(), navX + 2.0f, tabY, 3.0f, tabHeight,
                    NeverloseTheme.RADIUS_SM, ColorUtil.withAlpha(NeverloseTheme.ACCENT, closeProgress));
            }

            // Tab hover effect
            boolean tabHovered = RenderUtil.isHovered(navX, tabY, navWidth, tabHeight, mouseX, mouseY);
            if (tabHovered) {
                RenderUtil.drawFilledRect(guiGraphics.pose(), navX + 6.0f, tabY, navWidth - 12.0f, tabHeight,
                    ColorUtil.withAlpha(NeverloseTheme.TEXT, 0.05f * closeProgress));
            }

            // Category name
            String displayName = cat.displayName;
            FontRenderer catFont = FontPresets.productSans(13.0f);
            float textWidth = GlHelper.getStringWidth(displayName, catFont);
            float textX = navX + 10.0f;
            float textY = tabY + (tabHeight - catFont.getFont().getFontHeight()) / 2.0f + 1.0f;
            int catTextColor = ColorUtil.withAlpha(isFocused ? NeverloseTheme.ACCENT : NeverloseTheme.TEXT_MUTED, closeProgress);
            catFont.getFont().drawStringWithShadow(guiGraphics.pose(), displayName, textX, textY, catTextColor);

            tabY += tabHeight + 2.0f;
        }

        // Render category panels on the right
        for (CategoryPanel categoryPanel : categoryPanels) {
            categoryPanel.render(this, guiGraphics, guiGraphics.pose(), mouseX, mouseY, closeProgress, partialTicks);
        }
    }

    public void onClose() {
        this.closing = true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check left-nav clicks first
        float navWidth = NAV_WIDTH;
        float navX = getNavX();
        float navY = 36.0f;
        float tabHeight = 28.0f;
        float tabY = navY + 4.0f;
        for (int i = 0; i < categoryPanels.size(); i++) {
            if (RenderUtil.isHovered(navX, tabY, navWidth, tabHeight, (int)mouseX, (int)mouseY)) {
                focusedPanel = categoryPanels.get(i);
                return true;
            }
            tabY += tabHeight + 2.0f;
        }

        for (CategoryPanel categoryPanel : categoryPanels) {
            if (!categoryPanel.mouseClicked(mouseX, mouseY, button)) continue;
            focusedPanel = categoryPanel;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (CategoryPanel categoryPanel : categoryPanels) {
            categoryPanel.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    public boolean isPauseScreen() {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        for (CategoryPanel categoryPanel : categoryPanels) {
            if (!categoryPanel.mouseScrolled(mouseX, mouseY, scrollDelta)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (CategoryPanel panel : categoryPanels) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        for (CategoryPanel panel : categoryPanels) {
            if (panel.charTyped(character, modifiers)) return true;
        }
        return super.charTyped(character, modifiers);
    }

    static {
        categoryPanels = new ArrayList<>();
        for (Category category : Category.values()) {
            categoryPanels.add(new CategoryPanel(category));
        }
    }
}
