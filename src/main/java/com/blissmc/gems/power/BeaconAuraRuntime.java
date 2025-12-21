package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class BeaconAuraRuntime {
    private static final String KEY_AURA_TYPE = "beaconAuraType";
    private static final String KEY_AURA_UNTIL = "beaconAuraUntil";

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

    public static void start(ServerPlayerEntity player, AuraType type, int durationTicks) {
        if (durationTicks <= 0 || type == null) {
            return;
        }
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        nbt.putString(KEY_AURA_TYPE, type.id().toString());
        nbt.putLong(KEY_AURA_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static void tickEverySecond(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getEnergy(player) <= 0 || GemPlayerState.getActiveGem(player) != GemId.BEACON) {
            clear(player);
            return;
        }

        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!nbt.contains(KEY_AURA_UNTIL, NbtElement.LONG_TYPE)) {
            return;
        }
        long until = nbt.getLong(KEY_AURA_UNTIL);
        long now = GemsTime.now(player);
        if (until <= 0 || now >= until) {
            clear(player);
            return;
        }

        Identifier id = Identifier.tryParse(nbt.getString(KEY_AURA_TYPE));
        AuraType type = AuraType.fromId(id);
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

        ServerWorld world = player.getServerWorld();
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (ServerPlayerEntity other : world.getEntitiesByClass(ServerPlayerEntity.class, box, p -> true)) {
            if (other != player && !GemTrust.isTrusted(player, other)) {
                continue;
            }
            other.addStatusEffect(new StatusEffectInstance(type.effect(), refresh, amplifier, true, false, false));
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
        nbt.remove(KEY_AURA_UNTIL);
    }
}
