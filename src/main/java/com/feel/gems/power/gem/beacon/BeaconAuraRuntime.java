package com.feel.gems.power.gem.beacon;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;


public final class BeaconAuraRuntime {
    private static final String KEY_AURA_TYPE = "beaconAuraType";

    public enum AuraType {
        SPEED(PowerIds.BEACON_AURA_SPEED, "Speed", StatusEffects.SPEED),
        HASTE(PowerIds.BEACON_AURA_HASTE, "Haste", StatusEffects.HASTE),
        RESISTANCE(PowerIds.BEACON_AURA_RESISTANCE, "Resistance", StatusEffects.RESISTANCE),
        JUMP(PowerIds.BEACON_AURA_JUMP, "Jump Boost", StatusEffects.JUMP_BOOST),
        STRENGTH(PowerIds.BEACON_AURA_STRENGTH, "Strength", StatusEffects.STRENGTH),
        REGEN(PowerIds.BEACON_AURA_REGEN, "Regeneration", StatusEffects.REGENERATION);

        private final Identifier id;
        private final String label;
        private final RegistryEntry<StatusEffect> effect;

        AuraType(Identifier id, String label, RegistryEntry<StatusEffect> effect) {
            this.id = id;
            this.label = label;
            this.effect = effect;
        }

        public Identifier id() {
            return id;
        }

        public String label() {
            return label;
        }

        public RegistryEntry<StatusEffect> effect() {
            return effect;
        }

        public static AuraType fromId(Identifier id) {
            for (AuraType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return null;
        }
    }

    private BeaconAuraRuntime() {
    }

    public static void setActive(ServerPlayerEntity player, AuraType type) {
        AuraType prev = activeType(player);
        if (type == null) {
            if (prev != null) {
                clearAuraEffects(player, prev);
            }
            clear(player);
            return;
        }
        if (prev != null && prev != type) {
            clearAuraEffects(player, prev);
        }
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        nbt.putString(KEY_AURA_TYPE, type.id().toString());
    }

    public static AuraType activeType(ServerPlayerEntity player) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        Identifier id = Identifier.tryParse(nbt.getString(KEY_AURA_TYPE, ""));
        return AuraType.fromId(id);
    }

    public static void tickEverySecond(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getEnergy(player) <= 0 || GemPlayerState.getActiveGem(player) != GemId.BEACON) {
            clear(player);
            return;
        }

        AuraType type = activeType(player);
        if (type == null) {
            clear(player);
            return;
        }

        int radius = GemsBalance.v().beacon().auraRadiusBlocks();
        int refresh = GemsBalance.v().beacon().auraRefreshTicks();
        if (radius <= 0 || refresh <= 0) {
            return;
        }
        int amplifier = amplifierFor(type);

        ServerWorld world = player.getEntityWorld();
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (net.minecraft.entity.LivingEntity other : world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, box, e -> e.isAlive())) {
            boolean trusted = other instanceof ServerPlayerEntity otherPlayer && (GemTrust.isTrusted(player, otherPlayer) || otherPlayer == player);
            if (trusted) {
                other.addStatusEffect(new StatusEffectInstance(type.effect(), refresh, amplifier, true, false, false));
            } else {
                // Ensure enemies do not retain the positive aura from prior ticks or sources.
                other.removeStatusEffect(type.effect());
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, refresh, 0, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, refresh, 0, true, false, false));
            }
        }
    }

    private static int amplifierFor(AuraType type) {
        var cfg = GemsBalance.v().beacon();
        return switch (type) {
            case SPEED -> cfg.auraSpeedAmplifier();
            case HASTE -> cfg.auraHasteAmplifier();
            case RESISTANCE -> cfg.auraResistanceAmplifier();
            case JUMP -> cfg.auraJumpAmplifier();
            case STRENGTH -> cfg.auraStrengthAmplifier();
            case REGEN -> cfg.auraRegenAmplifier();
        };
    }

    private static void clear(ServerPlayerEntity player) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        nbt.remove(KEY_AURA_TYPE);
    }

    private static void clearAuraEffects(ServerPlayerEntity player, AuraType type) {
        if (type == null) {
            return;
        }
        int radius = GemsBalance.v().beacon().auraRadiusBlocks();
        ServerWorld world = player.getEntityWorld();
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (net.minecraft.entity.LivingEntity other : world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, box, e -> e.isAlive())) {
            other.removeStatusEffect(type.effect());
        }
    }
}
