package shit.zen.gui.neverloseGUI.page;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.config.ConfigData;
import shit.zen.gui.neverloseGUI.card.ConfigCard;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Page;
import shit.zen.gui.neverloseGUI.model.ConfigViewModel;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;

/** Neverlose-inspired configs workspace: compact toolbar, grouped rows and exact hit targets. */
public class ConfigPage extends Page {
    private static final float TOOLBAR_H = 26f;
    private static final float GAP = 6f;
    private static final float SAVE_W = 68f;
    private static final float SORT_W = 116f;
    private static final float CREATE_W = 84f;
    private static final float SEARCH_W = 154f;
    private static final float ROW_GAP = 5f;

    private static final String ICON_SAVE = "\uE161";
    private static final String ICON_SORT = "\uE164";
    private static final String ICON_CREATE = "\uE2C7";
    private static final String ICON_SEARCH = "\uE8B6";
    private static final String ICON_ARROW = "\uE5C5";
    private static final String ICON_CONFIRM = "\uE5CA";
    private static final String ICON_CANCEL = "\uE5CD";

    private enum InputMode { NONE, CREATE, RENAME }

    private final ConfigViewModel vm = new ConfigViewModel();
    private final List<ConfigCard> cards = new ArrayList<>();
    private final List<Toast> toasts = new ArrayList<>();
    private String search = "";
    private String selectedConfig;
    private String renameSource;
    private String input = "";
    private boolean searchActive;
    private boolean sortByName;
    private boolean dirty = true;
    private InputMode inputMode = InputMode.NONE;
    private float scroll, maxScroll;

    private float toolbarY, saveX, sortX, createX, searchX;
    private float sectionY, listY, listH, inputX, inputY, inputW, confirmX, cancelX;
    private float saveW, sortW, createW;
    private boolean compactToolbar;

    private record Toast(String msg, long at) {}

    private void toast(String message) {
        toasts.add(new Toast(message, System.currentTimeMillis()));
    }

    private final ConfigCard.Callback callback = (action, name) -> {
        switch (action) {
            case "Load" -> {
                vm.load(name);
                selectedConfig = name;
                toast("Loaded '" + name + "'");
            }
            case "Save" -> {
                vm.save(name);
                selectedConfig = name;
                dirty = true;
                toast("Saved '" + name + "'");
            }
            case "Ren" -> beginRename(name);
            case "Dup" -> {
                vm.duplicate(name);
                dirty = true;
                toast("Duplicated '" + name + "'");
            }
            case "Del" -> {
                vm.delete(name);
                if (name.equals(selectedConfig)) selectedConfig = null;
                dirty = true;
                toast("Deleted '" + name + "'");
            }
            default -> { }
        }
    };

    @Override
    public void onShow() {
        super.onShow();
        dirty = true;
        search = "";
        searchActive = false;
        inputMode = InputMode.NONE;
        scroll = 0f;
    }

    @Override
    public void closePopups() {
        closeMenus(null);
        searchActive = false;
        inputMode = InputMode.NONE;
    }

    private void rebuild() {
        List<ConfigData> configs = new ArrayList<>(vm.search(search.trim()));
        if (sortByName) configs.sort(Comparator.comparing(ConfigData::getName, String.CASE_INSENSITIVE_ORDER));
        else configs.sort(Comparator.comparingLong(ConfigData::getCreateTime).reversed());

        cards.clear();
        for (ConfigData config : configs) cards.add(new ConfigCard(x, 0f, w, config, callback));
        if (selectedConfig == null && !configs.isEmpty()) selectedConfig = configs.get(0).getName();
        dirty = false;
    }

