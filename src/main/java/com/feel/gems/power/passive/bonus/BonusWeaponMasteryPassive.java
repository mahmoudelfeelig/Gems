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
 * Weapon Mastery - Deal increased damage with all weapons.
 * Implementation via attribute modifier.
 */
public final class BonusWeaponMasteryPassive implements GemPassive {
    private static final Identifier MODIFIER_ID = Identifier.of("gems", "bonus_weapon_mastery");
    @Override
    public Identifier id() {
        return PowerIds.BONUS_WEAPON_MASTERY;
    }

    @Override
    public String name() {
        return "Weapon Mastery";
    }

    @Override
    public String description() {
        return "Gain +1 attack damage with all weapons.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (attr != null && attr.getModifier(MODIFIER_ID) == null) {
            attr.addPersistentModifier(new EntityAttributeModifier(
                    MODIFIER_ID,
                    GemsBalance.v().bonusPool().weaponMasteryBonusDamage,
                    EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
        }
    }
}
