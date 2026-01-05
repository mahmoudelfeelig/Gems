package com.feel.gems.bonus;

import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.runtime.AbilityDisables;
import com.feel.gems.power.runtime.AbilityRestrictions;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Set;

/**
 * Runtime for activating claimed bonus abilities.
 * Bonus abilities are cast using LAlt + 5 and LAlt + 6 (slot 0 and 1).
 */
public final class BonusAbilityRuntime {
    /** Ability index offset for bonus abilities in cooldown sync messages. */
    public static final int BONUS_ABILITY_INDEX_OFFSET = 100;

    private BonusAbilityRuntime() {
    }

    /**
     * Activate a bonus ability by slot index (0 or 1).
     */
    public static void activateBySlot(ServerPlayerEntity player, int slotIndex) {
        GemPlayerState.initIfNeeded(player);

        if (AbilityRestrictions.isStunned(player)) {
            player.sendMessage(Text.literal("You are stunned."), true);
            return;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            player.sendMessage(Text.literal("Your abilities are suppressed."), true);
            return;
        }

        int energy = GemPlayerState.getEnergy(player);
        if (energy < 10) {
            player.sendMessage(Text.literal("You need energy 10/10 to use bonus abilities."), true);
            return;
        }

        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;

        BonusClaimsState claims = BonusClaimsState.get(server);
        Set<Identifier> playerAbilities = claims.getPlayerAbilities(player.getUuid());

        if (playerAbilities.isEmpty()) {
            player.sendMessage(Text.literal("You have no bonus abilities claimed."), true);
            return;
        }

        // Convert set to ordered list for consistent slot access
        List<Identifier> abilityList = playerAbilities.stream()
                .sorted()
                .toList();

        if (slotIndex < 0 || slotIndex >= abilityList.size()) {
            player.sendMessage(Text.literal("Invalid bonus ability slot."), true);
            return;
        }

        Identifier abilityId = abilityList.get(slotIndex);

        if (GemsDisables.isBonusAbilityDisabledFor(player, abilityId)) {
            player.sendMessage(Text.literal("That bonus ability is disabled on this server."), true);
            return;
        }

        GemAbility ability = ModAbilities.get(abilityId);
        if (ability == null) {
            player.sendMessage(Text.literal("Bonus ability not registered: " + abilityId), true);
            return;
        }
        if (AbilityDisables.isDisabled(player, abilityId)) {
            player.sendMessage(Text.literal("That ability has been stolen from you."), true);
            return;
        }

        boolean noCooldowns = GemsAdmin.noCooldowns(player);
        long now = GemsTime.now(player);
        if (!noCooldowns) {
            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, abilityId);
            if (nextAllowed > now) {
                long remainingTicks = nextAllowed - now;
                player.sendMessage(Text.literal(ability.name() + " is on cooldown (" + ticksToSeconds(remainingTicks) + "s)"), true);
                return;
            }
        }

        boolean ok = ability.activate(player);
        if (!ok) {
            return;
        }

        int cooldown = Math.max(0, ability.cooldownTicks());
        if (cooldown > 0 && !noCooldowns) {
            GemAbilityCooldowns.setNextAllowedTick(player, abilityId, now + cooldown);
            // Use offset index for bonus abilities to distinguish from gem abilities
            ServerPlayNetworking.send(player, new AbilityCooldownPayload(-1, BONUS_ABILITY_INDEX_OFFSET + slotIndex, cooldown));
        }
    }

    private static int ticksToSeconds(long ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return (int) Math.max(1, (ticks + 19) / 20);
    }
}
