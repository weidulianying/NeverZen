package shit.zen.gui.neverloseGUI.hud.watermark;

/**
 * Periodic shine scan across watermark text — Neverlose-style 流光效果.
 * <p>
 * A highlight bar sweeps left-to-right across the text at a constant speed,
 * wrapping back to the start when it reaches the end.
 * <pre>
 *   Normal:   NeverZen
 *   Shine:    Nev▌erZen
 * </pre>
 */
public class TextGlow {

    private float shine;

    /** Advance the shine position each frame. */
    public void update() {
        shine += 0.02f;
        if (shine > 1f) shine = 0f;
    }

    /** Current shine position (0–1 across the text width). */
    public float shine() {
        return shine;
    }
}
