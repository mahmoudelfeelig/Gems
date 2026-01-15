package com.feel.gems.power.runtime;

import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.legendary.LegendaryCooldowns;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.chaos.ChaosSlotRuntime;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;




public final class GemAbilities {
    private GemAbilities() {
    }

    public static void activateByIndex(ServerPlayerEntity player, int abilityIndex) {
        GemPlayerState.initIfNeeded(player);

        if (AbilityRestrictions.isStunned(player)) {
            player.sendMessage(Text.translatable("gems.ability.stunned"), true);
            return;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            player.sendMessage(Text.translatable("gems.ability.suppressed"), true);
            return;
        }

        GemId gemId = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        if (energy <= 0) {
            player.sendMessage(Text.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }

        // Chaos gem uses independent ability slots and is unlocked by its passive at level 1.
        if (gemId == GemId.CHAOS && abilityIndex >= 0 && abilityIndex < ChaosSlotRuntime.slotCount()) {
            ChaosSlotRuntime.activateSlot(player, abilityIndex);
            return;
        }

        if (energy <= 1) {
            player.sendMessage(Text.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }

        // Prism gem uses selected abilities instead of definition
        if (gemId == GemId.PRISM) {
            activatePrismAbility(player, abilityIndex, energy);
            return;
        }

        GemDefinition def = GemRegistry.definition(gemId);
        List<Identifier> abilities = def.abilities();
        int unlocked = new GemEnergyState(energy).unlockedAbilityCount(abilities.size());
        if (unlocked <= 0) {
            player.sendMessage(Text.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }
        if (abilityIndex < 0 || abilityIndex >= unlocked) {
            player.sendMessage(Text.translatable("gems.ability.not_unlocked"), true);
            return;
        }

        Identifier abilityId = abilities.get(abilityIndex);
        if (GemsDisables.isAbilityDisabledFor(player, abilityId)) {
            player.sendMessage(Text.translatable("gems.ability.disabled_server"), true);
            return;
        }
        GemAbility ability = ModAbilities.get(abilityId);
        if (ability == null) {
            player.sendMessage(Text.translatable("gems.ability.not_registered", abilityId.toString()), true);
            return;
        }
        if (AbilityDisables.isDisabled(player, abilityId)) {
            player.sendMessage(Text.translatable("gems.ability.stolen"), true);
            return;
        }

        boolean noCooldowns = GemsAdmin.noCooldowns(player);
        long now = GemsTime.now(player);
        if (!noCooldowns) {
            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, abilityId);
            if (nextAllowed > now) {
                long remainingTicks = nextAllowed - now;
                player.sendMessage(Text.translatable("gems.ability.on_cooldown", ability.name(), ticksToSeconds(remainingTicks)), true);
                return;
            }
        }

        boolean ok = ability.activate(player);
        if (!ok) {
            return;
        }

        com.feel.gems.power.gem.trickster.TricksterPassiveRuntime.applyChaosEffect(player);
        SpyMimicSystem.onAbilityUsed(player.getEntityWorld().getServer(), player, abilityId);

        int cooldown = Math.max(0, ability.cooldownTicks());
        cooldown = applyCooldownModifiers(player, cooldown);
        if (cooldown > 0 && !noCooldowns) {
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

    /**
     * Activate a Prism gem ability from player's selections.
     */
    private static void activatePrismAbility(ServerPlayerEntity player, int abilityIndex, int energy) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }

        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUuid());
        List<Identifier> abilities = selection.gemAbilities();

        if (abilities.isEmpty()) {
            player.sendMessage(Text.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }

        // Prism abilities unlock progressively like other gems
        int unlocked = new GemEnergyState(energy).unlockedAbilityCount(abilities.size());
        if (unlocked <= 0) {
            player.sendMessage(Text.translatable("gems.ability.no_abilities_unlocked"), true);
            return;
        }
        if (abilityIndex < 0 || abilityIndex >= unlocked) {
            player.sendMessage(Text.translatable("gems.ability.not_unlocked"), true);
            return;
        }

        Identifier abilityId = abilities.get(abilityIndex);
        if (GemsDisables.isAbilityDisabledFor(player, abilityId) || 
            GemsDisables.isBonusAbilityDisabledFor(player, abilityId)) {
            player.sendMessage(Text.translatable("gems.ability.disabled_server"), true);
            return;
        }

        GemAbility ability = ModAbilities.get(abilityId);
        if (ability == null) {
            player.sendMessage(Text.translatable("gems.ability.not_registered", abilityId.toString()), true);
            return;
        }
        if (AbilityDisables.isDisabled(player, abilityId)) {
            player.sendMessage(Text.translatable("gems.ability.stolen"), true);
            return;
        }

        boolean noCooldowns = GemsAdmin.noCooldowns(player);
        long now = GemsTime.now(player);
        if (!noCooldowns) {
            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, abilityId);
            if (nextAllowed > now) {
                long remainingTicks = nextAllowed - now;
                player.sendMessage(Text.translatable("gems.ability.on_cooldown", ability.name(), ticksToSeconds(remainingTicks)), true);
                return;
            }
        }

        boolean ok = ability.activate(player);
        if (!ok) {
            return;
        }

        SpyMimicSystem.onAbilityUsed(server, player, abilityId);

        int cooldown = Math.max(0, ability.cooldownTicks());
        cooldown = applyCooldownModifiers(player, cooldown);
        if (cooldown > 0 && !noCooldowns) {
            GemAbilityCooldowns.setNextAllowedTick(player, abilityId, now + cooldown);
            ServerPlayNetworking.send(player, new AbilityCooldownPayload(GemId.PRISM.ordinal(), abilityIndex, cooldown));
        }
    }

    private static int applyCooldownModifiers(ServerPlayerEntity player, int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        float mult = BonusPassiveRuntime.getCooldownMultiplier(player) * LegendaryCooldowns.getCooldownMultiplier(player);
        int adjusted = (int) Math.ceil(baseTicks * mult);
        return Math.max(1, adjusted);
    }
}
