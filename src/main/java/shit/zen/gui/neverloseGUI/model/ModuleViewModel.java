package shit.zen.gui.neverloseGUI.model;

import java.util.ArrayList;
import java.util.List;
import shit.zen.modules.Module;
import shit.zen.settings.Setting;

/** UI-facing adapter for a Module — GUI never touches Module directly. */
public class ModuleViewModel {
    private final Module module;
    private final List<SettingViewModel> settings = new ArrayList<>();

    public ModuleViewModel(Module m) {
        this.module = m;
        for (Setting<?> s : m.getSettings()) settings.add(new SettingViewModel(s));
    }

    public String name() { return module.getName(); }
    public boolean isEnabled() { return module.isEnabled(); }
    public void setEnabled(boolean v) { module.setEnabled(v); }
    public List<SettingViewModel> settings() { return settings; }
    public int keyCode() { return module.getKey(); }
    public void setKeyCode(int k) { module.setKey(k); }
    public Module module() { return module; }
}
