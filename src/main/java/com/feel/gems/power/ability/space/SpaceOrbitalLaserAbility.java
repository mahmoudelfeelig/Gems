package com.feel.gems.power.ability.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.gem.space.SpaceAnomalies;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;


public final class SpaceOrbitalLaserAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_ORBITAL_LASER;
    }

    @Override
    public String name() {
        return "Orbital Laser";
    }

    @Override
    public String description() {
        return "Calls a strike from above at the block you're looking at. Sneak to mine.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().orbitalLaserCooldownTicks();
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
        boolean mining = player.isSneaking();
        SpaceAnomalies.scheduleOrbitalLaser(player, target, mining);
        player.sendMessage(Text.translatable(mining ? "gems.ability.space.orbital_laser.mining" : "gems.ability.space.orbital_laser.damage"), true);
        return true;
    }
}
