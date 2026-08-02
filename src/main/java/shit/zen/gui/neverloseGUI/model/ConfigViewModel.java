package shit.zen.gui.neverloseGUI.model;

import java.util.List;
import java.util.stream.Collectors;
import shit.zen.ZenClient;
import shit.zen.config.ConfigData;
import shit.zen.manager.ConfigManager;

/** UI-facing adapter for the config system. */
public class ConfigViewModel {
    private final ConfigManager cm = ZenClient.getInstance().getConfigManager();

    public List<ConfigData> getConfigs() { return cm.getConfigs(); }
    public List<ConfigData> search(String q) {
        return q.isEmpty() ? getConfigs() : getConfigs().stream().filter(c -> c.getName().toLowerCase().contains(q.toLowerCase())).collect(Collectors.toList());
    }
    public void load(String name) { cm.loadConfig(name); }
    public void save(String name) { cm.saveConfig(name); }
    public void rename(String old, String n) { cm.renameConfig(old, n); }
    public void duplicate(String name) { cm.duplicateConfig(name, name + "_copy"); }
    public void delete(String name) { cm.deleteConfig(name); }
}
