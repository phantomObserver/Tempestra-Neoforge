package moth.tempestra.client;

import moth.tempestra.client.lightning.ClientLightningVisualManager;
import moth.tempestra.client.lightning.TempestraLightningRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.entity.EntityType;

public class TempestraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Visuals are client-only and derive entirely from observed vanilla lightning entities.
        EntityRendererRegistry.register(EntityType.LIGHTNING_BOLT, TempestraLightningRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(ClientLightningVisualManager::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientLightningVisualManager.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientLightningVisualManager.clear());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientLightningVisualManager::render);
    }
}
