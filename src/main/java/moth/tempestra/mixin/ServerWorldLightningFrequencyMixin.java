package moth.tempestra.mixin;

import moth.tempestra.weather.TempestraLightningFrequency;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerWorldLightningFrequencyMixin {
    // Server-authoritative gameplay: client-only installs never run this on remote servers.
    @Inject(method = "tickChunk", at = @At("TAIL"))
    private void tempestra$tickExtraLightning(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        TempestraLightningFrequency.tickExtraLightning((ServerLevel) (Object) this, chunk);
    }
}
