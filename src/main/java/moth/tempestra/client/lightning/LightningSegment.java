package moth.tempestra.client.lightning;

import net.minecraft.world.phys.Vec3;

public record LightningSegment(
        Vec3 start,
        Vec3 end,
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
