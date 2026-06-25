package moth.tempestra.client.lightning;

import net.minecraft.world.phys.Vec3;

public final class LightningSpark {
    private Vec3 position;
    private Vec3 previousPosition;
    private Vec3 velocity;
    private final int lifetimeTicks;
    private final float radius;
    private final int color;
    private int ageTicks;

    public LightningSpark(Vec3 position, Vec3 velocity, int lifetimeTicks, float radius, int color) {
        this.position = position;
        this.previousPosition = position;
        this.velocity = velocity;
        this.lifetimeTicks = lifetimeTicks;
        this.radius = radius;
        this.color = color;
    }

    public void tick() {
        previousPosition = position;
        position = position.add(velocity);
        velocity = velocity.scale(0.86D).add(0.0D, -0.006D, 0.0D);
        ageTicks++;
    }

    public boolean isExpired() {
        return ageTicks >= lifetimeTicks;
    }

    public Vec3 interpolatedPosition(float tickDelta) {
        return previousPosition.lerp(position, tickDelta);
    }

    public Vec3 velocity() {
        return velocity;
    }

    public float ageProgress(float tickDelta) {
        return Math.min(1.0F, (ageTicks + tickDelta) / lifetimeTicks);
    }

    public float radius() {
        return radius;
    }

    public int color() {
        return color;
    }
}
