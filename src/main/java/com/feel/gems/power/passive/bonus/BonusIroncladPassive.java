package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Ironclad - Armor is 25% more effective.
 */
public final class BonusIroncladPassive implements GemPassive {
    private static final Identifier MODIFIER_ID = Identifier.of("gems", "bonus_ironclad");
    private static final double ARMOR_BONUS = 0.25; // 25% increase

    @Override
    public Identifier id() {
        return PowerIds.BONUS_IRONCLAD;
    }

    @Override
    public String name() {
        return "Ironclad";
    }

    @Override
    public String description() {
        return "Your armor provides 25% more protection.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.ARMOR);
        if (attr != null && attr.getModifier(MODIFIER_ID) == null) {
            attr.addPersistentModifier(new EntityAttributeModifier(
                    MODIFIER_ID, ARMOR_BONUS, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.ARMOR);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }
}
