package com.feel.gems.power.ability.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.space.SpaceAnomalies;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;



public final class SpaceBlackHoleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_BLACK_HOLE;
    }

    @Override
    public String name() {
        return "Black Hole";
    }

    @Override
    public String description() {
        return "Creates a black hole at your position that pulls and damages nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().blackHoleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().space().blackHoleDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.literal("Black Hole is disabled."), true);
            return false;
        }
        Vec3d center = player.getEntityPos().add(0.0D, 0.2D, 0.0D);
        SpaceAnomalies.spawnBlackHole(player, center);
        player.sendMessage(Text.literal("Black Hole spawned."), true);
        return true;
    }
}
