package shit.zen.gui.neverloseGUI.card;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.config.ConfigData;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Component;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;

/** Compact Neverlose-style config row with an exact, shared render/hit-test layout. */
public class ConfigCard extends Component {
    public static final float HEIGHT = 54f;
    private static final float ACTION_H = 24f;
    private static final float SAVE_W = 76f;
    private static final float MORE_W = 24f;
    private static final float MENU_W = 112f;
    private static final float MENU_ITEM_H = 22f;

    private static final String ICON_SAVE = "\uE161";
    private static final String ICON_MORE = "\uE5D4";
    private static final String[] MENU_LABELS = {"Load", "Rename", "Duplicate", "Delete"};
    private static final String[] MENU_ACTIONS = {"Load", "Ren", "Dup", "Del"};
    private static final String[] MENU_ICONS = {"\uE2C4", "\uE3C9", "\uE14D", "\uE872"};

    private final ConfigData config;
    private final Callback callback;
    private boolean menuOpen;
    private float menuBottom = Float.MAX_VALUE;
    private float saveX, actionY, moreX, menuX, menuY;

    public interface Callback { void on(String action, String name); }

    public ConfigCard(float x, float y, float w, ConfigData config, Callback callback) {
        super(x, y, w, HEIGHT);
        this.config = config;
        this.callback = callback;
        updateActionBounds();
    }

    @Override
    public void setPos(float x, float y) {
        super.setPos(x, y);
        updateActionBounds();
    }

    @Override
    public void setSize(float w, float h) {
        super.setSize(w, HEIGHT);
        updateActionBounds();
    }

    private void updateActionBounds() {
        actionY = y + (HEIGHT - ACTION_H) / 2f;
        saveX = x + w - SAVE_W - 8f;
        moreX = saveX - MORE_W - 6f;
        menuX = x + w - MENU_W - 8f;
        float menuH = MENU_LABELS.length * MENU_ITEM_H + 8f;
        float belowY = y + HEIGHT + 4f;
        menuY = belowY + menuH <= menuBottom ? belowY : y - menuH - 4f;
    }

    @Override
    public void draw(PoseStack ps, GuiGraphics g, int mx, int my, float alpha, float hover) {
        int bg = Render2D.alpha(Colors.CARD, alpha);
        if (hover > 0.01f) bg = Render2D.lerpColor(bg, Render2D.alpha(0xFF202530, alpha), hover * 0.55f);
        Render2D.drawRoundRect(ps, x, y, w, HEIGHT, 7f, bg);
        if (hover > 0.08f || menuOpen) {
            Render2D.drawRoundRect(ps, x, y, 2f, HEIGHT, 1f, Render2D.alpha(Colors.ACCENT, alpha * Math.max(hover, 0.65f)));
        }

        GlHelper.drawText(fit(config.getName(), Math.max(40f, moreX - x - 28f), Typography.BODY), x + 12f,
            centerTextY(y, HEIGHT / 2f, Typography.BODY), Typography.BODY, Render2D.alpha(Colors.TEXT_PRIMARY, alpha));
        String meta = "Modified: " + config.getDateString() + "  |  " + config.getSizeString();
        GlHelper.drawText(fit(meta, Math.max(40f, moreX - x - 28f), Typography.TINY), x + 12f,
            centerTextY(y + HEIGHT / 2f, HEIGHT / 2f, Typography.TINY), Typography.TINY,
            Render2D.alpha(Colors.TEXT_SECONDARY, alpha));

        boolean moreHover = Render2D.contains(moreX, actionY, MORE_W, ACTION_H, mx, my);
        boolean saveHover = Render2D.contains(saveX, actionY, SAVE_W, ACTION_H, mx, my);
        if (moreHover || menuOpen) {
            Render2D.drawRoundRect(ps, moreX, actionY, MORE_W, ACTION_H, 6f,
                Render2D.alpha(Colors.PANEL, alpha * (moreHover ? 0.95f : 0.7f)));
        }
        drawCenteredIcon(ps, ICON_MORE, moreX, actionY, MORE_W, ACTION_H,
            moreHover || menuOpen ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY, alpha);

        Render2D.drawRoundRect(ps, saveX, actionY, SAVE_W, ACTION_H, 6f,
            Render2D.alpha(saveHover ? Colors.ACCENT_DIM : Colors.PANEL, alpha));
        Render2D.drawRoundRect(ps, saveX + 1f, actionY + 1f, SAVE_W - 2f, ACTION_H - 2f, 5f,
            Render2D.alpha(Colors.CARD, alpha * (saveHover ? 0.35f : 0.72f)));
        drawIconText(ps, ICON_SAVE, "Save", saveX, actionY, SAVE_W, ACTION_H,
            saveHover ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY, alpha);
    }

