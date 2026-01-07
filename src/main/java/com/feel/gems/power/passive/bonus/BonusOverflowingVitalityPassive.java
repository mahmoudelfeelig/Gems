package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Overflowing Vitality - +4 max hearts permanently.
 */
public final class BonusOverflowingVitalityPassive implements GemPassive {
    private static final Identifier MODIFIER_ID = Identifier.of("gems", "bonus_overflowing_vitality");
    private static final double BONUS_HEALTH = 8.0; // 4 hearts = 8 HP

    @Override
    public Identifier id() {
        return PowerIds.BONUS_OVERFLOWING_VITALITY;
    }

    @Override
    public String name() {
        return "Overflowing Vitality";
    }

    @Override
    public String description() {
        return "Gain 4 additional hearts of max health.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attr != null && attr.getModifier(MODIFIER_ID) == null) {
            attr.addPersistentModifier(new EntityAttributeModifier(
                    MODIFIER_ID, BONUS_HEALTH, EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }
}
