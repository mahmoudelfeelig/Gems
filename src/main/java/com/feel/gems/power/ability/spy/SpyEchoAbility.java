package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsDisables;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.gem.spy.SpySystem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public final class SpyEchoAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_ECHO;
    }

    @Override
    public String name() {
        return "Echo";
    }

    @Override
    public String description() {
        return "Echo an observed ability; consumes 1 observation when cast.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spy().echoCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        Identifier selected = SpySystem.selectedEchoAbility(player);
        Identifier last = selected != null ? selected : SpySystem.lastSeenAbility(player);
        if (last == null) {
            player.sendMessage(Text.translatable("gems.ability.spy.echo.no_observed"), true);
            return false;
        }
        if (GemsDisables.isAbilityDisabled(last)) {
            player.sendMessage(Text.translatable("gems.message.ability_disabled_server"), true);
            return false;
        }
        if (!SpySystem.isEchoableAbility(last)) {
            player.sendMessage(Text.translatable("gems.ability.spy.echo.cannot_echo"), true);
            return false;
        }
        if (!SpySystem.canEcho(player, last)) {
            player.sendMessage(Text.translatable("gems.ability.spy.echo.no_observed"), true);
            return false;
        }

        GemAbility ability = ModAbilities.get(last);
        if (ability == null) {
            player.sendMessage(Text.translatable("gems.ability.spy.echo.unknown", last.toString()), true);
            return false;
        }
        boolean ok = ability.activate(player);
        if (!ok) {
            player.sendMessage(Text.translatable("gems.ability.spy.echo.failed"), true);
        } else {
            SpySystem.consumeObservedCount(player, last, 1);
            player.sendMessage(Text.translatable("gems.ability.spy.echo.echoed", ability.name()), true);
        }
        return ok;
    }
}
