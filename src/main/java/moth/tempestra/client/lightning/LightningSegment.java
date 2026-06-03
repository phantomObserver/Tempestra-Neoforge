package moth.tempestra.client.lightning;

import net.minecraft.util.math.Vec3d;

public record LightningSegment(
        Vec3d start,
        Vec3d end,
        float startProgress,
        float endProgress,
        float localStartProgress,
        float localEndProgress,
        float energy,
        float widthScale,
        long flickerSeed
) {
    public double length() {
        return start.distanceTo(end);
    }
}
