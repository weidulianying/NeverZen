package shit.zen.selfskin.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import shit.zen.selfskin.SkinData;

/** Minimal Yggdrasil profile resolver; it does not replace Minecraft's session service. */
public class YggdrasilProvider implements SkinProvider {
    private static final int MAX_PROFILE_BYTES = 1024 * 1024;
    protected final HttpClient client;
    protected final URI apiRoot;

    public YggdrasilProvider(String apiRoot) {
        this.apiRoot = checkedHttpUri(apiRoot);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public SkinData getSkin(UUID uuid) throws Exception {
        URI endpoint = resolve("sessionserver/session/minecraft/profile/" + undashed(uuid) + "?unsigned=false");
        JsonObject profile = getJson(endpoint);
        JsonArray properties = profile.getAsJsonArray("properties");
        if (properties == null) throw new IllegalStateException("Yggdrasil profile has no properties");
        for (JsonElement element : properties) {
            JsonObject property = element.getAsJsonObject();
            if (!"textures".equals(property.get("name").getAsString())) continue;
            String encoded = property.get("value").getAsString();
            JsonObject payload = JsonParser.parseString(
                    new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject textures = payload.getAsJsonObject("textures");
            JsonObject skin = textures == null ? null : textures.getAsJsonObject("SKIN");
            if (skin == null || !skin.has("url")) throw new IllegalStateException("Profile has no skin texture");
            JsonObject cape = textures.getAsJsonObject("CAPE");
            URI capeUri = cape != null && cape.has("url")
                    ? checkedHttpUri(cape.get("url").getAsString()) : null;
            String model = "default";
            JsonObject metadata = skin.getAsJsonObject("metadata");
            if (metadata != null && metadata.has("model")) model = metadata.get("model").getAsString();
            return new SkinData(checkedHttpUri(skin.get("url").getAsString()), capeUri, model);
        }
        throw new IllegalStateException("Yggdrasil profile has no textures property");
    }

    /** Resolves a third-party username through the standard Yggdrasil API server. */
    public UUID resolveUuid(String username) throws Exception {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is empty");
        String body = "[\"" + escapeJson(username.trim()) + "\"]";
        URI endpoint = resolve("api/profiles/minecraft");
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        String response = sendText(request);
        JsonArray profiles = JsonParser.parseString(response).getAsJsonArray();
        if (profiles.isEmpty()) throw new IllegalStateException("No Yggdrasil profile for " + username);
        return parseUuid(profiles.get(0).getAsJsonObject().get("id").getAsString());
    }

    protected JsonObject getJson(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).GET().build();
        return JsonParser.parseString(sendText(request)).getAsJsonObject();
    }

    protected String sendText(HttpRequest request) throws Exception {
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " from " + request.uri());
        }
        if (response.body().length > MAX_PROFILE_BYTES) throw new IllegalStateException("Yggdrasil response is too large");
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    protected URI resolve(String relative) {
        String root = apiRoot.toString();
        if (!root.endsWith("/")) root += "/";
        return URI.create(root).resolve(relative);
    }

    public static URI checkedHttpUri(String value) {
        URI uri = URI.create(value == null ? "" : value.trim());
        String scheme = uri.getScheme();
        if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
            throw new IllegalArgumentException("Only absolute HTTP(S) URLs are supported");
        }
        return uri;
    }

    public static UUID parseUuid(String value) {
        String clean = value.replace("-", "");
        if (clean.length() != 32) throw new IllegalArgumentException("Invalid UUID");
        return UUID.fromString(clean.substring(0, 8) + "-" + clean.substring(8, 12) + "-"
                + clean.substring(12, 16) + "-" + clean.substring(16, 20) + "-" + clean.substring(20));
    }

    protected static String undashed(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
