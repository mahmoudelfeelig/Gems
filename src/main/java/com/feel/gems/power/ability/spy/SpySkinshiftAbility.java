package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.util.Targeting;
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
        return GemsBalance.v().spy().skinshiftCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int range = GemsBalance.v().spy().skinshiftRangeBlocks();
        ServerPlayerEntity other = Targeting.raycastPlayer(player, range);
        if (other == null || other == player) {
            player.sendMessage(Text.translatable("gems.message.no_player_target"), true);
            return false;
        }
        if (!VoidImmunity.canBeTargeted(player, other)) {
            player.sendMessage(Text.translatable("gems.message.target_immune"), true);
            return false;
        }
        int duration = GemsBalance.v().spy().skinshiftDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.translatable("gems.ability.spy.skinshift.disabled"), true);
            return false;
        }
        return SpySystem.startSkinshift(player, other, duration);
    }
}
