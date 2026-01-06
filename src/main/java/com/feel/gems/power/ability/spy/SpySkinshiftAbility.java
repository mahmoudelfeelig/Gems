package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.util.Targeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public final class SpySkinshiftAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_SKINSHIFT;
    }

    @Override
    public String name() {
        return "Skinshift";
    }

    @Override
    public String description() {
        return "Steal a targeted player's appearance for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spyMimic().skinshiftCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int range = GemsBalance.v().spyMimic().skinshiftRangeBlocks();
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (!(target instanceof ServerPlayerEntity other) || other == player) {
            player.sendMessage(Text.translatable("gems.message.no_player_target"), true);
            return false;
        }
        int duration = GemsBalance.v().spyMimic().skinshiftDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.translatable("gems.ability.spy.skinshift.disabled"), true);
            return false;
        }
        return SpyMimicSystem.startSkinshift(player, other, duration);
    }
}
