package moth.tempestra.client;

import moth.tempestra.TempestraMod;
import moth.tempestra.client.lightning.ClientLightningVisualManager;
import moth.tempestra.client.lightning.TempestraLightningRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class TempestraClient {
    private TempestraClient() {
    }

    @EventBusSubscriber(modid = TempestraMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EntityType.LIGHTNING_BOLT, TempestraLightningRenderer::new);
        }
    }

    @EventBusSubscriber(modid = TempestraMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            ClientLightningVisualManager.tick(net.minecraft.client.Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientLightningVisualManager.clear();
        }

        @SubscribeEvent
        public static void render(RenderLevelStageEvent event) {
            ClientLightningVisualManager.render(event);
        }
    }
}
