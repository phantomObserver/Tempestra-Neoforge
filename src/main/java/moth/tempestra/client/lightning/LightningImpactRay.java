package moth.tempestra.client.lightning;

import net.minecraft.util.math.Vec3d;

public record LightningImpactRay(Vec3d contact, Vec3d offset, float widthScale, long flickerSeed) {
}
