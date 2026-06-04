package moth.tempestra.mixin;

import moth.tempestra.lightning.TempestraLightningEntityAccess;
import moth.tempestra.lightning.TempestraLightningSplitImpacts;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningEntity.class)
public abstract class LightningEntitySplitMixin implements TempestraLightningEntityAccess {
    @Unique
    private static final TrackedData<Boolean> TEMPESTRA_ALLOWS_SPLITTING =
            DataTracker.registerData(LightningEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique
    private boolean tempestra$splitImpactApplied;

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void tempestra$trackSplitState(CallbackInfo ci) {
        ((LightningEntity) (Object) this).getDataTracker().startTracking(TEMPESTRA_ALLOWS_SPLITTING, true);
    }

    @Inject(method = "setChanneler", at = @At("TAIL"))
    private void tempestra$preventChannelingSplits(ServerPlayerEntity channeler, CallbackInfo ci) {
        if (channeler != null) {
            tempestra$setAllowsSplitting(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tempestra$applySecondarySplitImpact(CallbackInfo ci) {
        LightningEntity lightning = (LightningEntity) (Object) this;
        if (tempestra$splitImpactApplied || lightning.getWorld().isClient) {
            return;
        }

        tempestra$splitImpactApplied = true;
        if (lightning.getWorld() instanceof ServerWorld world) {
            TempestraLightningSplitImpacts.tryApplySecondaryImpact(lightning, world);
        }
    }

    @Override
    public boolean tempestra$allowsSplitting() {
        return ((LightningEntity) (Object) this).getDataTracker().get(TEMPESTRA_ALLOWS_SPLITTING);
    }

    @Override
    public void tempestra$setAllowsSplitting(boolean allowsSplitting) {
        ((LightningEntity) (Object) this).getDataTracker().set(TEMPESTRA_ALLOWS_SPLITTING, allowsSplitting);
    }
}
