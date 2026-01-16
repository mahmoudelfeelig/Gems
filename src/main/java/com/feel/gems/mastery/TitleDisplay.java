package com.feel.gems.mastery;

import java.util.List;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.feel.gems.mastery.LeaderboardTracker;

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

    public static Text titlePrefix(ServerPlayerEntity player) {
        TitleInfo info = titleInfo(player);
        return info == null ? null : info.prefix();
    }

    private static Formatting titleColor(MasteryReward reward) {
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
            Formatting color = titleColor(selected);
            Text title = Text.translatable(selected.displayKey()).formatted(color);
            return new TitleInfo(bracket(title, color), color);
        }

        List<LeaderboardTracker.LeaderboardCategory> general = LeaderboardTracker.getTitles(player);
        if (!general.isEmpty()) {
            Formatting color = Formatting.GOLD;
            MutableText title = LeaderboardTracker.getTitleText(general.get(0)).copy();
            title.setStyle(title.getStyle().withColor(color));
            return new TitleInfo(bracket(title, color), color);
        }

        return null;
    }

    private record TitleInfo(Text prefix, Formatting color) {
    }
}
