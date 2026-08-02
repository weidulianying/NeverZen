package shit.zen.gui.neverloseGUI.layout;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ZenClient;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.animation.Animation;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.modules.Module;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;

/** Toolbar for config actions, module search, and client settings. */
public class TitleBar {
    public enum Action { NONE, SAVE, SELECT_CONFIG, SELECT_MODULE, SETTINGS, CONSUMED }

    private float saveX, selectorX, controlY, selectorW, searchX, settingsX;
    private boolean selectorOpen, showSearch, searchOpen;
    private int hoveredOption = -1;
    private int optionStart, visibleOptions, selectedOption = -1;
    private final List<Module> searchResults = new ArrayList<>();
    private final Animation selectorAnimation = new Animation();
    private float selectorProgress;
    private String searchQuery = "";
    private int hoveredSearchResult = -1, searchSelection = -1;
    private Module selectedModule;
    private float searchPopupX, searchPopupY, searchPopupW, searchPopupH;

    public void render(PoseStack ps, GuiGraphics g, float x, float y, float w,
                       int mx, int my, float alpha, String configName) {
        controlY = y + 11;
        selectorAnimation.animate(selectorOpen ? 1f : 0f);
        selectorProgress = selectorAnimation.update(Animation.SPEED_EXPAND);
        saveX = x + 14;
        selectorX = x + 50;
        settingsX = x + w - 36;
        showSearch = w >= 250;
        searchX = settingsX - 32;
        float rightEdge = (showSearch ? searchX : settingsX) - 10;
        selectorW = Math.max(84, Math.min(190, rightEdge - selectorX));

        drawIconButton(ps, "\uE161", saveX, controlY, mx, my, alpha);
        drawSelector(ps, configName == null || configName.isBlank() ? "default" : configName,
            selectorX, controlY, selectorW, 26, mx, my, alpha);
        if (showSearch) {
            if (searchOpen) Render2D.drawRoundRect(ps, searchX, controlY, 26, 26, 6f,
                Render2D.alpha(Colors.NAV_SELECTED, alpha));
            drawIconButton(ps, "\uE8B6", searchX, controlY, mx, my, alpha);
        }
        drawIconButton(ps, "\uE8B8", settingsX, controlY, mx, my, alpha);
        Render2D.drawRect(ps, x, y + 49, w, 1, Render2D.alpha(Colors.BORDER, alpha * 0.7f));
    }

    private void drawSelector(PoseStack ps, String text, float x, float y, float w, float h,
                              int mx, int my, float alpha) {
        boolean hover = Render2D.contains(x, y, w, h, mx, my);
        int background = Render2D.lerpColor(Colors.PANEL, Colors.CARD, selectorProgress);
        Render2D.drawRoundRect(ps, x, y, w, h, 6f,
            Render2D.alpha(background, alpha * (hover ? 1f : 0.82f)));
        GlHelper.drawText(fit(text, w - 32), x + 9, centerTextY(y, h, Typography.SMALL), Typography.SMALL,
            Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
        String arrow = selectorOpen ? "\uE5C7" : "\uE5C5";
        float arrowX = x + w - 8 - GlHelper.getStringWidth(arrow, Typography.ICON_SMALL);
        GlHelper.drawText(arrow, arrowX, centerTextY(y, h, Typography.ICON_SMALL),
            Typography.ICON_SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, alpha));
    }

    private void drawIconButton(PoseStack ps, String icon, float x, float y,
                                int mx, int my, float alpha) {
        boolean hover = Render2D.contains(x, y, 26, 26, mx, my);
        if (hover) Render2D.drawRoundRect(ps, x, y, 26, 26, 6f, Render2D.alpha(Colors.CARD, alpha));
        float iconX = x + (26f - GlHelper.getStringWidth(icon, Typography.ICON_SMALL)) / 2f;
        GlHelper.drawText(icon, iconX, centerTextY(y, 26f, Typography.ICON_SMALL), Typography.ICON_SMALL,
            Render2D.alpha(hover ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY, alpha));
    }

