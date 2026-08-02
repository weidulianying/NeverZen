package shit.zen.selfskin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/** Small on-disk PNG cache for SelfSkin. */
public final class SkinCache {
    private static final int MAX_SKIN_BYTES = 8 * 1024 * 1024;
    private static final long MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L;
    private final Path directory;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    public SkinCache(Path gameDirectory) {
        this.directory = gameDirectory.resolve("NeverZen").resolve("skin-cache");
    }

    public byte[] load(URI uri, boolean forceReload) throws Exception {
        if ("file".equalsIgnoreCase(uri.getScheme())) return Files.readAllBytes(Path.of(uri));
        Files.createDirectories(directory);
        Path cached = directory.resolve(hash(uri.toString()) + ".png");
        if (!forceReload && Files.isRegularFile(cached)
                && System.currentTimeMillis() - Files.getLastModifiedTime(cached).toMillis() < MAX_AGE_MILLIS) {
            byte[] bytes = Files.readAllBytes(cached);
            validatePng(bytes);
            return bytes;
        }
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            response.body().close();
            throw new IOException("Skin download failed (HTTP " + response.statusCode() + ")");
        }
        byte[] bytes;
        try (InputStream input = response.body()) {
            bytes = input.readNBytes(MAX_SKIN_BYTES + 1);
        }
        if (bytes.length > MAX_SKIN_BYTES) throw new IOException("Skin image is too large");
        validatePng(bytes);
        Path temporary = cached.resolveSibling(cached.getFileName() + ".tmp");
        Files.write(temporary, bytes);
        Files.move(temporary, cached, StandardCopyOption.REPLACE_EXISTING);
        return bytes;
    }

    private static void validatePng(byte[] bytes) throws IOException {
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length < signature.length) throw new IOException("Skin is not a PNG image");
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) throw new IOException("Skin is not a PNG image");
        }
    }

    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
