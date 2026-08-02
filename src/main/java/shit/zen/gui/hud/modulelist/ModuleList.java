package shit.zen.gui.hud.modulelist;

import java.util.List;
import shit.zen.ZenClient;
import shit.zen.hud.HudElement;
import shit.zen.modules.impl.render.Interface;
import shit.zen.render.DrawContext;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.Paint;
import shit.zen.render.RoundedRectangle;
import shit.zen.ui.neverlose.NeverloseTheme;
import shit.zen.utils.render.ColorUtil;

/** Draws a borderless, top-left module list with rainbow character colors. */
public final class ModuleList {
    private static final FontRenderer WATERMARK_FONT = FontPresets.productSans(17.0f);
    private static final FontRenderer MODULE_FONT = FontPresets.productSans(15.0f);
    private static final FontRenderer MODE_FONT = FontPresets.productSans(12.0f);
    private static final float PADDING_X = 6.0f;
    private static final float PADDING_TOP = 5.0f;
    private static final float PADDING_BOTTOM = 5.0f;
    private static final float HEADER_GAP = 4.0f;
    private static final float MODE_GAP = 6.0f;
    private static final float ROW_HEIGHT = 11.5f;
    private static final float SLIDE_DISTANCE = 24.0f;
    private static final int MODE_COLOR = 0xFFD0D2D8;
    private static final int GLASS_TOP = 0xB51B1E26;
    private static final int GLASS_BOTTOM = 0xC50D0F14;
    private static final int GLASS_GLOW = 0x38577FFF;
    private static final int GLASS_HIGHLIGHT = 0x46577FFF;

    private final ModuleAnimation animation = new ModuleAnimation();

    public Size render(DrawContext context, float x, float y) {
        List<ModuleEntry> entries = this.entries();
        this.animation.sync(entries);

        float headerHeight = WATERMARK_FONT.getMetrics().getLineHeight();
        float maxNameWidth = entries.stream().map(ModuleEntry::nameWidth).max(Float::compare).orElse(0.0f);
        float maxModeWidth = entries.stream().map(ModuleEntry::modeWidth).max(Float::compare).orElse(0.0f);
        float titleWidth = WATERMARK_FONT.getWidth("NeverZen");
        float contentWidth = Math.max(titleWidth,
            maxNameWidth + (maxModeWidth > 0.0f ? MODE_GAP + maxModeWidth : 0.0f));
        float panelWidth = contentWidth + PADDING_X * 2.0f;
        float panelHeight = PADDING_TOP + headerHeight + HEADER_GAP
            + entries.size() * ROW_HEIGHT + PADDING_BOTTOM;
        float textX = x + PADDING_X;
        float titleY = y + PADDING_TOP;
        float contentY = titleY + headerHeight + HEADER_GAP;
        float modeX = textX + maxNameWidth + (maxModeWidth > 0.0f ? MODE_GAP : 0.0f);

        try (Paint paint = new Paint()) {
            drawGlassPanel(context, paint, x, y, panelWidth, panelHeight);

            drawShadowed(context, paint, "Never", textX, titleY,
                WATERMARK_FONT, NeverloseTheme.ACCENT);
            drawShadowed(context, paint, "Zen", textX + WATERMARK_FONT.getWidth("Never"), titleY,
                WATERMARK_FONT, NeverloseTheme.TEXT);

            for (int row = 0; row < entries.size(); row++) {
                ModuleEntry entry = entries.get(row);
                float progress = this.animation.progress(entry.module());
                float rowX = textX + (1.0f - progress) * SLIDE_DISTANCE;
                float rowY = contentY + row * ROW_HEIGHT;
                drawRainbowName(context, paint, entry.name(), rowX, rowY, progress);

                if (!entry.mode().isEmpty()) {
                    int modeColor = withAlpha(MODE_COLOR, progress);
                    drawShadowed(context, paint, entry.mode(), modeX + (1.0f - progress) * SLIDE_DISTANCE,
                        rowY + 1.5f, MODE_FONT, modeColor);
                }
            }
        }

        return new Size(panelWidth, panelHeight);
    }

    private static void drawGlassPanel(DrawContext context, Paint paint, float x, float y,
                                       float width, float height) {
        RoundedRectangle panel = RoundedRectangle.ofXYWHR(x, y, width, height, NeverloseTheme.RADIUS);

        // Soft blue-black bloom behind the translucent panel creates the frosted-glass depth.
        context.drawBlurredRoundedRect(panel, 0.0f, 1.0f, NeverloseTheme.BLUR, 1.0f, GLASS_GLOW);
        paint.setGradCoords(new Paint.GradientCoords(x, y, x, y + height, GLASS_TOP, GLASS_BOTTOM));
        context.drawRoundedRect(panel, paint);
        paint.setGradCoords(null);

        // A restrained top reflection and accent edge match the Neverlose panel language.
        paint.setColor(0x24FFFFFF);
        context.drawRoundedRect(RoundedRectangle.ofXYWHR(x + 1.0f, y + 1.0f,
            width - 2.0f, 1.0f, 0.5f), paint);
        paint.setColor(GLASS_HIGHLIGHT);
        context.drawRoundedRect(RoundedRectangle.ofXYWHR(x, y + NeverloseTheme.RADIUS,
            1.0f, Math.max(0.0f, height - NeverloseTheme.RADIUS * 2.0f), 0.5f), paint);
    }

    private List<ModuleEntry> entries() {
        return ZenClient.getInstance().getModuleManager().getModules().stream()
            .filter(module -> module.isEnabled() && !module.getName().isEmpty())
            .filter(module -> !(module instanceof HudElement) && !(module instanceof Interface))
            .map(module -> ModuleEntry.of(module, MODULE_FONT, MODE_FONT))
            .sorted(ModuleEntry.byDescendingNameWidth())
            .toList();
    }

    private static void drawRainbowName(DrawContext context, Paint paint, String text,
                                         float x, float y, float alpha) {
        float cursorX = x;
        long now = System.currentTimeMillis();
        for (int index = 0; index < text.length(); index++) {
            String character = String.valueOf(text.charAt(index));
            int color = withAlpha(ColorUtil.rainbow(now + index * 100L), alpha);
            drawShadowed(context, paint, character, cursorX, y, MODULE_FONT, color);
            cursorX += MODULE_FONT.getWidth(character);
            if (index + 1 < text.length()) {
                cursorX += ModuleEntry.letterSpacing();
            }
        }
    }

    private static void drawShadowed(DrawContext context, Paint paint, String text, float x, float y,
                                     FontRenderer font, int color) {
        int alpha = color >>> 24;
        paint.setColor(alpha << 24);
        context.drawString(text, x + 0.5f, y + 0.5f, font, paint);
        paint.setColor(color);
        context.drawString(text, x, y, font, paint);
    }

    private static int withAlpha(int color, float alpha) {
        int scaledAlpha = Math.round((color >>> 24) * Math.max(0.0f, Math.min(1.0f, alpha)));
        return scaledAlpha << 24 | color & 0xFFFFFF;
    }

    public record Size(float width, float height) {
    }
}
