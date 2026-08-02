package shit.zen.selfskin;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class TextureLoader {
    public CompletableFuture<ResourceLocation> uploadSkin(byte[] png, String cacheKey) {
        return upload(png, cacheKey, true);
    }

    public CompletableFuture<ResourceLocation> uploadCape(byte[] png, String cacheKey) {
        return upload(png, cacheKey, false);
    }

    private CompletableFuture<ResourceLocation> upload(byte[] png, String cacheKey, boolean skin) {
        CompletableFuture<ResourceLocation> future = new CompletableFuture<>();
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
            if (skin && !((image.getWidth() == 64 && image.getHeight() == 64)
                    || (image.getWidth() == 64 && image.getHeight() == 32))) {
                image.close();
                throw new IllegalArgumentException("Minecraft skin must be 64x64 or legacy 64x32");
            }
            if (!skin && (image.getWidth() <= 0 || image.getHeight() <= 0
                    || image.getWidth() > 2048 || image.getHeight() > 1024)) {
                image.close();
                throw new IllegalArgumentException("Minecraft cape dimensions are invalid");
            }
            ResourceLocation location = ResourceLocation.tryParse("zen:selfskin/" + cacheKey);
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                try {
                    minecraft.getTextureManager().release(location);
                    minecraft.getTextureManager().register(location, new DynamicTexture(image));
                    future.complete(location);
                } catch (Throwable throwable) {
                    image.close();
                    future.completeExceptionally(throwable);
                }
            });
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
        return future;
    }
}
