package moth.tempestra;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TempestraMod.MOD_ID)
public class TempestraMod {
    public static final String MOD_ID = "tempestra";
    public static final Logger LOGGER = LoggerFactory.getLogger("Tempestra");

    public TempestraMod() {
        LOGGER.info("[Tempestra] Initializing Tempestra!");
    }

    @EventBusSubscriber(modid = MOD_ID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            moth.tempestra.weather.TempestraWeatherCommands.register(event.getDispatcher());
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
