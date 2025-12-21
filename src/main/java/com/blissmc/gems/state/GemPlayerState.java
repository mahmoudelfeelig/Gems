package com.feel.gems.state;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.EnumSet;
import com.feel.gems.assassin.AssassinState;

public final class GemPlayerState {
    private static final String KEY_ACTIVE_GEM = "activeGem";
    private static final String KEY_ENERGY = "energy";
    private static final String KEY_ENERGY_CAP_PENALTY = "energyCapPenalty";
    private static final String KEY_MAX_HEARTS = "maxHearts";
    private static final String KEY_OWNED_GEMS = "ownedGems";
    private static final String KEY_GEM_EPOCH = "gemEpoch";

    public static final int DEFAULT_ENERGY = 3;
    public static final int MIN_ENERGY = 0;
    public static final int MAX_ENERGY = 10;

    public static final int DEFAULT_MAX_HEARTS = 10;
    public static final int MIN_MAX_HEARTS = 5;
    public static final int MAX_MAX_HEARTS = 20;

    private static final Identifier HEARTS_MODIFIER_ID = Identifier.of(GemsMod.MOD_ID, "max_hearts");

    private GemPlayerState() {
    }

    public static void copy(ServerPlayerEntity from, ServerPlayerEntity to) {
        NbtCompound fromData = root(from);
        ((GemsPersistentDataHolder) to).gems$setPersistentData(fromData.copy());
    }

    public static void initIfNeeded(ServerPlayerEntity player) {
        NbtCompound data = root(player);

        if (!data.contains(KEY_ENERGY, NbtElement.INT_TYPE)) {
            data.putInt(KEY_ENERGY, DEFAULT_ENERGY);
        }
        if (!data.contains(KEY_ENERGY_CAP_PENALTY, NbtElement.INT_TYPE)) {
            data.putInt(KEY_ENERGY_CAP_PENALTY, 0);
        }
        if (!data.contains(KEY_MAX_HEARTS, NbtElement.INT_TYPE)) {
            data.putInt(KEY_MAX_HEARTS, DEFAULT_MAX_HEARTS);
        }
        if (!data.contains(KEY_GEM_EPOCH, NbtElement.INT_TYPE)) {
            data.putInt(KEY_GEM_EPOCH, 0);
        }
        if (!data.contains(KEY_ACTIVE_GEM, NbtElement.STRING_TYPE)) {
            GemId assigned = randomGem(player);
            data.putString(KEY_ACTIVE_GEM, assigned.name());
            setOwnedGems(player, EnumSet.of(assigned));
        } else if (!data.contains(KEY_OWNED_GEMS, NbtElement.LIST_TYPE)) {
            GemId current = getActiveGem(player);
            setOwnedGems(player, EnumSet.of(current));
        }
    }

    public static GemId getActiveGem(PlayerEntity player) {
        NbtCompound data = root(player);
        String raw = data.getString(KEY_ACTIVE_GEM);
        if (raw == null || raw.isEmpty()) {
            return GemId.ASTRA;
        }
        try {
            return GemId.valueOf(raw);
        } catch (IllegalArgumentException e) {
            GemsMod.LOGGER.warn("Unknown gem id '{}' in player data; defaulting to ASTRA", raw);
            return GemId.ASTRA;
        }
    }

    public static void setActiveGem(PlayerEntity player, GemId gem) {
        NbtCompound data = root(player);
        GemId prev = getActiveGem(player);
        if (prev != gem && player instanceof ServerPlayerEntity sp) {
            com.feel.gems.power.AbilityDisables.clear(sp);
            if (prev == GemId.SPY_MIMIC && gem != GemId.SPY_MIMIC) {
                com.feel.gems.power.SpyMimicSystem.clearOnGemSwitchAway(sp);
            }
        }
        data.putString(KEY_ACTIVE_GEM, gem.name());
        addOwnedGem(player, gem);
    }

    public static void resetToNew(ServerPlayerEntity player, GemId gem) {
        NbtCompound data = root(player);
        data.putInt(KEY_ENERGY, DEFAULT_ENERGY);
        data.putInt(KEY_MAX_HEARTS, DEFAULT_MAX_HEARTS);
        data.putString(KEY_ACTIVE_GEM, gem.name());
        setOwnedGems(player, EnumSet.of(gem));
    }

    public static EnumSet<GemId> getOwnedGems(PlayerEntity player) {
        NbtCompound data = root(player);
        if (!data.contains(KEY_OWNED_GEMS, NbtElement.LIST_TYPE)) {
            return EnumSet.of(getActiveGem(player));
        }
        NbtList list = data.getList(KEY_OWNED_GEMS, NbtElement.STRING_TYPE);
        EnumSet<GemId> result = EnumSet.noneOf(GemId.class);
        for (int i = 0; i < list.size(); i++) {
            String raw = list.getString(i);
            try {
                result.add(GemId.valueOf(raw));
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown ids for forward-compatibility.
            }
        }
        if (result.isEmpty()) {
            result.add(getActiveGem(player));
        }
        return result;
    }

