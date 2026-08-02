package shit.zen.gui.hud.modulelist;

import java.util.Comparator;
import shit.zen.modules.Module;
import shit.zen.render.FontRenderer;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.ModeSetting;

/** Immutable text and layout data for one enabled module. */
public final class ModuleEntry {
    private static final float LETTER_SPACING = 1.0f;

    private final Module module;
    private final String name;
    private final String mode;
    private final float nameWidth;
    private final float modeWidth;

    private ModuleEntry(Module module, String mode, FontRenderer nameFont, FontRenderer modeFont) {
        this.module = module;
        this.name = module.getName();
        this.mode = mode;
        this.nameWidth = spacedWidth(this.name, nameFont);
        this.modeWidth = mode.isEmpty() ? 0.0f : modeFont.getWidth(mode);
    }

    public static ModuleEntry of(Module module, FontRenderer nameFont, FontRenderer modeFont) {
        return new ModuleEntry(module, findMode(module), nameFont, modeFont);
    }

    public static Comparator<ModuleEntry> byDescendingNameWidth() {
        return Comparator.comparingDouble(ModuleEntry::nameWidth).reversed();
    }

    private static String findMode(Module module) {
        ModeSetting fallback = null;
        for (Setting<?> setting : module.getSettings()) {
            if (!(setting instanceof ModeSetting mode) || !isVisible(mode) || mode.getValue() == null) {
                continue;
            }
            if ("Mode".equalsIgnoreCase(mode.getName())) {
                return mode.getValue();
            }
            if (fallback == null) {
                fallback = mode;
            }
        }
        return fallback == null ? "" : fallback.getValue();
    }

    private static boolean isVisible(Setting<?> setting) {
        return setting.getVisibility() == null || setting.getVisibility().displayable();
    }

    public static float spacedWidth(String text, FontRenderer font) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            width += font.getWidth(String.valueOf(text.charAt(i)));
            if (i + 1 < text.length()) {
                width += LETTER_SPACING;
            }
        }
        return width;
    }

    public Module module() {
        return this.module;
    }

    public String name() {
        return this.name;
    }

    public String mode() {
        return this.mode;
    }

    public float nameWidth() {
        return this.nameWidth;
    }

    public float modeWidth() {
        return this.modeWidth;
    }

    public static float letterSpacing() {
        return LETTER_SPACING;
    }
}
