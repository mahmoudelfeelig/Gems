package com.blissmc.gems.power;

import com.blissmc.gems.core.GemDefinition;
import com.blissmc.gems.core.GemEnergyState;
import com.blissmc.gems.core.GemId;
import com.blissmc.gems.core.GemRegistry;
import com.blissmc.gems.state.GemPlayerState;
import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class GemAbilities {
    private static final String KEY_COOLDOWNS = "cooldowns";

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

        long now = player.getServerWorld().getTime();
        long nextAllowed = cooldowns(player).getLong(abilityId.toString());
        if (nextAllowed > now) {
            long remainingTicks = nextAllowed - now;
            player.sendMessage(Text.literal(ability.name() + " is on cooldown (" + ticksToSeconds(remainingTicks) + "s)"), true);
            return;
        }

        boolean ok = ability.activate(player);
        if (!ok) {
            player.sendMessage(Text.literal(ability.name() + " is not implemented yet."), true);
            return;
        }

        int cooldown = Math.max(0, ability.cooldownTicks());
        if (cooldown > 0) {
            cooldowns(player).putLong(abilityId.toString(), now + cooldown);
        }
    }

    private static NbtCompound cooldowns(ServerPlayerEntity player) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!root.contains(KEY_COOLDOWNS, NbtElement.COMPOUND_TYPE)) {
            root.put(KEY_COOLDOWNS, new NbtCompound());
        }
        return root.getCompound(KEY_COOLDOWNS);
    }

    private static int ticksToSeconds(long ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return (int) Math.max(1, (ticks + 19) / 20);
    }
}
