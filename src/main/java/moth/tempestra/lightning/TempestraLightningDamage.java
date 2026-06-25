package moth.tempestra.lightning;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;

public final class TempestraLightningDamage {
    public static final float BASE_DAMAGE = 20.0F;
    public static final float ARMOR_DAMAGE_BONUS_PER_PIECE = 0.2F;
    public static final float IRON_GOLEM_DAMAGE_MULTIPLIER = 0.5F;

    private TempestraLightningDamage() {
    }

    public static float calculate(Entity entity) {
        float damage = BASE_DAMAGE;
        if (entity instanceof LivingEntity livingEntity) {
            damage *= 1.0F + countEquippedArmorPieces(livingEntity) * ARMOR_DAMAGE_BONUS_PER_PIECE;
        }

        if (entity instanceof IronGolem) {
            damage *= IRON_GOLEM_DAMAGE_MULTIPLIER;
        }

        return damage;
    }

    private static int countEquippedArmorPieces(LivingEntity entity) {
        int equippedPieces = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (!stack.isEmpty()) {
                equippedPieces++;
            }
        }
        return equippedPieces;
    }
}
