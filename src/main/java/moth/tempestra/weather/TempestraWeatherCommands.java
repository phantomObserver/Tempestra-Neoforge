package moth.tempestra.weather;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class TempestraWeatherCommands {
    private static final String DURATION_ARGUMENT = "duration";

    private TempestraWeatherCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("tempestra")
                .requires(source -> source.hasPermission(2))
                .then(literal("weather")
                        .executes(context -> queryWeather(context.getSource()))
                        .then(stormCommand(TempestraWeatherType.THUNDER_STORM))
                        .then(stormCommand(TempestraWeatherType.MEDIUM_THUNDER_STORM))
                        .then(stormCommand(TempestraWeatherType.HEAVY_THUNDER_STORM))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> stormCommand(TempestraWeatherType weatherType) {
        return literal(weatherType.id())
                .executes(context -> setWeather(context.getSource(), weatherType, TempestraWeatherSettings.DEFAULT_STORM_DURATION_TICKS))
                .then(argument(DURATION_ARGUMENT, TimeArgument.time(1))
                        .executes(context -> setWeather(
                                context.getSource(),
                                weatherType,
                                IntegerArgumentType.getInteger(context, DURATION_ARGUMENT))));
    }

    private static int queryWeather(CommandSourceStack source) {
        TempestraWeatherType weatherType = TempestraWeatherState.get(source.getServer()).weatherType();
        source.sendSuccess(() -> Component.literal("Tempestra weather: " + weatherType.displayName()
                + " (" + weatherType.lightningFrequencyMultiplier() + "x lightning)"), false);
        return Math.round(weatherType.lightningFrequencyMultiplier() * 100.0F);
    }

    private static int setWeather(CommandSourceStack source, TempestraWeatherType weatherType, int durationTicks) {
        ServerLevel world = source.getServer().overworld();
        int resolvedDuration = resolveDuration(world, durationTicks);

        TempestraWeatherState.get(source.getServer()).setWeatherType(weatherType);
        world.setWeatherParameters(0, resolvedDuration, true, true);
        source.sendSuccess(() -> Component.literal("Set weather to " + weatherType.displayName()
                + " (" + weatherType.lightningFrequencyMultiplier() + "x lightning)."), true);
        return resolvedDuration;
    }

    private static int resolveDuration(ServerLevel world, int durationTicks) {
        if (durationTicks == TempestraWeatherSettings.DEFAULT_STORM_DURATION_TICKS) {
            return ServerLevel.THUNDER_DURATION.sample(world.getRandom());
        }

        return durationTicks;
    }
}
