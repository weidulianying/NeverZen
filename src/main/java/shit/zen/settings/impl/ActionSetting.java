package shit.zen.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import shit.zen.settings.Setting;

/** Non-persistent command row rendered as a GUI button. */
public final class ActionSetting extends Setting<String> {
    private final Runnable action;

    public ActionSetting(String name, Runnable action) {
        super(name, name);
        this.action = action;
    }

    public void invoke() {
        this.action.run();
    }

    @Override public void save(JsonObject json) { }
    @Override public void load(JsonElement json) { }
}
