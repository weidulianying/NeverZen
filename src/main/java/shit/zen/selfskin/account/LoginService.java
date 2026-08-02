package shit.zen.selfskin.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import shit.zen.selfskin.provider.YggdrasilProvider;

/** Standard Yggdrasil authenticate/validate calls scoped to SelfSkin only. */
public final class LoginService {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();

    public Account login(String server, String username, char[] password) throws Exception {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is empty");
        if (password == null || password.length == 0) throw new IllegalArgumentException("Password is empty");
        String clientToken = UUID.randomUUID().toString();
        JsonObject agent = new JsonObject();
        agent.addProperty("name", "Minecraft");
        agent.addProperty("version", 1);
        JsonObject requestJson = new JsonObject();
        requestJson.add("agent", agent);
        requestJson.addProperty("username", username.trim());
        requestJson.addProperty("password", new String(password));
        requestJson.addProperty("clientToken", clientToken);
        requestJson.addProperty("requestUser", true);
        JsonObject response = post(server, "authserver/authenticate", requestJson);
        JsonObject selected = response.getAsJsonObject("selectedProfile");
        if (selected == null) {
            JsonArray available = response.getAsJsonArray("availableProfiles");
            if (available != null && !available.isEmpty()) selected = available.get(0).getAsJsonObject();
        }
        if (selected == null) throw new IllegalStateException("Account has no selected Minecraft profile");
        return new Account(server.trim(), selected.has("name") ? selected.get("name").getAsString() : username.trim(),
                YggdrasilProvider.parseUuid(selected.get("id").getAsString()),
                response.get("accessToken").getAsString(),
                response.has("clientToken") ? response.get("clientToken").getAsString() : clientToken);
    }

    public boolean validate(Account account) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("accessToken", account.accessToken());
            json.addProperty("clientToken", account.clientToken());
            URI uri = endpoint(account.server(), "authserver/validate");
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8)).build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 204;
        } catch (Exception ignored) {
            return false;
        }
    }

    private JsonObject post(String server, String path, JsonObject json) throws Exception {
        URI uri = endpoint(server, path);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            String message = "Login failed (HTTP " + response.statusCode() + ")";
            try {
                JsonObject error = JsonParser.parseString(response.body()).getAsJsonObject();
                if (error.has("errorMessage")) message = error.get("errorMessage").getAsString();
            } catch (Exception ignored) { }
            throw new IllegalStateException(message);
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static URI endpoint(String server, String path) {
        URI root = YggdrasilProvider.checkedHttpUri(server);
        String value = root.toString();
        if (!value.endsWith("/")) value += "/";
        return URI.create(value).resolve(path);
    }
}
