package com.feel.gems.screen;

import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Screen handler for the Gem Seer item.
 * Allows selecting a player to view their gem info.
 */
public final class GemSeerScreenHandler extends ScreenHandler {
    private final List<PlayerInfo> playerInfos = new ArrayList<>();
    
    public record PlayerInfo(
            UUID uuid,
            String name,
            GemId activeGem,
            int energy,
            List<GemId> ownedGems
    ) {}

    public GemSeerScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.GEM_SEER, syncId);
        
        if (playerInventory.player instanceof ServerPlayerEntity serverPlayer) {
            MinecraftServer server = serverPlayer.getEntityWorld().getServer();
            if (server != null) {
                for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
                    if (target.equals(serverPlayer)) {
                        continue; // Skip self
                    }
                    GemId activeGem = GemPlayerState.getActiveGem(target);
                    int energy = GemPlayerState.getEnergy(target);
                    List<GemId> ownedGems = new ArrayList<>(GemPlayerState.getOwnedGems(target));
                    
                    playerInfos.add(new PlayerInfo(
                            target.getUuid(),
                            target.getName().getString(),
                            activeGem,
                            energy,
                            ownedGems
                    ));
                }
            }
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        if (id < 0 || id >= playerInfos.size()) {
            return false;
        }

        PlayerInfo info = playerInfos.get(id);
        sendPlayerInfo(serverPlayer, info);
        return true;
    }

    private void sendPlayerInfo(ServerPlayerEntity viewer, PlayerInfo info) {
        viewer.sendMessage(Text.empty(), false); // Blank line
        viewer.sendMessage(Text.literal("=== Gem Seer: " + info.name + " ===")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        
        Text activeText = Text.literal("Active Gem: ").formatted(Formatting.GRAY)
                .append(Text.literal(formatGemName(info.activeGem)).formatted(gemColor(info.activeGem)));
        viewer.sendMessage(activeText, false);
        
        Text energyText = Text.literal("Energy: ").formatted(Formatting.GRAY)
                .append(Text.literal("[" + info.energy + "/10]").formatted(energyColor(info.energy)));
        viewer.sendMessage(energyText, false);
        
        if (!info.ownedGems.isEmpty()) {
            StringBuilder owned = new StringBuilder();
            boolean first = true;
            for (GemId gem : info.ownedGems) {
                if (!first) owned.append(", ");
                owned.append(formatGemName(gem));
                first = false;
            }
            viewer.sendMessage(Text.literal("Owned Gems: ").formatted(Formatting.GRAY)
                    .append(Text.literal(owned.toString()).formatted(Formatting.WHITE)), false);
        }
    }

    public List<PlayerInfo> getPlayerInfos() {
        return playerInfos;
    }

    public static String formatGemName(GemId gem) {
        String name = gem.name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                cap = true;
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static Formatting gemColor(GemId gem) {
        return switch (gem) {
            case FIRE -> Formatting.RED;
            case FLUX -> Formatting.YELLOW;
            case LIFE -> Formatting.GREEN;
            case PUFF -> Formatting.WHITE;
            case SPEED -> Formatting.AQUA;
            case STRENGTH -> Formatting.DARK_RED;
            case WEALTH -> Formatting.GOLD;
            case TERROR -> Formatting.DARK_PURPLE;
            case SUMMONER -> Formatting.DARK_GREEN;
            case SPACE -> Formatting.DARK_BLUE;
            case REAPER -> Formatting.DARK_GRAY;
            case PILLAGER -> Formatting.GRAY;
            case SPY_MIMIC -> Formatting.LIGHT_PURPLE;
            case BEACON -> Formatting.BLUE;
            case AIR -> Formatting.WHITE;
            case ASTRA -> Formatting.DARK_PURPLE;
            case VOID -> Formatting.BLACK;
            case CHAOS -> Formatting.RED;
            case PRISM -> Formatting.LIGHT_PURPLE;
            case DUELIST -> Formatting.GOLD;
            case HUNTER -> Formatting.DARK_GREEN;
            case SENTINEL -> Formatting.BLUE;
            case TRICKSTER -> Formatting.DARK_PURPLE;
        };
    }

    public static Formatting energyColor(int energy) {
        if (energy >= 10) return Formatting.GOLD;
        if (energy >= 7) return Formatting.YELLOW;
        if (energy >= 4) return Formatting.WHITE;
        return Formatting.GRAY;
    }
}
