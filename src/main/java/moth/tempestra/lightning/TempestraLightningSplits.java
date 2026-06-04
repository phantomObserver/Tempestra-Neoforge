package moth.tempestra.lightning;

import moth.butterflyapi.math.Angles;
import moth.butterflyapi.math.Basis3;
import moth.butterflyapi.math.Scalars;
import moth.butterflyapi.math.Vecs;
import moth.tempestra.client.lightning.TempestraLightningVisuals;
import moth.tempestra.mixin.LightningEntityAccessor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class TempestraLightningSplits {
    private static final double MIN_SEGMENT_LENGTH = 6.0D;
    private static final double MAX_SEGMENT_LENGTH = 20.0D;
    private static final double MIN_GROUND_CLEARANCE = 5.0D;
    private static final double LIGHTNING_ROD_STRIKE_EPSILON = 1.0E-6D;

    private TempestraLightningSplits() {
    }

    public static boolean canCreateVisualSplits(LightningEntity entity, WorldView world) {
        if (entity instanceof TempestraLightningEntityAccess access && !access.tempestra$allowsSplitting()) {
            return false;
        }

        return !strikesLightningRod(world, entity.getPos());
    }

    public static Optional<Vec3d> findSecondaryImpact(LightningEntity entity, World world) {
        if (!canCreateGameplaySplits(entity, world)) {
            return Optional.empty();
        }

        Vec3d contact = entity.getPos();
        long seed = seed(entity, contact, world.getTime());
        Random random = new Random(seed);
        double cloudBaseY = cloudBaseY(contact.y);
        double verticalDistance = cloudBaseY - contact.y;
        if (verticalDistance < TempestraLightningVisuals.MIN_FULL_BOLT_VERTICAL_DISTANCE) {
            return Optional.empty();
        }

        float intensity = MathHelper.clamp(0.62F + random.nextFloat() * 0.55F, 0.55F, 1.12F);
        Vec3d origin = chooseCloudOrigin(contact, cloudBaseY, verticalDistance, random);
        return findConnectingGroundSplit(origin, contact, intensity, random, world);
    }

    public static Vec3d chooseSecondaryGroundContact(Vec3d primaryContact, Vec3d origin, Vec3d parentAxis, Random random, WorldView world) {
        double angle = Math.atan2(primaryContact.z - origin.z, primaryContact.x - origin.x)
                + random.nextDouble(-1.2D, 1.2D);
        if (Vecs.lengthSquared(parentAxis) > 1.0E-4D && random.nextBoolean()) {
            angle = Math.atan2(parentAxis.z, parentAxis.x) + random.nextDouble(-0.95D, 0.95D);
        }

        double distance = random.nextDouble(7.0D, 22.0D);
        double x = primaryContact.x + Math.cos(angle) * distance;
        double z = primaryContact.z + Math.sin(angle) * distance;
        return resolveGroundContact(world, x, z, primaryContact.y);
    }

    public static long seed(LightningEntity entity, Vec3d contact, long worldTime) {
        long seed = entity.getUuid().getMostSignificantBits() ^ Long.rotateLeft(entity.getUuid().getLeastSignificantBits(), 17);
        seed ^= ((long) entity.getId()) * 0x9E3779B97F4A7C15L;
        seed ^= Double.doubleToLongBits(contact.x * 31.0D);
        seed ^= Long.rotateLeft(Double.doubleToLongBits(contact.y * 17.0D), 21);
        seed ^= Long.rotateLeft(Double.doubleToLongBits(contact.z * 13.0D), 42);
        seed ^= worldTime * 0xD1B54A32D192ED03L;
        return seed;
    }

    private static boolean canCreateGameplaySplits(LightningEntity entity, WorldView world) {
        if (!canCreateVisualSplits(entity, world) || entity.getChanneler() != null) {
            return false;
        }

        return !(entity instanceof LightningEntityAccessor accessor) || !accessor.tempestra$isCosmetic();
    }

    private static boolean strikesLightningRod(WorldView world, Vec3d pos) {
        if (world == null) {
            return false;
        }

        BlockPos affectedPos = affectedBlockPos(pos);
        return world.isChunkLoaded(affectedPos) && world.getBlockState(affectedPos).isOf(Blocks.LIGHTNING_ROD);
    }

    private static BlockPos affectedBlockPos(Vec3d pos) {
        return BlockPos.ofFloored(pos.x, pos.y - LIGHTNING_ROD_STRIKE_EPSILON, pos.z);
    }

    private static Vec3d resolveGroundContact(WorldView world, double x, double z, double fallbackY) {
        if (world == null) {
            return new Vec3d(x, fallbackY, z);
        }

        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        BlockPos columnPos = BlockPos.ofFloored(x, fallbackY, z);
        if (!world.isChunkLoaded(columnPos)) {
            return new Vec3d(x, fallbackY, z);
        }

        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, blockX, blockZ);
        if (world.isOutOfHeightLimit(topY)) {
            return new Vec3d(x, fallbackY, z);
        }

        return new Vec3d(x, topY, z);
    }

    private static Optional<Vec3d> findConnectingGroundSplit(Vec3d origin, Vec3d contact, float intensity, Random random, WorldView world) {
        double totalDistance = origin.distanceTo(contact);
        double verticalDistance = Math.max(1.0D, origin.y - contact.y);
        double horizontalDistance = Math.hypot(origin.x - contact.x, origin.z - contact.z);
        int mainSegments = mainSegmentCount(totalDistance, verticalDistance, horizontalDistance, random);
        List<Vec3d> mainPoints = buildMainPoints(origin, contact, mainSegments, random);
        double averageSegmentLength = Math.max(MIN_SEGMENT_LENGTH, totalDistance / mainSegments);

        for (int index = 0; index < mainPoints.size() - 1; index++) {
            Vec3d start = mainPoints.get(index);
            Vec3d end = mainPoints.get(index + 1);
            float startProgress = index / (float) mainSegments;
            float endProgress = (index + 1) / (float) mainSegments;
            consumeSegment(random);

            if (random.nextFloat() < splitChance(endProgress) * TempestraLightningVisuals.SPLIT_DENSITY * intensity) {
                if (random.nextFloat() < TempestraLightningVisuals.CONNECTING_GROUND_SPLIT_CHANCE) {
                    return Optional.of(chooseSecondaryGroundContact(contact, origin, end.subtract(start), random, world));
                }

                List<SegmentPlan> split = buildSplitPathPlan(
                        end,
                        contact,
                        origin,
                        end.subtract(start),
                        endProgress,
                        averageSegmentLength,
                        mainSegments,
                        random
                );
                consumeSplitBranches(split, contact, averageSegmentLength, intensity, random);
            }

            float branchChance = mainBranchChance(startProgress, intensity);
            if (random.nextFloat() < branchChance) {
                consumeBranchPath(
                        start.add(end).multiply(0.5D),
                        contact,
                        origin,
                        end.subtract(start),
                        averageSegmentLength,
                        endProgress,
                        random
                );
            }
        }

        return Optional.empty();
    }

    private static List<Vec3d> buildMainPoints(Vec3d origin, Vec3d contact, int segments, Random random) {
        double[] weights = new double[segments];
        double totalWeight = 0.0D;
        for (int index = 0; index < segments; index++) {
            double weight = 0.82D + random.nextDouble() * 0.36D;
            weights[index] = weight;
            totalWeight += weight;
        }

        Vec3d boltAxis = contact.subtract(origin);
        Basis3 basis = Basis3.fromForward(boltAxis);
        double totalDistance = origin.distanceTo(contact);
        double horizontalDistance = Math.hypot(origin.x - contact.x, origin.z - contact.z);
        double displacementBase = Scalars.clamp((totalDistance * 0.052D) + (horizontalDistance * 0.1D), 1.4D, 13.5D);

        List<Vec3d> points = new ArrayList<>(segments + 1);
        points.add(origin);
        double cumulative = 0.0D;
        double previousAngle = random.nextDouble(Angles.TAU);
        for (int index = 1; index < segments; index++) {
            cumulative += weights[index - 1];
            double progress = cumulative / totalWeight;
            double middleStrength = Math.sin(progress * Math.PI);
            double curl = random.nextDouble(-0.85D, 0.85D);
            previousAngle += random.nextDouble(-1.35D, 1.35D) + curl;
            double displacement = displacementBase * middleStrength * random.nextDouble(0.45D, 1.08D);
            Vec3d basePoint = origin.lerp(contact, progress);
            Vec3d offset = basis.radial(previousAngle, displacement);
            points.add(basePoint.add(offset));
        }
        points.add(contact);
        return points;
    }

    private static List<SegmentPlan> buildSplitPathPlan(
            Vec3d start,
            Vec3d contact,
            Vec3d origin,
            Vec3d parentAxis,
            float startProgress,
            double averageSegmentLength,
            int mainSegments,
            Random random
    ) {
        double heightFraction = Scalars.clamp01((start.y - contact.y) / Math.max(1.0D, origin.y - contact.y));
        int maxSegments = Math.max(2, (int) Math.ceil(mainSegments * 0.66D * heightFraction));
        int segmentCount = MathHelper.clamp(2 + random.nextInt(Math.max(1, maxSegments)), 2, Math.max(2, maxSegments));
        double segmentLength = Scalars.clamp(averageSegmentLength * random.nextDouble(0.75D, 1.08D), MIN_SEGMENT_LENGTH, MAX_SEGMENT_LENGTH);
        Vec3d mainDirection = Vecs.safeNormalize(contact.subtract(start), new Vec3d(0.0D, -1.0D, 0.0D));
        Basis3 basis = Basis3.fromForward(Vecs.safeNormalize(parentAxis, mainDirection));
        double awayAngle = random.nextDouble(Angles.TAU);
        Vec3d away = basis.radial(awayAngle, 1.0D);
        Vec3d current = start;
        List<SegmentPlan> segments = new ArrayList<>(segmentCount);

        for (int index = 0; index < segmentCount; index++) {
            float localStart = index / (float) segmentCount;
            float localEnd = (index + 1) / (float) segmentCount;
            double downwardBias = Scalars.lerp(localStart, 0.58D, 0.38D);
            double awayBias = Scalars.lerp(localStart, 0.9D, 1.28D);
            Vec3d direction = mainDirection.multiply(downwardBias)
                    .add(away.multiply(awayBias))
                    .add(0.0D, -0.28D - random.nextDouble() * 0.24D, 0.0D);
            direction = Vecs.safeNormalize(direction, away.add(0.0D, -0.45D, 0.0D));
            Vec3d next = current.add(direction.multiply(segmentLength * random.nextDouble(0.72D, 1.16D)));
            double minY = contact.y + MIN_GROUND_CLEARANCE + ((origin.y - contact.y) * 0.08D * (1.0D - localEnd));
            if (next.y < minY) {
                next = Vecs.withY(next, minY + random.nextDouble(0.0D, 2.5D));
            }
            float globalEnd = Math.min(0.94F, startProgress + ((1.0F - startProgress) * localEnd * 0.72F));
            consumeSegment(random);
            segments.add(new SegmentPlan(current, next, localStart, globalEnd));
            current = next;
            away = Vecs.safeNormalize(away.multiply(0.7D).add(basis.radial(awayAngle + random.nextDouble(-0.75D, 0.75D), 0.6D)), away);
        }

        return segments;
    }

    private static void consumeBranchPath(
            Vec3d start,
            Vec3d contact,
            Vec3d origin,
            Vec3d parentAxis,
            double averageSegmentLength,
            float globalProgress,
            Random random
    ) {
        float lengthScale = branchLengthScale(globalProgress);
        int segmentCount = branchSegmentCount(lengthScale, random);
        double segmentLength = Scalars.clamp(averageSegmentLength * lengthScale * random.nextDouble(0.62D, 1.04D), 5.0D, 19.5D);
        Vec3d down = Vecs.safeNormalize(contact.subtract(start), new Vec3d(0.0D, -1.0D, 0.0D));
        Basis3 basis = Basis3.fromForward(Vecs.safeNormalize(parentAxis, down));
        double angle = random.nextDouble(Angles.TAU);
        Vec3d side = basis.radial(angle, 1.0D);
        Vec3d current = start;
        double longBranchBias = MathHelper.clamp((lengthScale - 1.18F) / 0.72F, 0.0F, 1.0F);

        for (int index = 0; index < segmentCount; index++) {
            float localStart = index / (float) segmentCount;
            float localEnd = (index + 1) / (float) segmentCount;
            boolean firstSegment = index == 0;
            double downBias = Scalars.lerp(localStart, 0.52D + longBranchBias * 0.38D, 0.36D + longBranchBias * 0.28D);
            double sideBias = Scalars.lerp(localStart, 0.42D - longBranchBias * 0.14D, 0.98D - longBranchBias * 0.22D);
            double verticalDrop = 0.24D + longBranchBias * 0.22D + random.nextDouble() * (0.12D + longBranchBias * 0.08D);
            if (firstSegment) {
                downBias += 0.48D + longBranchBias * 0.2D;
                sideBias *= 0.42D;
                verticalDrop += 0.34D;
            }

            Vec3d direction = down.multiply(downBias)
                    .add(side.multiply(sideBias))
                    .add(0.0D, -verticalDrop, 0.0D);
            direction = Vecs.safeNormalize(direction, down.add(0.0D, -0.65D, 0.0D));
            Vec3d next = current.add(direction.multiply(segmentLength * random.nextDouble(0.72D, 1.14D)));
            double minY = contact.y + MIN_GROUND_CLEARANCE + ((origin.y - contact.y) * 0.04D);
            if (next.y < minY) {
                next = Vecs.withY(next, minY + random.nextDouble(0.5D, 2.2D));
            }
            consumeSegment(random);
            current = next;
            side = Vecs.safeNormalize(side.multiply(0.7D).add(basis.radial(angle + random.nextDouble(-0.9D, 0.9D), 0.5D)), side);
        }
    }

    private static int branchSegmentCount(float lengthScale, Random random) {
        float boost = MathHelper.clamp((lengthScale - 1.18F) / 0.72F, 0.0F, 1.0F);
        float roll = random.nextFloat();
        float twoSegmentChance = MathHelper.lerp(boost, 0.56F, 0.22F);
        float fourSegmentChance = MathHelper.lerp(boost, 0.08F, 0.34F);
        if (roll < twoSegmentChance) {
            return 2;
        }
        return roll > 1.0F - fourSegmentChance ? 4 : 3;
    }

    private static float branchLengthScale(float globalProgress) {
        float upperMiddleBoost = smoothstep(0.12F, 0.28F, globalProgress)
                * (1.0F - smoothstep(0.66F, 0.90F, globalProgress));
        return 1.18F + upperMiddleBoost * 0.72F;
    }

    private static void consumeSplitBranches(
            List<SegmentPlan> split,
            Vec3d contact,
            double averageSegmentLength,
            float intensity,
            Random random
    ) {
        if (split.isEmpty()) {
            return;
        }

        Vec3d splitOrigin = split.get(0).start();
        for (SegmentPlan segment : split) {
            float chance = TempestraLightningVisuals.SPLIT_BRANCH_CHANCE_MAX
                    * intensity
                    * (1.0F - segment.localStartProgress());
            if (random.nextFloat() >= chance) {
                continue;
            }

            consumeBranchPath(
                    segment.start().add(segment.end()).multiply(0.5D),
                    contact,
                    splitOrigin.add(0.0D, Math.max(16.0D, averageSegmentLength * 2.0D), 0.0D),
                    segment.end().subtract(segment.start()),
                    averageSegmentLength * 0.72D,
                    segment.endProgress(),
                    random
            );
        }
    }

    private static void consumeSegment(Random random) {
        random.nextFloat();
        random.nextLong();
    }

    private static int mainSegmentCount(double totalDistance, double verticalDistance, double horizontalDistance, Random random) {
        double distanceFactor = totalDistance / 17.0D;
        double diagonalFactor = horizontalDistance / 28.0D;
        double verticalFactor = verticalDistance / 75.0D;
        int segments = (int) Math.round(distanceFactor + diagonalFactor + verticalFactor + random.nextDouble(-1.15D, 1.25D));
        return MathHelper.clamp(segments, TempestraLightningVisuals.MIN_MAIN_SEGMENTS, TempestraLightningVisuals.MAX_MAIN_SEGMENTS);
    }

    private static Vec3d chooseCloudOrigin(Vec3d contact, double cloudBaseY, double verticalDistance, Random random) {
        HorizontalBand band = chooseBand(verticalDistance, random);
        double distance = random.nextDouble(band.minDistance(), band.maxDistance());
        double angle = random.nextDouble(Angles.TAU);
        return new Vec3d(
                contact.x + Math.cos(angle) * distance,
                cloudBaseY,
                contact.z + Math.sin(angle) * distance
        );
    }

    private static HorizontalBand chooseBand(double verticalDistance, Random random) {
        double tallness = Scalars.clamp01((verticalDistance - TempestraLightningVisuals.MIN_FULL_BOLT_VERTICAL_DISTANCE) / 128.0D);
        double roll = random.nextDouble();
        double closeChance = Scalars.lerp(tallness, 0.78D, 0.18D);
        double middleChance = Scalars.lerp(tallness, 0.18D, 0.37D);
        if (roll < closeChance) {
            return HorizontalBand.CLOSE;
        }
        if (roll < closeChance + middleChance) {
            return HorizontalBand.MIDDLE;
        }
        return HorizontalBand.FAR;
    }

    private static float splitChance(float progress) {
        if (progress < 0.16F || progress > 0.94F) {
            return 0.0F;
        }
        if (progress < 0.32F) {
            return MathHelper.lerp((progress - 0.16F) / 0.16F, 0.0F, 0.30F);
        }
        if (progress < 0.48F) {
            return MathHelper.lerp((progress - 0.32F) / 0.16F, 0.30F, 0.45F);
        }
        if (progress < 0.62F) {
            return MathHelper.lerp((progress - 0.48F) / 0.14F, 0.45F, 0.30F);
        }
        if (progress < 0.74F) {
            return MathHelper.lerp((progress - 0.62F) / 0.12F, 0.30F, 0.20F);
        }
        if (progress < 0.84F) {
            return MathHelper.lerp((progress - 0.74F) / 0.10F, 0.20F, 0.10F);
        }
        return MathHelper.lerp((progress - 0.84F) / 0.10F, 0.10F, 0.0F);
    }

    private static float mainBranchChance(float progress, float intensity) {
        float upperMiddleBias = (float) (Math.sin(MathHelper.clamp(progress, 0.0F, 1.0F) * Math.PI) * 0.72F + 0.28F);
        float contactFade = 1.0F - smoothstep(0.68F, 0.96F, progress);
        float originFade = smoothstep(0.04F, 0.18F, progress);
        return TempestraLightningVisuals.MAIN_BRANCH_CHANCE_MAX * intensity * upperMiddleBias * contactFade * originFade * 0.72F;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static double cloudBaseY(double contactY) {
        return Math.max(TempestraLightningVisuals.FALLBACK_CLOUD_BASE_Y, contactY + TempestraLightningVisuals.COMPACT_BOLT_HEIGHT);
    }

    private enum HorizontalBand {
        CLOSE(0.0D, 16.0D),
        MIDDLE(16.0D, 32.0D),
        FAR(32.0D, 64.0D);

        private final double minDistance;
        private final double maxDistance;

        HorizontalBand(double minDistance, double maxDistance) {
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
        }

        double minDistance() {
            return minDistance;
        }

        double maxDistance() {
            return maxDistance;
        }
    }

    private record SegmentPlan(Vec3d start, Vec3d end, float localStartProgress, float endProgress) {
    }
}
