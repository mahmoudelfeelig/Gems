package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.gem.spy.SpySystem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public final class SpyStolenCastAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_STOLEN_CAST;
    }

    @Override
    public String name() {
        return "Stolen Cast";
    }

    @Override
    public String description() {
        return "Casts your selected stolen ability.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spy().stolenCastCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (player.isSneaking()) {
            boolean ok = SpySystem.cycleStolen(player);
            if (!ok) {
                player.sendMessage(Text.translatable("gems.ability.spy.stolen_cast.no_stolen"), true);
            }
            return ok;
        }

        Identifier id = SpySystem.selectedStolenCastAbility(player);
        if (id == null) {
            player.sendMessage(Text.translatable("gems.ability.spy.stolen_cast.no_stolen"), true);
            return false;
        }
        if (GemsDisables.isAbilityDisabled(id)) {
            player.sendMessage(Text.translatable("gems.message.ability_disabled_server"), true);
            return false;
        }
        GemAbility ability = ModAbilities.get(id);
        if (ability == null) {
            player.sendMessage(Text.translatable("gems.ability.spy.stolen_cast.not_found", id.toString()), true);
            return false;
        }
        boolean ok = ability.activate(player);
        if (!ok) {
            player.sendMessage(Text.translatable("gems.ability.spy.stolen_cast.failed"), true);
        }
        return ok;
    }
}