    public static boolean addOwnedGem(PlayerEntity player, GemId gem) {
        EnumSet<GemId> owned = getOwnedGems(player);
        boolean changed = owned.add(gem);
        if (changed) {
            setOwnedGems(player, owned);
        }
        return changed;
    }

    public static int getEnergy(PlayerEntity player) {
        NbtCompound data = root(player);
        if (!data.contains(KEY_ENERGY, NbtElement.INT_TYPE)) {
            return DEFAULT_ENERGY;
        }
        return clamp(data.getInt(KEY_ENERGY), MIN_ENERGY, getMaxEnergy(player));
    }

    public static int getGemEpoch(PlayerEntity player) {
        NbtCompound data = root(player);
        if (!data.contains(KEY_GEM_EPOCH, NbtElement.INT_TYPE)) {
            data.putInt(KEY_GEM_EPOCH, 0);
            return 0;
        }
        return data.getInt(KEY_GEM_EPOCH);
    }

    public static int bumpGemEpoch(PlayerEntity player) {
        NbtCompound data = root(player);
        int next = getGemEpoch(player) + 1;
        data.putInt(KEY_GEM_EPOCH, next);
        return next;
    }

    public static int setEnergy(PlayerEntity player, int energy) {
        int clamped = clamp(energy, MIN_ENERGY, getMaxEnergy(player));
        root(player).putInt(KEY_ENERGY, clamped);
        return clamped;
    }

    public static int addEnergy(PlayerEntity player, int delta) {
        return setEnergy(player, getEnergy(player) + delta);
    }

    public static int getEnergyCapPenalty(PlayerEntity player) {
        NbtCompound data = root(player);
        if (!data.contains(KEY_ENERGY_CAP_PENALTY, NbtElement.INT_TYPE)) {
            return 0;
        }
        return clamp(data.getInt(KEY_ENERGY_CAP_PENALTY), 0, MAX_ENERGY);
    }

    public static int getMaxEnergy(PlayerEntity player) {
        return Math.max(MIN_ENERGY, MAX_ENERGY - getEnergyCapPenalty(player));
    }

    /**
     * Applies a permanent reduction to the player's maximum energy cap and clamps current energy down if needed.
     *
     * <p>This is used by certain abilities as a permanent "life" cost.</p>
     */
    public static int addEnergyCapPenalty(PlayerEntity player, int delta) {
        NbtCompound data = root(player);
        int next = clamp(getEnergyCapPenalty(player) + delta, 0, MAX_ENERGY);
        data.putInt(KEY_ENERGY_CAP_PENALTY, next);
        // Ensure current energy respects the new cap.
        setEnergy(player, getEnergy(player));
        return next;
    }

    public static int getMaxHearts(PlayerEntity player) {
        NbtCompound data = root(player);
        if (!data.contains(KEY_MAX_HEARTS, NbtElement.INT_TYPE)) {
            return DEFAULT_MAX_HEARTS;
        }
        return clamp(data.getInt(KEY_MAX_HEARTS), MIN_MAX_HEARTS, MAX_MAX_HEARTS);
    }

    public static int setMaxHearts(PlayerEntity player, int hearts) {
        int clamped = clamp(hearts, MIN_MAX_HEARTS, MAX_MAX_HEARTS);
        root(player).putInt(KEY_MAX_HEARTS, clamped);
        return clamped;
    }

    public static int addMaxHearts(PlayerEntity player, int delta) {
        return setMaxHearts(player, getMaxHearts(player) + delta);
    }

    public static void applyMaxHearts(ServerPlayerEntity player) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(HEARTS_MODIFIER_ID);

        int hearts = AssassinState.isAssassin(player)
                ? AssassinState.getAssassinHeartsForAttribute(player)
                : getMaxHearts(player);
        double bonusHealth = (hearts - DEFAULT_MAX_HEARTS) * 2.0D;
        if (bonusHealth != 0.0D) {
            maxHealth.addPersistentModifier(new EntityAttributeModifier(
                    HEARTS_MODIFIER_ID,
                    bonusHealth,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        float newMax = (float) maxHealth.getValue();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private static void setOwnedGems(PlayerEntity player, EnumSet<GemId> owned) {
        NbtList list = new NbtList();
        for (GemId gem : owned) {
            list.add(NbtString.of(gem.name()));
        }
        root(player).put(KEY_OWNED_GEMS, list);
    }

    public static void setOwnedGemsExact(PlayerEntity player, EnumSet<GemId> owned) {
        if (owned == null || owned.isEmpty()) {
            owned = EnumSet.of(getActiveGem(player));
        }
        setOwnedGems(player, owned);
    }

    private static GemId randomGem(PlayerEntity player) {
        GemId[] values = GemId.values();
        return values[player.getRandom().nextInt(values.length)];
    }

    private static NbtCompound root(PlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
