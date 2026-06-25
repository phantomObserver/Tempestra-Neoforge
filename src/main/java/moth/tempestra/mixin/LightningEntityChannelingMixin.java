package moth.tempestra.mixin;

import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public abstract class LightningEntityChannelingMixin {
    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void tempestra$preventChannelingFire(int spreadAttempts, CallbackInfo ci) {
        if (((LightningBolt) (Object) this).getCause() != null) {
            ci.cancel();
        }
    }
}
