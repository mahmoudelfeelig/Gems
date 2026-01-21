package com.feel.gems.mastery;

import java.util.List;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
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
        String selectedId = GemMastery.getSelectedTitle(player);
        if (!selectedId.isEmpty() && selectedId.startsWith("leaderboard:")) {
            LeaderboardTracker.LeaderboardCategory category = generalFromId(selectedId);
            boolean forced = GemMastery.isSelectedTitleForced(player);
            if (category != null && (forced || LeaderboardTracker.holdsTitle(player, category))) {
                Formatting color = generalTitleColor(category);
                MutableText title = LeaderboardTracker.getTitleText(category).copy();
                title.setStyle(title.getStyle().withColor(color));
                return new TitleInfo(bracket(title, color), color);
            }
        }

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

    public static void refresh(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        PlayerListS2CPacket packet = new PlayerListS2CPacket(
                java.util.EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME),
                List.of(player)
        );
        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            viewer.networkHandler.sendPacket(packet);
        }
    }

    public static void refreshAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            refresh(player);
        }
    }

    private record TitleInfo(Text prefix, Formatting color) {
    }

    private static Formatting generalTitleColor(LeaderboardTracker.LeaderboardCategory category) {
        if (category.gem() != null) {
            return gemColor(category.gem());
        }
        return switch (category) {
            case LEAST_DEATHS -> Formatting.AQUA;
            case MOST_KILLS -> Formatting.RED;
            case MOST_HEARTS -> Formatting.DARK_PURPLE;
            case MAX_ENERGY -> Formatting.GOLD;
            case MOST_SYNERGY_TRIGGERS -> Formatting.DARK_AQUA;
            case MOST_ABILITY_CASTS -> Formatting.LIGHT_PURPLE;
            case MOST_DAMAGE_DEALT -> Formatting.DARK_RED;
            default -> Formatting.WHITE;
        };
    }

    private static LeaderboardTracker.LeaderboardCategory generalFromId(String id) {
        if (id == null || !id.startsWith("leaderboard:")) {
            return null;
        }
        String raw = id.substring("leaderboard:".length());
        for (LeaderboardTracker.LeaderboardCategory category : LeaderboardTracker.LeaderboardCategory.values()) {
            if (category.name().equalsIgnoreCase(raw)) {
                return category;
            }
        }
        return null;
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
