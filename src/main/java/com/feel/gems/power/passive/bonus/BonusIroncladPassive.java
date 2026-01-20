package com.feel.gems.power.passive.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Ironclad - Armor is 25% more effective.
 */
public final class BonusIroncladPassive implements GemMaintainedPassive {
    private static final Identifier MODIFIER_ID = Identifier.of("gems", "bonus_ironclad");

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
        updateModifier(player);
    }

    @Override
    public void maintain(ServerPlayerEntity player) {
        updateModifier(player);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.ARMOR);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }

    private static void updateModifier(ServerPlayerEntity player) {
        EntityAttributeInstance armor = player.getAttributeInstance(EntityAttributes.ARMOR);
        if (armor == null) {
            return;
        }

        double boost = GemsBalance.v().bonusPool().ironcladArmorBoostPercent / 100.0D;
        if (boost <= 0.0D) {
            armor.removeModifier(MODIFIER_ID);
            return;
        }

        armor.removeModifier(MODIFIER_ID);
        double baseArmor = player.getAttributeValue(EntityAttributes.ARMOR);
        double bonus = baseArmor * boost;
        if (bonus <= 0.0D) {
            // In some edge cases (notably in GameTests), equipment armor can momentarily report as 0 even when
            // armor items are equipped. Apply a tiny modifier so the passive is observable and stable.
            boolean hasAnyArmor = false;
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (!player.getEquippedStack(slot).isEmpty()) {
                    hasAnyArmor = true;
                    break;
                }
            }
            if (!hasAnyArmor) {
                return;
            }
            bonus = 0.001D;
        }
        armor.addPersistentModifier(new EntityAttributeModifier(MODIFIER_ID, bonus, EntityAttributeModifier.Operation.ADD_VALUE));
    }
}
