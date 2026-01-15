package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class BonusBerserkerRageAbility implements GemAbility {
    private static final String BERSERKER_RAGE_UNTIL = "bonus_berserker_rage_until";

    @Override
    public Identifier id() {
        return PowerIds.BONUS_BERSERKER_RAGE;
    }

    @Override
    public String name() {
        return "Berserker Rage";
    }

    @Override
    public String description() {
        return "Gain massive damage but take increased damage for a short duration.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().berserkerRageCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int durationTicks = GemsBalance.v().bonusPool().berserkerRageDurationSeconds * 20;
        if (durationTicks <= 0) {
            return false;
        }
        long until = world.getTime() + durationTicks;
        PlayerStateManager.setPersistent(player, BERSERKER_RAGE_UNTIL, String.valueOf(until));

        world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1, 0.5, 0.1);
        return true;
    }

    public static boolean isActive(ServerPlayerEntity player) {
        String untilStr = PlayerStateManager.getPersistent(player, BERSERKER_RAGE_UNTIL);
        if (untilStr == null || untilStr.isEmpty()) {
            return false;
        }
        long until = Long.parseLong(untilStr);
        if (player.getEntityWorld().getTime() > until) {
            PlayerStateManager.clearPersistent(player, BERSERKER_RAGE_UNTIL);
            return false;
        }
        return true;
    }

    public static float getDamageBoostMultiplier() {
        return 1.0f + (GemsBalance.v().bonusPool().berserkerRageDamageBoostPercent / 100.0f);
    }

    public static float getDamageTakenMultiplier() {
        return 1.0f + (GemsBalance.v().bonusPool().berserkerRageDamageTakenBoostPercent / 100.0f);
    }
}
