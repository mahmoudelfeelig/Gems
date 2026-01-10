package com.feel.gems.power.ability.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.gem.space.SpaceAnomalies;
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
            player.sendMessage(Text.translatable("gems.ability.space.white_hole.disabled"), true);
            return false;
        }
        Vec3d center = player.getEntityPos().add(0.0D, 0.2D, 0.0D);
        SpaceAnomalies.spawnWhiteHole(player, center);
        player.sendMessage(Text.translatable("gems.ability.space.white_hole.spawned"), true);
        return true;
    }
}
