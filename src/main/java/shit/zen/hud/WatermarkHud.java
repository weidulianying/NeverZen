package shit.zen.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.Mth;
import shit.zen.ClientBase;
import shit.zen.modules.impl.movement.Scaffold;
import shit.zen.render.DrawContext;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.Paint;
import shit.zen.ui.neverlose.NeverloseTheme;

public class WatermarkHud extends ClientBase implements IHudElement {
    private static final FontRenderer logoFont = FontPresets.zenIcon(36f);
    private static final FontRenderer subFont = FontPresets.poppinsMedium(12f);
    private static final int primaryColor = NeverloseTheme.TEXT_MUTED;
    private static final int accentColor = NeverloseTheme.ACCENT;
    private int lastTick = -1;
    private float maxSubWidth, line1Width, line2Width;
    private String line1Text, line2Text;

    private void updateCache() {
        if (mc == null || mc.player == null || lastTick == mc.player.tickCount) return;
        lastTick = mc.player.tickCount;
        String[] info = getServerInfo();
        line1Text = info[0]; line2Text = info[1];
        line1Width = subFont.getWidth(line1Text); line2Width = subFont.getWidth(line2Text);
        maxSubWidth = Math.max(line1Width, line2Width);
    }

    @Override public boolean hasBackground() { return true; }
    @Override public void renderGui(GuiGraphics g, PoseStack ps, float x, float y, float w, float h, float a) {}

    @Override
    public void render(DrawContext dc, float x, float y, float w, float h, float alpha) {
        if (mc == null || mc.player == null || alpha <= 0.01f) return;
        updateCache();
        float lw = logoFont.getWidth("Z"), sepW = subFont.getWidth("|");
        float betaW = subFont.getWidth("beta"), b1W = subFont.getWidth("b1");
        float sw = Math.max(betaW, b1W);
        float bw = lw + sepW * 2f + sw + 48f, tw = bw + maxSubWidth;
        float dx = x + (w - tw) / 2f - 1f, cy = y + h / 2f + 1f;
        int textColor = colorWithAlpha(NeverloseTheme.TEXT, alpha);
        int subColor = colorWithAlpha(primaryColor, alpha);
        int shadow = colorWithAlpha(new Color(0,0,0,100).getRGB(), alpha);
        try (Paint paint = new Paint()) {
            drawText(dc, paint, "Z", dx, cy + 4f, logoFont, logoFont.getMetrics().capHeight(), colorWithAlpha(accentColor, alpha), shadow, true);
            dx += lw + 12f;
            drawText(dc, paint, "|", (dx += 12f) - 13f, cy, subFont, subFont.getMetrics().capHeight(), subColor, shadow, true);
            float bx = (dx += sepW + 12f) + (sw - betaW) / 2f - 13f;
            drawText(dc, paint, "beta", bx, cy - 2f, subFont, 0, textColor, shadow, false);
            drawText(dc, paint, "b1", dx + (sw - b1W) / 2f - 13f, cy + 7f, subFont, 0, subColor, shadow, false);
            dx += sw;
            drawText(dc, paint, "|", (dx += 12f) - 13f, cy, subFont, subFont.getMetrics().capHeight(), subColor, shadow, true);
            drawText(dc, paint, line1Text, (dx += sepW + 12f) + (maxSubWidth - line1Width) / 2f - 13f, cy - 2f, subFont, 0, textColor, shadow, false);
            drawText(dc, paint, line2Text, dx + (maxSubWidth - line2Width) / 2f - 13f, cy + 7f, subFont, 0, subColor, shadow, false);
        }
    }

    private void drawText(DrawContext dc, Paint paint, String text, float x, float y, FontRenderer font, float lh, int color, int shadow, boolean cv) {
        float dy = cv ? y + lh / 2f : y;
        paint.setColor(shadow); dc.drawString(text, x + 0.5f, dy + 0.5f, font, paint);
        paint.setColor(color); dc.drawString(text, x, dy, font, paint);
    }

    @Override public IHudElement.Size getHudAlignment() { updateCache(); float lw = logoFont.getWidth("Z"), sepW = subFont.getWidth("|"), sw = Math.max(subFont.getWidth("beta"),subFont.getWidth("b1")); return new IHudElement.Size(lw+sepW*2f+sw+48f+maxSubWidth, 25f); }
    @Override public boolean isVisible() { return Scaffold.INSTANCE == null || !Scaffold.INSTANCE.isEnabled(); }

    private String[] getServerInfo() {
        if (mc.isSingleplayer()) return new String[]{"Singleplayer","1ms"};
        ServerData sd = mc.getCurrentServer();
        String ip = sd != null ? sd.ip : "Multiplayer";
        int ping = 0;
        if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo pi = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (pi != null) ping = pi.getLatency();
        }
        return new String[]{ip, Mth.clamp(ping,0,9999)+"ms"};
    }
}
