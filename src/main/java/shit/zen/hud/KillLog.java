package shit.zen.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import shit.zen.ZenClient;
import shit.zen.client.utils.entity.EntityFilter;
import shit.zen.event.EventTarget;
import shit.zen.event.impl.GlRenderEvent;
import shit.zen.event.impl.DisconnectEvent;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.event.impl.TickEvent;
import shit.zen.gui.NewClickGui;
import shit.zen.gui.OldClickGui;
import shit.zen.gui.PanelClickGui;
import shit.zen.gui.neverloseGUI.NeverloseScreen;
import shit.zen.settings.impl.BooleanSetting;

/**
 * KillLog HUD — displays recent kills with weapon icon, coloured text,
 * and multi-kill streak indicator.
 * <p>
 * Fades out over 3 seconds. Kill streaks within 5 seconds stack.
 * <pre>
 *   [🗡] Alex 使用 铁剑 击杀了 Steve
 *                Double Kill!
 * </pre>
 */
public class KillLog extends HudElement {
    private final BooleanSetting player = new BooleanSetting("Player", true);
    private final BooleanSetting mob = new BooleanSetting("MOB", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);

    private static final long PENDING_ATTACK_MS = 4000L;
    private static final long DUPLICATE_WINDOW_MS = 1000L;

    private final Map<Integer, PendingKill> pendingKills = new HashMap<>();
    private String killerName = "";
    private String weaponName = "";
    private String victimName = "";
    private ItemStack weaponStack = ItemStack.EMPTY;
    private long killTime;
    private int killCount;
    private long lastKillTime;
    private int lastRecordedEntityId = Integer.MIN_VALUE;
    private long lastRecordedTime;
    private boolean forgeRegistered;
    private boolean manuallyPositioned;
    private static final int DISPLAY_MS  = 3000;
    private static final int FADE_DELAY_MS = 1300;
    private static final int STREAK_MS   = 8000;

    // ── Streak texts ──
    private static final String[] STREAK_TEXTS = {
        "", "", "Double Kill!", "Triple Kill!", "Quadra Kill!", "Rampage!"
    };
    private static final String WEAPON_SEPARATOR = "  使用  ";
    private static final String KILL_SEPARATOR = "  击杀了  ";

    public KillLog() {
        super("KillLog");
        this.setWidth(300);
        this.setHeight(40);
        // (0, 0) is treated as the default anchor above the hotbar. A dragged
        // HUD keeps its manually selected coordinates afterward.
        this.x = 0;
        this.y = 0;
        this.setEnabled(true);
    }

    private record PendingKill(LivingEntity target, String killerName, String weaponName,
                               ItemStack weaponStack, long attackedAt) {
    }

    @Override
    protected void onEnable() {
        if (!this.forgeRegistered) {
            MinecraftForge.EVENT_BUS.register(this);
            this.forgeRegistered = true;
        }
    }

    @Override
    protected void onDisable() {
        if (this.forgeRegistered) {
            MinecraftForge.EVENT_BUS.unregister(this);
            this.forgeRegistered = false;
        }
        this.pendingKills.clear();
        this.clearLog();
    }

    /** Called by the client attack patch so the HUD does not depend on a server-side death event. */
    public static void trackLocalAttack(LivingEntity target) {
        if (ZenClient.getInstance() == null || ZenClient.getInstance().getHudManager() == null) return;
        KillLog killLog = ZenClient.getInstance().getHudManager().getHudElement(KillLog.class);
        if (killLog != null) killLog.trackAttack(target);
    }

    private void trackAttack(LivingEntity target) {
        if (!this.isEnabled() || mc == null || mc.player == null || mc.level == null
                || target == null || target == mc.player || target.level() != mc.level
                || target.isDeadOrDying() || !EntityFilter.canLog(target, this)) return;

        ItemStack stack = mc.player.getMainHandItem().copy();
        this.pendingKills.put(target.getId(), new PendingKill(
                target,
                mc.player.getName().getString(),
                getWeaponDisplayName(stack),
                stack,
                System.currentTimeMillis()));
    }

