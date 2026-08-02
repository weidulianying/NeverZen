package shit.zen.hud;

import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import shit.zen.ClientBase;
import shit.zen.event.impl.GlRenderEvent;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.render.DrawContext;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.Paint;
import shit.zen.ui.neverlose.NeverloseTheme;

public class NeverloseWatermark {
    private final FontRenderer boldFont = FontPresets.productSans(16f);
    private final FontRenderer regularFont = FontPresets.productSans(12f);
    private final FontRenderer tinyFont = FontPresets.materialIcons(15f);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final float logoWidth = 5f;
    private final Paint backgroundPaint = new Paint().setColor(NeverloseTheme.BG_PANEL);
    private final Paint textPaint = new Paint().setColor(NeverloseTheme.TEXT);
    private final Paint accentPaint = new Paint().setColor(NeverloseTheme.ACCENT);

    public void onRender2D(Render2DEvent e) {
        if (ClientBase.mc.options.renderDebug) return;
        // uses the GlRender path for actual drawing
    }

    public void onGlRender(GlRenderEvent e) {
        if (ClientBase.mc.options.renderDebug) return;
        float screenW = ClientBase.mc.getWindow().getGuiScaledWidth();
        float x = screenW / 2f - getTotalWidth() / 2f, y = 10f, gap = 6f, r = 4.5f;
        DrawContext dc = e.drawContext();
        x = renderSection(dc, x, y, "ZEN", boldFont, r, gap);
        x = renderSectionWithSub(dc, x, y, getServerName(), regularFont, "", r, gap);
        x = renderSectionWithSub(dc, x, y, getPingText(), regularFont, "", r, gap);
        x = renderSectionWithSub(dc, x, y, getFpsText(), regularFont, "", r, gap);
        x = renderSectionWithSub(dc, x, y, getTimeText(), regularFont, "", r, gap);
        renderSectionWithSub(dc, x, y, getCoordText(), regularFont, "", r, gap);
    }

    private float renderSection(DrawContext dc, float x, float y, String text, FontRenderer font, float r, float gap) {
        float pad = 8f, bh = GlHelper.getFontAscent(boldFont) + 10f;
        float tw = GlHelper.getStringWidth(text, font), bw = pad + tw + pad - 5f;
        GlHelper.drawRoundedRect(x, y, bw, bh, r, backgroundPaint);
        GlHelper.drawTextShadowLegacy(text, x + pad - 2f, y + (bh - GlHelper.getFontAscent(font)) / 2f + 3.5f, font, textPaint.getColor());
        return x + bw + gap;
    }

    private float renderSectionWithSub(DrawContext dc, float x, float y, String text, FontRenderer font, String icon, float r, float gap) {
        float pad = 8f, bh = GlHelper.getFontAscent(font) + 12f;
        float tw = GlHelper.getStringWidth(text, font), iw = GlHelper.getStringWidth(icon, tinyFont);
        float bw = pad + iw + 5f + tw + pad - 4f;
        GlHelper.drawRoundedRect(x, y, bw, bh, r, backgroundPaint);
        GlHelper.drawTextShadowLegacy(icon, x + pad - 1f, y + (bh - GlHelper.getFontAscent(tinyFont)) / 2f + 3f, tinyFont, accentPaint.getColor());
        GlHelper.drawTextShadowLegacy(text, x + pad + iw + 5f - 3f, y + (bh - GlHelper.getFontAscent(font)) / 2f + 1f, font, textPaint.getColor());
        return x + bw + gap;
    }

    private float measureText(String text, FontRenderer font) { float pad = 8f; return pad + GlHelper.getStringWidth(text, font) + pad - 5f; }
    private float getTotalWidth() {
        float t = 0, g = 6f; t += measureText("ZEN", boldFont) + g;
        t += measureText(getServerName(), regularFont) + g; t += measureText(getPingText(), regularFont) + g;
        t += measureText(getFpsText(), regularFont) + g; t += measureText(getTimeText(), regularFont) + g;
        return t += measureText(getCoordText(), regularFont);
    }

    private String getServerName() { return ClientBase.mc.player != null ? ClientBase.mc.player.getGameProfile().getName() : "Player"; }
    private String getPingText() { return "Default Config"; }
    private String getFpsText() {
        if (ClientBase.mc.player == null || ClientBase.mc.player.connection == null) return "0ms";
        PlayerInfo pi = ClientBase.mc.player.connection.getPlayerInfo(ClientBase.mc.player.getUUID());
        return pi != null ? pi.getLatency() + "ms" : "0ms";
    }
    private String getTimeText() { return ClientBase.mc.getFps() + "fps"; }
    private String getCoordText() {
        ServerData sd = ClientBase.mc.getCurrentServer();
        return sd != null ? sd.ip : "Singleplayer";
    }
}
