package shit.zen.gui.neverloseGUI.model;

import java.util.ArrayList;
import java.util.List;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.*;

/** UI-facing adapter for a Setting — exposes type-safe accessors. */
public class SettingViewModel {
    private final Setting<?> setting;

    public SettingViewModel(Setting<?> s) { this.setting = s; }

    public String name() { return setting.getName(); }
    public Setting<?> raw() { return setting; }

    public boolean isBoolean() { return setting instanceof BooleanSetting; }
    public boolean isNumber()  { return setting instanceof NumberSetting; }
    public boolean isMode()    { return setting instanceof ModeSetting; }
    public boolean isMulti()   { return setting instanceof MultiSelectSetting; }
    public boolean isString()  { return setting instanceof StringSetting; }
    public boolean isPassword(){ return setting instanceof PasswordSetting; }
    public boolean isAction()  { return setting instanceof ActionSetting; }
    public boolean isVisible() { return setting.getVisibility() == null || setting.getVisibility().displayable(); }

    public boolean getBoolean() { return ((BooleanSetting) setting).getValue(); }
    public void setBoolean(boolean v) { ((BooleanSetting) setting).setValue(v); }

    public float getNumber() { return ((NumberSetting) setting).getValue().floatValue(); }
    public void setNumber(float v) { ((NumberSetting) setting).setValue(v); }
    public float getMin() { return ((NumberSetting) setting).getMin().floatValue(); }
    public float getMax() { return ((NumberSetting) setting).getMax().floatValue(); }
    public float getStep() { return ((NumberSetting) setting).getStep().floatValue(); }

    public String[] getModes() { return ((ModeSetting) setting).getModes(); }
    public String getMode() { String v = ((ModeSetting) setting).getValue(); return v != null ? v : getModes()[0]; }
    public void setMode(String v) { ((ModeSetting) setting).setValue(v); }

    public String[] getMultiOptions() {
        return ((MultiSelectSetting) setting).getOptions().toArray(new String[0]);
    }
    public List<String> getMultiSelected() { return List.copyOf(((MultiSelectSetting) setting).getValue()); }
    public void toggleMulti(String option) {
        MultiSelectSetting multi = (MultiSelectSetting) setting;
        List<String> selected = new ArrayList<>(multi.getValue());
        if (selected.contains(option)) selected.remove(option); else selected.add(option);
        multi.setValue(selected);
    }

    public String getText() { return ((StringSetting) setting).getValue(); }
    public void setText(String value) { ((StringSetting) setting).setValue(value); }
    public int passwordLength() { return ((PasswordSetting) setting).getValue().length; }
    public void setPassword(String value) {
        PasswordSetting password = (PasswordSetting) setting;
        password.clear();
        password.setValue(value.toCharArray());
    }
    public void invoke() { ((ActionSetting) setting).invoke(); }
}
