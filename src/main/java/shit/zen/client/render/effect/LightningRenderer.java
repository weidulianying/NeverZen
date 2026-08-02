package shit.zen.client.render.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import shit.zen.ZenClient;

/** Renders a temporary visual-only vanilla lightning bolt. */
public final class LightningRenderer {
    private LightningRenderer() {
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition,
                              FakeLightning lightning, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        LightningBolt entity = EntityType.LIGHTNING_BOLT.create(minecraft.level);
        if (entity == null) return;

        entity.setVisualOnly(true);
        entity.setPos(lightning.x(), lightning.y(), lightning.z());

        double renderX = lightning.x() - cameraPosition.x;
        double renderY = lightning.y() - cameraPosition.y;
        double renderZ = lightning.z() - cameraPosition.z;
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.render(entity, renderX, renderY, renderZ, 0.0f, partialTick,
                poseStack, bufferSource, LightTexture.FULL_BRIGHT);
        bufferSource.endBatch(RenderType.lightning());

        if (lightning.markRenderLogged()) {
            ZenClient.logger.info("Render lightning: {} {} {}",
                    lightning.x(), lightning.y(), lightning.z());
        }
    }
}
