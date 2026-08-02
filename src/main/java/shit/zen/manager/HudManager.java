package shit.zen.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;
import shit.zen.ClientBase;
import shit.zen.ZenClient;
import shit.zen.event.impl.GlRenderEvent;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.event.impl.TickEvent;
import shit.zen.gui.IntroAnimation;
import shit.zen.hud.ArmorHud;
import shit.zen.hud.HudElement;
import shit.zen.hud.KeyBindsHud;
import shit.zen.hud.KillLog;
import shit.zen.hud.ModuleListHud;
import shit.zen.hud.PlayerListHud;
import shit.zen.hud.PotionEffectsHud;
import shit.zen.hud.TargetHud;
import shit.zen.event.EventTarget;

public class HudManager {
    private final Map<String, HudElement> hudElements = new HashMap<>();
    private boolean wasLeftDown = false;

    public HudManager() {
        this.init();
    }

    public void init() {
        this.registerHudElement(new TargetHud());
        this.registerHudElement(new ArmorHud());
        this.registerHudElement(new KillLog());
        this.registerHudElement(new KeyBindsHud());
        this.registerHudElement(new ModuleListHud());
        this.registerHudElement(new PlayerListHud());
        this.registerHudElement(new PotionEffectsHud());
    }

    private void registerHudElement(HudElement hudElement) {
        ZenClient.getInstance().getModuleManager().register(hudElement);
        this.hudElements.put(hudElement.getClass().getSimpleName(), hudElement);
    }

    public <T extends HudElement> T getHudElement(Class<T> clazz) {
        return clazz.cast(this.hudElements.get(clazz.getSimpleName()));
    }

    public HudElement getHudElementByName(String string) {
        return this.hudElements.values().stream()
                .filter(hudElement -> hudElement.getName().equalsIgnoreCase(string))
                .findFirst().orElse(null);
    }

    public Collection<HudElement> getHudElements() {
        return this.hudElements.values();
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (ClientBase.mc.screen == null) {
            try {
                for (HudElement hudElement : ZenClient.getInstance().getHudManager().getHudElements()) {
                    hudElement.stopDragging();
                }
            } catch (Exception exception) {
                ClientBase.logger.error(exception);
                ClientBase.logger.error(exception.getMessage());
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {
        // ── Mouse drag handling (always active, fires before intro check) ──
        //     Works when any screen is open — not just ChatScreen.
        handleMouseDrag();

        if (IntroAnimation.isRunning()) {
            return;
        }

        // ── Render HUD elements ──
        for (HudElement hudElement : this.getHudElements()) {
            if (!hudElement.isEnabled()
                    && !(hudElement instanceof KillLog && KillLog.isPositionEditorOpen())) continue;
            hudElement.onRender2D(render2DEvent, hudElement.getX(), hudElement.getY());
        }
    }

    /**
     * Routes mouse events to HUD elements for drag-to-reposition.
     * <p>
     * UX convention: a screen must be open to <em>start</em> a drag
     * (press T / Esc / E, then drag). Ongoing drags continue as long
     * as the button is held. Uses GLFW for mouse state and the Minecraft
     * mouse handler for screen-space coordinates.
     */
    private void handleMouseDrag() {
        if (ClientBase.mc == null || ClientBase.mc.getWindow() == null) return;

        long window = ClientBase.mc.getWindow().getWindow();
        boolean leftDown = GLFW.glfwGetMouseButton(window, 0) == 1;

        // Scale raw mouse coords → GUI-scaled coords
        double rawX = ClientBase.mc.mouseHandler.xpos();
        double rawY = ClientBase.mc.mouseHandler.ypos();
        int mouseX = (int) (rawX * ClientBase.mc.getWindow().getGuiScaledWidth()
            / ClientBase.mc.getWindow().getWidth());
        int mouseY = (int) (rawY * ClientBase.mc.getWindow().getGuiScaledHeight()
            / ClientBase.mc.getWindow().getHeight());

        Collection<HudElement> elements = this.getHudElements();

        // ── Continue / stop existing drags ──
        for (HudElement element : elements) {
            if (!element.isEnabled()
                    && !(element instanceof KillLog && KillLog.isPositionEditorOpen())) continue;
            if (element.isDragging()) {
                if (leftDown) {
                    element.mouseDragged(mouseX, mouseY);
                } else {
                    element.setDragging(false);
                }
            }
        }

        // ── Start new drag (only when a screen is open + fresh left-click) ──
        boolean screenOpen = ClientBase.mc.screen != null;
        if (leftDown && !wasLeftDown && screenOpen) {
            for (HudElement element : elements) {
                if (!element.isEnabled()
                        && !(element instanceof KillLog && KillLog.isPositionEditorOpen())) continue;
                if (element.mousePressed(mouseX, mouseY, 0)) {
                    break; // only one element captures the click
                }
            }
        }

        wasLeftDown = leftDown;
    }

    @EventTarget
    public void onGlRender(GlRenderEvent glRenderEvent) {
        if (IntroAnimation.isRunning()) {
            return;
        }
        for (HudElement hudElement : this.getHudElements()) {
            if (!hudElement.isEnabled()) continue;
            hudElement.onGlRender(glRenderEvent, hudElement.getX(), hudElement.getY());
        }
    }
}
