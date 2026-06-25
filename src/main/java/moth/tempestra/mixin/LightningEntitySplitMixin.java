package moth.tempestra.mixin;

import moth.tempestra.lightning.TempestraLightningEntityAccess;
import moth.tempestra.lightning.TempestraLightningSplitImpacts;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
public abstract class LightningEntitySplitMixin implements TempestraLightningEntityAccess {
    @Unique
    private static final EntityDataAccessor<Boolean> TEMPESTRA_ALLOWS_SPLITTING =
            SynchedEntityData.defineId(LightningBolt.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private boolean tempestra$splitImpactApplied;

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void tempestra$trackSplitState(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(TEMPESTRA_ALLOWS_SPLITTING, true);
    }

    @Inject(method = "setCause", at = @At("TAIL"))
    private void tempestra$preventChannelingSplits(ServerPlayer cause, CallbackInfo ci) {
        if (cause != null) {
            tempestra$setAllowsSplitting(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tempestra$applySecondarySplitImpact(CallbackInfo ci) {
        LightningBolt lightning = (LightningBolt) (Object) this;
        if (tempestra$splitImpactApplied || lightning.level().isClientSide) {
            return;
        }

        tempestra$splitImpactApplied = true;
        if (lightning.level() instanceof ServerLevel world) {
            TempestraLightningSplitImpacts.tryApplySecondaryImpact(lightning, world);
        }
    }

    @Override
    public boolean tempestra$allowsSplitting() {
        return ((LightningBolt) (Object) this).getEntityData().get(TEMPESTRA_ALLOWS_SPLITTING);
    }

    @Override
    public void tempestra$setAllowsSplitting(boolean allowsSplitting) {
        ((LightningBolt) (Object) this).getEntityData().set(TEMPESTRA_ALLOWS_SPLITTING, allowsSplitting);
    }
}
