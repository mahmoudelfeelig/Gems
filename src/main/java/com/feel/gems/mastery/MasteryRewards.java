package com.feel.gems.mastery;

import com.feel.gems.core.GemId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of mastery rewards available per gem.
 * Each gem has a progression track with epic titles and auras unlocked at certain thresholds.
 * Titles: 2 per gem (at 100 and 500 uses)
 * Auras: 4 per gem (at 25, 75, 150, 300 uses)
 */
public final class MasteryRewards {
    private static final Map<GemId, List<MasteryReward>> REWARDS = new EnumMap<>(GemId.class);

    // Epic title names per gem - each gem has 2 unique epic titles
    private static final Map<GemId, String[]> EPIC_TITLES = new EnumMap<>(GemId.class);

    static {
        // Define epic titles for each gem (tier 1 at 100 uses, tier 2 at 500 uses)
        EPIC_TITLES.put(GemId.ASTRA, new String[]{"Starweaver", "Voidwalker"});
        EPIC_TITLES.put(GemId.FIRE, new String[]{"Pyroclast", "Inferno Incarnate"});
        EPIC_TITLES.put(GemId.FLUX, new String[]{"Stormcaller", "Tempest Eternal"});
        EPIC_TITLES.put(GemId.LIFE, new String[]{"Lifebinder", "The Undying"});
        EPIC_TITLES.put(GemId.PUFF, new String[]{"Cloudstrider", "Skyborne"});
        EPIC_TITLES.put(GemId.SPEED, new String[]{"Windrunner", "The Blur"});
        EPIC_TITLES.put(GemId.STRENGTH, new String[]{"Ironwrought", "The Unbreakable"});
        EPIC_TITLES.put(GemId.WEALTH, new String[]{"Goldhand", "The Midas Touch"});
        EPIC_TITLES.put(GemId.TERROR, new String[]{"Dreadlord", "Nightmare Incarnate"});
        EPIC_TITLES.put(GemId.SUMMONER, new String[]{"Beastmaster", "Legion Commander"});
        EPIC_TITLES.put(GemId.SPACE, new String[]{"Riftwalker", "Dimension Breaker"});
        EPIC_TITLES.put(GemId.REAPER, new String[]{"Soulreaver", "Death's Hand"});
        EPIC_TITLES.put(GemId.PILLAGER, new String[]{"Warlord", "The Conqueror"});
        EPIC_TITLES.put(GemId.SPY, new String[]{"Shadowblade", "The Unseen"});
        EPIC_TITLES.put(GemId.BEACON, new String[]{"Lightwarden", "The Radiant"});
        EPIC_TITLES.put(GemId.AIR, new String[]{"Galeweaver", "Hurricane"});
        EPIC_TITLES.put(GemId.VOID, new String[]{"Abyssal", "The Hollow"});
        EPIC_TITLES.put(GemId.CHAOS, new String[]{"Anarchist", "Entropy Incarnate"});
        EPIC_TITLES.put(GemId.PRISM, new String[]{"Chromatic", "The Kaleidoscope"});
        EPIC_TITLES.put(GemId.DUELIST, new String[]{"Bladedancer", "The Champion"});
        EPIC_TITLES.put(GemId.HUNTER, new String[]{"Apex Predator", "The Huntsman"});
        EPIC_TITLES.put(GemId.SENTINEL, new String[]{"Bulwark", "The Immovable"});
        EPIC_TITLES.put(GemId.TRICKSTER, new String[]{"Illusionist", "Master of Deception"});

        // Initialize rewards for each gem
        for (GemId gem : GemId.values()) {
            REWARDS.put(gem, buildRewardsFor(gem));
        }
    }

    private MasteryRewards() {
    }

    /**
     * Get all mastery rewards for a gem.
     */
    public static List<MasteryReward> getRewards(GemId gem) {
        return REWARDS.getOrDefault(gem, List.of());
    }

    /**
     * Get rewards unlocked at or below a given ability usage count.
     */
    public static List<MasteryReward> getUnlockedRewards(GemId gem, int abilityUsage) {
        return REWARDS.getOrDefault(gem, List.of()).stream()
                .filter(r -> r.threshold() <= abilityUsage)
                .toList();
    }

    /**
     * Get titles unlocked for a gem at a given usage count.
     */
    public static List<MasteryReward> getUnlockedTitles(GemId gem, int abilityUsage) {
        return getUnlockedRewards(gem, abilityUsage).stream()
                .filter(r -> r.type() == MasteryReward.MasteryRewardType.TITLE)
                .toList();
    }

    /**
     * Get auras unlocked for a gem at a given usage count.
     */
    public static List<MasteryReward> getUnlockedAuras(GemId gem, int abilityUsage) {
        return getUnlockedRewards(gem, abilityUsage).stream()
                .filter(r -> r.type() == MasteryReward.MasteryRewardType.AURA)
                .toList();
    }

    /**
     * Find a reward by its id.
     */
    public static MasteryReward findById(String id) {
        for (List<MasteryReward> rewards : REWARDS.values()) {
            for (MasteryReward reward : rewards) {
                if (reward.id().equals(id)) {
                    return reward;
                }
            }
        }
        return null;
    }

    private static List<MasteryReward> buildRewardsFor(GemId gem) {
        String gemName = gem.name().toLowerCase();
        List<MasteryReward> rewards = new ArrayList<>();

        // Get epic titles for this gem (2 titles: at 100 and 500 uses)
        String[] titles = EPIC_TITLES.get(gem);
        if (titles == null || titles.length < 2) {
            // Fallback for any missing gems
            titles = new String[]{"Adept", "Master"};
        }

        // Title 1: at 100 ability uses
        rewards.add(new MasteryReward(
                gemName + "_title_1",
                MasteryReward.MasteryRewardType.TITLE,
                100,
                "gems.mastery.title." + gemName + ".epic1"
        ));

        // Title 2: at 500 ability uses
        rewards.add(new MasteryReward(
                gemName + "_title_2",
                MasteryReward.MasteryRewardType.TITLE,
                500,
                "gems.mastery.title." + gemName + ".epic2"
        ));

        // Auras at thresholds: 25, 75, 150, 300
        rewards.add(new MasteryReward(
                gemName + "_aura_spark",
                MasteryReward.MasteryRewardType.AURA,
                25,
                "gems.mastery.aura." + gemName + ".spark"
        ));
        rewards.add(new MasteryReward(
                gemName + "_aura_glow",
                MasteryReward.MasteryRewardType.AURA,
                75,
                "gems.mastery.aura." + gemName + ".glow"
        ));
        rewards.add(new MasteryReward(
                gemName + "_aura_radiance",
                MasteryReward.MasteryRewardType.AURA,
                150,
                "gems.mastery.aura." + gemName + ".radiance"
        ));
        rewards.add(new MasteryReward(
                gemName + "_aura_brilliance",
                MasteryReward.MasteryRewardType.AURA,
                300,
                "gems.mastery.aura." + gemName + ".brilliance"
        ));

        return List.copyOf(rewards);
    }

    /**
     * Get the epic title name for a gem at a given tier (0 or 1).
     * Used for translation fallbacks and display.
     */
    public static String getEpicTitleName(GemId gem, int tier) {
        String[] titles = EPIC_TITLES.get(gem);
        if (titles == null || tier < 0 || tier >= titles.length) {
            return tier == 0 ? "Adept" : "Master";
        }
        return titles[tier];
    }
}
