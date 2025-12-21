package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class SpaceWhiteHoleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_WHITE_HOLE;
    }

    @Override
    public String name() {
        return "White Hole";
    }

    @Override
    public String description() {
        return "Creates a white hole at your position that pushes nearby enemies away.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().whiteHoleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().space().whiteHoleDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.literal("White Hole is disabled."), true);
            return false;
        }
        Vec3d center = player.getPos().add(0.0D, 0.2D, 0.0D);
        SpaceAnomalies.spawnWhiteHole(player, center);
        player.sendMessage(Text.literal("White Hole spawned."), true);
        return true;
    }
}