    public void renderOverlay(PoseStack ps, GuiGraphics g, int mx, int my, float alpha,
                              String[] configs, int selectedIndex) {
        if (selectorProgress > 0.01f && configs.length > 0) {
            float popY = controlY + 30;
            float itemH = 24;
            int maxVisible = Math.max(3, (int) ((g.guiHeight() - popY - 10) / itemH));
            visibleOptions = Math.min(10, Math.min(configs.length, maxVisible));
            optionStart = Math.max(0, Math.min(optionStart, configs.length - visibleOptions));
            float popH = visibleOptions * itemH + 8;
            float transition = Animation.easeOutCubic(selectorProgress);
            hoveredOption = -1;
            ps.pushPose();
            ps.translate(0f, popY, 0f);
            ps.scale(1f, transition, 1f);
            ps.translate(0f, -popY, 0f);
            Render2D.drawShadow(ps, selectorX, popY, selectorW, popH, 9f, 10f,
                Render2D.alpha(0xFF000000, alpha * transition * 0.55f));
            Render2D.drawRoundRect(ps, selectorX, popY, selectorW, popH, 9f,
                Render2D.alpha(Colors.CARD, alpha * transition));
            for (int slot = 0; slot < visibleOptions; slot++) {
                int index = optionStart + slot;
                float optionY = popY + 4 + slot * itemH;
                boolean hover = Render2D.contains(selectorX + 4, optionY, selectorW - 8, itemH, mx, my);
                if (hover) hoveredOption = index;
                if (hover) Render2D.drawRoundRect(ps, selectorX + 4, optionY, selectorW - 8, itemH, 5,
                    Render2D.alpha(Colors.NAV_SELECTED, alpha * transition));
                int color = index == selectedIndex ? Colors.ACCENT : Colors.TEXT_SECONDARY;
                GlHelper.drawText(fit(configs[index], selectorW - 20), selectorX + 10, optionY + 7,
                    Typography.SMALL, Render2D.alpha(hover ? Colors.TEXT_PRIMARY : color, alpha * transition));
            }
            ps.popPose();
        }
        if (searchOpen) renderSearchOverlay(ps, mx, my, alpha);
    }

    private void renderSearchOverlay(PoseStack ps, int mx, int my, float alpha) {
        int visibleResults = Math.min(6, searchResults.size());
        boolean showEmpty = !searchQuery.isEmpty() && visibleResults == 0;
        searchPopupW = 210;
        searchPopupX = searchX + 26 - searchPopupW;
        searchPopupY = controlY + 30;
        searchPopupH = 36 + (showEmpty ? 26 : visibleResults * 26);
        Render2D.drawShadow(ps, searchPopupX, searchPopupY, searchPopupW, searchPopupH, 9f, 10f,
            Render2D.alpha(0xFF000000, alpha * 0.55f));
        Render2D.drawRoundRect(ps, searchPopupX, searchPopupY, searchPopupW, searchPopupH, 9f,
            Render2D.alpha(Colors.CARD, alpha));
        Render2D.drawRoundRect(ps, searchPopupX + 5, searchPopupY + 5, searchPopupW - 10, 26, 6f,
            Render2D.alpha(Colors.INPUT_BG, alpha));
        GlHelper.drawText("\uE8B6", searchPopupX + 12,
            centerTextY(searchPopupY + 5, 26, Typography.ICON_SMALL), Typography.ICON_SMALL,
            Render2D.alpha(Colors.TEXT_SECONDARY, alpha));
        String input = searchQuery.isEmpty() ? "Search functions" : searchQuery;
        int inputColor = searchQuery.isEmpty() ? Colors.TEXT_DISABLED : Colors.TEXT_PRIMARY;
        GlHelper.drawText(fit(input, searchPopupW - 48), searchPopupX + 35,
            centerTextY(searchPopupY + 5, 26, Typography.SMALL), Typography.SMALL,
            Render2D.alpha(inputColor, alpha));
        if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
            float cursorX = searchPopupX + 35 + GlHelper.getStringWidth(fit(searchQuery, searchPopupW - 48), Typography.SMALL);
            Render2D.drawRect(ps, cursorX + 1, searchPopupY + 11, 1, 14,
                Render2D.alpha(Colors.ACCENT, alpha));
        }

