package moth.tempestra.lightning;

import moth.tempestra.mixin.LightningEntityAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Difficulty;

import java.util.List;

public final class TempestraLightningSplitImpacts {
    private static final double DAMAGE_RADIUS = 3.0D;
    private static final double DAMAGE_HEIGHT_EXTENSION = 6.0D;

    private TempestraLightningSplitImpacts() {
    }

    public static void tryApplySecondaryImpact(LightningBolt source, ServerLevel world) {
        TempestraLightningSplits.findSecondaryImpact(source, world).ifPresent(contact -> applyImpact(world, contact));
    }

    private static void applyImpact(ServerLevel world, Vec3 contact) {
        if (!world.hasChunkAt(BlockPos.containing(contact))) {
            return;
        }

        LightningBolt impact = EntityType.LIGHTNING_BOLT.create(world);
        if (impact == null) {
            return;
        }

        impact.moveTo(contact.x, contact.y, contact.z, 0.0F, 0.0F);
        Difficulty difficulty = world.getDifficulty();
        if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
            ((LightningEntityAccessor) impact).tempestra$invokeSpawnFire(4);
        }

        AABB damageAABB = new AABB(
                contact.x - DAMAGE_RADIUS,
                contact.y - DAMAGE_RADIUS,
                contact.z - DAMAGE_RADIUS,
                contact.x + DAMAGE_RADIUS,
                contact.y + DAMAGE_HEIGHT_EXTENSION + DAMAGE_RADIUS,
                contact.z + DAMAGE_RADIUS
        );
        List<Entity> struckEntities = world.getEntities(impact, damageAABB, entity -> entity.isAlive() && !(entity instanceof LightningBolt));
        for (Entity entity : struckEntities) {
            entity.thunderHit(world, impact);
        }
    }
}
