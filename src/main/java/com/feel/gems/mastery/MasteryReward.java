package com.feel.gems.mastery;

import net.minecraft.util.Identifier;

/**
 * Represents a cosmetic reward unlocked through gem mastery.
 */
public record MasteryReward(
        String id,
        MasteryRewardType type,
        int threshold,
        String displayKey
) {
    public enum MasteryRewardType {
        TITLE,
        AURA
    }

    public Identifier identifier() {
        return Identifier.of("gems", id);
    }
}
