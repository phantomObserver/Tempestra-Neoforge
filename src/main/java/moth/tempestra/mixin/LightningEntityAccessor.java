package moth.tempestra.mixin;

import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LightningBolt.class)
public interface LightningEntityAccessor {
    @Accessor("visualOnly")
    boolean tempestra$isCosmetic();

    @Invoker("spawnFire")
    void tempestra$invokeSpawnFire(int spreadAttempts);
}