    private void updateLayout() {
        toolbarY = y;
        compactToolbar = w < 390f;
        saveW = compactToolbar ? TOOLBAR_H : SAVE_W;
        sortW = compactToolbar ? 100f : SORT_W;
        createW = compactToolbar ? TOOLBAR_H : CREATE_W;
        saveX = x;
        sortX = saveX + saveW + GAP;
        createX = sortX + sortW + GAP;
        searchX = x + w - TOOLBAR_H;
        sectionY = y + TOOLBAR_H + 18f;
        inputX = x;
        inputY = sectionY + 22f;
        inputW = w;
        cancelX = inputX + inputW - 24f;
        confirmX = cancelX - 24f;
        listY = sectionY + 28f + (inputMode == InputMode.NONE ? 0f : 34f);
        listH = Math.max(1f, y + h - listY);

        float contentH = cards.size() * (ConfigCard.HEIGHT + ROW_GAP) - (cards.isEmpty() ? 0f : ROW_GAP);
        maxScroll = Math.max(0f, contentH - listH);
        scroll = Math.max(0f, Math.min(maxScroll, scroll));
        float cardY = listY - scroll;
        for (ConfigCard card : cards) {
            card.setSize(w, ConfigCard.HEIGHT);
            card.setPos(x, cardY);
            card.setMenuBottom(y + h);
            cardY += ConfigCard.HEIGHT + ROW_GAP;
        }
    }

