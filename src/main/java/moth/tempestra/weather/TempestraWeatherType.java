package moth.tempestra.weather;

import java.util.Arrays;

public enum TempestraWeatherType {
    THUNDER_STORM("thunder_storm", "Thunder storm", 1.0F),
    MEDIUM_THUNDER_STORM("medium_thunder_storm", "Medium thunder storm", 1.5F),
    HEAVY_THUNDER_STORM("heavy_thunder_storm", "Heavy thunder storm", 2.0F);

    private final String id;
    private final String displayName;
    private final float lightningFrequencyMultiplier;

    TempestraWeatherType(String id, String displayName, float lightningFrequencyMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.lightningFrequencyMultiplier = lightningFrequencyMultiplier;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public float lightningFrequencyMultiplier() {
        return lightningFrequencyMultiplier;
    }

    public float extraLightningMultiplier() {
        return lightningFrequencyMultiplier - 1.0F;
    }

    public static TempestraWeatherType byId(String id) {
        return Arrays.stream(values())
                .filter(type -> type.id.equals(id))
                .findFirst()
                .orElse(THUNDER_STORM);
    }
}
