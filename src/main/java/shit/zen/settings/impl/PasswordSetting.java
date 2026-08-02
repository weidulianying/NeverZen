package shit.zen.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import shit.zen.settings.Setting;

/** In-memory-only secret. Config serializers intentionally do not recognize this type. */
public final class PasswordSetting extends Setting<char[]> {
    public PasswordSetting(String name) {
        super(name, new char[0]);
    }

    public void clear() {
        char[] current = getValue();
        if (current != null) Arrays.fill(current, '\0');
        setValue(new char[0]);
    }

    @Override public void save(JsonObject json) { }
    @Override public void load(JsonElement json) { }
}
