package shit.zen.client.render.effect;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import shit.zen.ZenClient;
import shit.zen.event.EventTarget;
import shit.zen.event.impl.EntityRemoveEvent;
import shit.zen.event.impl.RenderEvent;
import shit.zen.event.impl.TickEvent;
import shit.zen.event.impl.WorldChangeEvent;
import shit.zen.exception.ModuleNotFoundException;
import shit.zen.modules.impl.render.KillEffect;

/** Coordinates local player kills with client-only visual effects. */
public final class KillEffectManager {
    private static final long PENDING_KILL_TIMEOUT_MS = 2_000L;
    private static final long DUPLICATE_WINDOW_MS = 500L;
    private static final List<FakeLightning> LIGHTNING = new java.util.ArrayList<>();
    private static final Map<Integer, PendingKill> PENDING_KILLS = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> RECENT_KILLS = new ConcurrentHashMap<>();

    private record PendingKill(LivingEntity entity, long attackedAt) {
    }

    public KillEffectManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (event.getEntity().level() != minecraft.level) return;
        // Accept both direct attacks and kills credited through a projectile or
        // another indirect damage source owned by the local player.
        if (event.getSource().getEntity() != minecraft.player
                && event.getSource().getDirectEntity() != minecraft.player) return;

        triggerKill(event.getEntity());
    }

    /** Called after a target's death has been confirmed on the client. */
    public static void onKill(LivingEntity entity) {
        triggerKill(entity);
    }

    private static void triggerKill(LivingEntity entity) {
        KillEffect effect = getKillEffect();
        if (effect == null || !effect.isEnabled() || !effect.shouldAffect(entity)) return;

        long now = System.currentTimeMillis();
        Long lastKillTime = RECENT_KILLS.get(entity.getId());
        if (lastKillTime != null && now - lastKillTime < DUPLICATE_WINDOW_MS) return;
        RECENT_KILLS.put(entity.getId(), now);
        ZenClient.logger.info("KillEffect Trigger: {}", entity.getName().getString());

        if (effect.shouldSpawnLightning()) {
            spawnLightning(entity, effect.duration.getValue().longValue());
        }
        if (effect.shouldSpawnParticle()) {
            spawnParticle(entity);
        }
    }

    @EventTarget
    public void onLocalAttack(EntityRemoveEvent event) {
        if (event.dead() || !(event.entity() instanceof LivingEntity target)) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || target == minecraft.player
                || target.level() != minecraft.level || target.isDeadOrDying()) return;

        PENDING_KILLS.put(target.getId(), new PendingKill(target, System.currentTimeMillis()));
    }

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clearLightning();
            PENDING_KILLS.clear();
            return;
        }

        long now = System.currentTimeMillis();
        PENDING_KILLS.entrySet().removeIf(entry -> {
            PendingKill pendingKill = entry.getValue();
            LivingEntity target = pendingKill.entity();
            if (target.level() != minecraft.level || now - pendingKill.attackedAt() > PENDING_KILL_TIMEOUT_MS) {
                return true;
            }
            if (target.isDeadOrDying() || target.isRemoved() || target.getHealth() <= 0.0f) {
                triggerKill(target);
                return true;
            }
            return false;
        });
        RECENT_KILLS.entrySet().removeIf(entry -> now - entry.getValue() > DUPLICATE_WINDOW_MS);
        LIGHTNING.removeIf(lightning -> lightning.expired(now));
    }

    @EventTarget
    public void onRender(RenderEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || LIGHTNING.isEmpty()) return;

        for (FakeLightning lightning : LIGHTNING) {
            LightningRenderer.render(event.poseStack(),
                    minecraft.gameRenderer.getMainCamera().getPosition(),
                    lightning, event.partialTick());
        }
    }

    private static KillEffect getKillEffect() {
        if (ZenClient.getInstance() == null || ZenClient.getInstance().getModuleManager() == null) return null;
        try {
            return ZenClient.getInstance().getModuleManager().getModule(KillEffect.class);
        } catch (ModuleNotFoundException ignored) {
            return null;
        }
    }

    private static void spawnLightning(LivingEntity entity, long lifetimeMs) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || entity.level() != minecraft.level) return;

        LIGHTNING.add(new FakeLightning(entity, Math.max(1L, lifetimeMs)));
        ZenClient.logger.info("KillEffect Lightning Added");
    }

    private static void spawnParticle(LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || entity.level() != minecraft.level) return;

        double y = entity.getY() + entity.getBbHeight() / 2.0;
        for (int i = 0; i < 20; i++) {
            minecraft.level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    entity.getX(), y, entity.getZ(),
                    (Math.random() - 0.5) * 0.5,
                    Math.random() * 0.5,
                    (Math.random() - 0.5) * 0.5);
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent event) {
        clearLightning();
        PENDING_KILLS.clear();
        RECENT_KILLS.clear();
    }

    private static void clearLightning() {
        LIGHTNING.clear();
    }
}
