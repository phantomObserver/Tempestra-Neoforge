package moth.tempestra.mixin;

import moth.tempestra.lightning.TempestraLightningDamage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityLightningDamageMixin {
    // Keep vanilla's lightning DamageSource so death messages and downstream checks remain unchanged.
    @Redirect(
            method = "onStruckByLightning",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
            )
    )
    private boolean tempestra$useScaledLightningDamage(Entity entity, DamageSource source, float amount, ServerWorld world, LightningEntity lightning) {
        if (lightning.getChanneler() != null) {
            return entity.damage(source, amount);
        }

        return entity.damage(source, TempestraLightningDamage.calculate(entity));
    }
}
