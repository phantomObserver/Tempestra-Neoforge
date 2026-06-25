package moth.butterflyapi.math;

public final class Scalars {
    private Scalars() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp01(double value) {
        return clamp(value, 0.0D, 1.0D);
    }

    public static double lerp(double delta, double start, double end) {
        return start + (end - start) * delta;
    }
}
