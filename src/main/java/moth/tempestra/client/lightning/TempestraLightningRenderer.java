package moth.tempestra.client.lightning;

import moth.tempestra.TempestraMod;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.Identifier;

public class TempestraLightningRenderer extends EntityRenderer<LightningEntity> {
    public TempestraLightningRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(
            LightningEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        ClientLightningVisualManager.ensureVisual(entity);
    }

    @Override
    public Identifier getTexture(LightningEntity entity) {
        return TempestraMod.id("textures/entity/lightning.png");
    }
}
