package com.feel.gems.screen;

import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import java.util.Collections;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
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

    public record OpeningData(List<PlayerInfo> playerInfos) {
        public static final PacketCodec<RegistryByteBuf, OpeningData> PACKET_CODEC = PacketCodec.ofStatic(
                OpeningData::write,
                OpeningData::read
        );

        private static void write(RegistryByteBuf buf, OpeningData data) {
            List<PlayerInfo> players = data.playerInfos == null ? List.of() : data.playerInfos;
            buf.writeVarInt(players.size());
            for (PlayerInfo info : players) {
                buf.writeUuid(info.uuid());
                buf.writeString(info.name(), 128);
                buf.writeString(info.activeGem().name(), 32);
                buf.writeVarInt(info.energy());
                List<GemId> owned = info.ownedGems() == null ? List.of() : info.ownedGems();
                buf.writeVarInt(owned.size());
                for (GemId gem : owned) {
                    buf.writeString(gem.name(), 32);
                }
            }
        }

        private static OpeningData read(RegistryByteBuf buf) {
            int size = buf.readVarInt();
            if (size <= 0) {
                return new OpeningData(List.of());
            }
            List<PlayerInfo> players = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                UUID uuid = buf.readUuid();
                String name = buf.readString(128);
                GemId activeGem = safeGemId(buf.readString(32), GemId.PUFF);
                int energy = buf.readVarInt();

                int ownedSize = buf.readVarInt();
                if (ownedSize < 0) ownedSize = 0;
                if (ownedSize > 64) ownedSize = 64;
                List<GemId> owned = new ArrayList<>(ownedSize);
                for (int j = 0; j < ownedSize; j++) {
                    owned.add(safeGemId(buf.readString(32), GemId.PUFF));
                }
                players.add(new PlayerInfo(uuid, name, activeGem, energy, owned));
            }
            return new OpeningData(players);
        }

        private static GemId safeGemId(String raw, GemId fallback) {
            if (raw == null || raw.isEmpty()) {
                return fallback;
            }
            try {
                return GemId.valueOf(raw);
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }
    }

    public GemSeerScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.GEM_SEER, syncId);
    }

    public GemSeerScreenHandler(int syncId, PlayerInventory playerInventory, OpeningData data) {
        super(ModScreenHandlers.GEM_SEER, syncId);
        if (data != null && data.playerInfos != null) {
            playerInfos.addAll(data.playerInfos);
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
        viewer.sendMessage(Text.translatable("gems.screen.gem_seer.header", info.name)
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        
        Text activeText = Text.translatable("gems.screen.gem_seer.active_gem").formatted(Formatting.GRAY)
                .append(Text.literal(formatGemName(info.activeGem)).formatted(gemColor(info.activeGem)));
        viewer.sendMessage(activeText, false);
        
        Text energyText = Text.translatable("gems.screen.gem_seer.energy").formatted(Formatting.GRAY)
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
            viewer.sendMessage(Text.translatable("gems.screen.gem_seer.owned_gems").formatted(Formatting.GRAY)
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

    public static OpeningData buildOpeningData(ServerPlayerEntity viewer) {
        if (viewer == null) {
            return new OpeningData(List.of());
        }
        MinecraftServer server = viewer.getEntityWorld().getServer();
        if (server == null) {
            return new OpeningData(List.of());
        }

        List<PlayerInfo> infos = new ArrayList<>();
        for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
            if (target.equals(viewer)) {
                continue;
            }
            GemId activeGem = GemPlayerState.getActiveGem(target);
            int energy = GemPlayerState.getEnergy(target);
            List<GemId> ownedGems = new ArrayList<>(GemPlayerState.getOwnedGems(target));
            infos.add(new PlayerInfo(target.getUuid(), target.getName().getString(), activeGem, energy, ownedGems));
        }
        return new OpeningData(Collections.unmodifiableList(infos));
    }
}
