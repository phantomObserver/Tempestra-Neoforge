package moth.tempestra.client.lightning;

import moth.butterflyapi.math.Basis3;
import moth.butterflyapi.math.Vecs;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientLightningVisualManager {
    private static final Map<Integer, LightningBoltVisualData> ACTIVE_BOLTS = new LinkedHashMap<>();

    private ClientLightningVisualManager() {
    }

    public static void ensureVisual(LightningEntity entity) {
        if (!TempestraLightningVisuals.CUSTOM_VISUALS_ENABLED || !(entity.getWorld() instanceof ClientWorld world)) {
            return;
        }

        LightningBoltVisualData data = ACTIVE_BOLTS.get(entity.getId());
        if (data == null) {
            data = LightningShapeGenerator.generate(entity, world);
            ACTIVE_BOLTS.put(entity.getId(), data);
        }
        data.markSeen(world.getTime());
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            clear();
            return;
        }

        Iterator<LightningBoltVisualData> iterator = ACTIVE_BOLTS.values().iterator();
        while (iterator.hasNext()) {
            LightningBoltVisualData data = iterator.next();
            data.tick();
            if (data.isExpired()) {
                iterator.remove();
            }
        }
    }

    public static void render(WorldRenderContext context) {
        if (ACTIVE_BOLTS.isEmpty() || context.matrixStack() == null || context.consumers() == null) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getLightning());
        for (LightningBoltVisualData data : ACTIVE_BOLTS.values()) {
            renderBolt(positionMatrix, consumer, data, context.tickDelta(), cameraPos);
            renderImpact(positionMatrix, consumer, data, context.tickDelta());
            renderSparks(positionMatrix, consumer, data, context.tickDelta());
        }

        matrices.pop();
    }

    public static void clear() {
        ACTIVE_BOLTS.clear();
    }

    private static void renderBolt(Matrix4f positionMatrix, VertexConsumer consumer, LightningBoltVisualData data, float tickDelta, Vec3d cameraPos) {
        float lifetimeFade = 1.0F - smoothstep(0.58F, 1.0F, data.lifetimeProgress(tickDelta));
        float strikeFlash = 0.72F + flicker(data.seed(), data.renderAge(tickDelta), 0.0F) * 0.34F;
        for (LightningPath path : data.paths()) {
            for (LightningSegment segment : path.segments()) {
                float segmentFlicker = segmentFlicker(path.type(), segment, data.renderAge(tickDelta));
                if (segmentFlicker <= 0.01F) {
                    continue;
                }

                float baseAlphaScale = path.alphaScale() * segment.energy() * segmentFlicker * lifetimeFade * strikeFlash;
                float startAlphaScale = baseAlphaScale * pathFade(path.type(), segment.localStartProgress(), segment.startProgress());
                float endAlphaScale = baseAlphaScale * pathFade(path.type(), segment.localEndProgress(), segment.endProgress());
                if (startAlphaScale <= 0.01F && endAlphaScale <= 0.01F) {
                    continue;
                }

                float distanceReadability = distanceReadability(cameraPos, segment);
                if (path.type() != LightningPathType.MAIN) {
                    float readabilityBoost = 1.0F + distanceReadability * TempestraLightningVisuals.DISTANT_SECONDARY_ALPHA_BOOST;
                    startAlphaScale *= readabilityBoost;
                    endAlphaScale *= readabilityBoost;
                }
                float widthJitter = 0.84F + flicker(segment.flickerSeed() ^ 0x7A1D2E6B62D19F5AL, data.renderAge(tickDelta), 1.0F) * 0.28F;
                float radiusBoost = 1.0F + distanceReadability * TempestraLightningVisuals.DISTANT_RADIUS_BOOST;
                float widthScale = path.widthScale() * segment.widthScale() * widthJitter * (0.8F + data.intensity() * 0.22F) * radiusBoost;
                float startRadiusScale = pathRadiusScale(path.type(), segment.localStartProgress(), segment.startProgress());
                float endRadiusScale = pathRadiusScale(path.type(), segment.localEndProgress(), segment.endProgress());
                float flashBloom = path.type() == LightningPathType.MAIN
                        ? 1.0F - smoothstep(0.0F, 3.8F, data.renderAge(tickDelta))
                        : 0.0F;
                if (flashBloom > 0.01F) {
                    renderSegmentPass(
                            positionMatrix,
                            consumer,
                            segment,
                            TempestraLightningVisuals.MAIN_OUTER_RADIUS * widthScale * startRadiusScale * 1.75F,
                            TempestraLightningVisuals.MAIN_OUTER_RADIUS * widthScale * endRadiusScale * 1.48F,
                            colorForOuter(path.type()),
                            Math.round(38.0F * startAlphaScale * flashBloom),
                            Math.round(30.0F * endAlphaScale * flashBloom)
                    );
                }
                renderSegmentPass(
                        positionMatrix,
                        consumer,
                        segment,
                        TempestraLightningVisuals.MAIN_OUTER_RADIUS * widthScale * startRadiusScale,
                        TempestraLightningVisuals.MAIN_OUTER_RADIUS * widthScale * endRadiusScale,
                        colorForOuter(path.type()),
                        Math.round(108.0F * startAlphaScale),
                        Math.round(108.0F * endAlphaScale)
                );
                renderSegmentPass(
                        positionMatrix,
                        consumer,
                        segment,
                        TempestraLightningVisuals.MAIN_INNER_RADIUS * widthScale * startRadiusScale,
                        TempestraLightningVisuals.MAIN_INNER_RADIUS * widthScale * endRadiusScale,
                        TempestraLightningVisuals.INNER_COLOR,
                        Math.round(190.0F * startAlphaScale),
                        Math.round(190.0F * endAlphaScale)
                );
                if (path.type() == LightningPathType.MAIN || segmentFlicker > 0.46F) {
                    renderSegmentPass(
                            positionMatrix,
                            consumer,
                            segment,
                            TempestraLightningVisuals.MAIN_CORE_RADIUS * widthScale * startRadiusScale,
                            TempestraLightningVisuals.MAIN_CORE_RADIUS * widthScale * endRadiusScale,
                            TempestraLightningVisuals.CORE_COLOR,
                            Math.round(245.0F * startAlphaScale),
                            Math.round(245.0F * endAlphaScale)
                    );
                }
            }
        }
    }

    private static void renderImpact(Matrix4f positionMatrix, VertexConsumer consumer, LightningBoltVisualData data, float tickDelta) {
        float age = data.renderAge(tickDelta);
        float progress = Math.min(1.0F, age / 6.0F);
        float alphaScale = (1.0F - smoothstep(0.18F, 1.0F, progress))
                * (0.65F + flicker(data.seed() ^ 0x432AF7D84C2B6B0DL, age, 2.0F) * 0.35F);
        if (alphaScale <= 0.01F) {
            return;
        }

        for (LightningImpactRay ray : data.impactRays()) {
            Vec3d contact = ray.contact().add(0.0D, 0.08D, 0.0D);
            float rayFlicker = 0.52F + flicker(ray.flickerSeed(), age, 3.0F) * 0.58F;
            Vec3d end = contact.add(ray.offset().multiply(0.55D + progress * 0.55D));
            LightningSegment segment = new LightningSegment(contact, end, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, ray.flickerSeed());
            renderSegmentPass(
                    positionMatrix,
                    consumer,
                    segment,
                    TempestraLightningVisuals.MAIN_INNER_RADIUS * ray.widthScale() * (1.0F - progress * 0.45F),
                    TempestraLightningVisuals.MAIN_INNER_RADIUS * ray.widthScale() * (0.22F + progress * 0.08F),
                    TempestraLightningVisuals.IMPACT_COLOR,
                    Math.round(188.0F * alphaScale * rayFlicker),
                    Math.round(36.0F * alphaScale * rayFlicker)
            );
            renderSegmentPass(
                    positionMatrix,
                    consumer,
                    segment,
                    TempestraLightningVisuals.MAIN_OUTER_RADIUS * 0.42F * ray.widthScale(),
                    TempestraLightningVisuals.MAIN_OUTER_RADIUS * 0.08F * ray.widthScale(),
                    TempestraLightningVisuals.OUTER_COLOR,
                    Math.round(76.0F * alphaScale * rayFlicker),
                    Math.round(18.0F * alphaScale * rayFlicker)
            );
        }
    }

    private static void renderSparks(Matrix4f positionMatrix, VertexConsumer consumer, LightningBoltVisualData data, float tickDelta) {
        for (LightningSpark spark : data.sparks()) {
            float progress = spark.ageProgress(tickDelta);
            float alphaScale = 1.0F - smoothstep(0.25F, 1.0F, progress);
            if (alphaScale <= 0.01F) {
                continue;
            }
            Vec3d current = spark.interpolatedPosition(tickDelta);
            Vec3d tail = current.subtract(spark.velocity().multiply(1.85D));
            LightningSegment segment = new LightningSegment(tail, current, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, data.seed());
            float headRadius = spark.radius() * (1.0F - progress * 0.55F);
            renderSegmentPass(positionMatrix, consumer, segment, headRadius * 0.42F, headRadius, spark.color(), Math.round(74.0F * alphaScale), Math.round(218.0F * alphaScale));
            renderSegmentPass(positionMatrix, consumer, segment, spark.radius() * 0.82F, spark.radius() * 2.2F, TempestraLightningVisuals.OUTER_COLOR, Math.round(18.0F * alphaScale), Math.round(58.0F * alphaScale));
        }
    }

    private static void renderSegmentPass(Matrix4f positionMatrix, VertexConsumer consumer, LightningSegment segment, float radius, int color, int alpha) {
        renderSegmentPass(positionMatrix, consumer, segment, radius, color, alpha, alpha);
    }

    private static void renderSegmentPass(Matrix4f positionMatrix, VertexConsumer consumer, LightningSegment segment, float radius, int color, int startAlpha, int endAlpha) {
        renderSegmentPass(positionMatrix, consumer, segment, radius, radius * 0.84F, color, startAlpha, endAlpha);
    }

    private static void renderSegmentPass(Matrix4f positionMatrix, VertexConsumer consumer, LightningSegment segment, float startRadius, float endRadius, int color, int startAlpha, int endAlpha) {
        if ((startAlpha <= 0 && endAlpha <= 0) || (startRadius <= 0.0F && endRadius <= 0.0F)) {
            return;
        }
        boolean gradientAlpha = startAlpha != endAlpha;
        emitTubeSegment(
                positionMatrix,
                consumer,
                segment.start(),
                segment.end(),
                Math.max(0.0F, startRadius),
                Math.max(0.0F, endRadius),
                red(color),
                green(color),
                blue(color),
                MathHelper.clamp(startAlpha, 0, 255),
                MathHelper.clamp(gradientAlpha ? endAlpha : Math.round(endAlpha * 0.88F), 0, 255),
                TempestraLightningVisuals.TUBE_SIDES
        );
    }

    private static void emitTubeSegment(
            Matrix4f positionMatrix,
            VertexConsumer consumer,
            Vec3d start,
            Vec3d end,
            float startRadius,
            float endRadius,
            int red,
            int green,
            int blue,
            int startAlpha,
            int endAlpha,
            int sides
    ) {
        Vec3d axis = end.subtract(start);
        if (Vecs.lengthSquared(axis) < 1.0E-6D) {
            return;
        }

        Basis3 basis = Basis3.fromForward(axis);
        for (int side = 0; side < sides; side++) {
            double startAngle = (Math.PI * 2.0D * side) / sides;
            double endAngle = (Math.PI * 2.0D * (side + 1)) / sides;
            Vec3d startOffsetA = basis.radial(startAngle, startRadius);
            Vec3d startOffsetB = basis.radial(endAngle, startRadius);
            Vec3d endOffsetA = basis.radial(startAngle, endRadius);
            Vec3d endOffsetB = basis.radial(endAngle, endRadius);
            emitQuad(
                    positionMatrix,
                    consumer,
                    start.add(startOffsetA),
                    start.add(startOffsetB),
                    end.add(endOffsetB),
                    end.add(endOffsetA),
                    red,
                    green,
                    blue,
                    startAlpha,
                    endAlpha
            );
            emitQuad(
                    positionMatrix,
                    consumer,
                    start.add(startOffsetB),
                    start.add(startOffsetA),
                    end.add(endOffsetA),
                    end.add(endOffsetB),
                    red,
                    green,
                    blue,
                    startAlpha,
                    endAlpha
            );
        }
    }

    private static void emitQuad(
            Matrix4f positionMatrix,
            VertexConsumer consumer,
            Vec3d first,
            Vec3d second,
            Vec3d third,
            Vec3d fourth,
            int red,
            int green,
            int blue,
            int startAlpha,
            int endAlpha
    ) {
        consumer.vertex(positionMatrix, (float) first.x, (float) first.y, (float) first.z).color(red, green, blue, startAlpha).next();
        consumer.vertex(positionMatrix, (float) second.x, (float) second.y, (float) second.z).color(red, green, blue, startAlpha).next();
        consumer.vertex(positionMatrix, (float) third.x, (float) third.y, (float) third.z).color(red, green, blue, endAlpha).next();
        consumer.vertex(positionMatrix, (float) fourth.x, (float) fourth.y, (float) fourth.z).color(red, green, blue, endAlpha).next();
    }

    private static float segmentFlicker(LightningPathType type, LightningSegment segment, float age) {
        float sample = flicker(segment.flickerSeed(), age, segment.endProgress() * 6.0F);
        return switch (type) {
            case MAIN -> 0.72F + sample * 0.34F;
            case SPLIT -> sample < 0.08F ? 0.0F : 0.42F + sample * 0.66F;
            case BRANCH -> sample < 0.18F ? 0.0F : 0.34F + sample * 0.70F;
        };
    }

    private static float secondaryFade(LightningPathType type, float localEndProgress) {
        return Math.max(secondaryFadeFloor(type), 1.0F - smoothstep(0.58F, 1.0F, localEndProgress));
    }

    private static float pathFade(LightningPathType type, float localProgress, float globalProgress) {
        if (type == LightningPathType.MAIN) {
            return 1.0F;
        }

        float secondaryFade = secondaryFade(type, localProgress);
        float contactFade = Math.max(secondaryFadeFloor(type) * 0.72F, 1.0F - smoothstep(0.74F, 0.96F, globalProgress));
        return secondaryFade * contactFade;
    }

    private static float pathRadiusScale(LightningPathType type, float localProgress, float globalProgress) {
        return switch (type) {
            case MAIN -> Math.max(
                    0.78F,
                    (0.86F + smoothstep(0.0F, 0.14F, globalProgress) * 0.14F)
                            * (1.0F - smoothstep(0.84F, 1.0F, globalProgress) * 0.12F)
            );
            case SPLIT -> Math.max(0.48F, 1.0F - smoothstep(0.34F, 1.0F, localProgress) * 0.46F);
            case BRANCH -> Math.max(0.30F, 1.0F - smoothstep(0.18F, 1.0F, localProgress) * 0.68F);
        };
    }

    private static float secondaryFadeFloor(LightningPathType type) {
        return switch (type) {
            case MAIN -> 1.0F;
            case SPLIT -> TempestraLightningVisuals.SPLIT_FADE_ALPHA_FLOOR;
            case BRANCH -> TempestraLightningVisuals.BRANCH_FADE_ALPHA_FLOOR;
        };
    }

    private static float distanceReadability(Vec3d cameraPos, LightningSegment segment) {
        Vec3d midpoint = segment.start().add(segment.end()).multiply(0.5D);
        float distance = (float) cameraPos.distanceTo(midpoint);
        return smoothstep(
                TempestraLightningVisuals.DISTANT_READABILITY_START,
                TempestraLightningVisuals.DISTANT_READABILITY_END,
                distance
        );
    }

    private static float flicker(long seed, float age, float salt) {
        long phase = (long) Math.floor((age + salt) * 3.0F);
        long mixed = mix(seed ^ (phase * 0x9E3779B97F4A7C15L) ^ ((long) (salt * 1000.0F) * 0xD1B54A32D192ED03L));
        return ((mixed >>> 40) & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return value;
    }

    private static int colorForOuter(LightningPathType type) {
        return switch (type) {
            case MAIN -> TempestraLightningVisuals.OUTER_COLOR;
            case SPLIT -> TempestraLightningVisuals.VIOLET_EDGE_COLOR;
            case BRANCH -> TempestraLightningVisuals.OUTER_COLOR;
        };
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    private static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }
}
