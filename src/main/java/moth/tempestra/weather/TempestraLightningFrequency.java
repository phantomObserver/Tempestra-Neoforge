package moth.tempestra.weather;

import moth.tempestra.mixin.ServerWorldAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public final class TempestraLightningFrequency {
    private TempestraLightningFrequency() {
    }

    public static void tickExtraLightning(ServerLevel world, LevelChunk chunk) {
        if (world.dimension() != Level.OVERWORLD) {
            return;
        }

        TempestraWeatherState state = TempestraWeatherState.get(world.getServer());
        if (!world.isRaining() || !world.isThundering()) {
            state.setWeatherType(TempestraWeatherType.THUNDER_STORM);
            return;
        }

        TempestraWeatherType weatherType = state.weatherType();
        float extraMultiplier = weatherType.extraLightningMultiplier();
        if (extraMultiplier <= 0.0F) {
            return;
        }

        RandomSource random = world.getRandom();
        int guaranteedExtraRolls = (int) extraMultiplier;
        for (int attempt = 0; attempt < guaranteedExtraRolls; attempt++) {
            trySpawnExtraLightning(world, chunk, random, TempestraWeatherSettings.BASE_LIGHTNING_CHANCE);
        }

        float fractionalExtraRoll = extraMultiplier - guaranteedExtraRolls;
        if (fractionalExtraRoll > 0.0F) {
            int fractionalChance = Math.max(1, Math.round(TempestraWeatherSettings.BASE_LIGHTNING_CHANCE / fractionalExtraRoll));
            trySpawnExtraLightning(world, chunk, random, fractionalChance);
        }
    }

    private static void trySpawnExtraLightning(ServerLevel world, LevelChunk chunk, RandomSource random, int chance) {
        if (random.nextInt(chance) != 0) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        BlockPos randomPos = world.getBlockRandomPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ(), 15);
        BlockPos lightningPos = ((ServerWorldAccessor) world).tempestra$invokeGetLightningPos(randomPos);
        if (!world.isRainingAt(lightningPos)) {
            return;
        }

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
        if (lightning == null) {
            return;
        }

        Vec3 position = Vec3.atBottomCenterOf(lightningPos);
        lightning.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        world.addFreshEntity(lightning);
    }
}