        hoveredSearchResult = -1;
        if (showEmpty) {
            GlHelper.drawText("No functions found", searchPopupX + 11, searchPopupY + 44,
                Typography.SMALL, Render2D.alpha(Colors.TEXT_DISABLED, alpha));
            return;
        }
        for (int i = 0; i < visibleResults; i++) {
            float resultY = searchPopupY + 36 + i * 26;
            boolean hover = Render2D.contains(searchPopupX + 5, resultY, searchPopupW - 10, 26, mx, my);
            if (hover) hoveredSearchResult = i;
            boolean selected = hover || i == searchSelection;
            if (selected) Render2D.drawRoundRect(ps, searchPopupX + 5, resultY, searchPopupW - 10, 26, 5f,
                Render2D.alpha(Colors.NAV_SELECTED, alpha));
            GlHelper.drawText(fit(searchResults.get(i).getName(), searchPopupW - 30),
                searchPopupX + 12, resultY + 8, Typography.SMALL,
                Render2D.alpha(selected ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY, alpha));
        }
    }

    public Action mouseClicked(double mx, double my, int btn, String[] configs, int currentIndex) {
        if (btn != 0) return Action.NONE;
        if (showSearch && Render2D.contains(searchX, controlY, 26, 26, (float) mx, (float) my)) {
            searchOpen = !searchOpen;
            selectorOpen = false;
            if (searchOpen) refreshSearchResults();
            return Action.CONSUMED;
        }
        if (searchOpen) {
            if (hoveredSearchResult >= 0 && hoveredSearchResult < searchResults.size()) {
                selectedModule = searchResults.get(hoveredSearchResult);
                searchOpen = false;
                return Action.SELECT_MODULE;
            }
            if (Render2D.contains(searchPopupX, searchPopupY, searchPopupW, searchPopupH,
                    (float) mx, (float) my)) return Action.CONSUMED;
            searchOpen = false;
        }
        if (selectorOpen) {
            if (hoveredOption >= 0) {
                selectedOption = hoveredOption;
                selectorOpen = false;
                return Action.SELECT_CONFIG;
            }
            if (Render2D.contains(selectorX, controlY, selectorW, 26, (float) mx, (float) my)) {
                selectorOpen = false;
                return Action.CONSUMED;
            }
            selectorOpen = false;
        }
        if (Render2D.contains(saveX, controlY, 26, 26, (float) mx, (float) my)) return Action.SAVE;
        if (Render2D.contains(selectorX, controlY, selectorW, 26, (float) mx, (float) my)) {
            selectorOpen = configs.length > 0;
            searchOpen = false;
            optionStart = Math.max(0, currentIndex - 4);
            return Action.CONSUMED;
        }
        if (Render2D.contains(settingsX, controlY, 26, 26, (float) mx, (float) my)) return Action.SETTINGS;
        return Action.NONE;
    }

    public int consumeSelectedOption() {
        int result = selectedOption;
        selectedOption = -1;
        return result;
    }

    public Module consumeSelectedModule() {
        Module result = selectedModule;
        selectedModule = null;
        return result;
    }

    public boolean mouseScrolled(double delta, int optionCount) {
        if (!selectorOpen || visibleOptions <= 0) return false;
        optionStart = Math.max(0, Math.min(Math.max(0, optionCount - visibleOptions),
            optionStart - (int) Math.signum(delta)));
        return true;
    }

    public boolean keyPressed(int key) {
        if (searchOpen) {
            if (key == 256) { searchOpen = false; return true; }
            if (key == 259) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    refreshSearchResults();
                }
                return true;
            }
            if (key == 264 && !searchResults.isEmpty()) {
                searchSelection = Math.min(Math.min(5, searchResults.size() - 1), searchSelection + 1);
                return true;
            }
            if (key == 265 && !searchResults.isEmpty()) {
                searchSelection = Math.max(0, searchSelection - 1);
                return true;
            }
            if ((key == 257 || key == 335) && searchSelection >= 0 && searchSelection < searchResults.size()) {
                selectedModule = searchResults.get(searchSelection);
                searchOpen = false;
                return true;
            }
            return true;
        }
        if (key == 256 && selectorOpen) { selectorOpen = false; return true; }
        return false;
    }

    public boolean charTyped(char c) {
        if (!searchOpen) return false;
        if (c >= 32 && c < 127 && searchQuery.length() < 40) {
            searchQuery += c;
            refreshSearchResults();
        }
        return true;
    }

    public void closeDropdown() { selectorOpen = false; searchOpen = false; }

    private void refreshSearchResults() {
        searchResults.clear();
        String needle = searchQuery.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty() || !ZenClient.isReady()) {
            searchSelection = -1;
            return;
        }
        for (Module module : ZenClient.getInstance().getModuleManager().getModules()) {
            if (module.getName().toLowerCase(Locale.ROOT).contains(needle)) searchResults.add(module);
        }
        searchResults.sort(Comparator
            .comparing((Module module) -> !module.getName().toLowerCase(Locale.ROOT).startsWith(needle))
            .thenComparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        searchSelection = searchResults.isEmpty() ? -1 : 0;
    }

    private static float centerTextY(float y, float height, FontRenderer font) {
        return y + (height - font.getMetrics().capHeight()) / 2f;
    }

    private static String fit(String value, float maxWidth) {
        if (value == null || maxWidth <= 4) return "";
        if (GlHelper.getStringWidth(value, Typography.SMALL) <= maxWidth) return value;
        String out = value;
        while (out.length() > 1 && GlHelper.getStringWidth(out + "...", Typography.SMALL) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "...";
    }
}
