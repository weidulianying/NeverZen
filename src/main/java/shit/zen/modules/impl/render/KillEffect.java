package shit.zen.modules.impl.render;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import shit.zen.modules.Category;
import shit.zen.modules.Module;
import shit.zen.settings.impl.BooleanSetting;
import shit.zen.settings.impl.ModeSetting;
import shit.zen.settings.impl.MultiSelectSetting;
import shit.zen.settings.impl.NumberSetting;

public class KillEffect extends Module {
    public final MultiSelectSetting target = new MultiSelectSetting("Target", "Mob", "Player")
            .withDefaults("Mob", "Player");
    public final ModeSetting effect = new ModeSetting("Effect", "Lightning", "Particle", "Both")
            .withDefault("Both");
    public final NumberSetting duration = new NumberSetting("Duration", 1000, 100, 5000, 100);
    public final BooleanSetting lightning = new BooleanSetting("Lightning", true);
    public final BooleanSetting particle = new BooleanSetting("Particle", true);

    public KillEffect() {
        super("KillEffect", Category.RENDER);
    }

    public boolean shouldSpawnLightning() {
        return this.lightning.getValue() && !this.effect.is("Particle");
    }

    public boolean shouldSpawnParticle() {
        return this.particle.getValue() && !this.effect.is("Lightning");
    }

    public boolean shouldAffect(LivingEntity entity) {
        if (entity instanceof Player) {
            return this.target.isSelected("Player");
        }
        return entity instanceof Mob && this.target.isSelected("Mob");
    }
}
