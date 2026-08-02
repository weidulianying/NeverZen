package shit.zen.patch;

import asm.patchify.annotation.Inject;
import asm.patchify.annotation.Patch;
import asm.patchify.annotation.At;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import shit.zen.modules.impl.render.SelfSkin;

@Patch(AbstractClientPlayer.class)
public final class AbstractClientPlayerPatch {
    @Inject(method = "getSkinTextureLocation", desc = "()Lnet/minecraft/resources/ResourceLocation;",
            at = @At(At.Type.HEAD))
    public static void overrideLocalSkin(AbstractClientPlayer player, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        SelfSkin module = SelfSkin.INSTANCE;
        if (player != minecraft.player || module == null || !module.isEnabled()) return;
        ResourceLocation override = module.getTexture();
        if (override != null) {
            callback.result = override;
            callback.cancel();
        }
    }

    @Inject(method = "getModelName", desc = "()Ljava/lang/String;", at = @At(At.Type.HEAD))
    public static void overrideLocalModel(AbstractClientPlayer player, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        SelfSkin module = SelfSkin.INSTANCE;
        if (player != minecraft.player || module == null || !module.isEnabled() || module.getTexture() == null) return;
        callback.result = module.getSkinModel();
        callback.cancel();
    }

    @Inject(method = "getCloakTextureLocation", desc = "()Lnet/minecraft/resources/ResourceLocation;",
            at = @At(At.Type.HEAD))
    public static void overrideLocalCape(AbstractClientPlayer player, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        SelfSkin module = SelfSkin.INSTANCE;
        if (player != minecraft.player || module == null || !module.isEnabled()) return;
        ResourceLocation override = module.getCapeTexture();
        if (override != null) {
            callback.result = override;
            callback.cancel();
        }
    }

    @Inject(method = "isCapeLoaded", desc = "()Z", at = @At(At.Type.HEAD))
    public static void markLocalCapeLoaded(AbstractClientPlayer player, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        SelfSkin module = SelfSkin.INSTANCE;
        if (player == minecraft.player && module != null && module.isEnabled() && module.getCapeTexture() != null) {
            callback.result = true;
            callback.cancel();
        }
    }
}
