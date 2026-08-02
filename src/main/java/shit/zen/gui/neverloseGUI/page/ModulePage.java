package shit.zen.gui.neverloseGUI.page;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ZenClient;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.framework.Page;
import shit.zen.gui.neverloseGUI.model.ModuleViewModel;
import shit.zen.gui.neverloseGUI.popup.ModuleSettingsBlock;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.gui.neverloseGUI.widget.Toggle;
import shit.zen.modules.Category;
import shit.zen.modules.Module;
import shit.zen.render.GlHelper;

/** Category function area. Left-click toggles; right-click opens the settings block. */
public class ModulePage extends Page {
    private static final Category[][] TABS = {
        {Category.COMBAT}, {Category.MOVEMENT}, {Category.PLAYER},
        {Category.RENDER}, {Category.EXPLOIT, Category.WORLD}, {Category.MISC}
    };
    private static final String[] TITLES = {"COMBAT", "MOVEMENT", "PLAYER", "VISUAL", "WORLD", "MISC"};
    private static final float ROW_H = 38f;
    private static final float GAP = 8f;

    private final int tabIdx;
    private final List<FeatureRow> rows = new ArrayList<>();
    private boolean loaded;
    private float scrollSmooth, scrollTarget;
    private ModuleSettingsBlock settingsBlock;
    private Module focusedModule;
    private long focusedAt;

    public ModulePage(int tabIdx) { this.tabIdx = tabIdx; }

    private void ensureRows() {
        if (loaded || !ZenClient.isReady()) return;
        List<Module> modules = new ArrayList<>();
        for (Module module : ZenClient.getInstance().getModuleManager().getModules()) {
            for (Category category : TABS[tabIdx]) {
                if (module.getCategory() == category) { modules.add(module); break; }
            }
        }
        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        rows.clear();
        for (Module module : modules) rows.add(new FeatureRow(new ModuleViewModel(module)));
        loaded = true;
    }

    @Override public void onShow() { super.onShow(); ensureRows(); }

    @Override
    public void render(PoseStack ps, GuiGraphics g, int mx, int my, float alpha) {
        ensureRows();
        if (settingsBlock != null && settingsBlock.isClosed()) settingsBlock = null;
        GlHelper.drawText(TITLES[tabIdx], x + 2, y + 2, Typography.TINY,
            Render2D.alpha(Colors.TEXT_DISABLED, alpha));
        GlHelper.drawText(rows.size() + " functions", x + w - 76, y + 2, Typography.TINY,
            Render2D.alpha(Colors.TEXT_DISABLED, alpha));

        float listY = y + 20;
        float listH = h - 20;
        boolean twoColumns = w >= 430;
        float columnGap = 12;
        float columnW = twoColumns ? (w - columnGap) / 2f : w;
        int rowCount = twoColumns ? (rows.size() + 1) / 2 : rows.size();
        float contentH = rowCount * (ROW_H + GAP) - (rows.isEmpty() ? 0 : GAP);
        scrollTarget = Math.max(-Math.max(0, contentH - listH), Math.min(0, scrollTarget));
        scrollSmooth += (scrollTarget - scrollSmooth) * 0.16f;

        for (int i = 0; i < rows.size(); i++) {
            int column = twoColumns ? i % 2 : 0;
            int row = twoColumns ? i / 2 : i;
            float rowX = x + (column == 1 ? columnW + columnGap : 0);
            float rowY = listY + row * (ROW_H + GAP) + scrollSmooth;
            rows.get(i).setBounds(rowX, rowY, columnW);
        }

        Render2D.pushScissor((int) x, (int) listY, (int) w, (int) listH);
        for (FeatureRow row : rows) {
            if (row.y + ROW_H < listY || row.y > listY + listH) continue;
            boolean focused = row.vm.module() == focusedModule && System.currentTimeMillis() - focusedAt < 1600L;
            row.render(g, mx, my, alpha, focused);
        }
        Render2D.popScissor();

        if (settingsBlock != null) {
            FeatureRow anchor = findRow(settingsBlock.module());
            if (anchor != null) settingsBlock.setAnchor(anchor.x, anchor.y, anchor.w);
            settingsBlock.render(g, mx, my, alpha);
        }
    }

    private FeatureRow findRow(ModuleViewModel module) {
        for (FeatureRow row : rows) if (row.vm.module() == module.module()) return row;
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (settingsBlock != null) {
            if (settingsBlock.mouseClicked(mx, my, btn)) return true;
            settingsBlock.close();
            if (btn != 1) return true;
        }
        if (!Render2D.contains(x, y + 20, w, h - 20, (float) mx, (float) my)) return false;
        for (FeatureRow row : rows) {
            if (!row.contains(mx, my)) continue;
            if (btn == 1 || (btn == 0 && !row.toggle.contains(mx, my) && !row.vm.settings().isEmpty())) {
                settingsBlock = new ModuleSettingsBlock(row.vm, row.x, row.y, row.w);
                return true;
            }
            if (btn == 0) return row.mouseClicked(mx, my, btn);
        }
        return false;
    }

