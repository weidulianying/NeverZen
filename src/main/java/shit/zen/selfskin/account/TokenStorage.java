package shit.zen.selfskin.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class TokenStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;

    public TokenStorage(Path gameDirectory) {
        this.file = gameDirectory.resolve("NeverZen").resolve("account.json");
    }

    public Account load() throws IOException {
        if (!Files.isRegularFile(file)) return null;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!json.has("server") || !json.has("username") || !json.has("uuid")
                    || !json.has("accessToken")) return null;
            return new Account(json.get("server").getAsString(), json.get("username").getAsString(),
                    UUID.fromString(json.get("uuid").getAsString()), json.get("accessToken").getAsString(),
                    json.has("clientToken") ? json.get("clientToken").getAsString() : "");
        }
    }

    public void save(Account account) throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject json = new JsonObject();
        json.addProperty("server", account.server());
        json.addProperty("username", account.username());
        json.addProperty("uuid", account.uuid().toString());
        json.addProperty("accessToken", account.accessToken());
        json.addProperty("clientToken", account.clientToken());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
