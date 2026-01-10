package com.feel.gems.power.gem.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sentinel passive runtime implementations.
 */
public final class SentinelPassiveRuntime {
    private static final String FORTRESS_POS_KEY = "sentinel_fortress_pos";
    private static final String FORTRESS_START_KEY = "sentinel_fortress_start";

    private static final Map<UUID, GuardianAuraCacheEntry> GUARDIAN_AURA_CACHE = new HashMap<>();

    private SentinelPassiveRuntime() {}

    // ========== Guardian Aura ==========
    // Nearby trusted allies take 15% less damage.

    public static boolean isProtectedByGuardianAura(ServerPlayerEntity victim) {
        if (VoidImmunity.hasImmunity(victim)) {
            return false;
        }
        if (!(victim.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        long tick = world.getTime();
        GuardianAuraCacheEntry cached = GUARDIAN_AURA_CACHE.get(victim.getUuid());
        if (cached != null && cached.tick == tick) {
            return cached.protectedByAura;
        }
        if (GUARDIAN_AURA_CACHE.size() > 512) {
            GUARDIAN_AURA_CACHE.clear();
        }
        int range = GemsBalance.v().sentinel().guardianAuraRadiusBlocks();
        Box searchBox = victim.getBoundingBox().expand(range);

        List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
            ServerPlayerEntity.class,
            searchBox,
            p -> p != victim && p.isAlive() && GemPowers.isPassiveActive(p, PowerIds.SENTINEL_GUARDIAN_AURA)
        );

        for (ServerPlayerEntity sentinel : nearbyPlayers) {
            if (GemTrust.isTrusted(sentinel, victim)) {
                GUARDIAN_AURA_CACHE.put(victim.getUuid(), new GuardianAuraCacheEntry(tick, true));
                return true;
            }
        }
        GUARDIAN_AURA_CACHE.put(victim.getUuid(), new GuardianAuraCacheEntry(tick, false));
        return false;
    }

    public static float getGuardianAuraDamageReduction() {
        return GemsBalance.v().sentinel().guardianAuraDamageReduction();
    }

    // ========== Fortress ==========
    // While standing still for 2s, gain Resistance II.

    public static void tickFortress(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.SENTINEL_FORTRESS)) {
            clearFortress(player);
            return;
        }

        BlockPos currentPos = player.getBlockPos();
        String storedPosStr = PlayerStateManager.getPersistent(player, FORTRESS_POS_KEY);
        String startStr = PlayerStateManager.getPersistent(player, FORTRESS_START_KEY);

        String currentPosStr = currentPos.getX() + "," + currentPos.getY() + "," + currentPos.getZ();

        if (storedPosStr == null || !storedPosStr.equals(currentPosStr)) {
            // Player moved - reset timer
            PlayerStateManager.setPersistent(player, FORTRESS_POS_KEY, currentPosStr);
            PlayerStateManager.setPersistent(player, FORTRESS_START_KEY, String.valueOf(player.getEntityWorld().getTime()));
            return;
        }

        if (startStr == null) {
            PlayerStateManager.setPersistent(player, FORTRESS_START_KEY, String.valueOf(player.getEntityWorld().getTime()));
            return;
        }

        long startTime = Long.parseLong(startStr);
        int requiredTicks = GemsBalance.v().sentinel().fortressStandStillTicks();
        
        if (player.getEntityWorld().getTime() - startTime >= requiredTicks) {
            int amplifier = GemsBalance.v().sentinel().fortressResistanceAmplifier();
            int duration = 200; // 10 seconds - persists after movement
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, amplifier, false, false));
        }
    }

    public static void clearFortress(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, FORTRESS_POS_KEY);
        PlayerStateManager.clearPersistent(player, FORTRESS_START_KEY);
    }

    // ========== Retribution Thorns ==========
    // Attackers take 20% of damage dealt back as true damage.

    public static float getRetributionThornsDamagePercent() {
        return GemsBalance.v().sentinel().retributionThornsDamageMultiplier();
    }

    public static boolean hasRetributionThorns(ServerPlayerEntity player) {
        return GemPowers.isPassiveActive(player, PowerIds.SENTINEL_RETRIBUTION_THORNS);
    }

    private record GuardianAuraCacheEntry(long tick, boolean protectedByAura) {}
}
