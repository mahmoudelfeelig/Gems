package com.feel.gems.power.ability.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.space.SpaceAnomalies;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class SpaceOrbitalLaserMiningAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_ORBITAL_LASER_MINING;
    }

    @Override
    public String name() {
        return "Orbital Laser (Mining)";
    }

    @Override
    public String description() {
        return Text.translatable("gems.ability.space.orbital_laser.mining.desc").getString();
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().orbitalLaserMiningCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int range = GemsBalance.v().space().orbitalLaserRangeBlocks();
        HitResult hit = player.raycast(range, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult bhr)) {
            player.sendMessage(Text.translatable("gems.message.no_block_target"), true);
            return false;
        }
        BlockPos target = bhr.getBlockPos();
        SpaceAnomalies.scheduleOrbitalLaser(player, target, true);
        player.sendMessage(Text.translatable("gems.ability.space.orbital_laser.mining"), true);
        return true;
    }
}
