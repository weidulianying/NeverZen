package shit.zen.gui.panel;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.ZenClient;
import shit.zen.manager.ConfigManager;
import shit.zen.render.DrawContext;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.Rectangle;
import shit.zen.render.Renderer;
import shit.zen.render.TextGlow;
import shit.zen.utils.math.LerpUtil;
import shit.zen.utils.render.RenderUtil;

public class SettingsPopup
extends ClientBase {
    private boolean isOpen = false;
    private boolean isDragging = false;
    private int lastDragX = 0;
    private int lastDragY = 0;
    private int offsetX = 0;
    private int offsetY = 0;
    private float openAlpha = 0.0f;
    private float closeButtonHoverAlpha = 0.0f;
    private boolean isCloseButtonHovered = false;
    private final Map<String, Boolean> dropdownOpen = new HashMap<>();
    private final Map<String, Float> dropdownAlpha = new HashMap<>();
    private final Map<String, Map<String, Float>> dropdownItemHover = new HashMap<>();
    private static final String[] LANGUAGES = new String[]{"English", "Chinese"};
    private String selectedLanguage = "English";
    private static final String[] SCALES = new String[]{"50%", "75%", "100%", "125%", "150%"};
    private String selectedScale = "100%";
    private static final Color POPUP_BG_COLOR = new Color(20, 20, 24, 230);
    private final Consumer<Float> scaleChangeCallback;

    // Profile management fields
    private String profileInput = "";
    private boolean profileInputFocused = false;
    private long profileInputCursorTime = 0;
    private boolean profileSaveHovered = false;
    private float profileSaveHoverAlpha = 0.0f;
    private boolean profileDeleteHovered = false;
    private float profileDeleteHoverAlpha = 0.0f;
    private boolean showProfileInput = false;

    public SettingsPopup(Consumer<Float> scaleChangeCallback) {
        this.scaleChangeCallback = scaleChangeCallback;
        this.dropdownOpen.put("language", false);
        this.dropdownOpen.put("scale", false);
        this.dropdownAlpha.put("language", 0.0f);
        this.dropdownAlpha.put("scale", 0.0f);
        this.dropdownItemHover.put("language", new HashMap<>());
        this.dropdownItemHover.put("scale", new HashMap<>());
        // Profile dropdown
        this.dropdownOpen.put("profile", false);
        this.dropdownAlpha.put("profile", 0.0f);
        this.dropdownItemHover.put("profile", new HashMap<>());
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float scale, float alpha) {
        this.updatePopupPosition(mouseX, mouseY, scale);
        this.updateOpenAlpha();
        this.updateCloseButtonHover();
        this.updateDropdownAlpha();
        if (this.openAlpha > 0.01f) {
            this.clampPopupPosition(scale);
            this.renderPopupContent(guiGraphics, mouseX, mouseY, scale, alpha);
        }
    }

    private void renderPopupContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float scale, float alpha) {
        int popupWidth = (int)(220.0f * scale);
        int popupHeight = this.calculatePopupHeight(scale);
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int popupX = (screenWidth - popupWidth) / 2 + this.offsetX;
        int popupY = (screenHeight - (int)(200.0f * scale)) / 2 + this.offsetY;
        float effectiveAlpha = this.openAlpha * alpha;
        int alphaByte = (int)(255.0f * effectiveAlpha);
        TextGlow.drawBackground(guiGraphics.pose(), popupX, popupY, popupWidth, popupHeight, 12.0f * scale, effectiveAlpha);
        Renderer.renderConsumer((drawContext -> this.drawPopupBody(drawContext, guiGraphics, popupX, popupY, mouseX, mouseY, popupHeight, alphaByte, scale, popupWidth)));
    }

    private void drawPopupBody(DrawContext drawContext, GuiGraphics guiGraphics, int popupX, int popupY, int mouseX, int mouseY, int popupHeight, int alphaByte, float scale, int popupWidth) {
        int whiteColor = alphaByte << 24 | 0xFFFFFF;
        FontRenderer iconFont = FontPresets.materialIcons(18.0f * scale);
        GlHelper.drawText("", (float)popupX + 15.0f * scale, (float)popupY + 16.0f * scale, iconFont, whiteColor);
        FontRenderer titleFont = FontPresets.museoSans(22.0f * scale);
        String title = "ZENLESS.ZONE";
        float titleWidth = GlHelper.getStringWidth(title, titleFont);
        GlHelper.drawText(title, (float)popupX + ((float)popupWidth - titleWidth) / 2.0f, (float)popupY + 35.0f * scale, titleFont, whiteColor);
        this.drawCloseButton(popupX, popupY, iconFont, alphaByte, scale, popupWidth);
        FontRenderer labelFont = FontPresets.axiformaRegular(13.0f * scale);
        FontRenderer valueFont = FontPresets.axiformaRegular(13.0f * scale);
        int labelColor = alphaByte << 24 | 0xAAAAAA;
        int valueColor = alphaByte << 24 | 0xFFFFFF;
        int rowHeight = (int)(18.0f * scale);
        int rowY = (int)((float)popupY + 65.0f * scale);
        int rightEdge = (int)((float)(popupX + popupWidth) - 15.0f * scale);
        GlHelper.drawText("Username:", (float)popupX + 15.0f * scale, rowY, labelFont, labelColor);
        String userId = this.getUserId();
        float userIdWidth = GlHelper.getStringWidth(userId, valueFont);
        GlHelper.drawText(userId, (float)rightEdge - userIdWidth, rowY, valueFont, valueColor);
        GlHelper.drawText("Branch:", (float)popupX + 15.0f * scale, rowY += rowHeight, labelFont, labelColor);
        String userRole = this.getUserRole();
        float roleWidth = GlHelper.getStringWidth(userRole, valueFont);
        GlHelper.drawText(userRole, (float)rightEdge - roleWidth, rowY, valueFont, valueColor);
        GlHelper.drawText("Updated:", (float)popupX + 15.0f * scale, rowY += rowHeight, labelFont, labelColor);
        String updatedDate = "Aug 4 2025";
        float dateWidth = GlHelper.getStringWidth(updatedDate, valueFont);
        GlHelper.drawText(updatedDate, (float)rightEdge - dateWidth, rowY, valueFont, valueColor);
        rowY += rowHeight;
        rowY = (int)((float)rowY + 8.0f * scale);
        rowY += this.drawDropdown(drawContext, guiGraphics, "Language", this.selectedLanguage, LANGUAGES, "language", popupX, rowY, mouseX, mouseY, this.openAlpha, scale, popupWidth);
        rowY = (int)((float)rowY + 8.0f * scale);
        this.drawDropdown(drawContext, guiGraphics, "Menu Scale", this.selectedScale, SCALES, "scale", popupX, rowY, mouseX, mouseY, this.openAlpha, scale, popupWidth);

        // --- Profile section ---
        rowY = (int)((float)rowY + 12.0f * scale);
        this.drawProfileSection(drawContext, guiGraphics, popupX, rowY, mouseX, mouseY, scale, popupWidth, alphaByte);

        FontRenderer footerFont = FontPresets.axiformaRegular(12.0f * scale);
        String footer = "7unknown © 2024-2025";
        float footerWidth = GlHelper.getStringWidth(footer, footerFont);
        GlHelper.drawText(footer, (float)popupX + ((float)popupWidth - footerWidth) / 2.0f, (float)(popupY + popupHeight) - 15.0f * scale, footerFont, labelColor);
    }

    private void drawProfileSection(DrawContext drawContext, GuiGraphics guiGraphics, int popupX, int rowY, int mouseX, int mouseY, float scale, int popupWidth, int alphaByte) {
        FontRenderer labelFont = FontPresets.axiformaRegular(13.0f * scale);
        FontRenderer valueFont = FontPresets.axiformaRegular(12.0f * scale);
        int labelColor = alphaByte << 24 | 0xAAAAAA;
        int valueColor = alphaByte << 24 | 0xFFFFFF;

        // Separator line
        int sepY = rowY;
        float sepAlpha = this.openAlpha;
        RenderUtil.drawFilledRect(guiGraphics.pose(), (float)popupX + 15.0f * scale, sepY, (float)popupWidth - 30.0f * scale, 1.0f * scale,
                this.applyAlpha(new Color(0x444444).getRGB(), sepAlpha));

        int contentY = (int)(sepY + 10.0f * scale);

        // "Profile:" label
        GlHelper.drawText("Profile:", (float)popupX + 15.0f * scale, contentY, labelFont, labelColor);

        // Profile load dropdown
        int dropdownWidth = (int)(90.0f * scale);
        int dropdownX = (int)((float)(popupX + popupWidth - dropdownWidth) - 15.0f * scale);
        int headerHeight = (int)(20.0f * scale);
        int dropdownY = contentY - (int)(3.0f * scale);

        ConfigManager configManager = ZenClient.getInstance().getConfigManager();
        String activeProfile = configManager.getActiveProfile();
        String displayName = activeProfile != null ? activeProfile : "Default";

        // Build profile items
        List<String> profileNames = configManager.listProfiles();
        String[] profileItems = new String[profileNames.size() + 1];
        for (int i = 0; i < profileNames.size(); i++) {
            profileItems[i] = profileNames.get(i);
        }
        profileItems[profileItems.length - 1] = "[Save As New...]";

        float openFactor = this.dropdownAlpha.getOrDefault("profile", 0.0f).floatValue();
        int expandedHeight = (int)((float)(profileItems.length * (int)(18.0f * scale)) * openFactor);

        RenderUtil.drawRoundedRect(guiGraphics.pose(), dropdownX, dropdownY, dropdownWidth, headerHeight + expandedHeight,
                4.0f * scale, this.applyAlpha(POPUP_BG_COLOR.getRGB(), this.openAlpha));

        // Selected value text
        float valueX = (float)dropdownX + 8.0f * scale;
        float valueY = dropdownY + (headerHeight - valueFont.getMetrics().capHeight()) / 2.0f;
        GlHelper.drawText(displayName, valueX, valueY, valueFont, valueColor);

        // Arrow
        FontRenderer arrowFont = FontPresets.materialIcons(18.0f * scale);
        String arrowIcon = "";
        float arrowX = (float)(dropdownX + dropdownWidth) - 18.0f * scale;
        float arrowY = dropdownY + (headerHeight - arrowFont.getMetrics().capHeight()) / 2.0f;
        GlHelper.drawText(arrowIcon, arrowX, arrowY, arrowFont, valueColor);

        // Expanded items
        if (openFactor > 0.01f) {
            drawContext.save();
            drawContext.clip(Rectangle.ofXYWH(dropdownX, dropdownY + headerHeight, dropdownWidth, expandedHeight));
            Map<String, Float> itemHovers = this.dropdownItemHover.get("profile");
            int itemHeight = (int)(18.0f * scale);
            int itemY = dropdownY + headerHeight;
            for (String item : profileItems) {
                boolean hovered = this.isPointInBounds(mouseX, mouseY, dropdownX, itemY, dropdownWidth, itemHeight);
                this.updateItemHover(itemHovers, item, hovered);
                float hoverAmount = itemHovers.getOrDefault(item, 0.0f);
                float itemTextX = (float)dropdownX + 8.0f * scale;
                float itemTextY = itemY + (itemHeight - valueFont.getMetrics().capHeight()) / 2.0f;
                int itemColor = this.applyAlpha(valueColor, openFactor);
                float glowAmount = hoverAmount * openFactor;
                if (glowAmount > 0.01f) {
                    int glowColor = new Color(1.0f, 1.0f, 1.0f, glowAmount).getRGB();
                    TextGlow.drawGlowText(item, itemTextX, itemTextY, valueFont, itemColor, glowColor, 8.0f * scale);
                } else {
                    GlHelper.drawText(item, itemTextX, itemTextY, valueFont, itemColor);
                }
                itemY += itemHeight;
            }
            drawContext.restore();
        }

        // Delete button (only when an active profile exists)
        if (activeProfile != null) {
            float deleteBtnX = (float)dropdownX - 18.0f * scale;
            float deleteBtnY = dropdownY + (headerHeight - 14.0f * scale) / 2.0f;
            FontRenderer deleteFont = FontPresets.materialIcons(14.0f * scale);
            boolean deleteHovered = (float)mouseX >= deleteBtnX - 4.0f * scale && (float)mouseX <= deleteBtnX + 14.0f * scale
                    && (float)mouseY >= deleteBtnY - 2.0f * scale && (float)mouseY <= deleteBtnY + 14.0f * scale;
            this.profileDeleteHovered = deleteHovered;
            this.profileDeleteHoverAlpha = deleteHovered
                    ? LerpUtil.lerp(this.profileDeleteHoverAlpha, 1.0f, 0.16f)
                    : LerpUtil.lerp(this.profileDeleteHoverAlpha, 0.0f, 0.16f);
            int deleteColor = alphaByte << 24 | 0xFF5555;
            float deleteGlowAlpha = 180.0f * this.profileDeleteHoverAlpha * this.openAlpha;
            int deleteGlowColor = new Color(1.0f, 0.33f, 0.33f, deleteGlowAlpha / 255.0f).getRGB();
            TextGlow.drawGlowText("", deleteBtnX, deleteBtnY, deleteFont, deleteColor, deleteGlowColor, 6.0f * scale);
        }

        // Text input + Save button (when showProfileInput is true)
        if (this.showProfileInput) {
            int inputY = dropdownY + headerHeight + expandedHeight + (int)(5.0f * scale);
            int inputWidth = (int)(120.0f * scale);
            int inputHeight = (int)(20.0f * scale);
            int inputX = (int)((float)popupX + 15.0f * scale);

            // Input background
            RenderUtil.drawRoundedRect(guiGraphics.pose(), inputX, inputY, inputWidth, inputHeight,
                    4.0f * scale, this.applyAlpha(POPUP_BG_COLOR.getRGB(), this.openAlpha));

            // Input text or placeholder
            if (this.profileInput.isEmpty() && !this.profileInputFocused) {
                FontRenderer placeholderFont = FontPresets.axiformaRegular(12.0f * scale);
                int placeholderColor = alphaByte << 24 | 0x666666;
                float phY = inputY + (inputHeight - placeholderFont.getMetrics().capHeight()) / 2.0f;
                GlHelper.drawText("Profile name...", (float)inputX + 6.0f * scale, phY, placeholderFont, placeholderColor);
            } else {
                float textY = inputY + (inputHeight - valueFont.getMetrics().capHeight()) / 2.0f;
                GlHelper.drawText(this.profileInput, (float)inputX + 6.0f * scale, textY, valueFont, valueColor);
                // Cursor
                if (this.profileInputFocused) {
                    long sinceCursor = System.currentTimeMillis() - this.profileInputCursorTime;
                    float blinkAmount = (float)(Math.sin((double)sinceCursor / 200.0) * 0.5 + 0.5);
                    int cursorColor = (int)(blinkAmount * 255.0f) << 24 | 0xFFFFFF;
                    float cursorX = (float)inputX + 6.0f * scale + GlHelper.getStringWidth(this.profileInput, valueFont) + 1.0f * scale;
                    float cursorHeight = valueFont.getMetrics().capHeight();
                    RenderUtil.drawFilledRect(guiGraphics.pose(), cursorX, textY - cursorHeight + 1.0f * scale,
                            1.0f * scale, cursorHeight + 2.0f * scale, cursorColor);
                }
            }

            // Save button
            int saveBtnWidth = (int)(40.0f * scale);
            int saveBtnX = inputX + inputWidth + (int)(6.0f * scale);
            int saveBtnY = inputY;
            boolean saveHovered = (float)mouseX >= (float)saveBtnX && (float)mouseX <= (float)(saveBtnX + saveBtnWidth)
                    && (float)mouseY >= (float)saveBtnY && (float)mouseY <= (float)(saveBtnY + inputHeight);
            this.profileSaveHovered = saveHovered;
            this.profileSaveHoverAlpha = saveHovered
                    ? LerpUtil.lerp(this.profileSaveHoverAlpha, 1.0f, 0.16f)
                    : LerpUtil.lerp(this.profileSaveHoverAlpha, 0.0f, 0.16f);

            int saveBgColor = this.applyAlpha(new Color(60, 60, 70).getRGB(), this.openAlpha);
            RenderUtil.drawRoundedRect(guiGraphics.pose(), saveBtnX, saveBtnY, saveBtnWidth, inputHeight,
                    4.0f * scale, saveBgColor);

            FontRenderer saveFont = FontPresets.axiformaBold(12.0f * scale);
            String saveText = "Save";
            float saveTextWidth = GlHelper.getStringWidth(saveText, saveFont);
            float saveTextX = (float)saveBtnX + ((float)saveBtnWidth - saveTextWidth) / 2.0f;
            float saveTextY = saveBtnY + (inputHeight - saveFont.getMetrics().capHeight()) / 2.0f;
            int saveTextColor = this.applyAlpha(valueColor, this.openAlpha);
            if (this.profileSaveHoverAlpha > 0.01f) {
                int saveGlowColor = new Color(1.0f, 1.0f, 1.0f, 120.0f * this.profileSaveHoverAlpha * this.openAlpha / 255.0f).getRGB();
                TextGlow.drawGlowText(saveText, saveTextX, saveTextY, saveFont, saveTextColor, saveGlowColor, 6.0f * scale);
            } else {
                GlHelper.drawText(saveText, saveTextX, saveTextY, saveFont, saveTextColor);
            }
        }
    }

    private void drawCloseButton(int popupX, int popupY, FontRenderer iconFont, int alphaByte, float scale, int popupWidth) {
        float btnX = (float)(popupX + popupWidth) - 25.0f * scale;
        float btnY = (float)popupY + 16.0f * scale;
        Color colorFrom = new Color(255, 255, 255);
        Color colorTo = new Color(255, 255, 255);
        int r = (int)((float)colorFrom.getRed() + (float)(colorTo.getRed() - colorFrom.getRed()) * this.closeButtonHoverAlpha);
        int g = (int)((float)colorFrom.getGreen() + (float)(colorTo.getGreen() - colorFrom.getGreen()) * this.closeButtonHoverAlpha);
        int b = (int)((float)colorFrom.getBlue() + (float)(colorTo.getBlue() - colorFrom.getBlue()) * this.closeButtonHoverAlpha);
        int textColor = alphaByte << 24 | r << 16 | g << 8 | b;
        int glowAlpha = (int)(180.0f * this.closeButtonHoverAlpha * this.openAlpha);
        int glowColor = new Color(r, g, b, glowAlpha).getRGB();
        TextGlow.drawGlowText("", btnX, btnY, iconFont, textColor, glowColor, 10.0f * scale);
    }

    private int drawDropdown(DrawContext drawContext, GuiGraphics guiGraphics, String label, String selectedValue, String[] items, String key, int popupX, int rowY, int mouseX, int mouseY, float openAlpha, float scale, int popupWidth) {
        FontRenderer labelFont = FontPresets.axiformaRegular(13.0f * scale);
        FontRenderer valueFont = FontPresets.axiformaRegular(13.0f * scale);
        int labelColor = this.applyAlpha(new Color(0xAAAAAA).getRGB(), openAlpha);
        int valueColor = this.applyAlpha(new Color(0xFFFFFF).getRGB(), openAlpha);

        int dropdownWidth = (int)(90.0f * scale);
        int dropdownX = (int)((float)(popupX + popupWidth - dropdownWidth) - 15.0f * scale);
        int dropdownHeaderHeight = (int)(20.0f * scale);
        int itemHeight = (int)(18.0f * scale);

        float labelY = rowY + (dropdownHeaderHeight - labelFont.getMetrics().capHeight()) / 2.0f;
        GlHelper.drawText(label, (float)popupX + 15.0f * scale, labelY, labelFont, labelColor);

        float openFactor = this.dropdownAlpha.getOrDefault(key, 0.0f).floatValue();
        String[] filteredItems = this.filterDropdownItems(items, selectedValue);
        int expandedHeight = (int)((float)(filteredItems.length * itemHeight) * openFactor);
        RenderUtil.drawRoundedRect(guiGraphics.pose(), dropdownX, rowY, dropdownWidth, dropdownHeaderHeight + expandedHeight, 4.0f * scale, this.applyAlpha(POPUP_BG_COLOR.getRGB(), openAlpha));

        float valueX = (float)dropdownX + 8.0f * scale;
        float valueY = rowY + (dropdownHeaderHeight - valueFont.getMetrics().capHeight()) / 2.0f;
        GlHelper.drawText(selectedValue, valueX, valueY, valueFont, valueColor);

        FontRenderer arrowFont = FontPresets.materialIcons(18.0f * scale);
        String arrowIcon = "";
        float arrowX = (float)(dropdownX + dropdownWidth) - 18.0f * scale;
        float arrowY = rowY + (dropdownHeaderHeight - arrowFont.getMetrics().capHeight()) / 2.0f;
        GlHelper.drawText(arrowIcon, arrowX, arrowY, arrowFont, valueColor);

        if (openFactor > 0.01f) {
            drawContext.save();
            drawContext.clip(Rectangle.ofXYWH(dropdownX, rowY + dropdownHeaderHeight, dropdownWidth, expandedHeight));
            Map<String, Float> itemHovers = this.dropdownItemHover.get(key);
            int itemY = rowY + dropdownHeaderHeight;
            for (String item : filteredItems) {
                boolean hovered = this.isPointInBounds(mouseX, mouseY, dropdownX, itemY, dropdownWidth, itemHeight);
                this.updateItemHover(itemHovers, item, hovered);
                float hoverAmount = itemHovers.getOrDefault(item, 0.0f);
                float itemTextX = (float)dropdownX + 8.0f * scale;
                float itemTextY = itemY + (itemHeight - valueFont.getMetrics().capHeight()) / 2.0f;
                int itemColor = this.applyAlpha(valueColor, openFactor);
                float glowAmount = hoverAmount * openFactor;
                if (glowAmount > 0.01f) {
                    int glowColor = new Color(1.0f, 1.0f, 1.0f, glowAmount).getRGB();
                    TextGlow.drawGlowText(item, itemTextX, itemTextY, valueFont, itemColor, glowColor, 8.0f * scale);
                } else {
                    GlHelper.drawText(item, itemTextX, itemTextY, valueFont, itemColor);
                }
                itemY += itemHeight;
            }
            drawContext.restore();
        }
        return dropdownHeaderHeight + expandedHeight;
    }

    private String getUserId() {
        return ZenClient.username != null && !ZenClient.username.isEmpty() ? ZenClient.username : "Unknown";
    }

    private String getUserRole() {
        return "User";
    }

    public boolean onMouseClick(int mouseX, int mouseY, float scale) {
        int screenHeight;
        int popupY;
        int popupWidth = (int)(220.0f * scale);
        int popupHeight = this.calculatePopupHeight(scale);
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int popupX = (screenWidth - popupWidth) / 2 + this.offsetX;
        if (this.isMouseOverCloseButton(mouseX, mouseY, popupX, popupY = ((screenHeight = mc.getWindow().getGuiScaledHeight()) - (int)(200.0f * scale)) / 2 + this.offsetY, scale, popupWidth)) {
            this.toggleOpen();
            return true;
        }
        if (this.isDragging) {
            return true;
        }
        if (this.isMouseInRect(mouseX, mouseY, popupX, popupY, scale, popupWidth)) {
            this.beginDrag(mouseX, mouseY);
            return true;
        }

        // Language dropdown
        int dropdownWidth = (int)(90.0f * scale);
        int dropdownX = (int)((float)(popupX + popupWidth - dropdownWidth) - 15.0f * scale);
        int langRowY = (int)((float)popupY + 127.0f * scale);
        boolean langHandled = this.handleDropdownClick(mouseX, mouseY, dropdownX, langRowY, dropdownWidth, LANGUAGES, this.selectedLanguage, "language", value -> {
            this.selectedLanguage = value;
        }, scale);
        float langExpanded = (float)this.filterDropdownItems(LANGUAGES, this.selectedLanguage).length * (18.0f * scale) * this.dropdownAlpha.getOrDefault("language", 0.0f);
        int scaleRowY = (int)((float)langRowY + 20.0f * scale + langExpanded + 8.0f * scale);
        boolean scaleHandled = this.handleDropdownClick(mouseX, mouseY, dropdownX, scaleRowY, dropdownWidth, SCALES, this.selectedScale, "scale", value -> {
            this.selectedScale = value;
            try {
                float parsed = Float.parseFloat(value.replace("%", "")) / 100.0f;
                this.scaleChangeCallback.accept(parsed);
            } catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }, scale);

        // Profile section click handling
        float scaleExpanded = (float)this.filterDropdownItems(SCALES, this.selectedScale).length * (18.0f * scale) * this.dropdownAlpha.getOrDefault("scale", 0.0f);
        int profileSectionY = (int)((float)scaleRowY + 20.0f * scale + scaleExpanded + 12.0f * scale);
        boolean profileHandled = this.handleProfileSectionClick(mouseX, mouseY, popupX, profileSectionY, scale, popupWidth);

        boolean withinPopup = this.isPointInBounds(mouseX, mouseY, popupX, popupY, popupWidth, popupHeight);
        if (langHandled || scaleHandled || profileHandled) {
            return true;
        }
        if (withinPopup) {
            this.dropdownOpen.put("language", false);
            this.dropdownOpen.put("scale", false);
            this.dropdownOpen.put("profile", false);
            this.showProfileInput = false;
            this.profileInputFocused = false;
            return true;
        }
        return false;
    }

    private boolean handleProfileSectionClick(int mouseX, int mouseY, int popupX, int sectionY, float scale, int popupWidth) {
        ConfigManager configManager = ZenClient.getInstance().getConfigManager();
        int dropdownWidth = (int)(90.0f * scale);
        int dropdownX = (int)((float)(popupX + popupWidth - dropdownWidth) - 15.0f * scale);
        int headerHeight = (int)(20.0f * scale);
        int dropdownY = sectionY + (int)(10.0f * scale) - (int)(3.0f * scale); // after separator + label offset

        // Check delete button click (only when active profile exists)
        String activeProfile = configManager.getActiveProfile();
        if (activeProfile != null) {
            float deleteBtnX = (float)dropdownX - 18.0f * scale;
            float deleteBtnY = dropdownY + (headerHeight - 14.0f * scale) / 2.0f;
            if ((float)mouseX >= deleteBtnX - 4.0f * scale && (float)mouseX <= deleteBtnX + 14.0f * scale
                    && (float)mouseY >= deleteBtnY - 2.0f * scale && (float)mouseY <= deleteBtnY + 14.0f * scale) {
                configManager.deleteProfile(activeProfile);
                this.profileInput = "";
                this.showProfileInput = false;
                this.profileInputFocused = false;
                return true;
            }
        }

        // Check Save button click
        if (this.showProfileInput) {
            int itemHeight = (int)(18.0f * scale);
            List<String> profileNames = configManager.listProfiles();
            int numItems = profileNames.size() + 1;
            float openFactor = this.dropdownAlpha.getOrDefault("profile", 0.0f).floatValue();
            int expandedHeight = (int)((float)(numItems * itemHeight) * openFactor);

            int inputY = dropdownY + headerHeight + expandedHeight + (int)(5.0f * scale);
            int inputWidth = (int)(120.0f * scale);
            int inputHeight = (int)(20.0f * scale);
            int inputX = (int)((float)popupX + 15.0f * scale);
            int saveBtnWidth = (int)(40.0f * scale);
            int saveBtnX = inputX + inputWidth + (int)(6.0f * scale);

            if ((float)mouseX >= (float)inputX && (float)mouseX <= (float)(inputX + inputWidth)
                    && (float)mouseY >= (float)inputY && (float)mouseY <= (float)(inputY + inputHeight)) {
                this.profileInputFocused = true;
                this.profileInputCursorTime = System.currentTimeMillis();
                return true;
            }

            if ((float)mouseX >= (float)saveBtnX && (float)mouseX <= (float)(saveBtnX + saveBtnWidth)
                    && (float)mouseY >= (float)inputY && (float)mouseY <= (float)(inputY + inputHeight)) {
                this.doSaveProfile();
                return true;
            }
        }

        // Check profile dropdown click
        return this.handleProfileDropdownClick(mouseX, mouseY, dropdownX, dropdownY, dropdownWidth, scale);
    }

    private boolean handleProfileDropdownClick(int mouseX, int mouseY, int dropdownX, int dropdownY, int dropdownWidth, float scale) {
        ConfigManager configManager = ZenClient.getInstance().getConfigManager();
        boolean open = this.dropdownOpen.getOrDefault("profile", false);
        int itemHeight = (int)(18.0f * scale);
        int headerHeight = (int)(20.0f * scale);

        // Click on dropdown header
        if (this.isPointInBounds(mouseX, mouseY, dropdownX, dropdownY, dropdownWidth, headerHeight)) {
            this.dropdownOpen.put("profile", !open);
            // Close other dropdowns
            this.dropdownOpen.keySet().stream()
                    .filter(otherKey -> !otherKey.equals("profile"))
                    .forEach(otherKey -> this.dropdownOpen.put(otherKey, false));
            this.showProfileInput = false;
            this.profileInputFocused = false;
            return true;
        }

        // Click on dropdown items
        if (open) {
            List<String> profileNames = configManager.listProfiles();
            String[] profileItems = new String[profileNames.size() + 1];
            for (int i = 0; i < profileNames.size(); i++) {
                profileItems[i] = profileNames.get(i);
            }
            profileItems[profileItems.length - 1] = "[Save As New...]";

            for (int i = 0; i < profileItems.length; i++) {
                int itemY = dropdownY + headerHeight + i * itemHeight;
                if (this.isPointInBounds(mouseX, mouseY, dropdownX, itemY, dropdownWidth, itemHeight)) {
                    if (i == profileItems.length - 1) {
                        // "[Save As New...]" clicked
                        this.showProfileInput = true;
                        this.profileInput = "";
                        this.profileInputFocused = true;
                        this.profileInputCursorTime = System.currentTimeMillis();
                    } else {
                        // Profile clicked — load it
                        configManager.loadProfile(profileItems[i]);
                        this.profileInput = "";
                        this.showProfileInput = false;
                        this.profileInputFocused = false;
                    }
                    this.dropdownOpen.put("profile", false);
                    return true;
                }
            }
        }
        return false;
    }

    private void doSaveProfile() {
        if (this.profileInput.isEmpty()) return;
        ConfigManager configManager = ZenClient.getInstance().getConfigManager();
        configManager.saveProfile(this.profileInput.trim());
        this.profileInput = "";
        this.showProfileInput = false;
        this.profileInputFocused = false;
    }

    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!this.isOpen) return false;

        if (this.profileInputFocused) {
            if (keyCode == 256) { // Escape
                this.profileInputFocused = false;
                this.showProfileInput = false;
                this.profileInput = "";
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter / NumPad Enter
                this.doSaveProfile();
                return true;
            }
            if (keyCode == 259) { // Backspace
                if (!this.profileInput.isEmpty()) {
                    this.profileInput = this.profileInput.substring(0, this.profileInput.length() - 1);
                    this.profileInputCursorTime = System.currentTimeMillis();
                }
                return true;
            }
        }
        return false;
    }

    public boolean handleCharTyped(char c, int modifiers) {
        if (!this.isOpen || !this.profileInputFocused) return false;

        // Allow printable ASCII characters (space through ~) except delete (127)
        if (c >= 32 && c < 127) {
            // Limit profile name length
            if (this.profileInput.length() < 32) {
                this.profileInput += c;
                this.profileInputCursorTime = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    private boolean handleDropdownClick(int mouseX, int mouseY, int dropdownX, int dropdownY, int dropdownWidth, String[] items, String selectedValue, String key, Consumer<String> onSelect, float scale) {
        boolean open = this.dropdownOpen.getOrDefault(key, false);
        int itemHeight = (int)(18.0f * scale);
        int headerHeight = (int)(20.0f * scale);
        if (this.isPointInBounds(mouseX, mouseY, dropdownX, dropdownY, dropdownWidth, headerHeight)) {
            this.dropdownOpen.put(key, !open);
            this.dropdownOpen.keySet().stream().filter(otherKey -> !otherKey.equals(key)).forEach(otherKey -> this.dropdownOpen.put(otherKey, false));
            this.showProfileInput = false;
            this.profileInputFocused = false;
            return true;
        }
        if (open) {
            String[] filtered = this.filterDropdownItems(items, selectedValue);
            for (int i = 0; i < filtered.length; ++i) {
                if (!this.isPointInBounds(mouseX, mouseY, dropdownX, dropdownY + headerHeight + i * itemHeight, dropdownWidth, itemHeight)) continue;
                onSelect.accept(filtered[i]);
                this.dropdownOpen.put(key, false);
                return true;
            }
        }
        return false;
    }

    private boolean isMouseInRect(int mouseX, int mouseY, int popupX, int popupY, float scale, int popupWidth) {
        float closeBtnX = (float)(popupX + popupWidth) - 25.0f * scale;
        boolean overCloseBtn = (float)mouseX >= closeBtnX - 10.0f * scale && (float)mouseX <= closeBtnX + 15.0f * scale;
        return mouseX >= popupX && mouseX <= popupX + popupWidth && mouseY >= popupY && (float)mouseY <= (float)popupY + 30.0f * scale && !overCloseBtn;
    }

    private boolean isMouseOverCloseButton(int mouseX, int mouseY, int popupX, int popupY, float scale, int popupWidth) {
        float closeBtnX = (float)(popupX + popupWidth) - 25.0f * scale;
        float closeBtnY = (float)popupY + 16.0f * scale;
        return (float)mouseX >= closeBtnX - 10.0f * scale && (float)mouseX <= closeBtnX + 15.0f * scale && (float)mouseY >= closeBtnY - 10.0f * scale && (float)mouseY <= closeBtnY + 10.0f * scale;
    }

    private void beginDrag(int mouseX, int mouseY) {
        this.isDragging = true;
        this.lastDragX = mouseX;
        this.lastDragY = mouseY;
    }

    public void onMouseDrag(int mouseX, int mouseY) {
        if (this.isDragging) {
            this.offsetX += mouseX - this.lastDragX;
            this.offsetY += mouseY - this.lastDragY;
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;
        }
    }

    public void stopDrag() {
        this.isDragging = false;
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public void toggleOpen() {
        this.isOpen = !this.isOpen;
        if (!this.isOpen) {
            this.showProfileInput = false;
            this.profileInputFocused = false;
        }
    }

    private void updateOpenAlpha() {
        if (this.isOpen) {
            this.openAlpha = LerpUtil.lerp(this.openAlpha, 1.0f, 0.1f);
        } else {
            this.openAlpha = LerpUtil.lerp(this.openAlpha, 0.0f, 0.1f);
            if (this.openAlpha < 0.01f) {
                this.dropdownOpen.put("language", false);
                this.dropdownOpen.put("scale", false);
                this.dropdownOpen.put("profile", false);
            }
        }
    }

    private void updateDropdownAlpha() {
        for (String key : this.dropdownOpen.keySet()) {
            boolean open = this.dropdownOpen.getOrDefault(key, false);
            float current = this.dropdownAlpha.getOrDefault(key, 0.0f).floatValue();
            float target = open ? 1.0f : 0.0f;
            current = Math.abs(current - target) > 0.01f ? LerpUtil.smoothLerp(current, target, 0.22f) : target;
            this.dropdownAlpha.put(key, current);
        }
    }

    private void updatePopupPosition(int mouseX, int mouseY, float scale) {
        if (this.isOpen) {
            int popupWidth = (int)(220.0f * scale);
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            int popupX = (screenWidth - popupWidth) / 2 + this.offsetX;
            int popupY = (screenHeight - (int)(200.0f * scale)) / 2 + this.offsetY;
            this.isCloseButtonHovered = this.isMouseOverCloseButton(mouseX, mouseY, popupX, popupY, scale, popupWidth);
        } else {
            this.isCloseButtonHovered = false;
        }
    }

    private void updateCloseButtonHover() {
        this.closeButtonHoverAlpha = this.isCloseButtonHovered ? LerpUtil.lerp(this.closeButtonHoverAlpha, 1.0f, 0.16f) : LerpUtil.lerp(this.closeButtonHoverAlpha, 0.0f, 0.16f);
    }

    private void updateItemHover(Map<String, Float> hoverMap, String key, boolean hovered) {
        float current = hoverMap.getOrDefault(key, 0.0f).floatValue();
        float target = hovered ? 1.0f : 0.0f;
        current = Math.abs(current - target) > 0.01f ? LerpUtil.smoothLerp(current, target, 0.28f) : target;
        hoverMap.put(key, current);
    }

    private String[] filterDropdownItems(String[] items, String selectedValue) {
        return Stream.of((Object[])items).filter(item -> !Objects.equals(item, selectedValue)).toArray(String[]::new);
    }

    private boolean isPointInBounds(int pointX, int pointY, int boxX, int boxY, int boxWidth, int boxHeight) {
        return pointX >= boxX && pointX <= boxX + boxWidth && pointY >= boxY && pointY <= boxY + boxHeight;
    }

    private int applyAlpha(int color, float alpha) {
        int origAlpha = color >> 24 & 0xFF;
        int newAlpha = (int)((float)origAlpha * alpha);
        return newAlpha << 24 | color & 0xFFFFFF;
    }

    private int calculatePopupHeight(float scale) {
        float baseHeight = 200.0f * scale;
        float itemHeight = 18.0f * scale;
        float langExpanded = (float)this.filterDropdownItems(LANGUAGES, this.selectedLanguage).length * itemHeight * this.dropdownAlpha.getOrDefault("language", 0.0f).floatValue();
        float scaleExpanded = (float)this.filterDropdownItems(SCALES, this.selectedScale).length * itemHeight * this.dropdownAlpha.getOrDefault("scale", 0.0f).floatValue();

        // Profile section height
        float profileExpanded = 0.0f;
        ConfigManager configManager = ZenClient.getInstance().getConfigManager();
        if (configManager != null) {
            List<String> profileNames = configManager.listProfiles();
            int profileItemCount = profileNames.size() + 1; // +1 for "[Save As New...]"
            profileExpanded = (float)profileItemCount * itemHeight * this.dropdownAlpha.getOrDefault("profile", 0.0f).floatValue();
        }
        float profileSectionBase = 32.0f * scale; // separator + label + dropdown header + gaps
        float profileInputHeight = this.showProfileInput ? 30.0f * scale : 0.0f;

        return (int)(baseHeight + langExpanded + scaleExpanded + profileSectionBase + profileExpanded + profileInputHeight);
    }

    private void clampPopupPosition(float scale) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int popupHeight = this.calculatePopupHeight(scale);
        int popupWidth = (int)(220.0f * scale);
        int maxOffsetX = (screenWidth - popupWidth) / 2;
        int minOffsetX = -(screenWidth - popupWidth) / 2;
        int maxOffsetY = (screenHeight - popupHeight) / 2;
        int minOffsetY = -(screenHeight - (int)(200.0f * scale)) / 2;
        this.offsetX = Math.max(minOffsetX, Math.min(this.offsetX, maxOffsetX));
        this.offsetY = Math.max(minOffsetY, Math.min(this.offsetY, maxOffsetY));
    }
}