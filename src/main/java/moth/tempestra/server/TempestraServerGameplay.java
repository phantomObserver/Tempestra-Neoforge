package moth.tempestra.server;

import moth.tempestra.weather.TempestraWeatherCommands;

public final class TempestraServerGameplay {
    private TempestraServerGameplay() {
    }

    public static void init() {
        TempestraWeatherCommands.init();
    }
}
