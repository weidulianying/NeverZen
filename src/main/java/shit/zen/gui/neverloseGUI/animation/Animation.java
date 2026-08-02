package shit.zen.gui.neverloseGUI.animation;

/** Lerp-based animation — every transition flows through here. */
public class Animation {

    public enum Easing { LINEAR, EASE_OUT_CUBIC, EASE_IN_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUAD }

    // Pre-configured speeds
    public static final float SPEED_HOVER   = 0.22f;
    public static final float SPEED_TOGGLE  = 0.18f;
    public static final float SPEED_EXPAND  = 0.20f;
    public static final float SPEED_OPEN    = 0.05f;
    public static final float SPEED_SCROLL  = 0.14f;
    public static final float SPEED_DRAG    = 0.20f;
    public static final float SPEED_ALPHA   = 0.12f;

    private float value, target;
    private Easing easing = Easing.LINEAR;

    public Animation() { this(0f); }
    public Animation(float initial) { this.value = this.target = initial; }

    public void animate(float target) { this.target = target; }
    public void force(float value) { this.value = this.target = value; }

    public float update() { return update(SPEED_HOVER); }
    public float update(float speed) { value += (target - value) * applyEasing(speed); return value; }

    public float peek() { return value; }
    public boolean isDone() { return Math.abs(target - value) < 0.005f; }

    public Animation easing(Easing e) { this.easing = e; return this; }

    private float applyEasing(float t) {
        return switch (easing) {
            case EASE_OUT_CUBIC      -> easeOutCubic(t);
            case EASE_IN_CUBIC       -> easeInCubic(t);
            case EASE_IN_OUT_CUBIC   -> easeInOutCubic(t);
            case EASE_OUT_QUAD       -> easeOutQuad(t);
            default                  -> t;
        };
    }

    public static float easeOutCubic(float t)    { return (float)(1.0 - Math.pow(1.0 - t, 3.0)); }
    public static float easeInCubic(float t)     { return t * t * t; }
    public static float easeInOutCubic(float t)  { return t < 0.5f ? 4f * t * t * t : (float)(1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0); }
    public static float easeOutQuad(float t)     { return 1f - (1f - t) * (1f - t); }
    public static float clamp01(float v)         { return Math.max(0, Math.min(1, v)); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
