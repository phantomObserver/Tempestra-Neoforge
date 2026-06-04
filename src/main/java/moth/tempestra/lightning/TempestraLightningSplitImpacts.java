package moth.tempestra.lightning;

import moth.tempestra.mixin.LightningEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;

import java.util.List;

public final class TempestraLightningSplitImpacts {
    private static final double DAMAGE_RADIUS = 3.0D;
    private static final double DAMAGE_HEIGHT_EXTENSION = 6.0D;

    private TempestraLightningSplitImpacts() {
    }

    public static void tryApplySecondaryImpact(LightningEntity source, ServerWorld world) {
        TempestraLightningSplits.findSecondaryImpact(source, world).ifPresent(contact -> applyImpact(world, contact));
    }

    private static void applyImpact(ServerWorld world, Vec3d contact) {
        if (!world.isChunkLoaded(BlockPos.ofFloored(contact))) {
            return;
        }

        LightningEntity impact = EntityType.LIGHTNING_BOLT.create(world);
        if (impact == null) {
            return;
        }

        impact.refreshPositionAfterTeleport(contact);
        Difficulty difficulty = world.getDifficulty();
        if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
            ((LightningEntityAccessor) impact).tempestra$invokeSpawnFire(4);
        }

        Box damageBox = new Box(
                contact.x - DAMAGE_RADIUS,
                contact.y - DAMAGE_RADIUS,
                contact.z - DAMAGE_RADIUS,
                contact.x + DAMAGE_RADIUS,
                contact.y + DAMAGE_HEIGHT_EXTENSION + DAMAGE_RADIUS,
                contact.z + DAMAGE_RADIUS
        );
        List<Entity> struckEntities = world.getOtherEntities(impact, damageBox, entity -> entity.isAlive() && !(entity instanceof LightningEntity));
        for (Entity entity : struckEntities) {
            entity.onStruckByLightning(world, impact);
        }
    }
}
