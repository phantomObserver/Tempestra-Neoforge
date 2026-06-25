package moth.tempestra.client.lightning;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class LightningBoltVisualData {
    private final int entityId;
    private final long seed;
    private final Vec3 origin;
    private final Vec3 contact;
    private final List<LightningPath> paths;
    private final List<LightningImpactRay> impactRays;
    private final List<LightningSpark> sparks;
    private final boolean fullBolt;
    private final float intensity;
    private final int lifetimeTicks;
    private int ageTicks;
    private long lastSeenWorldTime;

    public LightningBoltVisualData(
            int entityId,
            long seed,
            Vec3 origin,
            Vec3 contact,
            List<LightningPath> paths,
            List<LightningImpactRay> impactRays,
            List<LightningSpark> sparks,
            boolean fullBolt,
            float intensity,
            int lifetimeTicks,
            long worldTime
    ) {
        this.entityId = entityId;
        this.seed = seed;
        this.origin = origin;
        this.contact = contact;
        this.paths = List.copyOf(paths);
        this.impactRays = List.copyOf(impactRays);
        this.sparks = new ArrayList<>(sparks);
        this.fullBolt = fullBolt;
        this.intensity = intensity;
        this.lifetimeTicks = lifetimeTicks;
        this.lastSeenWorldTime = worldTime;
    }

    public void tick() {
        ageTicks++;
        Iterator<LightningSpark> iterator = sparks.iterator();
        while (iterator.hasNext()) {
            LightningSpark spark = iterator.next();
            spark.tick();
            if (spark.isExpired()) {
                iterator.remove();
            }
        }
    }

    public boolean isExpired() {
        return ageTicks > lifetimeTicks && sparks.isEmpty();
    }

    public void markSeen(long worldTime) {
        lastSeenWorldTime = worldTime;
    }

    public int entityId() {
        return entityId;
    }

    public long seed() {
        return seed;
    }

    public Vec3 origin() {
        return origin;
    }

    public Vec3 contact() {
        return contact;
    }

    public List<LightningPath> paths() {
        return paths;
    }

    public List<LightningImpactRay> impactRays() {
        return impactRays;
    }

    public List<LightningSpark> sparks() {
        return sparks;
    }

    public boolean fullBolt() {
        return fullBolt;
    }

    public float intensity() {
        return intensity;
    }

    public int ageTicks() {
        return ageTicks;
    }

    public float renderAge(float tickDelta) {
        return ageTicks + tickDelta;
    }

    public float lifetimeProgress(float tickDelta) {
        return Math.min(1.0F, renderAge(tickDelta) / lifetimeTicks);
    }

    public long lastSeenWorldTime() {
        return lastSeenWorldTime;
    }
}
