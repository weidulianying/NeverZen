package shit.zen.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shit.zen.ZenClient;
import shit.zen.hud.HudElement;
import shit.zen.modules.Module;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.*;

/**
 * Serializes / deserializes the full client state (modules + settings) to JSON.
 * ConfigManager delegates all file I/O to this class — it never cares about JSON structure.
 *
 * JSON format per config file:
 * {
 *   "name": "pvp",
 *   "savedAt": 1234567890,
 *   "modules": { "KillAura": {"enabled": true, "key": 82}, ... },
 *   "settings": { "KillAura": {"Range": 3.2, "Mode": "Smart"}, ... },
 *   "hud": { "ModuleList": {"x": 100, "y": 200}, ... }
 * }
 */
public class ConfigSerializer {

    private static final Logger LOGGER = LogManager.getLogger("ConfigSerializer");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Write current client module state to a JSON file */
    public static void save(File file) {
        JsonObject root = new JsonObject();
        root.addProperty("name", file.getName().replace(".json", ""));
        root.addProperty("savedAt", System.currentTimeMillis());

        JsonObject modsJson = new JsonObject();
        JsonObject settingsJson = new JsonObject();
        JsonObject hudJson = new JsonObject();

        if (ZenClient.isReady()) {
            for (Module mod : ZenClient.getInstance().getModuleManager().getModules()) {
                // Module state
                JsonObject modObj = new JsonObject();
                modObj.addProperty("enabled", mod.isEnabled());
                modObj.addProperty("key", mod.getKey());
                modsJson.add(mod.getName(), modObj);

                // Settings
                JsonObject setObj = new JsonObject();
                for (Setting<?> s : mod.getSettings()) {
                    if (s instanceof BooleanSetting bs) setObj.addProperty(s.getName(), bs.getValue());
                    else if (s instanceof NumberSetting ns) setObj.addProperty(s.getName(), ns.getValue().floatValue());
                    else if (s instanceof ModeSetting ms) setObj.addProperty(s.getName(), ms.getValue());
                    else if (s instanceof MultiSelectSetting mss) {
                        JsonArray arr = new JsonArray();
                        for (String v : mss.getValue()) arr.add(v);
                        setObj.add(s.getName(), arr);
                    } else if (s instanceof StringSetting ss) setObj.addProperty(s.getName(), ss.getValue());
                }
                if (setObj.size() > 0) settingsJson.add(mod.getName(), setObj);

                // HUD positions
                if (mod instanceof HudElement hud) {
                    JsonObject pos = new JsonObject();
                    pos.addProperty("x", hud.getX());
                    pos.addProperty("y", hud.getY());
                    hudJson.add(mod.getName(), pos);
                }
            }
        }

        root.add("modules", modsJson);
        root.add("settings", settingsJson);
        root.add("hud", hudJson);

        try (Writer w = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        } catch (IOException e) {
            LOGGER.error("Failed to save config to {}", file, e);
        }
    }

    /** Load a JSON config file and apply all module states + settings */
    public static void load(File file) {
        if (!file.exists()) {
            LOGGER.error("Config file not found: {}", file);
            return;
        }
        try (Reader r = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;

            var mm = ZenClient.getInstance().getModuleManager();

            // Modules
            JsonObject modsJson = root.getAsJsonObject("modules");
            if (modsJson != null) {
                for (String name : modsJson.keySet()) {
                    Module mod = mm.getModule(name);
                    if (mod == null) continue;
                    JsonObject obj = modsJson.getAsJsonObject(name);
                    if (obj.has("enabled")) mod.setEnabled(obj.get("enabled").getAsBoolean());
                    if (obj.has("key")) mod.setKey(obj.get("key").getAsInt());
                }
            }

            // Settings
            JsonObject settingsJson = root.getAsJsonObject("settings");
            if (settingsJson != null) {
                for (String modName : settingsJson.keySet()) {
                    Module mod = mm.getModule(modName);
                    if (mod == null) continue;
                    JsonObject setObj = settingsJson.getAsJsonObject(modName);
                    for (Setting<?> s : mod.getSettings()) {
                        if (!setObj.has(s.getName())) continue;
                        JsonElement val = setObj.get(s.getName());
                        try {
                            if (s instanceof BooleanSetting bs) bs.setValue(val.getAsBoolean());
                            else if (s instanceof NumberSetting ns) ns.setValue(val.getAsFloat());
                            else if (s instanceof ModeSetting ms) ms.setValue(val.getAsString());
                            else if (s instanceof MultiSelectSetting mss) {
                                List<String> items = new ArrayList<>();
                                if (val.isJsonArray()) for (JsonElement e : val.getAsJsonArray()) items.add(e.getAsString());
                                mss.setValue(items);
                            } else if (s instanceof StringSetting ss) ss.setValue(val.getAsString());
                        } catch (Exception ignored) {}
                    }
                }
            }

            // HUD positions
            JsonObject hudJson = root.getAsJsonObject("hud");
            if (hudJson != null) {
                for (String modName : hudJson.keySet()) {
                    Module mod = mm.getModule(modName);
                    if (mod instanceof HudElement hud) {
                        JsonObject pos = hudJson.getAsJsonObject(modName);
                        if (pos.has("x")) hud.setX(pos.get("x").getAsFloat());
                        if (pos.has("y")) hud.setY(pos.get("y").getAsFloat());
                    }
                }
            }

            ZenClient.getInstance().getConfigManager().saveAll();
            LOGGER.info("Loaded config from {}", file.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to load config from {}", file, e);
        }
    }

    /** Update the name field inside a JSON config file (used after duplicate) */
    public static void updateName(File file, String newName) {
        if (!file.exists()) return;
        try (Reader r = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            root.addProperty("name", newName);
            root.addProperty("savedAt", System.currentTimeMillis());
            try (Writer w = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to update name in {}", file, e);
        }
    }
}
