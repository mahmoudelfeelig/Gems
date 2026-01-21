package com.feel.gems.gametest.leaderboard;

import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.mastery.LeaderboardTracker;
import com.feel.gems.stats.GemsStats;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;
import net.minecraft.network.packet.s2c.play.PositionFlag;

public final class GemsLeaderboardGameTests {
    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void leaderboardPerGemKills(TestContext context) {
        ServerWorld world = context.getWorld();
        GemsGameTestUtil.placeStoneFloor(context, 6);
        ServerPlayerEntity killer = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity victim = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(killer);
        GemsGameTestUtil.forceSurvival(victim);

        Vec3d base = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(killer, world, base.x, base.y, base.z, 0.0F, 0.0F);
        teleport(victim, world, base.x + 2.0D, base.y, base.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(killer);
        GemPlayerState.initIfNeeded(victim);
        GemPlayerState.setActiveGem(killer, GemId.ASTRA);
        GemPlayerState.setActiveGem(victim, GemId.FIRE);

        GemsStats.recordPlayerKill(killer, victim, false, false, false);
        LeaderboardTracker.updateLeaderboards(world.getServer());

        LeaderboardTracker.LeaderboardEntry leader = LeaderboardTracker.getLeader(LeaderboardTracker.LeaderboardCategory.MOST_KILLS_ASTRA);
        if (leader == null || !leader.playerId().equals(killer.getUuid())) {
            context.throwGameTestException("Per-gem leaderboard did not select Astra leader");
            return;
        }
        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void leaderboardLifetimeStats(TestContext context) {
        ServerWorld world = context.getWorld();
        GemsGameTestUtil.placeStoneFloor(context, 6);
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity other = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(other);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.initIfNeeded(other);

        Identifier testAbility = Identifier.of("gems", "test_cast");
        GemsStats.recordAbilityUse(player, testAbility);
        GemsStats.recordAbilityUse(player, testAbility);
        GemsStats.recordSynergyTrigger(player, Identifier.of("gems", "test_synergy"));
        GemsStats.recordDamageDealt(player, 12.0F);

        LeaderboardTracker.updateLeaderboards(world.getServer());

        LeaderboardTracker.LeaderboardEntry casts = LeaderboardTracker.getLeader(LeaderboardTracker.LeaderboardCategory.MOST_ABILITY_CASTS);
        LeaderboardTracker.LeaderboardEntry synergies = LeaderboardTracker.getLeader(LeaderboardTracker.LeaderboardCategory.MOST_SYNERGY_TRIGGERS);
        LeaderboardTracker.LeaderboardEntry damage = LeaderboardTracker.getLeader(LeaderboardTracker.LeaderboardCategory.MOST_DAMAGE_DEALT);

        if (casts == null || !casts.playerId().equals(player.getUuid())) {
            context.throwGameTestException("Ability cast leaderboard did not select player");
            return;
        }
        if (synergies == null || !synergies.playerId().equals(player.getUuid())) {
            context.throwGameTestException("Synergy trigger leaderboard did not select player");
            return;
        }
        if (damage == null || !damage.playerId().equals(player.getUuid())) {
            context.throwGameTestException("Damage dealt leaderboard did not select player");
            return;
        }
        context.complete();
    }
}
