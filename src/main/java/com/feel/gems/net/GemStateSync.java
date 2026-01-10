package com.feel.gems.net;

import com.feel.gems.bonus.BonusClaimsState;
import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;




public final class GemStateSync {
    private GemStateSync() {
    }

    public static void send(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        GemId active = GemPlayerState.getActiveGem(player);
        ServerPlayNetworking.send(player, new StateSyncPayload(
                active.ordinal(),
                GemPlayerState.getEnergy(player),
                GemPlayerState.getMaxHearts(player)
        ));

        GemCooldownSync.send(player);
        GemExtraStateSync.send(player);
        sendBonusAbilitiesSync(player);
        
        // Send Prism abilities if the player has the Prism gem
        if (active == GemId.PRISM) {
            sendPrismAbilitiesSync(player);
        }
    }

    public static void sendBonusAbilitiesSync(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        
        BonusClaimsState claims = BonusClaimsState.get(server);
        Set<Identifier> playerAbilities = claims.getPlayerAbilities(player.getUuid());
        
        if (playerAbilities.isEmpty()) {
            ServerPlayNetworking.send(player, new BonusAbilitiesSyncPayload(List.of()));
            return;
        }
        
        long now = GemsTime.now(player);
        List<BonusAbilitiesSyncPayload.BonusAbilityInfo> abilities = new ArrayList<>();
        
        // Sort for consistent ordering
        List<Identifier> sorted = playerAbilities.stream().sorted().toList();
        
        for (Identifier id : sorted) {
            GemAbility ability = ModAbilities.get(id);
            String name = ability != null ? ability.name() : id.getPath();
            
            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, id);
            int remaining = nextAllowed > now ? (int) (nextAllowed - now) : 0;
            
            abilities.add(new BonusAbilitiesSyncPayload.BonusAbilityInfo(id, name, remaining));
        }
        
        ServerPlayNetworking.send(player, new BonusAbilitiesSyncPayload(abilities));
    }

    /**
     * Send Prism ability sync to client for HUD display.
     */
    public static void sendPrismAbilitiesSync(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        
        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUuid());
        List<Identifier> selectedAbilities = selection.allAbilities();
        
        if (selectedAbilities.isEmpty()) {
            ServerPlayNetworking.send(player, new PrismAbilitiesSyncPayload(List.of()));
            return;
        }
        
        long now = GemsTime.now(player);
        List<PrismAbilitiesSyncPayload.PrismAbilityInfo> abilities = new ArrayList<>();
        
        for (Identifier id : selectedAbilities) {
            GemAbility ability = ModAbilities.get(id);
            String name = ability != null ? ability.name() : id.getPath();
            
            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, id);
            int remaining = nextAllowed > now ? (int) (nextAllowed - now) : 0;
            
            abilities.add(new PrismAbilitiesSyncPayload.PrismAbilityInfo(id, name, remaining));
        }
        
        ServerPlayNetworking.send(player, new PrismAbilitiesSyncPayload(abilities));
    }
}
