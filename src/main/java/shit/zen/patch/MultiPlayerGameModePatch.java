package shit.zen.patch;

import asm.patchify.annotation.At;
import asm.patchify.annotation.Inject;
import asm.patchify.annotation.Patch;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import shit.zen.ClientBase;
import shit.zen.ZenClient;
import shit.zen.hud.KillLog;

/** Captures local melee attacks so client HUDs can confirm kills without server-only events. */
@Patch(MultiPlayerGameMode.class)
public final class MultiPlayerGameModePatch {
    private MultiPlayerGameModePatch() {
    }

    @Inject(
            method = "attack",
            desc = "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(At.Type.HEAD)
    )
    public static void onAttack(MultiPlayerGameMode gameMode, Player player, Entity target, CallbackInfo callbackInfo) {
        if (!ZenClient.isReady() || ClientBase.mc == null || player != ClientBase.mc.player) return;
        if (target instanceof LivingEntity livingEntity) {
            KillLog.trackLocalAttack(livingEntity);
        }
    }
}
