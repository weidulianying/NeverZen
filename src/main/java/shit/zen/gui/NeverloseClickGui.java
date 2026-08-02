package shit.zen.gui;

import java.awt.Color;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import shit.zen.ZenClient;
import shit.zen.config.ConfigData;
import shit.zen.manager.ConfigManager;
import shit.zen.modules.Category;
import shit.zen.modules.Module;
import shit.zen.render.DrawContext;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.Renderer;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.*;
import shit.zen.utils.math.LerpUtil;
import shit.zen.utils.render.RenderUtil;

/**
 * Neverlose-style ClickGUI with card-based module layout, smooth animations,
 * and a full Configs page (search, 5-action cards, double-click load, toasts).
 */
public class NeverloseClickGui extends Screen {

    // ── Layout ──────────────────────────────────────────
    private static final float W = 700, H = 460;
    private static final float SIDEBAR_W = 100, TOP_H = 36, BOT_H = 24;

    // ── Theme ───────────────────────────────────────────
    private static final int C_BG      = 0xEE111111;
    private static final int C_SIDEBAR = 0xF0141418;
    private static final int C_CARD    = 0xF01C1C24;
    private static final int C_CARD_HDR= 0xF0222230;
    private static final int C_ACCENT  = 0xFF4F8BFF;
    private static final int C_TEXT    = 0xFFFFFFFF;
    private static final int C_MUTED   = 0xFFA0A0A0;
    private static final int C_BORDER  = 0xFF2E2E3A;
    private static final int C_TOGGLE_ON  = 0xFF4F8BFF;
    private static final int C_TOGGLE_OFF = 0xFF3A3A48;
    private static final int C_KNOB   = 0xFFFFFFFF;

    // ── Sidebar tabs ────────────────────────────────────
    private static final String[] TAB_NAMES = {"Home", "Move", "Misc", "Configs"};
    private static final Category[][] TAB_CATS = {
        {Category.COMBAT},
        {Category.MOVEMENT, Category.PLAYER},
        {Category.RENDER, Category.EXPLOIT, Category.WORLD, Category.MISC},
        null
    };

    // ── State ───────────────────────────────────────────
    private int selectedTab = 0;
    private final Anim openAnim = new Anim();
    private final Anim sidebarHover = new Anim();
    private int hoveredTab = -1;
    private float scrollTarget, cardScroll;

    // Modules
    private final Map<Category, List<Module>> catMods = new LinkedHashMap<>();
    private final Map<Module, Anim> cardExpand  = new HashMap<>();
    private final Map<Module, Anim> cardHover   = new HashMap<>();
    private Module expandedModule, lastExpanded;
    private Setting<?> editingSetting;
    private final StringBuilder settingInput = new StringBuilder();

    // Configs tab
    private String configSearch = "";
    private boolean configSearchActive;
    private String configNewName = "";
    private boolean configNewInputActive;
    private long configNewInputBlink;
    private int configHovered = -1, configBtnHovered = -1;
    private String configRenameTarget;
    private String configRenameInput = "";
    private long configRenameBlink, configLastClickTime;
    private int configLastClickIdx = -1;
    // Toasts
    private final List<ToastMsg> toasts = new ArrayList<>();
    private static class ToastMsg { String msg; long at; ToastMsg(String m) { msg = m; at = System.currentTimeMillis(); } }

    // Search (bottom bar)
    private boolean searchActive;
    private String searchQuery = "";
    private long cursorBlink;

    // ══════════════════════════════════════════════════════
    private static class Anim {
        float v, t;
        void to(float t) { this.t = t; }
        void set(float v) { this.v = this.t = v; }
        float get() { return v += (t - v) * 0.14f; }
        float get(float s) { return v += (t - v) * s; }
    }

    public NeverloseClickGui() {
        super(Component.literal("NeverZen"));
        initModules();
    }

    private void initModules() {
        for (Category c : Category.values()) catMods.put(c, new ArrayList<>());
        if (!ZenClient.isReady()) return;
        for (Module m : ZenClient.getInstance().getModuleManager().getModules()) {
            List<Module> list = catMods.get(m.getCategory());
            if (list != null) list.add(m);
            cardExpand.put(m, new Anim());
            cardHover.put(m, new Anim());
        }
    }

    // ══════════════════════════════════════════════════════
    //  Render
    // ══════════════════════════════════════════════════════
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        LerpUtil.update();
        openAnim.to(1f); openAnim.get(); sidebarHover.get();

        float a = ease(openAnim.get());
        float cx = (width - W) / 2f, cy = (height - H) / 2f;

        g.fill(0, 0, width, height, alpha(C_BG, 0.6f * a));
        RenderUtil.drawRoundedRect(g.pose(), cx, cy, W, H, 10f, alpha(C_BG, a));
        RenderUtil.drawRoundedRect(g.pose(), cx + 2, cy + 2, SIDEBAR_W - 2, H - 4, 8f, alpha(C_SIDEBAR, a));

