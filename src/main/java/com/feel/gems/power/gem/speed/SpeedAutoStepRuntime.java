package com.feel.gems.power.gem.speed;

import com.feel.gems.GemsMod;
import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Manages the auto-step toggle state for Speed gem players.
 * Modifies STEP_HEIGHT attribute to allow stepping up full blocks.
 */
public final class SpeedAutoStepRuntime {
    private static final String KEY_AUTO_STEP = "speedAutoStep";
    private static final Identifier MODIFIER_ID = Identifier.of(GemsMod.MOD_ID, "speed_auto_step");

    private SpeedAutoStepRuntime() {
    }

    public static boolean isActive(ServerPlayerEntity player) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return nbt.getBoolean(KEY_AUTO_STEP, false);
    }

    public static void setActive(ServerPlayerEntity player, boolean active) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        nbt.putBoolean(KEY_AUTO_STEP, active);
        applyModifier(player, active);
    }

    /**
     * Called every second to ensure the modifier is correctly applied based on state.
     */
    public static void tickEverySecond(ServerPlayerEntity player) {
        if (!isActive(player)) {
            removeModifier(player);
            return;
        }

        if (GemPlayerState.getEnergy(player) <= 0) {
            removeModifier(player);
            return;
        }

        GemId activeGem = GemPlayerState.getActiveGem(player);
        if (activeGem != GemId.SPEED) {
            // Also allow Prism gem players with this ability selected
            if (activeGem != GemId.PRISM || !PrismSelectionsState.hasAbility(player, PowerIds.SPEED_AUTO_STEP)) {
                removeModifier(player);
                return;
            }
        }

        applyModifier(player, true);
    }

    /**
     * Clear the auto-step effect (e.g., on gem swap or energy loss).
     */
    public static void clear(ServerPlayerEntity player) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        nbt.remove(KEY_AUTO_STEP);
        removeModifier(player);
    }

    private static void applyModifier(ServerPlayerEntity player, boolean active) {
        EntityAttributeInstance stepHeight = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (stepHeight == null) {
            return;
        }

        // Remove existing modifier first
        stepHeight.removeModifier(MODIFIER_ID);

        if (active) {
            // Default player step height is 0.6 blocks; horses have 1.0
            // Add 0.4 to reach 1.0 (full block step)
            double bonus = GemsBalance.v().speed().autoStepHeightBonus();
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    MODIFIER_ID,
                    bonus,
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
            stepHeight.addTemporaryModifier(modifier);
        }
    }

    private static void removeModifier(ServerPlayerEntity player) {
        EntityAttributeInstance stepHeight = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (stepHeight != null) {
            stepHeight.removeModifier(MODIFIER_ID);
        }
    }
}
