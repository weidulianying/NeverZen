package shit.zen.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import shit.zen.event.impl.GlRenderEvent;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.utils.render.RenderUtil;

/**
 * Armor HUD — horizontal display of the player's equipped armour.
 * <p>
 * Layout: Helmet → Chestplate → Leggings → Boots (left to right).
 * Empty slots are skipped. Draggable like any other HUD element.
 */
public class ArmorHud extends HudElement {

    private static final float ITEM_SIZE = 18f;
    private static final float GAP       = 3f;
    private static final float PAD       = 5f;
    private static final int   BG_COLOR  = 0x50000000;

    public ArmorHud() {
        super("ArmorHUD");
        this.setWidth(ITEM_SIZE * 4 + GAP * 3 + PAD * 2);
        this.setHeight(ITEM_SIZE + PAD * 2);
        this.x = 6;
        this.y = 70;
        this.setEnabled(true);
    }

    @Override
    public void onGlRender(GlRenderEvent event, float x, float y) {
    }

    @Override
    public void onRender2D(Render2DEvent event, float x, float y) {
        if (mc.player == null) return;

        ItemStack[] armor = {
            mc.player.getInventory().getArmor(3), // helmet
            mc.player.getInventory().getArmor(2), // chestplate
            mc.player.getInventory().getArmor(1), // leggings
            mc.player.getInventory().getArmor(0), // boots
        };

        int count = 0;
        for (ItemStack s : armor) if (!s.isEmpty()) count++;
        if (count == 0) return;

        float panelW = count * ITEM_SIZE + (count - 1) * GAP + PAD * 2;
        float panelH = ITEM_SIZE + PAD * 2;

        this.setWidth(panelW);
        this.setHeight(panelH);

        PoseStack ps = event.poseStack();

        // Background
        RenderUtil.drawRoundedRect(ps, x, y, panelW, panelH, 5f, BG_COLOR);

        // Items (left to right)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float cx = x + PAD;
        for (ItemStack stack : armor) {
            if (stack.isEmpty()) continue;
            event.guiGraphics().renderItem(stack, (int) cx, (int) (y + PAD));
            event.guiGraphics().renderItemDecorations(mc.font, stack, (int) cx, (int) (y + PAD));
            cx += ITEM_SIZE + GAP;
        }
        RenderSystem.disableBlend();
    }

    @Override
    public void onSettings() {
    }
}
