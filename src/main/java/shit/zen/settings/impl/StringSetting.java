package shit.zen.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import shit.zen.settings.Setting;
import shit.zen.settings.SettingVisibility;

/** A persisted text value used by settings that accept a path, URL, or UUID. */
public class StringSetting extends Setting<String> {
    public StringSetting(String name, String value) {
        super(name, value);
    }

    public StringSetting withVisibility(SettingVisibility visibility) {
        this.setVisibility(visibility);
        return this;
    }

    @Override
    public void save(JsonObject json) {
        json.addProperty(this.getName(), this.getValue());
    }

    @Override
    public void load(JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            this.setValue(json.getAsString());
        }
    }
}
