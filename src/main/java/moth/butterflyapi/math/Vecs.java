package moth.butterflyapi.math;

import net.minecraft.world.phys.Vec3;

public final class Vecs {
    private Vecs() {
    }

    public static double lengthSquared(Vec3 vector) {
        return vector.lengthSqr();
    }

    public static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        return vector.lengthSqr() < 1.0E-12D ? fallback : vector.normalize();
    }

    public static Vec3 withY(Vec3 vector, double y) {
        return new Vec3(vector.x, y, vector.z);
    }
}
