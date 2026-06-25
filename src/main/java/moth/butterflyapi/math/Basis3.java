package moth.butterflyapi.math;

import net.minecraft.world.phys.Vec3;

public record Basis3(Vec3 right, Vec3 up, Vec3 forward) {
    public static Basis3 fromForward(Vec3 forward) {
        Vec3 f = Vecs.safeNormalize(forward, new Vec3(0.0D, -1.0D, 0.0D));
        Vec3 reference = Math.abs(f.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = reference.cross(f).normalize();
        Vec3 up = f.cross(right).normalize();
        return new Basis3(right, up, f);
    }

    public Vec3 radial(double angle, double radius) {
        return right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
    }
}