    @Override
    public void render(PoseStack ps, GuiGraphics g, int mx, int my, float alpha) {
        if (dirty) rebuild();
        updateLayout();
        drawToolbar(ps, mx, my, alpha);

        Render2D.drawRect(ps, x, sectionY + 8f, 10f, 1f, Render2D.alpha(Colors.TEXT_DISABLED, alpha));
        GlHelper.drawText("My Items", x + 17f, centerTextY(sectionY, 18f, Typography.BODY), Typography.BODY,
            Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
        GlHelper.drawText(Integer.toString(cards.size()), x + 17f + GlHelper.getStringWidth("My Items", Typography.BODY) + 8f,
            centerTextY(sectionY, 18f, Typography.TINY), Typography.TINY, Render2D.alpha(Colors.TEXT_DISABLED, alpha));
        Render2D.drawRect(ps, x, sectionY + 25f, w, 1f, Render2D.alpha(Colors.BORDER, alpha * 0.7f));

        if (inputMode != InputMode.NONE) drawNameInput(ps, mx, my, alpha);

        Render2D.pushScissor((int) x, (int) listY, (int) w, (int) listH);
        if (cards.isEmpty()) {
            String empty = search.isEmpty() ? "No configs yet - create your first one" : "No configs match your search";
            float ew = GlHelper.getStringWidth(empty, Typography.SMALL);
            GlHelper.drawText(empty, x + (w - ew) / 2f, listY + 24f, Typography.SMALL,
                Render2D.alpha(Colors.TEXT_DISABLED, alpha));
        } else {
            for (ConfigCard card : cards) {
                if (card.y() + card.h() < listY || card.y() > listY + listH) continue;
                card.render(g, mx, my, alpha);
            }
        }
        Render2D.popScissor();

        for (ConfigCard card : cards) if (card.isMenuOpen()) card.drawOverlay(ps, mx, my, alpha);
        drawToasts(ps, alpha);
    }

    private void drawToolbar(PoseStack ps, int mx, int my, float alpha) {
        if (compactToolbar) drawIconButton(ps, saveX, toolbarY, ICON_SAVE, mx, my, alpha);
        else drawToolbarButton(ps, saveX, toolbarY, saveW, ICON_SAVE, "Save", false, mx, my, alpha);

        boolean sortHover = contains(sortX, toolbarY, sortW, TOOLBAR_H, mx, my);
        Render2D.drawRoundRect(ps, sortX, toolbarY, sortW, TOOLBAR_H, 6f,
            Render2D.alpha(sortHover ? Colors.CARD : Colors.PANEL, alpha));
        GlHelper.drawText(ICON_SORT, sortX + 10f, centerTextY(toolbarY, TOOLBAR_H, Typography.ICON_SMALL),
            Typography.ICON_SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, alpha));
        String sortLabel = sortByName ? "Name" : "Last Modify";
        GlHelper.drawText(sortLabel, sortX + 32f, centerTextY(toolbarY, TOOLBAR_H, Typography.SMALL),
            Typography.SMALL, Render2D.alpha(sortHover ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY, alpha));
        GlHelper.drawText(ICON_ARROW, sortX + sortW - 21f, centerTextY(toolbarY, TOOLBAR_H, Typography.ICON_SMALL),
            Typography.ICON_SMALL, Render2D.alpha(Colors.TEXT_DISABLED, alpha));

        if (compactToolbar) {
            boolean createHover = contains(createX, toolbarY, createW, TOOLBAR_H, mx, my);
            Render2D.drawRoundRect(ps, createX, toolbarY, createW, TOOLBAR_H, 6f,
                Render2D.alpha(createHover ? Colors.ACCENT_HOVER : Colors.ACCENT, alpha * 0.9f));
            float createIconX = createX + (createW - GlHelper.getStringWidth(ICON_CREATE, Typography.ICON_SMALL)) / 2f;
            GlHelper.drawText(ICON_CREATE, createIconX, centerTextY(toolbarY, TOOLBAR_H, Typography.ICON_SMALL),
                Typography.ICON_SMALL, Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
        } else {
            drawToolbarButton(ps, createX, toolbarY, createW, ICON_CREATE, "Create", true, mx, my, alpha);
        }

        if (searchActive || !search.isEmpty()) {
            searchX = x + w - SEARCH_W;
            boolean hover = contains(searchX, toolbarY, SEARCH_W, TOOLBAR_H, mx, my);
            Render2D.drawRoundRect(ps, searchX, toolbarY, SEARCH_W, TOOLBAR_H, 6f,
                Render2D.alpha(hover || searchActive ? Colors.CARD : Colors.PANEL, alpha));
            GlHelper.drawText(ICON_SEARCH, searchX + 9f, centerTextY(toolbarY, TOOLBAR_H, Typography.ICON_SMALL),
                Typography.ICON_SMALL, Render2D.alpha(Colors.TEXT_SECONDARY, alpha));
            String shown = search.isEmpty() ? "Search configs..." : fit(search, SEARCH_W - 46f, Typography.SMALL);
            int color = search.isEmpty() ? Colors.TEXT_DISABLED : Colors.TEXT_PRIMARY;
            GlHelper.drawText(shown, searchX + 31f, centerTextY(toolbarY, TOOLBAR_H, Typography.SMALL),
                Typography.SMALL, Render2D.alpha(color, alpha));
            if (searchActive && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                float cursorX = searchX + 31f + (search.isEmpty() ? 0f : GlHelper.getStringWidth(fit(search, SEARCH_W - 46f, Typography.SMALL), Typography.SMALL));
                Render2D.drawRect(ps, cursorX + 1f, toolbarY + 8f, 1f, 14f, Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
            }
        } else {
            searchX = x + w - TOOLBAR_H;
            drawIconButton(ps, searchX, toolbarY, ICON_SEARCH, mx, my, alpha);
        }
    }

    private void drawNameInput(PoseStack ps, int mx, int my, float alpha) {
        Render2D.drawRoundRect(ps, inputX, inputY, inputW, 26f, 6f, Render2D.alpha(Colors.PANEL, alpha));
        String label = inputMode == InputMode.CREATE ? "New config" : "Rename";
        GlHelper.drawText(label, inputX + 10f, centerTextY(inputY, 26f, Typography.TINY), Typography.TINY,
            Render2D.alpha(Colors.TEXT_SECONDARY, alpha));
        float valueX = inputX + 82f;
        String shown = input.isEmpty() ? "Type a name..." : fit(input, inputW - 148f, Typography.SMALL);
        GlHelper.drawText(shown, valueX, centerTextY(inputY, 26f, Typography.SMALL), Typography.SMALL,
            Render2D.alpha(input.isEmpty() ? Colors.TEXT_DISABLED : Colors.TEXT_PRIMARY, alpha));
        if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
            float cursorX = valueX + (input.isEmpty() ? 0f : GlHelper.getStringWidth(shown, Typography.SMALL));
            Render2D.drawRect(ps, cursorX + 1f, inputY + 6f, 1f, 14f, Render2D.alpha(Colors.ACCENT, alpha));
        }
        drawInlineIconButton(ps, confirmX, inputY + 3f, ICON_CONFIRM, Colors.ACCENT, mx, my, alpha);
        drawInlineIconButton(ps, cancelX, inputY + 3f, ICON_CANCEL, Colors.TEXT_SECONDARY, mx, my, alpha);
    }

    private void drawToolbarButton(PoseStack ps, float bx, float by, float bw, String icon, String label,
                                   boolean accent, int mx, int my, float alpha) {
        boolean hover = contains(bx, by, bw, TOOLBAR_H, mx, my);
        int bg = accent ? (hover ? Colors.ACCENT_HOVER : Colors.ACCENT) : (hover ? Colors.CARD : Colors.PANEL);
        Render2D.drawRoundRect(ps, bx, by, bw, TOOLBAR_H, 6f, Render2D.alpha(bg, alpha * (accent ? 0.9f : 1f)));
        float iconW = GlHelper.getStringWidth(icon, Typography.ICON_SMALL);
        float textW = GlHelper.getStringWidth(label, Typography.SMALL);
        float startX = bx + (bw - iconW - 6f - textW) / 2f;
        int color = accent || hover ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY;
        GlHelper.drawText(icon, startX, centerTextY(by, TOOLBAR_H, Typography.ICON_SMALL), Typography.ICON_SMALL, Render2D.alpha(color, alpha));
        GlHelper.drawText(label, startX + iconW + 6f, centerTextY(by, TOOLBAR_H, Typography.SMALL), Typography.SMALL, Render2D.alpha(color, alpha));
    }

    private void drawIconButton(PoseStack ps, float bx, float by, String icon, int mx, int my, float alpha) {
        boolean hover = contains(bx, by, TOOLBAR_H, TOOLBAR_H, mx, my);
        if (hover) Render2D.drawRoundRect(ps, bx, by, TOOLBAR_H, TOOLBAR_H, 6f, Render2D.alpha(Colors.CARD, alpha));
        float ix = bx + (TOOLBAR_H - GlHelper.getStringWidth(icon, Typography.ICON_SMALL)) / 2f;
        GlHelper.drawText(icon, ix, centerTextY(by, TOOLBAR_H, Typography.ICON_SMALL), Typography.ICON_SMALL,
            Render2D.alpha(hover ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY, alpha));
    }

    private void drawInlineIconButton(PoseStack ps, float bx, float by, String icon, int color,
                                      int mx, int my, float alpha) {
        float size = 20f;
        boolean hover = contains(bx, by, size, size, mx, my);
        Render2D.drawRoundRect(ps, bx, by, size, size, 5f,
            Render2D.alpha(hover ? Colors.NAV_SELECTED : Colors.CARD, alpha * 0.9f));
        float ix = bx + (size - GlHelper.getStringWidth(icon, Typography.ICON_SMALL)) / 2f;
        GlHelper.drawText(icon, ix, centerTextY(by, size, Typography.ICON_SMALL), Typography.ICON_SMALL,
            Render2D.alpha(hover ? Colors.TEXT_PRIMARY : color, alpha));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;

        for (ConfigCard card : cards) {
            if (card.isMenuOpen()) {
                if (card.mouseClicked(mx, my, button)) return true;
                break;
            }
        }

        if (contains(saveX, toolbarY, saveW, TOOLBAR_H, mx, my)) {
            String target = selectedConfig == null ? "default" : selectedConfig;
            vm.save(target);
            selectedConfig = target;
            dirty = true;
            toast("Saved '" + target + "'");
            return true;
        }
        if (contains(sortX, toolbarY, sortW, TOOLBAR_H, mx, my)) {
            sortByName = !sortByName;
            dirty = true;
            scroll = 0f;
            return true;
        }
        if (contains(createX, toolbarY, createW, TOOLBAR_H, mx, my)) {
            inputMode = InputMode.CREATE;
            input = "";
            renameSource = null;
            searchActive = false;
            closeMenus(null);
            return true;
        }
        float activeSearchX = searchActive || !search.isEmpty() ? x + w - SEARCH_W : x + w - TOOLBAR_H;
        float activeSearchW = searchActive || !search.isEmpty() ? SEARCH_W : TOOLBAR_H;
        if (contains(activeSearchX, toolbarY, activeSearchW, TOOLBAR_H, mx, my)) {
            searchActive = true;
            inputMode = InputMode.NONE;
            closeMenus(null);
            return true;
        }
        searchActive = false;

        if (inputMode != InputMode.NONE) {
            if (contains(confirmX, inputY + 3f, 20f, 20f, mx, my)) { commitInput(); return true; }
            if (contains(cancelX, inputY + 3f, 20f, 20f, mx, my)) {
                inputMode = InputMode.NONE;
                input = "";
                renameSource = null;
                return true;
            }
            if (contains(inputX, inputY, inputW, 26f, mx, my)) return true;
        }

        if (my >= listY && my <= listY + listH) {
            for (ConfigCard card : cards) {
                if (card.contains(mx, my)) {
                    closeMenus(card);
                    return card.mouseClicked(mx, my, button);
                }
            }
        }
        closeMenus(null);
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!contains(x, listY, w, listH, mx, my) || maxScroll <= 0f) return false;
        closeMenus(null);
        scroll = Math.max(0f, Math.min(maxScroll, scroll - (float) delta * 28f));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (inputMode != InputMode.NONE) {
            if (key == 257 || key == 335) { commitInput(); return true; }
            if (key == 256) { inputMode = InputMode.NONE; input = ""; renameSource = null; return true; }
            if (key == 259 && !input.isEmpty()) { input = input.substring(0, input.length() - 1); return true; }
            return true;
        }
        if (searchActive) {
            if (key == 256) { searchActive = false; return true; }
            if (key == 259 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); dirty = true; scroll = 0f; return true; }
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (Character.isISOControl(c)) return false;
        if (inputMode != InputMode.NONE && input.length() < 32) { input += c; return true; }
        if (searchActive && search.length() < 40) { search += c; dirty = true; scroll = 0f; return true; }
        return false;
    }

