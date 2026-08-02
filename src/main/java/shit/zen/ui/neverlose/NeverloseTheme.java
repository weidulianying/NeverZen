package shit.zen.ui.neverlose;

/**
 * Centralized Neverlose-style design tokens.
 * All HUD and GUI code should reference these constants instead of hardcoded colors.
 *
 * Neverlose design language:
 * - Dark semi-transparent backgrounds (#121212)
 * - Blue-purple accent (#577FFF)
 * - Rounded corners (8px standard)
 * - Gaussian blur + shadows
 * - Smooth EaseOutExpo animations
 */
public final class NeverloseTheme {

    // ── Backgrounds ──────────────────────────────────────────
    /** Semi-transparent panel background (18,18,18 ~70% alpha) */
    public static final int BG_PANEL     = 0xB3121212;

    /** Opaque solid background for panels */
    public static final int BG_SOLID     = 0xFF181818;

    /** Slightly lighter element background */
    public static final int BG_ELEMENT   = 0xFF222222;

    // ── Accent Colors ────────────────────────────────────────
    /** Primary accent: blue-purple #577FFF (87,127,255) */
    public static final int ACCENT       = 0xFF577FFF;

    /** Dimmer accent variant for gradients / inactive states */
    public static final int ACCENT_DIM   = 0xFF3D5FC4;

    /** Glow / bloom color #759BFF (117,155,255) */
    public static final int GLOW         = 0xFF759BFF;

    // ── Text ─────────────────────────────────────────────────
    /** Primary white text */
    public static final int TEXT         = 0xFFFFFFFF;

    /** Secondary / muted text (180,180,180) */
    public static final int TEXT_MUTED   = 0xFFB4B4B4;

    // ── Dimensions ───────────────────────────────────────────
    /** Standard corner radius for panels */
    public static final float RADIUS     = 8.0f;

    /** Small corner radius for toggles, pills, buttons */
    public static final float RADIUS_SM  = 5.0f;

    /** Standard blur radius */
    public static final float BLUR       = 15.0f;

    /** Standard inner padding */
    public static final float PADDING    = 8.0f;

    // ── Animation ────────────────────────────────────────────
    /** Standard animation duration in seconds */
    public static final double ANIM_SPEED = 0.25;

    // ── Shadow ───────────────────────────────────────────────
    /** Shadow opacity (0.0 - 1.0) */
    public static final float SHADOW_ALPHA = 0.4f;

    /** Shadow blur radius */
    public static final float SHADOW_RADIUS = 12.0f;

    private NeverloseTheme() {
        throw new UnsupportedOperationException("Utility class");
    }
}