    // ════════════════════════════════════════════════════
    //  Kill detection (Forge LivingDeathEvent)
    // ════════════════════════════════════════════════════

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!this.isEnabled() || mc == null || mc.player == null || mc.level == null) return;
        if (event.getEntity().level() != mc.level) return;
        if (event.getSource().getEntity() != mc.player) return;
        if (event.getSource().getDirectEntity() != mc.player) return;

        LivingEntity target = event.getEntity();
        if (!EntityFilter.canLog(target, this)) return;

        this.pendingKills.remove(target.getId());
        String currentKiller = mc.player.getName().getString();
        ItemStack currentWeapon = mc.player.getMainHandItem().copy();
        this.recordKill(target, currentKiller,
                getWeaponDisplayName(currentWeapon),
                currentWeapon, System.currentTimeMillis());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || mc == null || mc.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, PendingKill>> iterator = this.pendingKills.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingKill pending = iterator.next().getValue();
            if (now - pending.attackedAt() > PENDING_ATTACK_MS
                    || pending.target().level() != mc.level) {
                iterator.remove();
                continue;
            }
            if (pending.target().isRemoved() || pending.target().isDeadOrDying()
                    || pending.target().getHealth() <= 0.0f) {
                iterator.remove();
                this.recordKill(pending.target(), pending.killerName(), pending.weaponName(),
                        pending.weaponStack(), now);
            }
        }
    }

    @EventTarget
    public void onDisconnect(DisconnectEvent event) {
        this.pendingKills.clear();
        this.clearLog();
    }

    private void recordKill(LivingEntity target, String killer, String weapon,
                            ItemStack stack, long now) {
        if (target.getId() == this.lastRecordedEntityId
                && now - this.lastRecordedTime < DUPLICATE_WINDOW_MS) return;

        this.killerName = killer;
        this.victimName = target.getName().getString();
        this.weaponStack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.weaponName = weapon == null ? "" : weapon;
        if (now - this.lastKillTime <= STREAK_MS) {
            this.killCount++;
        } else {
            this.killCount = 1;
        }
        this.lastKillTime = now;
        this.killTime = now;
        this.lastRecordedEntityId = target.getId();
        this.lastRecordedTime = now;
    }

    private void clearLog() {
        this.killerName = "";
        this.weaponName = "";
        this.victimName = "";
        this.weaponStack = ItemStack.EMPTY;
        this.killTime = 0L;
        this.killCount = 0;
        this.lastKillTime = 0L;
        this.lastRecordedEntityId = Integer.MIN_VALUE;
        this.lastRecordedTime = 0L;
    }

    public boolean isPlayerEnabled() {
        return this.player.getValue();
    }

    public boolean isMobEnabled() {
        return this.mob.getValue();
    }

    public boolean isAnimalEnabled() {
        return this.animals.getValue();
    }

    // ════════════════════════════════════════════════════
    //  Rendering
    // ════════════════════════════════════════════════════

    @Override
    public void onGlRender(GlRenderEvent event, float x, float y) {
    }

    @Override
    public void onRender2D(Render2DEvent event, float x, float y) {
        this.placeAboveHotbarIfNeeded();
        x = this.x;
        y = this.y;

        boolean preview = isPositionEditorOpen();
        long elapsed = System.currentTimeMillis() - killTime;
        if (!preview && (elapsed > DISPLAY_MS || killerName.isEmpty())) return;

        float alpha = preview ? 1f : elapsed <= FADE_DELAY_MS
                ? 1f
                : 1f - (float) (elapsed - FADE_DELAY_MS) / (DISPLAY_MS - FADE_DELAY_MS);
        if (alpha <= 0f) return;

        String displayKiller = preview ? (mc.player == null ? "玩家" : mc.player.getName().getString()) : this.killerName;
        String displayWeapon = preview ? "钻石剑" : getWeaponDisplayName(this.weaponStack, this.weaponName);
        String displayVictim = preview ? "示例目标" : this.victimName;
        ItemStack displayWeaponStack = preview
                ? (this.weaponStack.isEmpty() ? new ItemStack(Items.DIAMOND_SWORD) : this.weaponStack)
                : this.weaponStack;
        int displayKillCount = preview ? 2 : this.killCount;

        // ── Weapon icon ──
        float iconSize = 18f;
        float iconX = x + 4f;
        float iconY = y + 4f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        if (!displayWeaponStack.isEmpty()) {
            event.guiGraphics().renderItem(displayWeaponStack, (int) iconX, (int) iconY);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // ── Kill text ──
        MutableComponent text = Component.literal(displayKiller)
            .withStyle(ChatFormatting.GREEN)
            .append(Component.literal(WEAPON_SEPARATOR).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayWeapon).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(KILL_SEPARATOR).withStyle(ChatFormatting.RED))
            .append(Component.literal(displayVictim).withStyle(ChatFormatting.WHITE));

        float textX = iconX + iconSize + 6;
        float textY = iconY + 2;
        float panelWidth = Math.max(120f, textX - x + mc.font.width(text) + 4f);
        this.setWidth(panelWidth);
        int textColor = new Color(1f, 1f, 1f, alpha).getRGB();
        event.guiGraphics().drawString(mc.font, text, (int) textX, (int) textY, textColor);

        // ── Streak text ──
        String streak = displayKillCount > 1 && displayKillCount < STREAK_TEXTS.length
            ? STREAK_TEXTS[displayKillCount] : "";
        if (!streak.isEmpty()) {
            MutableComponent streakText = Component.literal(streak)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            float streakW = mc.font.width(streakText);
            event.guiGraphics().drawString(mc.font, streakText,
                (int) (x + Math.max(0f, (panelWidth - streakW) / 2f)), (int) (textY + 14),
                new Color(1f, 0.85f, 0f, alpha).getRGB());
        }

        RenderSystem.disableBlend();

        // Keep the configured HUD position stable while updating the drag bounds.
        this.setHeight(streak.isEmpty() ? 28f : 42f);
    }

    @Override
    public void onSettings() {
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY) {
        this.manuallyPositioned = true;
        super.mouseDragged(mouseX, mouseY);
    }

    private void placeAboveHotbarIfNeeded() {
        if (this.manuallyPositioned || this.x != 0f || this.y != 0f
                || mc == null || mc.getWindow() == null) return;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        this.x = screenW / 2f - this.width / 2f;
        this.y = screenH - 22f - this.height - 4f;
    }

    /**
     * Screens where the player can see and drag a stable KillLog preview.
     * Only the player's inventory is included; chest and other container
     * screens must not expose the position editor.
     */
    public static boolean isPositionEditorOpen() {
        return mc != null && (mc.screen instanceof ChatScreen
                || mc.screen instanceof InventoryScreen
                || mc.screen instanceof NewClickGui
                || mc.screen instanceof OldClickGui
                || mc.screen instanceof PanelClickGui
                || mc.screen instanceof NeverloseScreen);
    }

    private static String getWeaponDisplayName(ItemStack stack) {
        return getWeaponDisplayName(stack, null);
    }

    private static String getWeaponDisplayName(ItemStack stack, String fallback) {
        String name = fallback;
        if (name == null || name.isBlank()) {
            name = stack == null || stack.isEmpty() ? "空手" : stack.getHoverName().getString();
        }
        name = name.trim();
        return name.isEmpty() ? "空手" : name;
    }
}
