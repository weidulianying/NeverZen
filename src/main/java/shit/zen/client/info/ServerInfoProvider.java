package shit.zen.client.info;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/** Extracts the current server name — singleplayer, multiplayer, or menu. */
public final class ServerInfoProvider {
    private ServerInfoProvider() {}

    public static String getServerName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return "Menu";

        // Integrated server (singleplayer world loaded)
        if (mc.hasSingleplayerServer() || mc.isSingleplayer()) return "Local World";

        // Multiplayer server
        ServerData sd = mc.getCurrentServer();
        if (sd != null) return formatServer(sd.ip);

        // Legacy / realms fallback
        if (mc.getConnection() != null && !mc.isSingleplayer()) return "Multiplayer";

        return "Menu";
    }

    private static String formatServer(String ip) {
        if (ip == null || ip.isEmpty()) return "Server";
        // Strip port
        if (ip.contains(":")) return ip.split(":")[0];
        return ip;
    }
}
