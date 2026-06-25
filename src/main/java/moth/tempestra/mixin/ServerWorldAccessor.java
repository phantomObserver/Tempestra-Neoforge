package moth.tempestra.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerLevel.class)
public interface ServerWorldAccessor {
    @Invoker("findLightningTargetAround")
    BlockPos tempestra$invokeGetLightningPos(BlockPos pos);
}
