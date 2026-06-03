package moth.tempestra.mixin;

import moth.tempestra.weather.TempestraLightningFrequency;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldLightningFrequencyMixin {
    // Server-authoritative gameplay: client-only installs never run this on remote servers.
    @Inject(method = "tickChunk", at = @At("TAIL"))
    private void tempestra$tickExtraLightning(WorldChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        TempestraLightningFrequency.tickExtraLightning((ServerWorld) (Object) this, chunk);
    }
}
