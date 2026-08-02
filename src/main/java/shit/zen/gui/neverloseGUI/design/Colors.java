package shit.zen.gui.neverloseGUI.design;

/** ARGB color tokens — the single source of truth for every colour in the GUI. */
public final class Colors {
    private Colors() {}

    // Backgrounds
    public static final int BACKGROUND      = 0xF20D0F14;
    public static final int SIDEBAR         = 0xF50A0C10;
    public static final int PANEL           = 0xFF14141C;
    public static final int CARD            = 0xFF1B1C25;
    public static final int NAV_SELECTED    = 0xFF292C35;
    public static final int BORDER          = 0xFF242630;
    public static final int INPUT_BG        = 0xFF1B1C25;

    // Accent
    public static final int ACCENT          = 0xFF4F7FFF;
    public static final int ACCENT_HOVER    = 0xFF7199FF;
    public static final int ACCENT_DIM      = 0xFF365BB8;

    // Text
    public static final int TEXT_PRIMARY    = 0xFFE8E9ED;
    public static final int TEXT_SECONDARY  = 0xFF9A9CA5;
    public static final int TEXT_DISABLED   = 0xFF626570;

    // Semantic
    public static final int SUCCESS         = 0xFF4CAF50;
    public static final int WARNING         = 0xFFFFB74D;
    public static final int DANGER          = 0xFFEF5350;

    // Toggle
    public static final int TOGGLE_ON       = 0xFF4F7FFF;
    public static final int TOGGLE_OFF      = 0xFF292C35;
    public static final int TOGGLE_KNOB     = 0xFFFFFFFF;

    // Dot Toggle
    public static final int DOT_ON          = 0xFF4F7FFF;   // accent blue filled
    public static final int DOT_OFF         = 0xFF5A5A5A;   // gray outline

    // Line Slider
    public static final int LINE_BG         = 0xFF2E3038;   // slider background line

    // Overlay
    public static final int OVERLAY         = 0x80000000;
    public static final int TOAST_BG        = 0xF0222230;
}
