package com.feel.gems.power.ability.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.terror.TerrorRigRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;


public final class TerrorRigAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_RIG;
    }

    @Override
    public String name() {
        return "Rig";
    }

    @Override
    public String description() {
        return "Rig a block to explode when an enemy steps on or uses it.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().terror().rigCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int range = GemsBalance.v().terror().rigRangeBlocks();
        HitResult hit = player.raycast(range, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            player.sendMessage(Text.literal("No block targeted."), true);
            return false;
        }
        if (!TerrorRigRuntime.arm(player, blockHit.getBlockPos())) {
            player.sendMessage(Text.literal("Block cannot be rigged."), true);
            return false;
        }

        AbilityFeedback.burstAt(player.getEntityWorld(), blockHit.getPos(), ParticleTypes.SMOKE, 10, 0.15D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_TNT_PRIMED, 0.7F, 1.2F);
        player.sendMessage(Text.literal("Block rigged."), true);
        return true;
    }
}
