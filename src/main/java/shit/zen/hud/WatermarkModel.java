package shit.zen.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import shit.zen.client.info.ServerInfoProvider;
import shit.zen.config.ProfileAvatar;

/** Data model for the watermark — one update per frame max. */
public class WatermarkModel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private String username = "";
    private String server = "";
    private int fps, ping;
    private String time = "";
    private float smoothFps;
    private long lastTick;

    public void tick(Minecraft mc) {
        if (mc == null) return;
        long now = System.currentTimeMillis();
        if (now - lastTick < 200) return; // 5 Hz
        lastTick = now;

        // Keep the watermark identity in sync with ~/.zen/profile.json, just like
        // the ClickGUI account footer. ProfileAvatar provides the Minecraft name
        // as a safe fallback when the profile is absent or has no username.
        username = ProfileAvatar.username();

        // FPS with smooth lerp
        float raw = mc.getFps();
        smoothFps += (raw - smoothFps) * 0.35f;
        fps = Math.round(smoothFps);

        // Ping
        ping = 0;
        if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo pi = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (pi != null) ping = pi.getLatency();
        }

        // Server
        server = ServerInfoProvider.getServerName();

        time = LocalTime.now().format(TIME_FMT);
    }

    public int fps() { return fps; }
    public int ping() { return ping; }
    public String server() { return server; }
    public String username() { return username; }
    public String time() { return time; }
}
