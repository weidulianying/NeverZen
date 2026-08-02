package shit.zen.selfskin.provider;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class MojangProvider extends YggdrasilProvider {
    public MojangProvider() {
        super("https://sessionserver.mojang.com/");
    }

    @Override
    protected URI resolve(String relative) {
        if (relative.startsWith("sessionserver/session/")) {
            relative = relative.substring("sessionserver/".length());
        }
        return super.resolve(relative);
    }

    @Override
    public UUID resolveUuid(String username) throws Exception {
        URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/"
                + URLEncoder.encode(username.trim(), StandardCharsets.UTF_8));
        JsonObject profile = getJson(uri);
        if (!profile.has("id")) throw new IllegalStateException("No Mojang profile for " + username);
        return parseUuid(profile.get("id").getAsString());
    }
}