    @Override public void mouseReleased(double mx, double my, int btn) {
        if (settingsBlock != null) settingsBlock.mouseReleased(mx, my, btn);
        for (FeatureRow row : rows) row.toggle.mouseReleased(mx, my, btn);
    }

    @Override public void mouseDragged(double mx, double my) {
        if (settingsBlock != null) settingsBlock.mouseDragged(mx, my);
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (settingsBlock != null && settingsBlock.mouseScrolled(mx, my, delta)) return true;
        if (!Render2D.contains(x, y + 20, w, h - 20, (float) mx, (float) my)) return false;
        scrollTarget += (float) delta * 26;
        return true;
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (settingsBlock != null) {
            if (settingsBlock.keyPressed(key, scan, mods)) return true;
            if (key == 256) { settingsBlock.close(); return true; }
        }
        return false;
    }

    @Override public boolean charTyped(char c, int mods) {
        return settingsBlock != null && settingsBlock.charTyped(c, mods);
    }

    @Override public void closePopups() { if (settingsBlock != null) settingsBlock.close(); }

    public void focusModule(Module module) {
        ensureRows();
        int index = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).vm.module() == module) { index = i; break; }
        }
        if (index < 0) return;
        boolean twoColumns = w >= 430;
        int logicalRow = twoColumns ? index / 2 : index;
        int rowCount = twoColumns ? (rows.size() + 1) / 2 : rows.size();
        float listH = Math.max(1, h - 20);
        float contentH = rowCount * (ROW_H + GAP) - (rows.isEmpty() ? 0 : GAP);
        float maxScroll = Math.max(0, contentH - listH);
        float target = (listH - ROW_H) / 2f - logicalRow * (ROW_H + GAP);
        scrollTarget = Math.max(-maxScroll, Math.min(0, target));
        scrollSmooth = scrollTarget;
        settingsBlock = null;
        focusedModule = module;
        focusedAt = System.currentTimeMillis();
    }

    private static final class FeatureRow {
        final ModuleViewModel vm;
        final Toggle toggle;
        float x, y, w;

        FeatureRow(ModuleViewModel vm) {
            this.vm = vm;
            this.toggle = new Toggle(0, 0, vm.isEnabled(), vm::setEnabled);
        }

        void setBounds(float x, float y, float w) {
            this.x = x; this.y = y; this.w = w;
            toggle.setPos(x + w - 44, y + 10);
        }

        boolean contains(double mx, double my) {
            return Render2D.contains(x, y, w, ROW_H, (float) mx, (float) my);
        }

        void render(GuiGraphics g, int mx, int my, float alpha, boolean focused) {
            PoseStack ps = g.pose();
            boolean hover = contains(mx, my);
            if (focused) {
                Render2D.drawRoundRect(ps, x, y, w, ROW_H, 8f, Render2D.alpha(Colors.ACCENT, alpha));
                Render2D.drawRoundRect(ps, x + 1, y + 1, w - 2, ROW_H - 2, 7f,
                    Render2D.alpha(Colors.CARD, alpha));
            } else {
                Render2D.drawRoundRect(ps, x, y, w, ROW_H, 8f,
                    Render2D.alpha(hover ? Colors.CARD : Colors.PANEL, alpha));
            }
            int text = vm.isEnabled() ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY;
            GlHelper.drawText(fit(vm.name(), w - 92), x + 12, y + 13, Typography.BODY,
                Render2D.alpha(text, alpha));
            if (!vm.settings().isEmpty()) GlHelper.drawText("...", x + w - 68, y + 10,
                Typography.BODY, Render2D.alpha(Colors.TEXT_DISABLED, alpha));
            toggle.setOn(vm.isEnabled());
            toggle.render(g, mx, my, alpha);
        }

        boolean mouseClicked(double mx, double my, int btn) {
            if (toggle.contains(mx, my)) return toggle.mouseClicked(mx, my, btn);
            if (btn == 0) { vm.setEnabled(!vm.isEnabled()); return true; }
            return false;
        }

        private static String fit(String value, float maxWidth) {
            if (GlHelper.getStringWidth(value, Typography.BODY) <= maxWidth) return value;
            String out = value;
            while (out.length() > 1 && GlHelper.getStringWidth(out + "...", Typography.BODY) > maxWidth) {
                out = out.substring(0, out.length() - 1);
            }
            return out + "...";
        }
    }
}
