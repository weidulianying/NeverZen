package shit.zen.client.utils.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import shit.zen.hud.KillLog;

public final class EntityFilter {
    private EntityFilter() {
    }

    public static boolean canLog(LivingEntity entity, KillLog module) {
        if (entity instanceof Player) {
            return module.isPlayerEnabled();
        }
        if (entity instanceof Enemy) {
            return module.isMobEnabled();
        }
        if (entity instanceof Animal) {
            return module.isAnimalEnabled();
        }
        return false;
    }
}
