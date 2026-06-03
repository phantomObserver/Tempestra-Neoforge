package moth.tempestra.client.lightning;

import java.util.List;

public record LightningPath(
        LightningPathType type,
        List<LightningSegment> segments,
        float widthScale,
        float alphaScale
) {
    public LightningPath {
        segments = List.copyOf(segments);
    }
}
