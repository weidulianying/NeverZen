package shit.zen.modules.impl.render;

import shit.zen.event.impl.GlRenderEvent;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.hud.DynamicIsland;
import shit.zen.hud.NeverZenWatermark;
import shit.zen.hud.NeverloseWatermark;
import shit.zen.modules.Category;
import shit.zen.modules.Module;
import shit.zen.settings.impl.ModeSetting;
import shit.zen.event.EventTarget;

public class Watermark extends Module {
    final ModeSetting styleSetting = new ModeSetting("Style", "NeverZen", "DynamicIsland", "Neverlose").withDefault("NeverZen");
    private final NeverZenWatermark neverZenWatermark = new NeverZenWatermark();
    private final DynamicIsland dynamicIsland = new DynamicIsland();
    private final NeverloseWatermark neverloseWatermark = new NeverloseWatermark();

    public Watermark() { super("Watermark", Category.RENDER); }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (!this.isEnabled()) return;
        switch (this.styleSetting.getValue()) {
            case "NeverZen"      -> neverZenWatermark.onRender2D(e);
            case "DynamicIsland" -> dynamicIsland.onRender2D(e);
            case "Neverlose"     -> neverloseWatermark.onRender2D(e);
        }
    }

    @EventTarget
    public void onGlRender(GlRenderEvent e) {
        if (!this.isEnabled()) return;
        if ("Neverlose".equals(this.styleSetting.getValue())) neverloseWatermark.onGlRender(e);
    }
}
