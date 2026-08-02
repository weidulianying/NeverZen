package shit.zen.client.render.effect;

import net.minecraft.world.entity.LivingEntity;

/** Client-only data used by {@link LightningRenderer}. */
public final class FakeLightning {
    private final double x;
    private final double y;
    private final double z;
    private final long spawnTime;
    private final long lifetimeMs;
    private boolean renderLogged;

    public FakeLightning(LivingEntity entity, long lifetimeMs) {
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
        this.spawnTime = System.currentTimeMillis();
        this.lifetimeMs = lifetimeMs;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public double z() {
        return this.z;
    }

    public boolean markRenderLogged() {
        if (this.renderLogged) return false;
        this.renderLogged = true;
        return true;
    }

    public boolean expired(long now) {
        return now - this.spawnTime >= this.lifetimeMs;
    }
}