    /** Drawn after all rows so the context menu cannot be covered by the next row. */
    public void drawOverlay(PoseStack ps, int mx, int my, float alpha) {
        if (!menuOpen) return;
        float menuH = MENU_LABELS.length * MENU_ITEM_H + 8f;
        Render2D.drawShadow(ps, menuX, menuY, MENU_W, menuH, 8f, 9f,
            Render2D.alpha(0xFF000000, alpha * 0.5f));
        Render2D.drawRoundRect(ps, menuX, menuY, MENU_W, menuH, 8f, Render2D.alpha(Colors.PANEL, alpha));
        for (int i = 0; i < MENU_LABELS.length; i++) {
            float iy = menuY + 4f + i * MENU_ITEM_H;
            boolean itemHover = Render2D.contains(menuX + 4f, iy, MENU_W - 8f, MENU_ITEM_H, mx, my);
            if (itemHover) Render2D.drawRoundRect(ps, menuX + 4f, iy, MENU_W - 8f, MENU_ITEM_H, 5f,
                Render2D.alpha(i == 3 ? 0xFF49272B : Colors.NAV_SELECTED, alpha));
            int color = i == 3 ? Colors.DANGER : (itemHover ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY);
            GlHelper.drawText(MENU_ICONS[i], menuX + 11f, centerTextY(iy, MENU_ITEM_H, Typography.ICON_SMALL),
                Typography.ICON_SMALL, Render2D.alpha(color, alpha));
            GlHelper.drawText(MENU_LABELS[i], menuX + 34f, centerTextY(iy, MENU_ITEM_H, Typography.SMALL),
                Typography.SMALL, Render2D.alpha(color, alpha));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (menuOpen) {
            for (int i = 0; i < MENU_LABELS.length; i++) {
                float iy = menuY + 4f + i * MENU_ITEM_H;
                if (Render2D.contains(menuX + 4f, iy, MENU_W - 8f, MENU_ITEM_H, (float) mx, (float) my)) {
                    menuOpen = false;
                    callback.on(MENU_ACTIONS[i], config.getName());
                    return true;
                }
            }
            if (!contains(mx, my)) { menuOpen = false; return false; }
        }
        if (Render2D.contains(saveX, actionY, SAVE_W, ACTION_H, (float) mx, (float) my)) {
            callback.on("Save", config.getName());
            return true;
        }
        if (Render2D.contains(moreX, actionY, MORE_W, ACTION_H, (float) mx, (float) my)) {
            menuOpen = !menuOpen;
            return true;
        }
        return false;
    }

    public boolean isMenuOpen() { return menuOpen; }
    public void closeMenu() { menuOpen = false; }
    public void setMenuBottom(float bottom) { menuBottom = bottom; updateActionBounds(); }

    private static void drawCenteredIcon(PoseStack ps, String icon, float x, float y, float w, float h, int color, float alpha) {
        float ix = x + (w - GlHelper.getStringWidth(icon, Typography.ICON_SMALL)) / 2f;
        GlHelper.drawText(icon, ix, centerTextY(y, h, Typography.ICON_SMALL), Typography.ICON_SMALL, Render2D.alpha(color, alpha));
    }

    private static void drawIconText(PoseStack ps, String icon, String text, float x, float y, float w, float h, int color, float alpha) {
        float iconW = GlHelper.getStringWidth(icon, Typography.ICON_SMALL);
        float textW = GlHelper.getStringWidth(text, Typography.SMALL);
        float startX = x + (w - iconW - 6f - textW) / 2f;
        GlHelper.drawText(icon, startX, centerTextY(y, h, Typography.ICON_SMALL), Typography.ICON_SMALL, Render2D.alpha(color, alpha));
        GlHelper.drawText(text, startX + iconW + 6f, centerTextY(y, h, Typography.SMALL), Typography.SMALL, Render2D.alpha(color, alpha));
    }

    private static float centerTextY(float y, float h, FontRenderer font) {
        return y + (h - font.getMetrics().capHeight()) / 2f;
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
