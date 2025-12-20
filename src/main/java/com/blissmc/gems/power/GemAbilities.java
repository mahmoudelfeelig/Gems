package com.feel.gems.power;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class GemAbilities {
    private GemAbilities() {
    }

    public static void activateByIndex(ServerPlayerEntity player, int abilityIndex) {
        GemPlayerState.initIfNeeded(player);

        if (AbilityRestrictions.isStunned(player)) {
            player.sendMessage(Text.literal("You are stunned."), true);
            return;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            player.sendMessage(Text.literal("Your gem abilities are suppressed."), true);
            return;
        }

        GemId gemId = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        if (energy <= 1) {
            player.sendMessage(Text.literal("No abilities unlocked at this energy."), true);
            return;
        }

        GemDefinition def = GemRegistry.definition(gemId);
        List<Identifier> abilities = def.abilities();
        int unlocked = new GemEnergyState(energy).unlockedAbilityCount(abilities.size());
        if (unlocked <= 0) {
            player.sendMessage(Text.literal("No abilities unlocked at this energy."), true);
            return;
        }
        if (abilityIndex < 0 || abilityIndex >= unlocked) {
            player.sendMessage(Text.literal("That ability is not unlocked."), true);
            return;
        }

        Identifier abilityId = abilities.get(abilityIndex);
        GemAbility ability = ModAbilities.get(abilityId);
        if (ability == null) {
            player.sendMessage(Text.literal("Ability not registered: " + abilityId), true);
            return;
        }

        long now = GemsTime.now(player);
        long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, abilityId);
        if (nextAllowed > now) {
            long remainingTicks = nextAllowed - now;
            player.sendMessage(Text.literal(ability.name() + " is on cooldown (" + ticksToSeconds(remainingTicks) + "s)"), true);
            return;
        }

        boolean ok = ability.activate(player);
        if (!ok) {
            return;
        }

        int cooldown = Math.max(0, ability.cooldownTicks());
        if (cooldown > 0) {
            GemAbilityCooldowns.setNextAllowedTick(player, abilityId, now + cooldown);
            ServerPlayNetworking.send(player, new AbilityCooldownPayload(gemId.ordinal(), abilityIndex, cooldown));
        }
    }

    private static int ticksToSeconds(long ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return (int) Math.max(1, (ticks + 19) / 20);
    }
}
