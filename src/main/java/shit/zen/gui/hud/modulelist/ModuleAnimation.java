package shit.zen.gui.hud.modulelist;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import shit.zen.modules.Module;
import shit.zen.utils.animation.SmoothAnimationTimer;
import shit.zen.utils.math.Easings;

/** Owns per-module slide-in progress independently from HUD rendering. */
public final class ModuleAnimation {
    private static final double DURATION_SECONDS = 0.2;
    private final Map<Module, SmoothAnimationTimer> animations = new IdentityHashMap<>();

    public void sync(Collection<ModuleEntry> entries) {
        this.animations.keySet().removeIf(module -> entries.stream().noneMatch(entry -> entry.module() == module));

        for (ModuleEntry entry : entries) {
            SmoothAnimationTimer timer = this.animations.get(entry.module());
            if (timer == null) {
                timer = new SmoothAnimationTimer();
                timer.setCurrentValue(0.0);
                timer.animate(1.0, DURATION_SECONDS, Easings.EASE_OUT_EXPO);
                this.animations.put(entry.module(), timer);
            }
            timer.tick();
        }
    }

    public float progress(Module module) {
        SmoothAnimationTimer timer = this.animations.get(module);
        return timer == null ? 1.0f : Math.max(0.0f, Math.min(1.0f, timer.getValueF()));
    }
}