        Renderer.renderConsumer(dc -> {
            drawTopBar(dc, g, cx, cy, a);
            drawSidebar(dc, g, cx, cy, mx, my, a);
            if (selectedTab == 3) drawConfigsTab(dc, g, cx, cy, mx, my, a);
            else drawModulesTab(dc, g, cx, cy, mx, my, a);
            drawBottomBar(dc, g, cx, cy, mx, my, a);
            drawToasts(dc, g, cx, a);
        });
    }

    private void drawTopBar(DrawContext dc, GuiGraphics g, float cx, float cy, float a) {
        float x = cx + SIDEBAR_W + 4;
        RenderUtil.drawFilledRect(g.pose(), x, cy + TOP_H, W - SIDEBAR_W - 8, 1f, alpha(C_BORDER, a * 0.5f));
        GlHelper.drawText("NeverZen", x + 12, cy + 10, FontPresets.museoSans(18f), alpha(C_ACCENT, a));
    }

    private void drawSidebar(DrawContext dc, GuiGraphics g, float cx, float cy, int mx, int my, float a) {
        float x = cx + 6, y = cy + TOP_H + 8, tabH = 32;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            float ty = y + i * (tabH + 4);
            boolean sel = i == selectedTab;
            boolean hov = mx >= x && mx <= x + SIDEBAR_W - 12 && my >= ty && my <= ty + tabH;
            if (hov) { hoveredTab = i; sidebarHover.to(1f); }
            else if (hoveredTab == i) { hoveredTab = -1; sidebarHover.to(0f); }
            float ha = hov ? sidebarHover.get(0.3f) : 0f;
            if (sel) RenderUtil.drawRoundedRect(g.pose(), x, ty, SIDEBAR_W - 12, tabH, 6f, alpha(C_ACCENT, a * 0.35f));
            else if (ha > 0.01f) RenderUtil.drawRoundedRect(g.pose(), x, ty, SIDEBAR_W - 12, tabH, 6f, alpha(C_CARD_HDR, a * ha));
            FontRenderer f = FontPresets.axiformaRegular(13f);
            GlHelper.drawText(TAB_NAMES[i], x + 14, ty + (tabH - f.getMetrics().capHeight()) / 2f, f, alpha(sel ? C_ACCENT : C_MUTED, a));
        }
        RenderUtil.drawFilledRect(g.pose(), x + 10, y + 3 * (tabH + 4) - 2, SIDEBAR_W - 32, 1f, alpha(C_BORDER, a * 0.4f));
    }

    // ── Module cards tab ─────────────────────────────────
    private void drawModulesTab(DrawContext dc, GuiGraphics g, float cx, float cy, int mx, int my, float a) {
        float areaX = cx + SIDEBAR_W + 10, areaY = cy + TOP_H + 8;
        float areaW = W - SIDEBAR_W - 20, areaH = H - TOP_H - BOT_H - 16;

        List<Module> mods = getTabModules();
        if (!searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase();
            mods = mods.stream().filter(m -> m.getName().toLowerCase().contains(q)).collect(Collectors.toList());
        }

        float cardH = 36, cardGap = 4, contentH = 0;
        for (Module m : mods) {
            float eh = cardH;
            if (m == expandedModule) eh = cardH + m.getSettings().size() * 22 + 10;
            else if (m == lastExpanded) {
                Anim ex = cardExpand.get(m); ex.to(0f);
                eh = cardH + m.getSettings().size() * 22 * ex.get() + 10 * ex.get();
            }
            contentH += eh + cardGap;
        }
        scrollTarget = Math.max(-Math.max(0, contentH - areaH), Math.min(0, scrollTarget));
        cardScroll += (scrollTarget - cardScroll) * 0.14f;

        float itemY = areaY + cardScroll;
        for (Module m : mods) {
            float ch = cardH;
            boolean expanded = m == expandedModule, transitioning = m == lastExpanded && m != expandedModule;
            if (expanded) ch = cardH + m.getSettings().size() * 22 + 10;
            else if (transitioning) { Anim ex = cardExpand.get(m); ch = cardH + m.getSettings().size() * 22 * ex.get() + 10 * ex.get(); }
            if (itemY + ch < areaY || itemY > areaY + areaH) { itemY += ch + cardGap; continue; }
            boolean hov = mx >= areaX && mx <= areaX + areaW && my >= itemY && my <= itemY + ch;
            cardHover.get(m).to(hov ? 1f : 0f);
            drawModuleCard(g, areaX, itemY, areaW, ch, m, expanded || transitioning, mx, my, a);
            itemY += ch + cardGap;
        }
    }

    private void drawModuleCard(GuiGraphics g, float x, float y, float w, float h, Module m, boolean expanded, int mx, int my, float a) {
        float hv = cardHover.get(m).get(expanded ? 0.1f : 0.2f);
        int cardBg = alpha(C_CARD, a);
        if (hv > 0.01f) cardBg = lerpColor(cardBg, C_CARD_HDR, hv * 0.5f);
        RenderUtil.drawRoundedRect(g.pose(), x, y, w, h, 8f, cardBg);
        RenderUtil.drawRoundedRect(g.pose(), x, y, w, h, 8f, alpha(C_BORDER, a * 0.3f));
        float headerH = 36;
        FontRenderer nf = FontPresets.axiformaBold(14f);
        GlHelper.drawText(m.getName(), x + 12, y + (headerH - nf.getMetrics().capHeight()) / 2f, nf, alpha(m.isEnabled() ? C_TEXT : C_MUTED, a));
        float tw = 34, th = 18;
        drawToggle(g, x + w - tw - 14, y + (headerH - th) / 2f, tw, th, m.isEnabled(), a);
        if (expanded) {
            float sy = y + headerH + 4;
            RenderUtil.drawFilledRect(g.pose(), x + 10, sy, w - 20, 1f, alpha(C_BORDER, a * 0.4f));
            sy += 8;
            for (Setting<?> s : m.getSettings()) {
                if (sy + 20 > y + h) break;
                drawSetting(g, x + 12, sy, w - 24, s, mx, my, a);
                sy += 22;
            }
        }
    }

    private void drawToggle(GuiGraphics g, float x, float y, float w, float h, boolean on, float a) {
        RenderUtil.drawRoundedRect(g.pose(), x, y, w, h, h / 2f, alpha(on ? C_TOGGLE_ON : C_TOGGLE_OFF, a * (on ? 0.7f : 0.6f)));
        float kr = h / 2f - 2;
        RenderUtil.drawRoundedRect(g.pose(), on ? x + w - kr * 2 - 2 : x + 2, y + 2, kr * 2, kr * 2, kr, alpha(C_KNOB, a));
    }

    private void drawSetting(GuiGraphics g, float x, float y, float w, Setting<?> s, int mx, int my, float a) {
        FontRenderer sf = FontPresets.axiformaRegular(12f);
        GlHelper.drawText(s.getName(), x, y + 3, sf, alpha(C_MUTED, a));
        if (s instanceof BooleanSetting bs) {
            drawToggle(g, x + w - 30, y + 1, 30, 15, bs.getValue(), a);
        } else if (s instanceof NumberSetting ns) {
            float sw = w * 0.4f, sx = x + w - sw, sy = y + 8;
            float frac = (ns.getValue().floatValue() - ns.getMin().floatValue()) / (ns.getMax().floatValue() - ns.getMin().floatValue());
            RenderUtil.drawRoundedRect(g.pose(), sx, sy, sw, 4f, 2f, alpha(C_TOGGLE_OFF, a));
            RenderUtil.drawRoundedRect(g.pose(), sx, sy, sw * frac, 4f, 2f, alpha(C_ACCENT, a));
            RenderUtil.drawRoundedRect(g.pose(), sx + sw * frac - 3, sy - 2, 6f, 8f, 3f, alpha(C_TEXT, a));
            FontRenderer vf = FontPresets.axiformaRegular(11f);
            String val = String.format("%.1f", ns.getValue().floatValue());
            GlHelper.drawText(val, sx - GlHelper.getStringWidth(val, vf) - 6, y + 3, vf, alpha(C_TEXT, a));
        } else if (s instanceof ModeSetting ms) {
            FontRenderer vf = FontPresets.axiformaRegular(11f);
            String val = ms.getValue();
            GlHelper.drawText(val, x + w - GlHelper.getStringWidth(val, vf), y + 3, vf, alpha(C_ACCENT, a));
        } else if (s instanceof StringSetting || s instanceof PasswordSetting) {
            FontRenderer vf = FontPresets.axiformaRegular(11f);
            float inputW = w * 0.58f, inputX = x + w - inputW;
            boolean active = editingSetting == s;
            RenderUtil.drawRoundedRect(g.pose(), inputX, y, inputW, 18, 4, alpha(active ? C_CARD_HDR : C_SIDEBAR, a));
            String raw;
            if (active) raw = settingInput.toString();
            else if (s instanceof StringSetting string) raw = string.getValue() == null ? "" : string.getValue();
            else raw = "•".repeat(((PasswordSetting) s).getValue().length);
            String shown = s instanceof PasswordSetting && active && !raw.isEmpty()
                    ? "•".repeat(Math.min(raw.length(), 24)) : raw;
            if (shown.isEmpty()) shown = "...";
            while (shown.length() > 1 && GlHelper.getStringWidth(shown, vf) > inputW - 10) shown = shown.substring(1);
            GlHelper.drawText(shown, inputX + inputW - 5 - GlHelper.getStringWidth(shown, vf), y + 3, vf,
                    alpha(shown.equals("...") ? C_MUTED : C_TEXT, a * (shown.equals("...") ? .5f : 1f)));
        } else if (s instanceof ActionSetting) {
            FontRenderer vf = FontPresets.axiformaRegular(11f);
            float buttonW = w * .38f, buttonX = x + w - buttonW;
            RenderUtil.drawRoundedRect(g.pose(), buttonX, y, buttonW, 18, 4, alpha(C_ACCENT, a * .45f));
            String label = s.getName();
            GlHelper.drawText(label, buttonX + (buttonW - GlHelper.getStringWidth(label, vf)) / 2f, y + 3, vf, alpha(C_TEXT, a));
        }
    }

    // ── Configs tab (search, 5-action cards, double-click, toasts) ─
    private void drawConfigsTab(DrawContext dc, GuiGraphics g, float cx, float cy, int mx, int my, float a) {
        float ax = cx + SIDEBAR_W + 10, ay = cy + TOP_H + 6;
        float aw = W - SIDEBAR_W - 20;
        ConfigManager cm = ZenClient.getInstance().getConfigManager();
        List<ConfigData> all = cm.getConfigs();
        String q = configSearch.toLowerCase().trim();
        List<ConfigData> configs = q.isEmpty() ? all : all.stream().filter(c -> c.getName().toLowerCase().contains(q)).collect(Collectors.toList());

        FontRenderer tf = FontPresets.axiformaBold(14f);
        FontRenderer sf = FontPresets.axiformaRegular(12f);
        FontRenderer tn = FontPresets.axiformaRegular(11f);

        // Top: "Configs" + search + [+New]
        GlHelper.drawText("Configs", ax, ay + 6, tf, alpha(C_TEXT, a));
        GlHelper.drawText(configs.size() + " configs", ax + GlHelper.getStringWidth("Configs", tf) + 10, ay + 8, tn, alpha(C_MUTED, a));

        // Search box
        float sw = 150, sh = 20;
        float sx = ax + 180, sy = ay + 2;
        int sbg = configSearchActive ? alpha(C_CARD_HDR, a) : alpha(C_SIDEBAR, a * 0.6f);
        RenderUtil.drawRoundedRect(g.pose(), sx, sy, sw, sh, 4f, sbg);
        if (configSearch.isEmpty() && !configSearchActive)
            GlHelper.drawText("Search configs...", sx + 8, sy + (sh - tn.getMetrics().capHeight()) / 2f, tn, alpha(C_MUTED, a * 0.45f));
        else {
            GlHelper.drawText(configSearch, sx + 8, sy + (sh - tn.getMetrics().capHeight()) / 2f, tn, alpha(C_TEXT, a));
            if (configSearchActive && (System.currentTimeMillis() / 500) % 2 == 0) {
                float cw = GlHelper.getStringWidth(configSearch, tn);
                RenderUtil.drawFilledRect(g.pose(), sx + 8 + cw + 1, sy + 3, 1f, 14, alpha(C_TEXT, a * 0.5f));
            }
        }

        // [+ New Config] top-right
        float nbw = 100, nbh = 20;
        float nbx = ax + aw - nbw, nby = ay + 2;
        boolean nbHov = mx >= nbx && mx <= nbx + nbw && my >= nby && my <= nby + nbh;
        RenderUtil.drawRoundedRect(g.pose(), nbx, nby, nbw, nbh, 4f, alpha(nbHov ? C_ACCENT : C_CARD_HDR, a * (nbHov ? 0.5f : 0.35f)));
        String nl = "+ New Config";
        GlHelper.drawText(nl, nbx + (nbw - GlHelper.getStringWidth(nl, tn)) / 2f, nby + (nbh - tn.getMetrics().capHeight()) / 2f, tn, alpha(C_TEXT, a));

        // New config inline input
        if (configNewInputActive) {
            float iw = 160, ih = 20;
            RenderUtil.drawRoundedRect(g.pose(), nbx - iw - 8, nby, iw, ih, 4f, alpha(C_CARD_HDR, a));
            if (configNewName.isEmpty())
                GlHelper.drawText("Name...", nbx - iw - 2, nby + (ih - tn.getMetrics().capHeight()) / 2f, tn, alpha(C_MUTED, a * 0.5f));
            else {
                GlHelper.drawText(configNewName, nbx - iw - 2, nby + (ih - tn.getMetrics().capHeight()) / 2f, tn, alpha(C_TEXT, a));
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    float cw = GlHelper.getStringWidth(configNewName, tn);
                    RenderUtil.drawFilledRect(g.pose(), nbx - iw + 6 + cw, nby + 3, 1f, 14, alpha(C_TEXT, a * 0.5f));
                }
            }
        }

        // Separator
        float sepY = configNewInputActive ? nby + nbh + 8 : nby + nbh + 6;
        RenderUtil.drawFilledRect(g.pose(), ax, sepY, aw, 1f, alpha(C_BORDER, a * 0.3f));

        // ── Cards ──
        float cardY = sepY + 6, cardH = 62, cardGap = 5;
        configHovered = -1; configBtnHovered = -1;

        for (int i = 0; i < configs.size(); i++) {
            ConfigData cfg = configs.get(i);
            float cyc = cardY + i * (cardH + cardGap);
            if (cyc + cardH < ay - 20 || cyc > cy + H - BOT_H) continue;

            boolean cardHov = mx >= ax && mx <= ax + aw && my >= cyc && my <= cyc + cardH;
            boolean renaming = configRenameTarget != null && configRenameTarget.equals(cfg.getName());

            // Card bg
            int cb = cardHov ? alpha(C_CARD_HDR, a) : alpha(C_CARD, a);
            RenderUtil.drawRoundedRect(g.pose(), ax, cyc, aw, cardH, 8f, cb);
            if (cardHov || renaming) RenderUtil.drawRoundedRect(g.pose(), ax, cyc, aw, cardH, 8f, alpha(C_ACCENT, a * (cardHov ? 0.18f : 0.12f)));

            if (renaming) {
                float rw = aw * 0.45f, rh = 20;
                float rx = ax + 12, ry = cyc + 10;
                RenderUtil.drawRoundedRect(g.pose(), rx, ry, rw, rh, 4f, alpha(C_CARD_HDR, a));
                GlHelper.drawText(configRenameInput, rx + 6, ry + (rh - sf.getMetrics().capHeight()) / 2f, sf, alpha(C_TEXT, a));
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    float cw = GlHelper.getStringWidth(configRenameInput, sf);
                    RenderUtil.drawFilledRect(g.pose(), rx + 6 + cw + 1, ry + 3, 1f, 14, alpha(C_TEXT, a * 0.5f));
                }
                GlHelper.drawText("↵ confirm  ✕ cancel", rx + rw + 10, ry + 4, tn, alpha(C_MUTED, a * 0.45f));
            } else {
                GlHelper.drawText(cfg.getName(), ax + 12, cyc + 10, tf, alpha(C_TEXT, a));
            }
            GlHelper.drawText("Created: " + cfg.getDateString() + "  ·  " + cfg.getSizeString(), ax + 12, cyc + 30, tn, alpha(C_MUTED, a));

            // 5 action buttons
            String[] labels = {"Load", "Save", "Ren", "Dup", "Del"};
            int[]     tints  = {C_ACCENT, C_ACCENT, C_MUTED, C_MUTED, 0xFFFF6666};
            float btnY = cyc + cardH - 22, btnX = ax + 12, btnH = 18;

            if (cardHov && !renaming) {
                configHovered = i; configBtnHovered = -1;
                float bx = btnX;
                for (int b = 0; b < 5; b++) {
                    float bw = GlHelper.getStringWidth(labels[b], tn) + 14;
                    if (mx >= bx && mx <= bx + bw && my >= btnY && my <= btnY + btnH) { configBtnHovered = b; break; }
                    bx += bw + 4;
                }
            }
            float bx = btnX;
            for (int b = 0; b < 5; b++) {
                float bw = GlHelper.getStringWidth(labels[b], tn) + 14;
                boolean btnHov = cardHov && !renaming && configBtnHovered == b && configHovered == i;
                int btnBg = btnHov ? (b == 4 ? alpha(0xFF553333, a) : alpha(C_ACCENT, a * 0.35f)) : alpha(C_SIDEBAR, a * 0.5f);
                RenderUtil.drawRoundedRect(g.pose(), bx, btnY, bw, btnH, 4f, btnBg);
                GlHelper.drawText(labels[b], bx + 7, btnY + (btnH - tn.getMetrics().capHeight()) / 2f, tn, alpha(btnHov ? C_TEXT : tints[b], a * 0.75f));
                bx += bw + 4;
            }
        }
    }

    // ── Toasts ────────────────────────────────────────────
    private void drawToasts(DrawContext dc, GuiGraphics g, float cx, float a) {
        long now = System.currentTimeMillis();
        toasts.removeIf(t -> now - t.at > 2500);
        FontRenderer tf = FontPresets.axiformaRegular(12f);
        float y = cy() + H - BOT_H - 40;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            ToastMsg t = toasts.get(i);
            float age = (now - t.at) / 1000f;
            float ta = age < 0.3f ? age / 0.3f : age > 2.2f ? (2.5f - age) / 0.3f : 1f;
            ta = Math.max(0, Math.min(1, ta)) * a;
            float tw = GlHelper.getStringWidth(t.msg, tf);
            float tx = cx + SIDEBAR_W + (W - SIDEBAR_W - tw) / 2f;
            RenderUtil.drawRoundedRect(g.pose(), tx - 6, y - 4, tw + 12, 20, 5f, alpha(0xFF222230, ta * 0.9f));
            GlHelper.drawText(t.msg, tx, y, tf, alpha(C_ACCENT, ta));
            y -= 26;
        }
    }

    private float cy() { return (height - H) / 2f; }

    // ── Bottom bar ───────────────────────────────────────
    private void drawBottomBar(DrawContext dc, GuiGraphics g, float cx, float cy, int mx, int my, float a) {
        float bx = cx + SIDEBAR_W + 4, by = cy + H - BOT_H - 2, bw = W - SIDEBAR_W - 8;
        RenderUtil.drawFilledRect(g.pose(), bx, by - 1, bw, 1f, alpha(C_BORDER, a * 0.4f));
        FontRenderer bf = FontPresets.axiformaRegular(11f);
        float sw = 140, sx = bx + 8, sy = by + 4;
        int sbg = searchActive ? alpha(C_CARD_HDR, a) : alpha(C_SIDEBAR, a * 0.7f);
        RenderUtil.drawRoundedRect(g.pose(), sx, sy, sw, 16, 4f, sbg);
        if (searchActive || !searchQuery.isEmpty()) {
            GlHelper.drawText(searchQuery, sx + 6, sy + 2, bf, alpha(C_TEXT, a));
            if (searchActive && (System.currentTimeMillis() / 500) % 2 == 0)
                RenderUtil.drawFilledRect(g.pose(), sx + 6 + GlHelper.getStringWidth(searchQuery, bf) + 1, sy + 2, 1f, 12, alpha(C_TEXT, a * 0.6f));
        } else GlHelper.drawText("Search modules...", sx + 6, sy + 2, bf, alpha(C_MUTED, a * 0.5f));
        String fps = this.minecraft.getFps() + " FPS";
        float fw = GlHelper.getStringWidth(fps, bf);
        GlHelper.drawText(fps, bx + bw - fw - 8, sy + 2, bf, alpha(C_MUTED, a * 0.6f));
        String time = String.format("%02d:%02d", java.time.LocalTime.now().getHour(), java.time.LocalTime.now().getMinute());
        GlHelper.drawText(time, bx + bw - fw - GlHelper.getStringWidth(time, bf) - 24, sy + 2, bf, alpha(C_MUTED, a * 0.5f));
    }

    // ══════════════════════════════════════════════════════
    //  Mouse
    // ══════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (openAnim.get() < 0.9f) return true;
        float cx = (width - W) / 2f, cy = (height - H) / 2f;
        if (mx < cx || mx > cx + W || my < cy || my > cy + H) { onClose(); return true; }

        // Sidebar
        float sx = cx + 6, sy = cy + TOP_H + 8;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            float ty = sy + i * 36;
            if (mx >= sx && mx <= sx + SIDEBAR_W - 12 && my >= ty && my <= ty + 32) {
                selectedTab = i; expandedModule = null; scrollTarget = 0; cardScroll = 0;
                if (i == 3) { configSearch = ""; configSearchActive = false; configRenameTarget = null; configNewInputActive = false; }
                return true;
            }
        }

        // Bottom search
        float bsx = cx + SIDEBAR_W + 12, bsy = cy + H - BOT_H + 2;
        if (mx >= bsx && mx <= bsx + 140 && my >= bsy && my <= bsy + 16) { searchActive = true; cursorBlink = System.currentTimeMillis(); return true; }
        else searchActive = false;

        float ax = cx + SIDEBAR_W + 10, ay = cy + TOP_H + 6, aw = W - SIDEBAR_W - 20;
        if (selectedTab == 3) return handleConfigsClick(mx, my, ax, ay, aw);

        // Module cards
        float ah = H - TOP_H - BOT_H - 16;
        List<Module> mods = getTabModules();
        if (!searchQuery.isEmpty()) { String q = searchQuery.toLowerCase(); mods = mods.stream().filter(m -> m.getName().toLowerCase().contains(q)).collect(Collectors.toList()); }
        float itemY = ay + 2 + cardScroll;
        for (Module m : mods) {
            float ch = m == expandedModule ? 36 + m.getSettings().size() * 22 + 10 : 36;
            if (itemY + ch < ay || itemY > ay + ah) { itemY += ch + 4; continue; }
            if (mx >= ax && mx <= ax + aw && my >= itemY && my <= itemY + ch) {
                float rx = (float)mx - ax, ry = (float)my - itemY;
                if (rx >= aw - 34 - 14 && ry <= 36) { m.toggle(); return true; }
                if (ry <= 36) {
                    if (expandedModule == m) expandedModule = null;
                    else { lastExpanded = expandedModule; expandedModule = m; }
                    if (lastExpanded != null) cardExpand.get(lastExpanded).to(0f);
                    return true;
                }
                if (m == expandedModule && ry > 40) {
                    handleSettingClick(m, (float)mx, (float)my, ax, itemY + 40, aw);
                    return true;
                }
                return true;
            }
            itemY += ch + 4;
        }
        return super.mouseClicked(mx, my, btn);
    }

    private boolean handleConfigsClick(double mx, double my, float ax, float ay, float aw) {
        ConfigManager cm = ZenClient.getInstance().getConfigManager();
        List<ConfigData> all = cm.getConfigs();
        String q = configSearch.toLowerCase().trim();
        List<ConfigData> configs = q.isEmpty() ? all : all.stream().filter(c -> c.getName().toLowerCase().contains(q)).collect(Collectors.toList());

        // Close rename on outside click
        if (configRenameTarget != null) {
            boolean insideCard = false;
            float cardY = ay + 60, cardH = 62, cardGap = 5;
            for (int i = 0; i < configs.size(); i++) {
                float cyc = cardY + i * (cardH + cardGap);
                if (mx >= ax && mx <= ax + aw && my >= cyc && my <= cyc + cardH && configRenameTarget.equals(configs.get(i).getName())) { insideCard = true; break; }
            }
            if (!insideCard) { configRenameTarget = null; configRenameInput = ""; return true; }
        }

        // [+ New Config] button  (top-right)
        float nbw = 100, nbh = 20;
        float nbx = ax + aw - nbw, nby = ay + 2;
        if (mx >= nbx && mx <= nbx + nbw && my >= nby && my <= nby + nbh) {
            configNewInputActive = !configNewInputActive; configNewName = ""; configNewInputBlink = System.currentTimeMillis(); configRenameTarget = null; return true;
        }
        // Search click
        float sw = 150, sh = 20, sx = ax + 180, sy = ay + 2;
        if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) { configSearchActive = true; return true; }
        else configSearchActive = false;

        // Card actions
        float cardY = ay + 60 - 22 /* sepY offset */, cardH = 62, cardGap = 5;
        // Recalculate sepY
        float actualCardY = ay + ((configNewInputActive ? nby + nbh + 8 : nby + nbh + 6) + 6);
        // Actually, let me just use the stored hover state
        if (configHovered >= 0 && configHovered < configs.size()) {
            ConfigData cfg = configs.get(configHovered);
            if (configBtnHovered == 0) { // Load
                cm.loadConfig(cfg.getName()); toasts.add(new ToastMsg("Config '" + cfg.getName() + "' loaded")); return true;
            } else if (configBtnHovered == 1) { // Save
                cm.saveConfig(cfg.getName()); toasts.add(new ToastMsg("Config '" + cfg.getName() + "' saved")); return true;
            } else if (configBtnHovered == 2) { // Rename
                configRenameTarget = cfg.getName(); configRenameInput = cfg.getName(); configRenameBlink = System.currentTimeMillis(); configNewInputActive = false; return true;
            } else if (configBtnHovered == 3) { // Duplicate
                String dupName = cfg.getName() + "_copy";
                cm.duplicateConfig(cfg.getName(), dupName); toasts.add(new ToastMsg("Duplicated to '" + dupName + "'")); return true;
            } else if (configBtnHovered == 4) { // Delete
                cm.deleteConfig(cfg.getName()); toasts.add(new ToastMsg("Config '" + cfg.getName() + "' deleted")); return true;
            }
        }

        // Double-click card to load
        long now = System.currentTimeMillis();
        if (configHovered >= 0 && configHovered < configs.size()) {
            if (configHovered == configLastClickIdx && now - configLastClickTime < 400) {
                ConfigData cfg = configs.get(configHovered);
                cm.loadConfig(cfg.getName()); toasts.add(new ToastMsg("Config '" + cfg.getName() + "' loaded ✓")); configLastClickIdx = -1; return true;
            }
            configLastClickIdx = configHovered; configLastClickTime = now;
        } else { configLastClickIdx = -1; }

        return true;
    }

    private void handleSettingClick(Module m, float mx, float my, float cx, float sy, float aw) {
        List<Setting<?>> settings = m.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            float ry = sy + i * 22; Setting<?> s = settings.get(i);
            if (my < ry || my > ry + 20) continue;
            if (s instanceof BooleanSetting bs) bs.setValue(!bs.getValue());
            else if (s instanceof NumberSetting ns) {
                float sw = aw * 0.4f, sx = cx + aw - sw - 12;
                if (mx >= sx && mx <= sx + sw) {
                    float frac = (mx - sx) / sw;
                    float val = ns.getMin().floatValue() + frac * (ns.getMax().floatValue() - ns.getMin().floatValue());
                    val = Math.round(val / ns.getStep().floatValue()) * ns.getStep().floatValue();
                    ns.setValue(Math.max(ns.getMin().floatValue(), Math.min(ns.getMax().floatValue(), val)));
                }
            } else if (s instanceof ModeSetting ms) {
                String[] modes = ms.getModes(); int ci = -1;
                for (int j = 0; j < modes.length; j++) if (modes[j].equals(ms.getValue())) { ci = j; break; }
                ms.setValue(modes[(ci + 1) % modes.length]);
            } else if (s instanceof StringSetting || s instanceof PasswordSetting) {
                editingSetting = s;
                settingInput.setLength(0);
                if (s instanceof StringSetting string && string.getValue() != null) settingInput.append(string.getValue());
            } else if (s instanceof ActionSetting action) {
                action.invoke();
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (selectedTab != 3) scrollTarget += delta * 25;
        return true;
    }

    // ══════════════════════════════════════════════════════
    //  Keyboard
    // ══════════════════════════════════════════════════════
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (editingSetting != null) {
            if (key == 257 || key == 335) { commitSettingInput(); return true; }
            if (key == 256) { editingSetting = null; settingInput.setLength(0); return true; }
            if (key == 259 && !settingInput.isEmpty()) settingInput.deleteCharAt(settingInput.length() - 1);
            return true;
        }
        if (key == 256) { onClose(); return true; }

        // Rename: Enter/Esc
        if (selectedTab == 3 && configRenameTarget != null) {
            if (key == 257 || key == 335) { // Enter
                ConfigManager cm = ZenClient.getInstance().getConfigManager();
                if (!configRenameInput.trim().isEmpty() && !configRenameInput.equals(configRenameTarget)) {
                    cm.renameConfig(configRenameTarget, configRenameInput.trim()); toasts.add(new ToastMsg("Renamed to '" + configRenameInput.trim() + "'"));
                }
                configRenameTarget = null; configRenameInput = ""; return true;
            }
            if (key == 256) { configRenameTarget = null; configRenameInput = ""; return true; } // Esc
            if (key == 259 && !configRenameInput.isEmpty()) { configRenameInput = configRenameInput.substring(0, configRenameInput.length() - 1); configRenameBlink = System.currentTimeMillis(); return true; }
        }

        // New config input
        if (selectedTab == 3 && configNewInputActive) {
            if (key == 257 || key == 335) {
                if (!configNewName.trim().isEmpty()) {
                    ZenClient.getInstance().getConfigManager().saveConfig(configNewName.trim()); toasts.add(new ToastMsg("Created config '" + configNewName.trim() + "'"));
                    configNewName = "";
                }
                configNewInputActive = false; return true;
            }
            if (key == 256) { configNewInputActive = false; return true; }
            if (key == 259 && !configNewName.isEmpty()) { configNewName = configNewName.substring(0, configNewName.length() - 1); configNewInputBlink = System.currentTimeMillis(); return true; }
        }

        // Config search
        if (selectedTab == 3 && configSearchActive) {
            if (key == 259 && !configSearch.isEmpty()) { configSearch = configSearch.substring(0, configSearch.length() - 1); return true; }
        }

        // Module search
        if (searchActive && key == 259 && !searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); cursorBlink = System.currentTimeMillis(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (editingSetting != null && !Character.isISOControl(c) && settingInput.length() < 512) { settingInput.append(c); return true; }
        if (selectedTab == 3 && configRenameTarget != null && c >= 32 && c < 127 && configRenameInput.length() < 32) { configRenameInput += c; configRenameBlink = System.currentTimeMillis(); return true; }
        if (selectedTab == 3 && configNewInputActive && c >= 32 && c < 127 && configNewName.length() < 32) { configNewName += c; configNewInputBlink = System.currentTimeMillis(); return true; }
        if (selectedTab == 3 && configSearchActive && c >= 32 && c < 127 && configSearch.length() < 40) { configSearch += c; return true; }
        if (searchActive && c >= 32 && c < 127 && searchQuery.length() < 40) { searchQuery += c; cursorBlink = System.currentTimeMillis(); return true; }
        return super.charTyped(c, mods);
    }

    private void commitSettingInput() {
        if (editingSetting instanceof StringSetting string) {
            string.setValue(settingInput.toString());
        } else if (editingSetting instanceof PasswordSetting password) {
            password.clear();
            password.setValue(settingInput.toString().toCharArray());
        }
        editingSetting = null;
        settingInput.setLength(0);
    }

    @Override public void onClose() { if (ZenClient.isReady()) ZenClient.getInstance().getConfigManager().saveAll(); super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }

    // ══════════════════════════════════════════════════════
    private List<Module> getTabModules() {
        List<Module> out = new ArrayList<>();
        for (Category cat : TAB_CATS[selectedTab]) { List<Module> l = catMods.get(cat); if (l != null) out.addAll(l); }
        return out;
    }

    private static float ease(float t) { return (float)(1.0 - Math.pow(1.0 - t, 3.0)); }
    private static int alpha(int c, float a) { return ((int)((c >> 24 & 0xFF) * Math.max(0, Math.min(1, a)))) << 24 | c & 0xFFFFFF; }
    private static int lerpColor(int a, int b, float t) {
        return ((int)((a>>24&0xFF)+((b>>24&0xFF)-(a>>24&0xFF))*t))<<24
            | ((int)((a>>16&0xFF)+((b>>16&0xFF)-(a>>16&0xFF))*t))<<16
            | ((int)((a>>8&0xFF)+((b>>8&0xFF)-(a>>8&0xFF))*t))<<8
            | (int)((a&0xFF)+((b&0xFF)-(a&0xFF))*t);
    }
}
