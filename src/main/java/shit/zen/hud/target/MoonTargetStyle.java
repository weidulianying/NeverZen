package shit.zen.hud.target;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.modules.impl.render.NameProtect;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.Paint;
import shit.zen.render.Renderer;
import shit.zen.utils.animation.SmoothAnimationTimer;
import shit.zen.utils.math.Easings;
import shit.zen.utils.render.RenderUtil;

/**
 * Moon-style TargetHUD — player head on the left, name + health on the right,
 * health bar below the head, equipment icons below the name.
 * Smooth fade/slide/scale animations on target change and hurt.
 */
public class MoonTargetStyle
        extends TargetStyle {
    private static final Color COLOR_PANEL_BG    = new Color(0, 0, 0, 80);
    private static final Color COLOR_HEALTH_BG   = new Color(0, 0, 0, 100);
    private static final Color COLOR_HEALTH_BAR  = new Color(0, 190, 255);
    private static final Color COLOR_HEALTH_BAR2 = new Color(0, 230, 255);
    private static final Color COLOR_HEALTH_LAG  = new Color(99, 99, 99, 120);

    private final FontRenderer nameFont = FontPresets.pingfang(14.0f);
    private final FontRenderer subFont  = FontPresets.astaSans(13.0f);
    private final SmoothAnimationTimer scaleAnim   = new SmoothAnimationTimer();
    private final SmoothAnimationTimer fadeAnim    = new SmoothAnimationTimer();
    private final SmoothAnimationTimer slideAnim   = new SmoothAnimationTimer();
    private final SmoothAnimationTimer contentAnim = new SmoothAnimationTimer();

    private LivingEntity lastTarget;
    private int lastHurtTime;
    private final ItemStack[] equipmentSlots = new ItemStack[4];
    private final Paint panelPaint     = new Paint();
    private final Paint healthBgPaint  = new Paint();
    private final Paint healthLagPaint = new Paint();
    private boolean visible;
    private LivingEntity currentTarget;
    private long lastActiveTime;

    private static final String STYLE_NAME = "Moon";

    public MoonTargetStyle() {
        super(STYLE_NAME);
        this.scaleAnim.setCurrentValue(1.0);
        this.fadeAnim.setCurrentValue(0.0);
        this.slideAnim.setCurrentValue(5.0);
        this.contentAnim.setCurrentValue(0.0);
    }

    @Override
    public void render(Render2DEvent event, LivingEntity target, SmoothAnimationTimer healthAnim,
                       SmoothAnimationTimer healthLagAnim, float healthPct, float x, float y) {

        float panelW = 120f, panelH = 43f;
        boolean hasTarget = target != null;
        long now = System.currentTimeMillis();
        boolean targetChanged = false;

        if (hasTarget) {
            lastActiveTime = now;
            if (currentTarget != target) {
                currentTarget = target;
                lastTarget = target;
                targetChanged = true;
            }
        }

        boolean shouldShow = hasTarget || now - lastActiveTime < 300L;
        if (shouldShow != visible || (shouldShow && fadeAnim.getValueF() <= 0.01f)) {
            visible = shouldShow;
            if (visible) {
                fadeAnim.animate(1.0, 0.35, Easings.EASE_OUT_POW3);
                slideAnim.setCurrentValue(5.0); slideAnim.setStartTime(0L);
                contentAnim.setCurrentValue(0.0); contentAnim.setStartTime(0L);
                scaleAnim.setCurrentValue(1.0); scaleAnim.animate(1.0, 0.0);
            } else {
                fadeAnim.animate(0.0, 0.15, Easings.EASE_IN_POW3);
                slideAnim.animate(5.0, 0.15, Easings.EASE_IN_POW3);
                contentAnim.animate(0.0, 0.15, Easings.EASE_IN_POW3);
            }
        } else if (targetChanged && visible) {
            fadeAnim.animate(1.0, 0.35, Easings.EASE_OUT_POW3);
            slideAnim.setCurrentValue(5.0); slideAnim.setStartTime(0L);
            contentAnim.setCurrentValue(0.0); contentAnim.setStartTime(0L);
            scaleAnim.setCurrentValue(1.0); scaleAnim.animate(1.0, 0.0);
        }

        fadeAnim.tick();
        if (fadeAnim.isAnimating() && visible) {
            if (fadeAnim.getProgress() >= 0.08 && slideAnim.getStartTime() == 0L)
                slideAnim.animate(0.0, 0.3, Easings.EASE_OUT_POW3);
            if (fadeAnim.getProgress() >= 0.15 && contentAnim.getStartTime() == 0L)
                contentAnim.animate(1.0, 0.4, Easings.EASE_OUT_POW3);
        }
        if (slideAnim.getStartTime() != 0L) slideAnim.tick();
        if (contentAnim.getStartTime() != 0L) contentAnim.tick();

        float fade = fadeAnim.getValueF();
        if (fade <= 0.01f || currentTarget == null) return;

        // ── Hurt bounce ──
        if (hasTarget && target.hurtTime > lastHurtTime) {
            scaleAnim.setCurrentValue(0.7f);
            scaleAnim.animate(1.0, 1.5, Easings.EASE_OUT_ELASTIC);
        }
        if (hasTarget) lastHurtTime = target.hurtTime;
        scaleAnim.tick();

        float scaleVal = scaleAnim.getValueF();
        float combinedScale = scaleVal * Math.max(0.7f, fade);

        // ── Background blur + panel ──
        PoseStack ps = event.guiGraphics().pose();
        ps.pushPose();
        RenderUtil.drawBlurredRect(ps, x, y - 2f, panelW, panelH + 2f, 5f, 15f, 0.95f * fade, 0);
        ps.popPose();

        Renderer.renderConsumer(dc -> {
            panelPaint.setColor(new Color(0, 0, 0, (int) (COLOR_PANEL_BG.getAlpha() * fade)).getRGB());
            GlHelper.drawRoundedRect(x, y - 2f, panelW, panelH + 2f, 5f, panelPaint);

            // ── Player head ──
            float headSize = 30f * combinedScale;
            float headX = x + 4f + (30f - headSize) / 2f;
            float headY = y + (panelH - headSize) / 2f - 2f;
            if (currentTarget instanceof AbstractClientPlayer acp) {
                GlHelper.drawPlayerHeadRounded(acp, headX, headY, headSize, headSize, fade, 5f * combinedScale);
            }

            // ── Name + health ──
            float contentX = x + 4f + 30f + 4f;
            float contentW = panelW - (contentX - x) - 3f;
            float nameY = y + 5f;
            float slideOff = slideAnim.getValueF();
            String displayName = currentTarget == mc.player ? NameProtect.getProtectedName()
                    : currentTarget.getName().getString();
            GlHelper.drawTextShadowLegacy(displayName, contentX, nameY + 1f + slideOff, nameFont,
                    new Color(1f, 1f, 1f, fade).getRGB());

            int healthInt = (int) Math.ceil(currentTarget.getHealth());
            float healthTextW = GlHelper.getStringWidth(String.valueOf(healthInt), nameFont);
            GlHelper.drawTextShadowLegacy(String.valueOf(healthInt),
                    contentX + contentW - healthTextW - 2f, nameY + 1f + slideOff, nameFont,
                    new Color(0, 230, 255, (int) (255f * fade)).getRGB());

            // ── Health bar below head ──
            float barX = x + 4f, barW = panelW - 8f, barH = 4f;
            float barY = headY + headSize + 3f;
            healthBgPaint.setColor(new Color(0, 0, 0, (int) (COLOR_HEALTH_BG.getAlpha() * fade)).getRGB());
            GlHelper.drawRoundedRect(barX, barY, barW, barH, 2f, healthBgPaint);

            // Lag bar
            healthLagPaint.setColor(new Color(99, 99, 99, (int) (COLOR_HEALTH_LAG.getAlpha() * fade)).getRGB());
            float lagW = healthLagAnim.getValueF() * barW;
            GlHelper.drawRoundedRect(barX, barY, lagW, barH, 2f, healthLagPaint);

            // Health bar
            float contentVal = contentAnim.getValueF();
            float barFillW = healthAnim.getValueF() * barW * contentVal;
            Color barC1 = new Color(COLOR_HEALTH_BAR.getRed(), COLOR_HEALTH_BAR.getGreen(),
                    COLOR_HEALTH_BAR.getBlue(), (int) (255f * fade));
            Color barC2 = new Color(COLOR_HEALTH_BAR2.getRed(), COLOR_HEALTH_BAR2.getGreen(),
                    COLOR_HEALTH_BAR2.getBlue(), (int) (255f * fade));
            GlHelper.drawGradientRoundedRect(barX, barY, barFillW, barH, 2f, barC1, barC2);
        });

        // ── Equipment icons ──
        if (currentTarget != null) {
            equipmentSlots[0] = currentTarget.getItemBySlot(EquipmentSlot.HEAD);
            equipmentSlots[1] = currentTarget.getItemBySlot(EquipmentSlot.CHEST);
            equipmentSlots[2] = currentTarget.getItemBySlot(EquipmentSlot.LEGS);
            equipmentSlots[3] = currentTarget.getItemBySlot(EquipmentSlot.FEET);
        }
        float itemX = x + 4f + 30f + 3f;
        float itemScale = 0.8f, itemSize = 16f * itemScale, itemGap = 2f;
        float nameAscent = GlHelper.getFontAscent(nameFont);
        float itemY = y + 5f + nameAscent + 4f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (ItemStack stack : equipmentSlots) {
            if (stack != null && !stack.isEmpty()) {
                PoseStack itemPs = event.guiGraphics().pose();
                itemPs.pushPose();
                itemPs.translate(itemX, itemY, 0f);
                itemPs.scale(itemScale, itemScale, 1f);
                RenderSystem.setShaderColor(1f, 1f, 1f, fade);
                event.guiGraphics().renderItem(stack, 0, 0);
                itemPs.popPose();
            }
            itemX += itemSize + itemGap;
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
