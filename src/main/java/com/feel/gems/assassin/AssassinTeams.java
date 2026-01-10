package com.feel.gems.assassin;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.number.StyledNumberFormat;




public final class AssassinTeams {
    private static final String TEAM_NAME = "gems_assassins";
    private static final String POINTS_OBJECTIVE_NAME = "gems_assassin_points";

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

        // Show assassin points in the player list (tab) via a LIST scoreboard objective.
        var obj = scoreboard.getNullableObjective(POINTS_OBJECTIVE_NAME);
        if (obj == null) {
            obj = scoreboard.addObjective(
                    POINTS_OBJECTIVE_NAME,
                    ScoreboardCriterion.DUMMY,
                    Text.translatable("gems.assassin.points_objective").formatted(Formatting.RED),
                    ScoreboardCriterion.RenderType.INTEGER,
                    false,
                    StyledNumberFormat.RED
            );
        }
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.LIST, obj);

        ScoreHolder holder = ScoreHolder.fromName(entry);
        if (shouldBeMember) {
            scoreboard.getOrCreateScore(holder, obj).setScore(AssassinState.assassinPoints(player));
        } else {
            scoreboard.removeScore(holder, obj);
        }
    }
}
