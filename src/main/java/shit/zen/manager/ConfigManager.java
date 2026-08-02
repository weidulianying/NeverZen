package shit.zen.manager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shit.zen.ZenClient;
import shit.zen.config.Config;
import shit.zen.config.ConfigData;
import shit.zen.config.ConfigSerializer;
import shit.zen.config.ModulesConfig;
import shit.zen.config.ValuesConfig;

public class ConfigManager {
    public static final Logger LOGGER = LogManager.getLogger("ConfigManager");
    public static final File CONFIG_DIR = new File(ZenClient.configDir, "configs");
    public static final File CLICK_GUI_CONFIG_DIR = new File(CONFIG_DIR, "config");
    public static final File PROFILES_DIR = new File(CONFIG_DIR, "profiles");
    private static final File ACTIVE_FILE = new File(CONFIG_DIR, "active.txt");
    private final List<Config> configs;
    private String activeProfile;

    public ConfigManager() {
        this.configs = new ArrayList<>();
        if (!CONFIG_DIR.exists() && CONFIG_DIR.mkdir()) {
            LOGGER.info("Created config directory");
        }
        if (!PROFILES_DIR.exists() && PROFILES_DIR.mkdir()) {
            LOGGER.info("Created profiles directory");
        }
        if (!CLICK_GUI_CONFIG_DIR.exists() && !CLICK_GUI_CONFIG_DIR.mkdirs()) {
            LOGGER.error("Failed to create ClickGUI config directory: {}", CLICK_GUI_CONFIG_DIR);
        }
        this.configs.add(new ModulesConfig());
        this.configs.add(new ValuesConfig());

        // Restore last active profile
        this.activeProfile = readActiveProfile();
        if (this.activeProfile != null) {
            LOGGER.info("Active profile: {}", this.activeProfile);
        }
    }

