package shit.zen.selfskin;

import java.net.URI;

/** Resolved player textures and the model hint supplied by the skin server. */
public record SkinData(URI textureUri, URI capeUri, String model) {
    public SkinData {
        if (textureUri == null) throw new IllegalArgumentException("textureUri");
        model = "slim".equalsIgnoreCase(model) ? "slim" : "default";
    }

    public SkinData(URI textureUri, String model) {
        this(textureUri, null, model);
    }
}
