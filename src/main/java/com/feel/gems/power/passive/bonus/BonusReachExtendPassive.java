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
 * Reach Extend - Extended melee and block interaction range.
 * Implementation via attribute modifier.
 */
public final class BonusReachExtendPassive implements GemPassive {
    private static final Identifier MODIFIER_ID_INTERACT = Identifier.of("gems", "bonus_reach_extend_interact");
    private static final Identifier MODIFIER_ID_ATTACK = Identifier.of("gems", "bonus_reach_extend_attack");
    @Override
    public Identifier id() {
        return PowerIds.BONUS_REACH_EXTEND;
    }

    @Override
    public String name() {
        return "Reach Extend";
    }

    @Override
    public String description() {
        return "Extend melee and block interaction range by 1.5 blocks.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        double bonus = GemsBalance.v().bonusPool().reachExtendBlocks;
        EntityAttributeInstance interact = player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
        if (interact != null && interact.getModifier(MODIFIER_ID_INTERACT) == null) {
            interact.addPersistentModifier(new EntityAttributeModifier(
                    MODIFIER_ID_INTERACT, bonus, EntityAttributeModifier.Operation.ADD_VALUE));
        }
        EntityAttributeInstance blockInteract = player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
        if (blockInteract != null && blockInteract.getModifier(MODIFIER_ID_ATTACK) == null) {
            blockInteract.addPersistentModifier(new EntityAttributeModifier(
                    MODIFIER_ID_ATTACK, bonus, EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        EntityAttributeInstance interact = player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
        if (interact != null) {
            interact.removeModifier(MODIFIER_ID_INTERACT);
        }
        EntityAttributeInstance blockInteract = player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
        if (blockInteract != null) {
            blockInteract.removeModifier(MODIFIER_ID_ATTACK);
        }
    }
}
