package shit.zen.gui;

import java.awt.Color;
import shit.zen.ClientBase;
import shit.zen.ZenClient;
import shit.zen.event.impl.GlRenderEvent;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.Paint;
import shit.zen.event.EventTarget;

public class IntroAnimation
extends ClientBase {
    private static volatile boolean isActive = false;
    private long startTime = -1L;
    private boolean finished = false;

    // Neverlose accent color: #4A7DFF
    private static final Color ACCENT_COLOR = new Color(74, 125, 255);
    private static final Color ZEN_COLOR = Color.WHITE;
    private static final long NEVER_APPEAR_DURATION = 1100L;
    private static final long NEVER_MOVE_START = 1300L;
    private static final long NEVER_MOVE_DURATION = 900L;
    private static final long ZEN_APPEAR_START = NEVER_MOVE_START + NEVER_MOVE_DURATION;
    private static final long ZEN_APPEAR_DURATION = 850L;
    private static final long HOLD_DURATION = 900L;
    private static final long FADE_OUT_DURATION = 600L;

    public IntroAnimation() {
        isActive = true;
    }

    public static boolean isRunning() {
        return isActive;
    }

    @EventTarget(value=4)
    public void onRender(GlRenderEvent glRenderEvent) {
        if (this.finished) return;
        if (this.startTime < 0L) this.startTime = System.currentTimeMillis();

        long elapsed = System.currentTimeMillis() - this.startTime;
        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float screenHeight = mc.getWindow().getGuiScaledHeight();
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f;

        long fadeOutStart = ZEN_APPEAR_START + ZEN_APPEAR_DURATION + HOLD_DURATION;

        // Background alpha
        float bgAlpha;
        if (elapsed <= 500L) {
            bgAlpha = 0.75f * easeOutCubic(clamp01((float)elapsed / 500.0f));
        } else if (elapsed <= fadeOutStart) {
            bgAlpha = 0.75f;
        } else if (elapsed <= fadeOutStart + FADE_OUT_DURATION) {
            bgAlpha = 0.75f * (1.0f - easeInCubic(
                    clamp01((float)(elapsed - fadeOutStart) / FADE_OUT_DURATION)));
        } else {
            this.finish();
            return;
        }

        Paint paint = GlHelper.toPaint(new Color(8, 8, 12, (int)(bgAlpha * 255.0f)));
        GlHelper.drawRect(0.0f, 0.0f, screenWidth, screenHeight, paint);

        float finalFade = 1.0f;
        if (elapsed > fadeOutStart) {
            finalFade = 1.0f - easeInCubic(
                    clamp01((float)(elapsed - fadeOutStart) / FADE_OUT_DURATION));
        }

        FontRenderer titleFont = FontPresets.museoSans(52.0f);
        String neverText = "Never";
        String zenText = "Zen";
        float neverWidth = GlHelper.getStringWidth(neverText, titleFont);
        float zenWidth = GlHelper.getStringWidth(zenText, titleFont);
        float centeredNeverX = centerX - neverWidth / 2.0f;
        float combinedNeverX = centerX - (neverWidth + zenWidth) / 2.0f;
        float textY = centerY - titleFont.getMetrics().capHeight() / 2.0f;

        // Never first fades in at the exact centre, then glides left to make
        // room for Zen. Smootherstep keeps both ends of the move at rest.
        float neverAppearProgress = smootherStep(clamp01(
                (float)elapsed / NEVER_APPEAR_DURATION));
        float neverMoveProgress = smootherStep(clamp01(
                (float)(elapsed - NEVER_MOVE_START) / NEVER_MOVE_DURATION));
        float neverX = lerp(centeredNeverX, combinedNeverX, neverMoveProgress);
        float neverY = lerp(textY + 24.0f, textY, neverAppearProgress);
        int neverColor = new Color(
                ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), ACCENT_COLOR.getBlue(),
                (int)(clamp01(neverAppearProgress * finalFade) * 255.0f)
        ).getRGB();
        GlHelper.drawText(neverText, neverX, neverY, titleFont, neverColor);

        // Once Never has settled, Zen rises from below and locks to its right.
        float zenProgress = smootherStep(clamp01(
                (float)(elapsed - ZEN_APPEAR_START) / ZEN_APPEAR_DURATION));
        float zenX = combinedNeverX + neverWidth;
        float zenY = lerp(textY + 24.0f, textY, zenProgress);
        int zenColor = new Color(
                ZEN_COLOR.getRed(), ZEN_COLOR.getGreen(), ZEN_COLOR.getBlue(),
                (int)(clamp01(zenProgress * finalFade) * 255.0f)
        ).getRGB();
        GlHelper.drawText(zenText, zenX, zenY, titleFont, zenColor);
    }

    private void finish() {
        if (!this.finished) {
            this.finished = true;
            try {
                ZenClient.instance.getEventBus().unregister(this);
            } catch (Throwable throwable) {
                // empty catch block
            }
            isActive = false;
        }
    }

    private static float clamp01(float value) {
        return value < 0.0f ? 0.0f : (value > 1.0f ? 1.0f : value);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static float easeOutCubic(float t) {
        float clamped = IntroAnimation.clamp01(t);
        clamped = (float)(1.0 - Math.pow(1.0f - clamped, 3.0));
        return clamped;
    }

    private static float easeInCubic(float t) {
        float clamped = IntroAnimation.clamp01(t);
        clamped = clamped * clamped * clamped;
        return clamped;
    }

    private static float smootherStep(float t) {
        float clamped = clamp01(t);
        return clamped * clamped * clamped
                * (clamped * (clamped * 6.0f - 15.0f) + 10.0f);
    }
}
