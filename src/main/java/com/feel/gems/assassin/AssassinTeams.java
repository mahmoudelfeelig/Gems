package com.feel.gems.assassin;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class AssassinTeams {
    private static final String TEAM_NAME = "gems_assassins";

    private AssassinTeams() {
    }

    public static void sync(MinecraftServer server, ServerPlayerEntity player) {
        if (server == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addTeam(TEAM_NAME);
            team.setColor(Formatting.RED);
            team.setDisplayName(Text.literal("Assassins").formatted(Formatting.RED));
            // No prefix/suffix: keep it as “names are red in tab”, matching the requested UX.
        }

        String entry = player.getNameForScoreboard();
        boolean shouldBeMember = AssassinState.isAssassin(player);
        boolean isMember = team.getPlayerList().contains(entry);
        if (shouldBeMember && !isMember) {
            scoreboard.addScoreHolderToTeam(entry, team);
        } else if (!shouldBeMember && isMember) {
            scoreboard.removeScoreHolderFromTeam(entry, team);
        }
    }
}

