package com.feel.gems.net;

import com.feel.gems.bonus.BonusPoolRegistry;
import com.feel.gems.bonus.BonusClaimsState;
import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.net.GemCooldownSync;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side networking for Prism gem ability/passive selection.
 */
public final class ServerPrismNetworking {
    private static final int MAX_GEM_ABILITIES = 3;
    private static final int MAX_GEM_PASSIVES = 3;
    private static final int MAX_BONUS_ABILITIES = 2;
    private static final int MAX_BONUS_PASSIVES = 2;

    private ServerPrismNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PrismSelectionOpenRequestPayload.ID, (payload, context) ->
                context.player().getEntityWorld().getServer().execute(() -> openEditor(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(PrismSelectionClaimPayload.ID, (payload, context) ->
                context.player().getEntityWorld().getServer().execute(() -> handleClaim(context.player(), payload)));
    }

    public static void openEditor(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        int energy = GemPlayerState.getEnergy(player);
        if (energy < 10) {
            player.sendMessage(Text.translatable("gems.prism.need_energy_access"), true);
            return;
        }

        GemId activeGem = GemPlayerState.getActiveGem(player);
        if (activeGem != GemId.PRISM) {
            player.sendMessage(Text.translatable("gems.prism.not_prism_gem"), true);
            return;
        }

        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;

        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        UUID playerId = player.getUuid();
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(playerId);

        BonusClaimsState bonusClaims = BonusClaimsState.get(server);
        Set<Identifier> claimedBonusAbilities = bonusClaims.getPlayerAbilities(playerId);
        Set<Identifier> claimedBonusPassives = bonusClaims.getPlayerPassives(playerId);

        // Build gem abilities list (from all normal gems except Void, Chaos, Prism)
        List<PrismSelectionScreenPayload.PowerEntry> gemAbilities = new ArrayList<>();
        for (GemId gemId : GemId.values()) {
            if (gemId == GemId.VOID || gemId == GemId.CHAOS || gemId == GemId.PRISM) {
                continue;
            }
            try {
                GemDefinition def = GemRegistry.definition(gemId);
                String gemName = formatGemName(gemId);
                for (Identifier abilityId : def.abilities()) {
                    if (BonusPoolRegistry.isBlacklisted(abilityId)) {
                        continue;
                    }
                    GemAbility ability = ModAbilities.get(abilityId);
                    if (ability == null) continue;
                    gemAbilities.add(new PrismSelectionScreenPayload.PowerEntry(
                            abilityId,
                            ability.name(),
                            ability.description(),
                            gemName,
                            true,
                            false
                    ));
                }
            } catch (Exception ignored) {
            }
        }

        // Build bonus abilities list
        List<PrismSelectionScreenPayload.PowerEntry> bonusAbilities = new ArrayList<>();
        for (Identifier abilityId : BonusPoolRegistry.BONUS_ABILITIES) {
            GemAbility ability = ModAbilities.get(abilityId);
            if (ability == null) continue;
            boolean claimed = claimedBonusAbilities.contains(abilityId);
            boolean available = claimed || bonusClaims.isAbilityAvailable(abilityId);
            bonusAbilities.add(new PrismSelectionScreenPayload.PowerEntry(
                    abilityId,
                    ability.name(),
                    ability.description(),
                    "Bonus",
                    available,
                    claimed
            ));
        }

        // Build gem passives list
        List<PrismSelectionScreenPayload.PowerEntry> gemPassives = new ArrayList<>();
        for (GemId gemId : GemId.values()) {
            if (gemId == GemId.VOID || gemId == GemId.CHAOS || gemId == GemId.PRISM) {
                continue;
            }
            try {
                GemDefinition def = GemRegistry.definition(gemId);
                String gemName = formatGemName(gemId);
                for (Identifier passiveId : def.passives()) {
                    if (BonusPoolRegistry.isBlacklisted(passiveId)) {
                        continue;
                    }
                    GemPassive passive = ModPassives.get(passiveId);
                    if (passive == null) continue;
                    gemPassives.add(new PrismSelectionScreenPayload.PowerEntry(
                            passiveId,
                            passive.name(),
                            passive.description(),
                            gemName,
                            true,
                            false
                    ));
                }
            } catch (Exception ignored) {
            }
        }

        // Build bonus passives list
        List<PrismSelectionScreenPayload.PowerEntry> bonusPassives = new ArrayList<>();
        for (Identifier passiveId : BonusPoolRegistry.BONUS_PASSIVES) {
            GemPassive passive = ModPassives.get(passiveId);
            if (passive == null) continue;
            boolean claimed = claimedBonusPassives.contains(passiveId);
            boolean available = claimed || bonusClaims.isPassiveAvailable(passiveId);
            bonusPassives.add(new PrismSelectionScreenPayload.PowerEntry(
                    passiveId,
                    passive.name(),
                    passive.description(),
                    "Bonus",
                    available,
                    claimed
            ));
        }

        ServerPlayNetworking.send(player, new PrismSelectionScreenPayload(
                gemAbilities,
                bonusAbilities,
                gemPassives,
                bonusPassives,
                selection.gemAbilities(),
                claimedBonusAbilities.stream().sorted().toList(),
                selection.gemPassives(),
                claimedBonusPassives.stream().sorted().toList(),
                MAX_GEM_ABILITIES,
                MAX_BONUS_ABILITIES,
                MAX_GEM_PASSIVES,
                MAX_BONUS_PASSIVES
        ));
    }

    private static void handleClaim(ServerPlayerEntity player, PrismSelectionClaimPayload payload) {
        GemPlayerState.initIfNeeded(player);

        int energy = GemPlayerState.getEnergy(player);
        if (energy < 10) {
            player.sendMessage(Text.translatable("gems.prism.need_energy_claim"), true);
            return;
        }

        GemId activeGem = GemPlayerState.getActiveGem(player);
        if (activeGem != GemId.PRISM) {
            player.sendMessage(Text.translatable("gems.prism.not_prism_gem"), true);
            return;
        }

        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;

        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        BonusClaimsState bonusClaims = BonusClaimsState.get(server);
        UUID playerId = player.getUuid();
        Identifier powerId = payload.powerId();

        boolean idIsBonus = payload.isAbility()
                ? BonusPoolRegistry.isBonusAbility(powerId)
                : BonusPoolRegistry.isBonusPassive(powerId);
        boolean effectiveBonus = payload.isBonus() || idIsBonus;

        if (payload.claim()) {
            boolean success;
            String name;
            if (payload.isAbility()) {
                if (effectiveBonus) {
                    success = bonusClaims.claimAbility(playerId, powerId);
                } else {
                    success = prismState.addGemAbility(playerId, powerId);
                }
                GemAbility ability = ModAbilities.get(powerId);
                name = ability != null ? ability.name() : powerId.toString();
                if (success) {
                    String key = effectiveBonus ? "gems.bonus.claimed_ability" : "gems.prism.selected_ability";
                    player.sendMessage(Text.translatable(key, name), false);
                } else {
                    String key = effectiveBonus ? "gems.bonus.cannot_claim_ability" : "gems.prism.cannot_select_ability";
                    player.sendMessage(Text.translatable(key), true);
                }
            } else {
                if (effectiveBonus) {
                    success = bonusClaims.claimPassive(playerId, powerId);
                } else {
                    success = prismState.addGemPassive(playerId, powerId);
                }
                GemPassive passive = ModPassives.get(powerId);
                name = passive != null ? passive.name() : powerId.toString();
                if (success) {
                    String key = effectiveBonus ? "gems.bonus.claimed_passive" : "gems.prism.selected_passive";
                    player.sendMessage(Text.translatable(key, name), false);
                    // Apply the passive immediately (bonus and prism selections).
                    if (passive != null) passive.apply(player);
                } else {
                    String key = effectiveBonus ? "gems.bonus.cannot_claim_passive" : "gems.prism.cannot_select_passive";
                    player.sendMessage(Text.translatable(key), true);
                }
            }
        } else {
            // Releasing
            if (payload.isAbility()) {
                if (effectiveBonus) {
                    bonusClaims.releaseAbility(playerId, powerId);
                    GemAbility ability = ModAbilities.get(powerId);
                    String name = ability != null ? ability.name() : powerId.toString();
                    player.sendMessage(Text.translatable("gems.bonus.released_ability", name), false);
                } else {
                    prismState.removeAbility(playerId, powerId);
                    GemAbility ability = ModAbilities.get(powerId);
                    String name = ability != null ? ability.name() : powerId.toString();
                    player.sendMessage(Text.translatable("gems.prism.released_ability", name), false);
                }
            } else {
                GemPassive passive = ModPassives.get(powerId);
                if (passive != null) passive.remove(player);
                if (effectiveBonus) {
                    bonusClaims.releasePassive(playerId, powerId);
                    String name = passive != null ? passive.name() : powerId.toString();
                    player.sendMessage(Text.translatable("gems.bonus.released_passive", name), false);
                } else {
                    prismState.removePassive(playerId, powerId);
                    String name = passive != null ? passive.name() : powerId.toString();
                    player.sendMessage(Text.translatable("gems.prism.released_passive", name), false);
                }
            }
        }

        GemStateSync.sendPrismAbilitiesSync(player);
        GemStateSync.sendBonusAbilitiesSync(player);
        GemCooldownSync.send(player);
        com.feel.gems.power.runtime.GemPowers.sync(player);

        // Refresh the screen
        openEditor(player);
    }

    private static String formatGemName(GemId gemId) {
        String name = gemId.name().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
