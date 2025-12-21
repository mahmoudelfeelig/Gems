package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

public final class BeaconSupportRuntime {
    private static final String KEY_CORE_NEXT = "beaconCoreNext";

    private BeaconSupportRuntime() {
    }

    public static void tickEverySecond(ServerPlayerEntity player) {
        tickCore(player);
        tickStabilize(player);
    }

    public static void applyRally(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BEACON_RALLY)) {
            return;
        }
        int radius = GemsBalance.v().beacon().rallyRadiusBlocks();
        int duration = GemsBalance.v().beacon().rallyDurationTicks();
        int hearts = GemsBalance.v().beacon().rallyAbsorptionHearts();
        if (radius <= 0 || duration <= 0 || hearts <= 0) {
            return;
        }

        int amplifier = Math.max(0, (hearts / 2) - 1);
        ServerWorld world = player.getServerWorld();
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (ServerPlayerEntity other : world.getEntitiesByClass(ServerPlayerEntity.class, box, p -> true)) {
            if (other != player && !GemTrust.isTrusted(player, other)) {
                continue;
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, amplifier, true, true, false));
        }
    }

    private static void tickCore(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BEACON_CORE)) {
            return;
        }
        int radius = GemsBalance.v().beacon().coreRadiusBlocks();
        int pulse = GemsBalance.v().beacon().corePulsePeriodTicks();
        int duration = GemsBalance.v().beacon().coreRegenDurationTicks();
        int amp = GemsBalance.v().beacon().coreRegenAmplifier();
        if (radius <= 0 || pulse <= 0 || duration <= 0) {
            return;
        }

        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long now = GemsTime.now(player);
        long next = nbt.getLong(KEY_CORE_NEXT);
        if (next > now) {
            return;
        }
        nbt.putLong(KEY_CORE_NEXT, now + pulse);

        ServerWorld world = player.getServerWorld();
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (ServerPlayerEntity other : world.getEntitiesByClass(ServerPlayerEntity.class, box, p -> true)) {
            if (other != player && !GemTrust.isTrusted(player, other)) {
                continue;
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, amp, true, false, false));
        }
    }

    private static void tickStabilize(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BEACON_STABILIZE)) {
            return;
        }
        int radius = GemsBalance.v().beacon().stabilizeRadiusBlocks();
        int reduceTicks = GemsBalance.v().beacon().stabilizeReduceTicksPerSecond();
        if (radius <= 0 || reduceTicks <= 0) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (ServerPlayerEntity other : world.getEntitiesByClass(ServerPlayerEntity.class, box, p -> true)) {
            if (other != player && !GemTrust.isTrusted(player, other)) {
                continue;
            }
            for (StatusEffectInstance effect : java.util.List.copyOf(other.getStatusEffects())) {
                if (effect.getEffectType().value().getCategory() != StatusEffectCategory.HARMFUL) {
                    continue;
                }
                int remaining = effect.getDuration() - reduceTicks;
                if (remaining <= 0) {
                    other.removeStatusEffect(effect.getEffectType());
                    continue;
                }
                other.addStatusEffect(new StatusEffectInstance(
                        effect.getEffectType(),
                        remaining,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.shouldShowParticles(),
                        effect.shouldShowIcon()
                ));
            }
        }
    }
}
