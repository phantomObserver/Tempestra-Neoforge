package moth.tempestra.weather;

import moth.tempestra.mixin.ServerWorldAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public final class TempestraLightningFrequency {
    private TempestraLightningFrequency() {
    }

    public static void tickExtraLightning(ServerWorld world, WorldChunk chunk) {
        if (!world.getRegistryKey().equals(World.OVERWORLD)) {
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

        Random random = world.getRandom();
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

    private static void trySpawnExtraLightning(ServerWorld world, WorldChunk chunk, Random random, int chance) {
        if (random.nextInt(chance) != 0) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        BlockPos randomPos = world.getRandomPosInChunk(chunkPos.getStartX(), 0, chunkPos.getStartZ(), 15);
        BlockPos lightningPos = ((ServerWorldAccessor) world).tempestra$invokeGetLightningPos(randomPos);
        if (!world.hasRain(lightningPos)) {
            return;
        }

        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
        if (lightning == null) {
            return;
        }

        lightning.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(lightningPos));
        world.spawnEntity(lightning);
    }
}
