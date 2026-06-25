package moth.tempestra.client.lightning;

import net.minecraft.world.phys.Vec3;

public record LightningImpactRay(Vec3 contact, Vec3 offset, float widthScale, long flickerSeed) {
}
