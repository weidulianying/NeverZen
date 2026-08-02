package shit.zen.hud;

import shit.zen.ZenClient;
import shit.zen.event.EventTarget;
import shit.zen.event.impl.GlRenderEvent;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.gui.hud.modulelist.ModuleList;
import shit.zen.modules.impl.render.Interface;

/** HUD lifecycle adapter for the top-left rainbow module list. */
public class ModuleListHud extends HudElement {
    private final ModuleList moduleList = new ModuleList();

    public ModuleListHud() {
        super("ModuleList");
        this.x = 5.0f;
        this.y = 5.0f;
    }

    @Override
    public void onRender2D(Render2DEvent event, float x, float y) {
    }

    @EventTarget
    public void onGlRenderDirect(GlRenderEvent event) {
        if (!this.isEnabled() || event.drawContext() == null) {
            return;
        }

        Interface interfaceModule = ZenClient.getInstance().getModuleManager().getModule(Interface.class);
        if (!interfaceModule.isEnabled()) {
            return;
        }

        ModuleList.Size size = this.moduleList.render(event.drawContext(), this.x, this.y);
        this.width = size.width();
        this.height = size.height();
    }

    @Override
    public void onGlRender(GlRenderEvent event, float x, float y) {
    }

    @Override
    public void onSettings() {
    }
}
