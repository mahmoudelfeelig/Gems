package com.feel.gems.mastery;

import java.util.List;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.feel.gems.mastery.LeaderboardTracker;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.core.GemId;

public final class TitleDisplay {
    private TitleDisplay() {
    }

    public static Text withTitlePrefix(ServerPlayerEntity player, Text baseName) {
        TitleInfo info = titleInfo(player);
        if (info == null) {
            return baseName;
        }
        Text coloredName = baseName.copy().setStyle(baseName.getStyle().withColor(info.color()));
        return Text.empty().append(info.prefix()).append(coloredName);
    }

    public static Formatting titleColor(ServerPlayerEntity player) {
        TitleInfo info = titleInfo(player);
        return info == null ? null : info.color();
    }

    public static Text titlePrefix(ServerPlayerEntity player) {
        TitleInfo info = titleInfo(player);
        return info == null ? null : info.prefix();
    }

    public static Formatting titleColorForReward(MasteryReward reward) {
        if (reward == null) {
            return null;
        }
        GemId gem = GemMastery.gemFromRewardId(reward.id());
        if (gem != null) {
            return gemColor(gem);
        }
        if (reward.threshold() >= 500) {
            return Formatting.GOLD;
        }
        return Formatting.LIGHT_PURPLE;
    }

    private static MutableText bracket(Text title, Formatting color) {
        return Text.literal("[").formatted(color)
            .append(title)
            .append(Text.literal("] ").formatted(color));
    }

    private static TitleInfo titleInfo(ServerPlayerEntity player) {
        MasteryReward selected = GemMastery.getSelectedTitleReward(player);
        if (selected != null) {
            Formatting color = titleColorForReward(selected);
            Text title = Text.translatable(selected.displayKey()).formatted(color);
            return new TitleInfo(bracket(title, color), color);
        }

        List<LeaderboardTracker.LeaderboardCategory> general = LeaderboardTracker.getTitles(player);
        if (!general.isEmpty()) {
            Formatting color = generalTitleColor(general.get(0));
            MutableText title = LeaderboardTracker.getTitleText(general.get(0)).copy();
            title.setStyle(title.getStyle().withColor(color));
            return new TitleInfo(bracket(title, color), color);
        }

        GemId active = GemPlayerState.getActiveGem(player);
        int usage = GemMastery.getUsage(player, active);
        List<MasteryReward> unlocked = MasteryRewards.getUnlockedTitles(active, usage);
        if (!unlocked.isEmpty()) {
            MasteryReward reward = unlocked.get(unlocked.size() - 1);
            Formatting color = titleColorForReward(reward);
            Text title = Text.translatable(reward.displayKey()).formatted(color);
            return new TitleInfo(bracket(title, color), color);
        }

        return null;
    }

    private record TitleInfo(Text prefix, Formatting color) {
    }

    private static Formatting generalTitleColor(LeaderboardTracker.LeaderboardCategory category) {
        return switch (category) {
            case LEAST_DEATHS -> Formatting.AQUA;
            case MOST_KILLS -> Formatting.RED;
            case MOST_HEARTS -> Formatting.DARK_PURPLE;
            case MAX_ENERGY -> Formatting.GOLD;
        };
    }

    private static Formatting gemColor(GemId gem) {
        return switch (gem) {
            case FIRE -> Formatting.RED;
            case FLUX -> Formatting.YELLOW;
            case LIFE -> Formatting.GREEN;
            case PUFF -> Formatting.WHITE;
            case SPEED -> Formatting.AQUA;
            case STRENGTH -> Formatting.DARK_RED;
            case WEALTH -> Formatting.GOLD;
            case TERROR -> Formatting.DARK_PURPLE;
            case SUMMONER -> Formatting.DARK_GREEN;
            case SPACE -> Formatting.DARK_BLUE;
            case REAPER -> Formatting.DARK_GRAY;
            case PILLAGER -> Formatting.GRAY;
            case SPY -> Formatting.LIGHT_PURPLE;
            case BEACON -> Formatting.BLUE;
            case AIR -> Formatting.WHITE;
            case ASTRA -> Formatting.DARK_PURPLE;
            case VOID -> Formatting.BLACK;
            case CHAOS -> Formatting.RED;
            case PRISM -> Formatting.LIGHT_PURPLE;
            case DUELIST -> Formatting.GOLD;
            case HUNTER -> Formatting.DARK_GREEN;
            case SENTINEL -> Formatting.BLUE;
            case TRICKSTER -> Formatting.DARK_PURPLE;
        };
    }
}
