package moth.tempestra.mixin;

import moth.tempestra.lightning.TempestraLightningDamage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityLightningDamageMixin {
    // Keep vanilla's lightning DamageSource so death messages and downstream checks remain unchanged.
    @Redirect(
            method = "thunderHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean tempestra$useScaledLightningDamage(Entity entity, DamageSource source, float amount, ServerLevel world, LightningBolt lightning) {
        if (lightning.getCause() != null) {
            return entity.hurt(source, amount);
        }

        return entity.hurt(source, TempestraLightningDamage.calculate(entity));
    }
}