    private String readActiveProfile() {
        if (!ACTIVE_FILE.exists()) return null;
        try {
            List<String> lines = Files.readAllLines(ACTIVE_FILE.toPath(), StandardCharsets.UTF_8);
            if (!lines.isEmpty()) {
                String name = lines.get(0).trim();
                if (!name.isEmpty()) return name;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read active profile", e);
        }
        return null;
    }

    private void writeActiveProfile(String name) {
        try {
            Files.write(ACTIVE_FILE.toPath(), name.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("Failed to write active profile", e);
        }
    }

    private void clearActiveProfile() {
        if (ACTIVE_FILE.exists()) {
            ACTIVE_FILE.delete();
        }
    }

    private void setConfigBaseDir(File dir) {
        for (Config config : this.configs) {
            config.setFile(new File(dir, config.getName()));
        }
    }

    public void loadAll() {
        // Route to profile directory if active, otherwise use default
        if (this.activeProfile != null) {
            setConfigBaseDir(new File(PROFILES_DIR, this.activeProfile));
        } else {
            setConfigBaseDir(CONFIG_DIR);
        }

        for (Config config : this.configs) {
            try {
                File file = config.getFile();
                if (file.exists()) {
                    readConfigFile(config, file);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load config " + config.getName(), e);
            }
        }
    }

    private void readConfigFile(Config config, File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            config.read(reader);
        }
    }

    public void saveAll() {
        // Route to profile directory if active, otherwise use default
        if (this.activeProfile != null) {
            File profileDir = new File(PROFILES_DIR, this.activeProfile);
            if (!profileDir.exists() && !profileDir.mkdirs()) {
                LOGGER.error("Failed to create profile directory: {}", profileDir);
                return;
            }
            setConfigBaseDir(profileDir);
        } else {
            setConfigBaseDir(CONFIG_DIR);
        }

        for (Config config : this.configs) {
            this.saveConfig(config);
        }
        LOGGER.info("Saved all configs" + (this.activeProfile != null ? " to profile: " + this.activeProfile : ""));
    }

    private void saveConfig(Config config) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(config.getFile().toPath()), StandardCharsets.UTF_8))) {
            config.save(writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save config " + config.getName(), e);
        }
    }

    // --- Profile management ---

    public String getActiveProfile() {
        return this.activeProfile;
    }

    public List<String> listProfiles() {
        if (!PROFILES_DIR.exists()) return new ArrayList<>();
        File[] dirs = PROFILES_DIR.listFiles(File::isDirectory);
        if (dirs == null) return new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (File dir : dirs) {
            names.add(dir.getName());
        }
        return names;
    }

    public void saveProfile(String name) {
        this.activeProfile = name;
        writeActiveProfile(name);
        saveAll();
        LOGGER.info("Saved profile: {}", name);
    }

    public void loadProfile(String name) {
        File profileDir = new File(PROFILES_DIR, name);
        if (!profileDir.exists() || !profileDir.isDirectory()) {
            LOGGER.error("Profile does not exist: {}", name);
            return;
        }
        this.activeProfile = name;
        writeActiveProfile(name);
        loadAll();
        LOGGER.info("Loaded profile: {}", name);
    }

    public void deleteProfile(String name) {
        File profileDir = new File(PROFILES_DIR, name);
        if (!profileDir.exists()) {
            LOGGER.error("Profile does not exist: {}", name);
            return;
        }
        deleteDir(profileDir);

        // If deleted profile was active, reset to defaults
        if (name.equals(this.activeProfile)) {
            this.activeProfile = null;
            clearActiveProfile();
            loadAll();
        }
        LOGGER.info("Deleted profile: {}", name);
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDir(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    // ── JSON Config management (uses ConfigSerializer) ──────

    /**
     * List all saved JSON configs as ConfigData objects.
     * GUI calls this — it never touches File I/O.
     */
    public List<ConfigData> getConfigs() {
        List<ConfigData> list = new ArrayList<>();
        File[] files = CLICK_GUI_CONFIG_DIR.listFiles((d, n) -> n.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                list.add(new ConfigData(f.getName().replace(".json", ""), f));
            }
        }
        list.sort((a, b) -> Long.compare(b.getCreateTime(), a.getCreateTime()));
        return list;
    }

    /** Save current state as a named JSON config */
    public void saveConfig(String name) {
        File file = new File(CLICK_GUI_CONFIG_DIR, name + ".json");
        ConfigSerializer.save(file);
        LOGGER.info("Saved config: {}", name);
    }

    /** Load a JSON config and apply all module states */
    public void loadConfig(String name) {
        File file = new File(CLICK_GUI_CONFIG_DIR, name + ".json");
        ConfigSerializer.load(file);
        // Also save to current active profile
        saveAll();
    }

    /** Delete a JSON config file */
    public void deleteConfig(String name) {
        File file = new File(CLICK_GUI_CONFIG_DIR, name + ".json");
        if (file.exists()) file.delete();
        LOGGER.info("Deleted config: {}", name);
    }

    /** Rename an existing config */
    public boolean renameConfig(String oldName, String newName) {
        File oldFile = new File(CLICK_GUI_CONFIG_DIR, oldName + ".json");
        File newFile = new File(CLICK_GUI_CONFIG_DIR, newName + ".json");
        if (!oldFile.exists() || newFile.exists()) return false;
        boolean ok = oldFile.renameTo(newFile);
        if (ok) LOGGER.info("Renamed config: {} → {}", oldName, newName);
        return ok;
    }

    /** Duplicate an existing config under a new name */
    public void duplicateConfig(String sourceName, String newName) {
        File src = new File(CLICK_GUI_CONFIG_DIR, sourceName + ".json");
        File dst = new File(CLICK_GUI_CONFIG_DIR, newName + ".json");
        if (!src.exists()) return;
        try {
            java.nio.file.Files.copy(src.toPath(), dst.toPath());
            // Update the name field inside the JSON
            ConfigSerializer.updateName(dst, newName);
            LOGGER.info("Duplicated config: {} → {}", sourceName, newName);
        } catch (java.io.IOException e) {
            LOGGER.error("Failed to duplicate config: {}", sourceName, e);
        }
    }
}
