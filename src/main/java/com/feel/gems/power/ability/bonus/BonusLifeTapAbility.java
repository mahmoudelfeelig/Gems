package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class BonusLifeTapAbility implements GemAbility {
    private static final String KEY_COOLDOWNS = "cooldowns";

    @Override
    public Identifier id() {
        return PowerIds.BONUS_LIFE_TAP;
    }

    @Override
    public String name() {
        return "Life Tap";
    }

    @Override
    public String description() {
        return "Sacrifice health to reduce your current cooldowns.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().lifeTapCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();

        float cost = Math.max(0.0F, GemsBalance.v().bonusPool().lifeTapHealthCost);
        if (player.getHealth() <= cost) {
            return false;
        }

        player.setHealth(Math.max(0.0F, player.getHealth() - cost));

        float reductionPercent = Math.max(0.0F, GemsBalance.v().bonusPool().lifeTapCooldownReductionPercent);
        if (reductionPercent > 0.0F) {
            reduceCooldowns(player, reductionPercent);
        }

        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1, player.getZ(), 18, 0.5, 1, 0.5, 0.1);
        return true;
    }

    private static void reduceCooldowns(ServerPlayerEntity player, float reductionPercent) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound cooldowns = root.getCompound(KEY_COOLDOWNS).orElse(null);
        if (cooldowns == null || cooldowns.getKeys().isEmpty()) {
            return;
        }

        long now = GemsTime.now(player);
        float factor = 1.0F - (Math.min(100.0F, reductionPercent) / 100.0F);
        String selfKey = PowerIds.BONUS_LIFE_TAP.toString();

        for (String key : java.util.List.copyOf(cooldowns.getKeys())) {
            if (selfKey.equals(key)) {
                continue;
            }
            long nextAllowed = cooldowns.getLong(key, 0L);
            if (nextAllowed <= now) {
                continue;
            }
            long remaining = nextAllowed - now;
            long reducedRemaining = (long) Math.ceil(remaining * factor);
            cooldowns.putLong(key, now + Math.max(0L, reducedRemaining));
        }
    }
}
