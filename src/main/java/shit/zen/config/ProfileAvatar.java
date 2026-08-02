package shit.zen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shit.zen.ZenClient;

/** Stores the ClickGUI account name and its optional local avatar. */
public final class ProfileAvatar {
    private static final Logger LOGGER = LogManager.getLogger("ProfileAvatar");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PROFILE_FILE_NAME = "profile.json";
    public static final String AVATAR_FILE_NAME = "avatar.png";
    private static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.fromNamespaceAndPath("zen", "profile_avatar");

    private static String username = "";
    private static DynamicTexture texture;
    private static boolean textureLoaded;

    private ProfileAvatar() {}

    public static void load() {
        username = "";
        File profile = profileFile();
        if (profile.isFile()) {
            try (Reader reader = Files.newBufferedReader(profile.toPath(), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    if (json.has("username") && !json.get("username").isJsonNull()) {
                        username = json.get("username").getAsString().trim();
                    }
                }
            } catch (Exception exception) {
                LOGGER.warn("Failed to read profile configuration {}", profile, exception);
            }
        }
        textureLoaded = false;
        closeTexture();
        save();
    }

    public static void save() {
        File profile = profileFile();
        File parent = profile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOGGER.warn("Failed to create profile directory {}", parent);
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("username", username == null ? "" : username);
        json.addProperty("avatarFile", AVATAR_FILE_NAME);
        json.addProperty("avatarPathHint", new File(ZenClient.configDir, AVATAR_FILE_NAME).getAbsolutePath());
        try (Writer writer = Files.newBufferedWriter(profile.toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        } catch (IOException exception) {
            LOGGER.warn("Failed to save profile configuration {}", profile, exception);
        }
    }

    public static String username() {
        if (username != null && !username.isBlank()) return username;
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getUser() == null ? "" : minecraft.getUser().getName();
    }

    public static ResourceLocation avatarTexture() {
        if (textureLoaded) return texture == null ? null : TEXTURE_LOCATION;
        textureLoaded = true;
        File avatar = new File(ZenClient.configDir, AVATAR_FILE_NAME);
        if (!avatar.isFile()) return null;
        try (InputStream input = Files.newInputStream(avatar.toPath())) {
            NativeImage image = NativeImage.read(input);
            texture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_LOCATION, texture);
            return TEXTURE_LOCATION;
        } catch (Exception exception) {
            LOGGER.warn("Failed to load profile avatar {}", avatar, exception);
            closeTexture();
            return null;
        }
    }

    private static File profileFile() {
        return new File(ZenClient.configDir, PROFILE_FILE_NAME);
    }

    private static void closeTexture() {
        if (texture != null) {
            texture.close();
            texture = null;
        }
    }
}
