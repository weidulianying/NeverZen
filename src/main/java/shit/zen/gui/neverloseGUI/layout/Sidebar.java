package shit.zen.gui.neverloseGUI.layout;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shit.zen.gui.neverloseGUI.design.Colors;
import shit.zen.gui.neverloseGUI.design.Typography;
import shit.zen.gui.neverloseGUI.render.Render2D;
import shit.zen.config.ProfileAvatar;
import shit.zen.modules.Category;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.utils.misc.Assets;
import shit.zen.utils.render.RenderUtil;

/** Neverlose-style branded navigation rail. */
public class Sidebar {

    private static final Logger LOGGER = LogManager.getLogger("NeverloseSidebar");
    private static final String LOGO_ASSET =
        "/assets/zen/textures/gui/neverlose/nz.png";
    private static final ResourceLocation LOGO_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("zen", "runtime/neverlose_logo");
    private static boolean logoLoadAttempted;
    private static DynamicTexture logoTexture;
    private static final float BRAND_HEIGHT = 64f;
    public static final String[] TABS = {"Combat", "Movement", "Player", "Visual", "World", "Misc", "Configs"};
    public static final Category[] CATS = {Category.COMBAT, Category.MOVEMENT, Category.PLAYER, Category.RENDER, null, Category.MISC, null};
    private static final String[] ICONS = {"\uE074", "\uE511", "\uE7FD", "\uE8F4", "\uE894", "\uE24D", "\uE2C7"};

    private final float[] rowY = new float[TABS.length];
    private int selected;
    private float x, y, w, h, rowHeight = 34f;

    public int selected() { return selected; }

    public void render(PoseStack ps, GuiGraphics g, float ox, float oy, float sw, float sh,
                       int mx, int my, float a, int cur) {
        x = ox; y = oy; w = sw; h = sh; selected = cur;

        Render2D.drawRoundRect(ps, x, y, w, h, 16f, Render2D.alpha(Colors.SIDEBAR, a));
        Render2D.drawRect(ps, x + w - 1, y, 1, h, Render2D.alpha(Colors.BORDER, a * 0.65f));

        // Brand block
        Render2D.drawRect(ps, x, y + 16, w - 1, BRAND_HEIGHT - 16,
            Render2D.alpha(Colors.SIDEBAR, a));
        float logoX = x + (w < 140 ? 12 : 15);
        float brandX = x + (w < 140 ? 54 : 63);
        if (loadLogoTexture()) {
            // Draw the texture object we just uploaded. Looking it up again by
            // ResourceLocation can return Minecraft's missing-texture object on
            // the injected class-loader path even after registration.
            RenderUtil.drawTexture(logoTexture.getId(), ps, logoX, y + 22,
                20, 20, a, 0xFFFFFFFF);
        }
        GlHelper.drawText(fit("NeverZen", Typography.H2, x + w - brandX - 8), brandX, y + 17,
            Typography.H2, Render2D.alpha(Colors.TEXT_PRIMARY, a));
        if (w >= 150) GlHelper.drawText("Minecraft", brandX, y + 36, Typography.TINY,
            Render2D.alpha(Colors.TEXT_DISABLED, a));
        Render2D.drawRect(ps, x + 14, y + BRAND_HEIGHT, w - 28, 1,
            Render2D.alpha(Colors.BORDER, a * 0.5f));

        float firstY = y + 96;
        float accountY = y + h - 44;
        float accountSeparatorY = accountY - 12;
        float navBottom = accountSeparatorY - 10;
        float rowH = Math.min(34f, Math.max(18f, (navBottom - firstY - 44f) / 7f));
        rowHeight = rowH;
        GlHelper.drawText("MODULES", x + 18, y + 77, Typography.TINY, Render2D.alpha(Colors.TEXT_DISABLED, a * 0.85f));
        for (int i = 0; i < 6; i++) {
            rowY[i] = firstY + i * (rowH + 2);
            drawRow(ps, mx, my, a, i, rowY[i], rowH);
        }

        float clientLabelY = firstY + 6 * (rowH + 2) + 14;
        GlHelper.drawText("CLIENT", x + 18, clientLabelY, Typography.TINY, Render2D.alpha(Colors.TEXT_DISABLED, a * 0.85f));
        rowY[6] = clientLabelY + 18;
        drawRow(ps, mx, my, a, 6, rowY[6], rowH);

        // Account block is fixed to the bottom of the rail.
        Render2D.drawRect(ps, x + 14, accountSeparatorY, w - 28, 1, Render2D.alpha(Colors.BORDER, a * 0.5f));
        ResourceLocation avatar = ProfileAvatar.avatarTexture();
        if (avatar != null) {
            Render2D.drawCircle(ps, x + 31, accountY + 18, 12, Render2D.alpha(Colors.CARD, a));
            GlHelper.drawTextureRounded(avatar, x + 19, accountY + 6, 24, 24, a, 6f);
        }
        String username = ProfileAvatar.username();
        GlHelper.drawText(fit(username, Typography.BODY, w - 78), x + 50, accountY + 7,
            Typography.BODY, Render2D.alpha(Colors.TEXT_PRIMARY, a));
        GlHelper.drawText("NeverZen user", x + 50, accountY + 27, Typography.TINY, Render2D.alpha(Colors.TEXT_DISABLED, a));
        GlHelper.drawText("\uE5CC", x + w - 28, accountY + 12, Typography.ICON, Render2D.alpha(Colors.TEXT_SECONDARY, a));
    }

