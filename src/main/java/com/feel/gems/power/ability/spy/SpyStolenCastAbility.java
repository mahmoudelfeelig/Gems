package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
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
        return "Casts your selected stolen ability. Sneak to cycle selection.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spyMimic().stolenCastCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (player.isSneaking()) {
            boolean ok = SpyMimicSystem.cycleStolen(player);
            if (!ok) {
                player.sendMessage(Text.literal("No stolen abilities."), true);
            }
            return ok;
        }

        Identifier id = SpyMimicSystem.selectedStolenAbility(player);
        if (id == null) {
            player.sendMessage(Text.literal("No stolen abilities."), true);
            return false;
        }
        if (GemsDisables.isAbilityDisabled(id)) {
            player.sendMessage(Text.literal("That ability is disabled on this server."), true);
            return false;
        }
        GemAbility ability = ModAbilities.get(id);
        if (ability == null) {
            player.sendMessage(Text.literal("Stolen ability not found: " + id), true);
            return false;
        }
        boolean ok = ability.activate(player);
        if (!ok) {
            player.sendMessage(Text.literal("Stolen cast failed."), true);
        }
        return ok;
    }
}