    private void beginRename(String name) {
        inputMode = InputMode.RENAME;
        renameSource = name;
        input = name;
        searchActive = false;
        closeMenus(null);
    }

    private void commitInput() {
        String name = input.trim();
        if (!validConfigName(name)) {
            toast("Use a valid config name");
            return;
        }
        if (inputMode == InputMode.CREATE) {
            vm.save(name);
            selectedConfig = name;
            toast("Created '" + name + "'");
        } else if (inputMode == InputMode.RENAME && renameSource != null && !renameSource.equals(name)) {
            vm.rename(renameSource, name);
            if (renameSource.equals(selectedConfig)) selectedConfig = name;
            toast("Renamed to '" + name + "'");
        }
        inputMode = InputMode.NONE;
        input = "";
        renameSource = null;
        dirty = true;
    }

    private static boolean validConfigName(String name) {
        return !name.isEmpty() && !name.endsWith(".") && !name.matches(".*[\\/:*?\"<>|].*");
    }

    private void closeMenus(ConfigCard except) {
        for (ConfigCard card : cards) if (card != except) card.closeMenu();
    }

    private void drawToasts(PoseStack ps, float alpha) {
        long now = System.currentTimeMillis();
        toasts.removeIf(toast -> now - toast.at() > 2500L);
        float toastY = y + h - 26f;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast toast = toasts.get(i);
            float age = (now - toast.at()) / 1000f;
            float fade = age < 0.3f ? age / 0.3f : age > 2.2f ? (2.5f - age) / 0.3f : 1f;
            fade = Math.max(0f, Math.min(1f, fade)) * alpha;
            float toastW = GlHelper.getStringWidth(toast.msg(), Typography.SMALL) + 24f;
            float toastX = x + w - toastW;
            Render2D.drawRoundRect(ps, toastX, toastY, toastW, 22f, 6f, Render2D.alpha(Colors.TOAST_BG, fade));
            Render2D.drawCircle(ps, toastX + 10f, toastY + 11f, 2.5f, Render2D.alpha(Colors.ACCENT, fade));
            GlHelper.drawText(toast.msg(), toastX + 17f, centerTextY(toastY, 22f, Typography.SMALL), Typography.SMALL,
                Render2D.alpha(Colors.TEXT_PRIMARY, fade));
            toastY -= 27f;
        }
    }

    private static boolean contains(float bx, float by, float bw, float bh, double px, double py) {
        return px >= bx && px <= bx + bw && py >= by && py <= by + bh;
    }

    private static float centerTextY(float y, float height, FontRenderer font) {
        return y + (height - font.getMetrics().capHeight()) / 2f;
    }

    private static String fit(String value, float maxWidth, FontRenderer font) {
        if (value == null || maxWidth <= 4f) return "";
        if (GlHelper.getStringWidth(value, font) <= maxWidth) return value;
        String out = value;
        while (out.length() > 1 && GlHelper.getStringWidth(out + "...", font) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "...";
    }
}
