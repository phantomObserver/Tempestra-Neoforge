package moth.tempestra;

import moth.butterflyapi.ButterflyApi;
import moth.butterflyapi.mod.ModContext;
import moth.tempestra.server.TempestraServerGameplay;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

public class TempestraMod implements ModInitializer {
    public static final String MOD_ID = "tempestra";
    public static final ModContext MOD = ButterflyApi.mod(MOD_ID, "Tempestra");
    public static final Logger LOGGER = MOD.logger();

    @Override
    public void onInitialize() {
        LOGGER.info("[Tempestra] Initializing shared lightning systems...");
        TempestraServerGameplay.init();
    }

    public static Identifier id(String path) {
        return MOD.id(path);
    }
}
