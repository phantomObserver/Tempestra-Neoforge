package moth.tempestra.weather;

import moth.tempestra.TempestraMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class TempestraWeatherState extends SavedData {
    private static final String ID = TempestraMod.MOD_ID + "_weather";
    private static final String WEATHER_TYPE_KEY = "WeatherType";
    private static final SavedData.Factory<TempestraWeatherState> FACTORY = new SavedData.Factory<>(
            TempestraWeatherState::new,
            TempestraWeatherState::load,
            null
    );

    private TempestraWeatherType weatherType = TempestraWeatherType.THUNDER_STORM;

    public static TempestraWeatherState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public static TempestraWeatherState load(CompoundTag nbt, HolderLookup.Provider registries) {
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
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putString(WEATHER_TYPE_KEY, weatherType.id());
        return nbt;
    }
}
