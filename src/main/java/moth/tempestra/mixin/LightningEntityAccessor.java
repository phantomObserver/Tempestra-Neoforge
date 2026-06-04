package moth.tempestra.mixin;

import net.minecraft.entity.LightningEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LightningEntity.class)
public interface LightningEntityAccessor {
    @Accessor("cosmetic")
    boolean tempestra$isCosmetic();

    @Invoker("spawnFire")
    void tempestra$invokeSpawnFire(int spreadAttempts);
}
