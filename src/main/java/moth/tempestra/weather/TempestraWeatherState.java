package moth.tempestra.weather;

import moth.tempestra.TempestraMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

public class TempestraWeatherState extends PersistentState {
    private static final String ID = TempestraMod.MOD_ID + "_weather";
    private static final String WEATHER_TYPE_KEY = "WeatherType";

    private TempestraWeatherType weatherType = TempestraWeatherType.THUNDER_STORM;

    public static TempestraWeatherState get(MinecraftServer server) {
        return server.getOverworld()
                .getPersistentStateManager()
                .getOrCreate(TempestraWeatherState::fromNbt, TempestraWeatherState::new, ID);
    }

    public static TempestraWeatherState fromNbt(NbtCompound nbt) {
        TempestraWeatherState state = new TempestraWeatherState();
        state.weatherType = TempestraWeatherType.byId(nbt.getString(WEATHER_TYPE_KEY));
        return state;
    }

    public TempestraWeatherType weatherType() {
        return weatherType;
    }

    public void setWeatherType(TempestraWeatherType weatherType) {
        if (this.weatherType == weatherType) {
            return;
        }

        this.weatherType = weatherType;
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putString(WEATHER_TYPE_KEY, weatherType.id());
        return nbt;
    }
}