    private void drawRow(PoseStack ps, int mx, int my, float a, int index, float ry, float rowH) {
        boolean sel = index == selected;
        boolean hov = Render2D.contains(x + 10, ry, w - 20, rowH, mx, my);
        if (sel || hov) {
            int bg = sel ? Colors.NAV_SELECTED : Colors.CARD;
            Render2D.drawRoundRect(ps, x + 10, ry, w - 20, rowH, 7f, Render2D.alpha(bg, a * (sel ? 0.95f : 0.55f)));
        }
        int iconColor = sel ? Colors.ACCENT : (hov ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY);
        int textColor = sel || hov ? Colors.TEXT_PRIMARY : Colors.TEXT_SECONDARY;
        FontRenderer navFont = w < 150 ? Typography.SMALL : Typography.BODY;
        float iconY = ry + (rowH - Typography.ICON.getMetrics().capHeight()) / 2f;
        float textY = ry + (rowH - navFont.getMetrics().capHeight()) / 2f;
        GlHelper.drawText(ICONS[index], x + 20, iconY, Typography.ICON, Render2D.alpha(iconColor, a));
        GlHelper.drawText(fit(TABS[index], navFont, w - 58), x + 48, textY, navFont,
            Render2D.alpha(textColor, a));
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return false;
        for (int i = 0; i < TABS.length; i++) {
            if (Render2D.contains(x + 10, rowY[i], w - 20, rowHeight, (float) mx, (float) my)) {
                selected = i;
                return true;
            }
        }
        return false;
    }

    private static String fit(String value, FontRenderer font, float maxWidth) {
        if (maxWidth <= 4) return "";
        if (GlHelper.getStringWidth(value, font) <= maxWidth) return value;
        String out = value;
        while (out.length() > 1 && GlHelper.getStringWidth(out + "...", font) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "...";
    }

    /**
     * Registers the bundled logo explicitly instead of relying on Minecraft's
     * resource-pack lookup. On the EXE injection path classes are defined onto
     * the game class loader and non-class entries are extracted separately, so
     * a plain ResourceLocation resolves to Minecraft's missing texture.
     */
    private static boolean loadLogoTexture() {
        if (logoTexture != null) return true;
        if (logoLoadAttempted) return false;
        logoLoadAttempted = true;

        try (InputStream input = Assets.open(LOGO_ASSET)) {
            if (input == null) {
                LOGGER.warn("Neverlose logo asset is missing: {}", LOGO_ASSET);
                return false;
            }
            NativeImage image = NativeImage.read(input);
            logoTexture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(LOGO_TEXTURE, logoTexture);
            return true;
        } catch (Exception exception) {
            LOGGER.warn("Failed to load Neverlose logo {}", LOGO_ASSET, exception);
            if (logoTexture != null) {
                logoTexture.close();
                logoTexture = null;
            }
            return false;
        }
    }
}
