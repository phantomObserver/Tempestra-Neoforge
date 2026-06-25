package moth.tempestra.client.lightning;

import moth.tempestra.TempestraMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.resources.ResourceLocation;

public class TempestraLightningRenderer extends EntityRenderer<LightningBolt> {
    private static final double RENDER_DISTANCE = 256.0D;

    public TempestraLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(LightningBolt entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return entity.distanceToSqr(cameraX, cameraY, cameraZ) <= RENDER_DISTANCE * RENDER_DISTANCE
                && frustum.isVisible(entity.getBoundingBoxForCulling().inflate(64.0D));
    }

    @Override
    public void render(
            LightningBolt entity,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light
    ) {
        ClientLightningVisualManager.ensureVisual(entity);
    }

    @Override
    public ResourceLocation getTextureLocation(LightningBolt entity) {
        return TempestraMod.id("textures/entity/lightning.png");
    }
}
