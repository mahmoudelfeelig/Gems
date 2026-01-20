package com.feel.gems.power.passive.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Attack Speed - Increased melee attack speed.
 */
public final class BonusAttackSpeedPassive implements GemPassive {
    private static final Identifier MODIFIER_ID = Identifier.of("gems", "bonus_attack_speed");
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ATTACK_SPEED;
    }

    @Override
    public String name() {
        return "Attack Speed";
    }

    @Override
    public String description() {
        return "Gain 15% melee attack speed.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
        if (attr != null && attr.getModifier(MODIFIER_ID) == null) {
            double bonus = GemsBalance.v().bonusPool().attackSpeedBoostPercent / 100.0f;
            attr.addPersistentModifier(new EntityAttributeModifier(
                    MODIFIER_ID, bonus, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }
}
