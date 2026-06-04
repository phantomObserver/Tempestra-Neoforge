package moth.tempestra.client.lightning;

import moth.butterflyapi.math.Angles;
import moth.butterflyapi.math.Basis3;
import moth.butterflyapi.math.Scalars;
import moth.butterflyapi.math.Vecs;
import moth.tempestra.lightning.TempestraLightningSplits;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LightningShapeGenerator {
    private static final double MIN_SEGMENT_LENGTH = 6.0D;
    private static final double MAX_SEGMENT_LENGTH = 20.0D;
    private static final double MIN_GROUND_CLEARANCE = 5.0D;

    private LightningShapeGenerator() {
    }

    public static LightningBoltVisualData generate(LightningEntity entity, ClientWorld world) {
        Vec3d contact = entity.getPos();
        long worldTime = world == null ? 0L : world.getTime();
        long seed = TempestraLightningSplits.seed(entity, contact, worldTime);
        Random random = new Random(seed);
        double cloudBaseY = cloudBaseY(world, contact.y);
        double verticalDistance = cloudBaseY - contact.y;
        boolean fullBolt = verticalDistance >= TempestraLightningVisuals.MIN_FULL_BOLT_VERTICAL_DISTANCE;
        boolean allowSplits = TempestraLightningSplits.canCreateVisualSplits(entity, world);
        float intensity = MathHelper.clamp(0.62F + random.nextFloat() * 0.55F, 0.55F, 1.12F);

        Vec3d origin = fullBolt
                ? chooseCloudOrigin(contact, cloudBaseY, verticalDistance, random)
                : chooseCompactOrigin(contact, random);
        ShapeBuild build = buildPaths(origin, contact, fullBolt, allowSplits, intensity, random, world);
        List<LightningImpactRay> impactRays = buildImpactRays(build.impactContacts(), random, fullBolt, intensity);
        List<LightningSpark> sparks = buildSparks(build.impactContacts(), build.sparkPoints(), random, fullBolt, intensity);
        int lifetime = fullBolt
                ? TempestraLightningVisuals.VISUAL_LIFETIME_TICKS
                : TempestraLightningVisuals.COMPACT_VISUAL_LIFETIME_TICKS;

        return new LightningBoltVisualData(
                entity.getId(),
                seed,
                origin,
                contact,
                build.paths(),
                impactRays,
                sparks,
                fullBolt,
                intensity,
                lifetime,
                worldTime
        );
    }

    private static ShapeBuild buildPaths(
            Vec3d origin,
            Vec3d contact,
            boolean fullBolt,
            boolean allowSplits,
            float intensity,
            Random random,
            ClientWorld world
    ) {
        double totalDistance = origin.distanceTo(contact);
        double verticalDistance = Math.max(1.0D, origin.y - contact.y);
        double horizontalDistance = Math.hypot(origin.x - contact.x, origin.z - contact.z);
        int mainSegments = mainSegmentCount(totalDistance, verticalDistance, horizontalDistance, fullBolt, random);
        List<Vec3d> mainPoints = buildMainPoints(origin, contact, mainSegments, random);
        List<LightningPath> paths = new ArrayList<>(Math.max(4, mainSegments));
        List<LightningSegment> mainPathSegments = new ArrayList<>(mainSegments);
        List<Vec3d> impactContacts = new ArrayList<>(2);
        List<Vec3d> sparkPoints = new ArrayList<>(mainPoints.size() + 8);
        double averageSegmentLength = Math.max(MIN_SEGMENT_LENGTH, totalDistance / mainSegments);
        boolean connectingGroundSplitCreated = false;

        impactContacts.add(contact);
        sparkPoints.addAll(mainPoints);

        for (int index = 0; index < mainPoints.size() - 1; index++) {
            Vec3d start = mainPoints.get(index);
            Vec3d end = mainPoints.get(index + 1);
            float startProgress = index / (float) mainSegments;
            float endProgress = (index + 1) / (float) mainSegments;
            mainPathSegments.add(segment(start, end, startProgress, endProgress, startProgress, endProgress, 1.0F, random));

            if (!fullBolt || !allowSplits) {
                continue;
            }

            if (random.nextFloat() < splitChance(endProgress) * TempestraLightningVisuals.SPLIT_DENSITY * intensity) {
                if (!connectingGroundSplitCreated && random.nextFloat() < TempestraLightningVisuals.CONNECTING_GROUND_SPLIT_CHANCE) {
                    GroundSplit groundSplit = buildConnectingGroundSplit(
                            end,
                            contact,
                            origin,
                            end.subtract(start),
                            endProgress,
                            averageSegmentLength,
                            mainSegments - index,
                            random,
                            world
                    );
                    LightningPath groundPath = groundSplit.path();
                    if (!groundPath.segments().isEmpty()) {
                        paths.add(groundPath);
                        impactContacts.add(groundSplit.contact());
                        sparkPoints.addAll(groundSplit.points());
                        connectingGroundSplitCreated = true;
                    }
                } else {
                    LightningPath split = buildSplitPath(
                            end,
                            contact,
                            origin,
                            end.subtract(start),
                            endProgress,
                            averageSegmentLength,
                            mainSegments,
                            random
                    );
                    if (!split.segments().isEmpty()) {
                        paths.add(split);
                        maybeAddSplitBranches(paths, split, contact, averageSegmentLength, intensity, random);
                    }
                }
            }

            float branchChance = mainBranchChance(startProgress, intensity);
            if (random.nextFloat() < branchChance) {
                LightningPath branch = buildBranchPath(
                        start.add(end).multiply(0.5D),
                        contact,
                        origin,
                        end.subtract(start),
                        averageSegmentLength,
                        endProgress,
                        random
                );
                if (!branch.segments().isEmpty()) {
                    paths.add(branch);
                }
            }
        }

        paths.add(0, new LightningPath(LightningPathType.MAIN, mainPathSegments, 1.0F, 1.0F));
        return new ShapeBuild(paths, impactContacts, sparkPoints);
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

    private static LightningPath buildSplitPath(
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
        List<LightningSegment> segments = new ArrayList<>(segmentCount);

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
            segments.add(segment(current, next, startProgress, globalEnd, localStart, localEnd, 0.72F, random));
            current = next;
            away = Vecs.safeNormalize(away.multiply(0.7D).add(basis.radial(awayAngle + random.nextDouble(-0.75D, 0.75D), 0.6D)), away);
        }

        return new LightningPath(LightningPathType.SPLIT, segments, TempestraLightningVisuals.SPLIT_WIDTH_SCALE, 0.68F);
    }

    private static GroundSplit buildConnectingGroundSplit(
            Vec3d start,
            Vec3d primaryContact,
            Vec3d origin,
            Vec3d parentAxis,
            float startProgress,
            double averageSegmentLength,
            int remainingMainSegments,
            Random random,
            ClientWorld world
    ) {
        Vec3d secondaryContact = TempestraLightningSplits.chooseSecondaryGroundContact(primaryContact, origin, parentAxis, random, world);
        int segmentCount = connectingGroundSplitSegmentCount(start, secondaryContact, averageSegmentLength, remainingMainSegments, random);
        List<Vec3d> points = buildMainPoints(start, secondaryContact, segmentCount, random);
        List<LightningSegment> segments = new ArrayList<>(segmentCount);

        for (int index = 0; index < points.size() - 1; index++) {
            Vec3d current = points.get(index);
            Vec3d next = points.get(index + 1);
            float localStart = index / (float) segmentCount;
            float localEnd = (index + 1) / (float) segmentCount;
            float globalStart = startProgress + ((1.0F - startProgress) * localStart);
            float globalEnd = startProgress + ((1.0F - startProgress) * localEnd);
            segments.add(segment(current, next, globalStart, globalEnd, localStart, localEnd, 0.96F, random));
        }

        return new GroundSplit(
                new LightningPath(LightningPathType.MAIN, segments, 0.94F, 0.94F),
                points,
                secondaryContact
        );
    }

    private static int connectingGroundSplitSegmentCount(
            Vec3d start,
            Vec3d contact,
            double averageSegmentLength,
            int remainingMainSegments,
            Random random
    ) {
        int distanceSegments = (int) Math.round(start.distanceTo(contact) / Math.max(MIN_SEGMENT_LENGTH, averageSegmentLength));
        int segments = distanceSegments + random.nextInt(3);
        int maxSegments = Math.max(3, Math.min(TempestraLightningVisuals.MAX_MAIN_SEGMENTS, remainingMainSegments + 3));
        return MathHelper.clamp(segments, 3, maxSegments);
    }

    private static LightningPath buildBranchPath(
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
        List<LightningSegment> segments = new ArrayList<>(segmentCount);
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
            float globalEnd = Math.min(0.96F, globalProgress + ((1.0F - globalProgress) * localEnd * 0.36F));
            segments.add(segment(current, next, globalProgress, globalEnd, localStart, localEnd, 0.58F, random));
            current = next;
            side = Vecs.safeNormalize(side.multiply(0.7D).add(basis.radial(angle + random.nextDouble(-0.9D, 0.9D), 0.5D)), side);
        }

        return new LightningPath(LightningPathType.BRANCH, segments, TempestraLightningVisuals.BRANCH_WIDTH_SCALE, 0.54F);
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

    private static void maybeAddSplitBranches(
            List<LightningPath> paths,
            LightningPath split,
            Vec3d contact,
            double averageSegmentLength,
            float intensity,
            Random random
    ) {
        List<LightningSegment> splitSegments = split.segments();
        if (splitSegments.isEmpty()) {
            return;
        }

        Vec3d splitOrigin = splitSegments.get(0).start();
        for (LightningSegment segment : splitSegments) {
            float chance = TempestraLightningVisuals.SPLIT_BRANCH_CHANCE_MAX
                    * intensity
                    * (1.0F - segment.localStartProgress());
            if (random.nextFloat() >= chance) {
                continue;
            }
            LightningPath branch = buildBranchPath(
                    segment.start().add(segment.end()).multiply(0.5D),
                    contact,
                    splitOrigin.add(0.0D, Math.max(16.0D, averageSegmentLength * 2.0D), 0.0D),
                    segment.end().subtract(segment.start()),
                    averageSegmentLength * 0.72D,
                    segment.endProgress(),
                    random
            );
            if (!branch.segments().isEmpty()) {
                paths.add(branch);
            }
        }
    }

    private static List<LightningImpactRay> buildImpactRays(List<Vec3d> contacts, Random random, boolean fullBolt, float intensity) {
        int count = fullBolt ? 9 + random.nextInt(5) : 5 + random.nextInt(3);
        double radius = (fullBolt ? 1.5D + intensity * 0.65D : 0.75D) * TempestraLightningVisuals.IMPACT_SIZE_SCALE;
        List<LightningImpactRay> rays = new ArrayList<>(count * contacts.size());
        for (Vec3d contact : contacts) {
            for (int index = 0; index < count; index++) {
                double angle = random.nextDouble(Angles.TAU);
                double length = radius * random.nextDouble(0.45D, 1.0D);
                Vec3d offset = new Vec3d(
                        Math.cos(angle) * length,
                        random.nextDouble(0.02D, 0.16D) * TempestraLightningVisuals.IMPACT_SIZE_SCALE,
                        Math.sin(angle) * length
                );
                rays.add(new LightningImpactRay(
                        contact,
                        offset,
                        (0.55F + random.nextFloat() * 0.55F) * TempestraLightningVisuals.IMPACT_SIZE_SCALE,
                        random.nextLong()
                ));
            }
        }
        return rays;
    }

    private static List<LightningSpark> buildSparks(List<Vec3d> impactContacts, List<Vec3d> sparkPoints, Random random, boolean fullBolt, float intensity) {
        int impactCount = fullBolt ? 14 + random.nextInt(9) : 7 + random.nextInt(5);
        int atmosphericCount = fullBolt && sparkPoints.size() >= 3
                ? Math.min(18, Math.max(6, sparkPoints.size()))
                : 0;
        List<LightningSpark> sparks = new ArrayList<>(impactContacts.size() * impactCount + atmosphericCount);
        for (Vec3d contact : impactContacts) {
            for (int index = 0; index < impactCount; index++) {
                double angle = random.nextDouble(Angles.TAU);
                double speed = random.nextDouble(0.055D, 0.17D) * intensity * TempestraLightningVisuals.IMPACT_SIZE_SCALE;
                Vec3d velocity = new Vec3d(
                        Math.cos(angle) * speed,
                        random.nextDouble(0.06D, 0.18D) * intensity * TempestraLightningVisuals.IMPACT_SIZE_SCALE,
                        Math.sin(angle) * speed
                );
                int lifetime = TempestraLightningVisuals.IMPACT_SPARK_LIFETIME_TICKS + random.nextInt(5);
                float radius = (fullBolt ? random.nextFloat(0.018F, 0.034F) : random.nextFloat(0.012F, 0.024F))
                        * TempestraLightningVisuals.IMPACT_SIZE_SCALE;
                sparks.add(new LightningSpark(contact.add(0.0D, 0.12D, 0.0D), velocity, lifetime, radius, TempestraLightningVisuals.IMPACT_COLOR));
            }
        }

        if (!fullBolt || sparkPoints.size() < 3) {
            return sparks;
        }

        for (int index = 0; index < atmosphericCount; index++) {
            int pointIndex = 1 + random.nextInt(sparkPoints.size() - 2);
            Vec3d base = sparkPoints.get(pointIndex);
            double scatterAngle = random.nextDouble(Angles.TAU);
            double scatter = random.nextDouble(0.35D, 2.2D);
            Vec3d position = base.add(Math.cos(scatterAngle) * scatter, random.nextDouble(-0.8D, 0.8D), Math.sin(scatterAngle) * scatter);
            Vec3d velocity = new Vec3d(
                    Math.cos(scatterAngle) * random.nextDouble(0.012D, 0.042D),
                    random.nextDouble(-0.035D, 0.02D),
                    Math.sin(scatterAngle) * random.nextDouble(0.012D, 0.042D)
            );
            int color = random.nextBoolean() ? TempestraLightningVisuals.OUTER_COLOR : TempestraLightningVisuals.VIOLET_EDGE_COLOR;
            sparks.add(new LightningSpark(position, velocity, TempestraLightningVisuals.ATMOSPHERIC_SPARK_LIFETIME_TICKS + random.nextInt(6), random.nextFloat(0.01F, 0.022F), color));
        }
        return sparks;
    }

    private static LightningSegment segment(
            Vec3d start,
            Vec3d end,
            float startProgress,
            float endProgress,
            float localStart,
            float localEnd,
            float energy,
            Random random
    ) {
        float widthScale = MathHelper.lerp(
                random.nextFloat(),
                TempestraLightningVisuals.SEGMENT_WIDTH_RANDOM_MIN,
                TempestraLightningVisuals.SEGMENT_WIDTH_RANDOM_MAX
        );
        return new LightningSegment(start, end, startProgress, endProgress, localStart, localEnd, energy, widthScale, random.nextLong());
    }

    private static int mainSegmentCount(double totalDistance, double verticalDistance, double horizontalDistance, boolean fullBolt, Random random) {
        if (!fullBolt) {
            return MathHelper.clamp(
                    TempestraLightningVisuals.MIN_COMPACT_SEGMENTS + random.nextInt(TempestraLightningVisuals.MAX_COMPACT_SEGMENTS),
                    TempestraLightningVisuals.MIN_COMPACT_SEGMENTS,
                    TempestraLightningVisuals.MAX_COMPACT_SEGMENTS
            );
        }

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

    private static Vec3d chooseCompactOrigin(Vec3d contact, Random random) {
        double distance = random.nextDouble(1.0D, 7.0D);
        double angle = random.nextDouble(Angles.TAU);
        return new Vec3d(
                contact.x + Math.cos(angle) * distance,
                contact.y + TempestraLightningVisuals.COMPACT_BOLT_HEIGHT + random.nextDouble(-4.0D, 5.0D),
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

    private static double cloudBaseY(ClientWorld world, double contactY) {
        if (world != null) {
            float cloudsHeight = world.getDimensionEffects().getCloudsHeight();
            if (!Float.isNaN(cloudsHeight)) {
                return cloudsHeight;
            }
        }
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

    private record GroundSplit(LightningPath path, List<Vec3d> points, Vec3d contact) {
    }

    private record ShapeBuild(List<LightningPath> paths, List<Vec3d> impactContacts, List<Vec3d> sparkPoints) {
    }
}
