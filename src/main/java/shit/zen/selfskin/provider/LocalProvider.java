package shit.zen.selfskin.provider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import shit.zen.selfskin.SkinData;

public final class LocalProvider implements SkinProvider {
    private final Path path;
    private final Path capePath;

    public LocalProvider(String path, String capePath) {
        this.path = Path.of(path).toAbsolutePath().normalize();
        this.capePath = capePath == null || capePath.isBlank()
                ? null : Path.of(capePath).toAbsolutePath().normalize();
    }

    @Override
    public SkinData getSkin(UUID uuid) {
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException("Skin file does not exist: " + path);
        if (capePath != null && !Files.isRegularFile(capePath)) {
            throw new IllegalArgumentException("Cape file does not exist: " + capePath);
        }
        return new SkinData(path.toUri(), capePath == null ? null : capePath.toUri(), "default");
    }
}
