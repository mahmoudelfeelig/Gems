package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/**
 * Overcharge - Next ability deals double damage but costs health.
 * Implementation uses a temporary player state flag consumed on the next damage event.
 */
public final class BonusOverchargeAbility implements GemAbility {
    private static final String OVERCHARGE_UNTIL_KEY = "bonus_overcharge_until";

    @Override
    public Identifier id() {
        return PowerIds.BONUS_OVERCHARGE;
    }

    @Override
    public String name() {
        return "Overcharge";
    }

    @Override
    public String description() {
        return "Your next ability deals double damage but costs health.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().overchargeCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        var cfg = GemsBalance.v().bonusPool();
        float healthCost = cfg.overchargeHealthCost;
        if (healthCost <= 0.0f) {
            return false;
        }
        if (player.getHealth() <= healthCost) {
            return false; // Can't afford health cost
        }
        if (isActive(player)) {
            return false;
        }

        // Pay health cost
        player.damage(world, world.getDamageSources().magic(), healthCost);

        // Mark player for overcharge bonus
        int durationTicks = cfg.overchargeDurationSeconds * 20;
        long until = player.getEntityWorld().getTime() + Math.max(0, durationTicks);
        PlayerStateManager.setPersistent(player, OVERCHARGE_UNTIL_KEY, String.valueOf(until));

        // Energy buildup particles
        for (int i = 0; i < 30; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * 1.5;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + world.random.nextDouble() * 2;
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0.2, 0, 0.05);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 2.0f);
        return true;
    }

    public static boolean isActive(ServerPlayerEntity player) {
        String untilStr = PlayerStateManager.getPersistent(player, OVERCHARGE_UNTIL_KEY);
        if (untilStr == null || untilStr.isEmpty()) {
            return false;
        }
        long until = Long.parseLong(untilStr);
        if (player.getEntityWorld().getTime() > until) {
            PlayerStateManager.clearPersistent(player, OVERCHARGE_UNTIL_KEY);
            return false;
        }
        return true;
    }

    public static float consumeDamageMultiplier(ServerPlayerEntity player) {
        if (!isActive(player)) {
            return 1.0f;
        }
        PlayerStateManager.clearPersistent(player, OVERCHARGE_UNTIL_KEY);
        return GemsBalance.v().bonusPool().overchargeDamageMultiplier;
    }
}
