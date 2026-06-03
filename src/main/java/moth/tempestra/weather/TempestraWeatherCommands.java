package moth.tempestra.weather;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class TempestraWeatherCommands {
    private static final String DURATION_ARGUMENT = "duration";

    private TempestraWeatherCommands() {
    }

    public static void init() {
        // Fabric only invokes this against server command dispatchers, including the integrated server.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("tempestra")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("weather")
                        .executes(context -> queryWeather(context.getSource()))
                        .then(stormCommand(TempestraWeatherType.THUNDER_STORM))
                        .then(stormCommand(TempestraWeatherType.MEDIUM_THUNDER_STORM))
                        .then(stormCommand(TempestraWeatherType.HEAVY_THUNDER_STORM))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> stormCommand(TempestraWeatherType weatherType) {
        return literal(weatherType.id())
                .executes(context -> setWeather(context.getSource(), weatherType, TempestraWeatherSettings.DEFAULT_STORM_DURATION_TICKS))
                .then(argument(DURATION_ARGUMENT, TimeArgumentType.time(1))
                        .executes(context -> setWeather(
                                context.getSource(),
                                weatherType,
                                IntegerArgumentType.getInteger(context, DURATION_ARGUMENT))));
    }

    private static int queryWeather(ServerCommandSource source) {
        TempestraWeatherType weatherType = TempestraWeatherState.get(source.getServer()).weatherType();
        source.sendFeedback(() -> Text.literal("Tempestra weather: " + weatherType.displayName()
                + " (" + weatherType.lightningFrequencyMultiplier() + "x lightning)"), false);
        return Math.round(weatherType.lightningFrequencyMultiplier() * 100.0F);
    }

    private static int setWeather(ServerCommandSource source, TempestraWeatherType weatherType, int durationTicks) {
        ServerWorld world = source.getServer().getOverworld();
        int resolvedDuration = resolveDuration(world, durationTicks);

        TempestraWeatherState.get(source.getServer()).setWeatherType(weatherType);
        world.setWeather(0, resolvedDuration, true, true);
        source.sendFeedback(() -> Text.literal("Set weather to " + weatherType.displayName()
                + " (" + weatherType.lightningFrequencyMultiplier() + "x lightning)."), true);
        return resolvedDuration;
    }

    private static int resolveDuration(ServerWorld world, int durationTicks) {
        if (durationTicks == TempestraWeatherSettings.DEFAULT_STORM_DURATION_TICKS) {
            return ServerWorld.THUNDER_WEATHER_DURATION_PROVIDER.get(world.getRandom());
        }

        return durationTicks;
    }
}
