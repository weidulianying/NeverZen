package shit.zen.gui.neverloseGUI.theme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import shit.zen.gui.neverloseGUI.design.Colors;

/** Central config for accent, blur, animation, scale, language. */
public final class ThemeManager {
    private ThemeManager() {}

    // ── Current values ──────────────────────────────────
    private static int    accentColor    = Colors.ACCENT;
    private static float  blurRadius     = 8f;
    private static float  animationSpeed = 0.18f;
    private static float  uiScale        = 1f;
    private static String language       = "English";
    private static String fontFamily     = "Inter";

    private static final List<Consumer<String>> listeners = new ArrayList<>();

    // ── Accessors ───────────────────────────────────────
    public static int    accentColor()    { return accentColor; }
    public static float  blurRadius()     { return blurRadius; }
    public static float  animationSpeed() { return animationSpeed; }
    public static float  uiScale()        { return uiScale; }
    public static String language()       { return language; }
    public static String fontFamily()     { return fontFamily; }

    // ── Mutators (fire onChange) ────────────────────────
    public static void accentColor(int c)    { accentColor = c; fire("accent"); }
    public static void blurRadius(float r)   { blurRadius = r; fire("blur"); }
    public static void animationSpeed(float s) { animationSpeed = s; fire("anim"); }
    public static void uiScale(float s)      { uiScale = s; fire("scale"); }
    public static void language(String l)    { language = l; fire("lang"); }
    public static void fontFamily(String f)  { fontFamily = f; fire("font"); }

    public static void reset() {
        accentColor = Colors.ACCENT; blurRadius = 8f; animationSpeed = 0.18f;
        uiScale = 1f; language = "English"; fontFamily = "Inter";
        fire("reset");
    }

    // ── Theme presets ───────────────────────────────────
    public static final int BLUE    = 0xFF4C82FF;
    public static final int PURPLE  = 0xFFA78BFA;
    public static final int CYAN    = 0xFF22D3EE;
    public static final int GREEN   = 0xFF4ADE80;
    public static final int RED     = 0xFFF87171;

    public static int[] accentPresets() { return new int[]{BLUE, PURPLE, CYAN, GREEN, RED}; }
    public static String[] accentNames() { return new String[]{"Blue","Purple","Cyan","Green","Red"}; }

    public static String[] languages() { return new String[]{"English","简体中文","日本語"}; }
    public static String[] fonts() { return new String[]{"Inter","SF Pro","JetBrains Mono"}; }

    // ── Listeners ───────────────────────────────────────
    public static void onChange(Consumer<String> l) { listeners.add(l); }
    private static void fire(String key) { for (Consumer<String> l : listeners) l.accept(key); }
}
