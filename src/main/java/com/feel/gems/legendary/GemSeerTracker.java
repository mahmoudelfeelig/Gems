package com.feel.gems.legendary;

import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

/**
 * Persistent tracker for Gem Seer player gem data (including offline players).
 */
public final class GemSeerTracker {
    private static final String STATE_KEY = "gems_gem_seer_tracker";
    private static final PersistentStateType<State> STATE_TYPE =
            new PersistentStateType<>(STATE_KEY, State::new, State.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    private static final String KEY_PLAYERS = "players";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_NAME = "name";
    private static final String KEY_ACTIVE_GEM = "active_gem";
    private static final String KEY_ENERGY = "energy";
    private static final String KEY_OWNED = "owned";
    private static final String KEY_LAST_SEEN = "last_seen";

    private GemSeerTracker() {
    }

    public static void tick(MinecraftServer server) {
        State state = state(server);
        long now = GemsTime.now(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            GemPlayerState.initIfNeeded(player);
            state.players.put(player.getUuid(), Snapshot.fromPlayer(player, now));
        }
        state.markDirty();
    }

    public static Snapshot snapshot(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
        if (online != null) {
            GemPlayerState.initIfNeeded(online);
            return Snapshot.fromPlayer(online, GemsTime.now(server));
        }
        return state(server).players.get(uuid);
    }

    public static List<Snapshot> knownSnapshots(MinecraftServer server) {
        return new ArrayList<>(state(server).players.values());
    }

    private static State state(MinecraftServer server) {
        PersistentStateManager mgr = server.getOverworld().getPersistentStateManager();
        return mgr.getOrCreate(STATE_TYPE);
    }

    public record Snapshot(
            UUID uuid,
            String name,
            String activeGem,
            int energy,
            List<String> ownedGems,
            long lastSeenTick
    ) {
        static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Uuids.CODEC.fieldOf(KEY_UUID).forGetter(Snapshot::uuid),
                Codec.STRING.fieldOf(KEY_NAME).forGetter(Snapshot::name),
                Codec.STRING.fieldOf(KEY_ACTIVE_GEM).forGetter(Snapshot::activeGem),
                Codec.INT.fieldOf(KEY_ENERGY).forGetter(Snapshot::energy),
                Codec.STRING.listOf().optionalFieldOf(KEY_OWNED, List.of()).forGetter(Snapshot::ownedGems),
                Codec.LONG.fieldOf(KEY_LAST_SEEN).forGetter(Snapshot::lastSeenTick)
        ).apply(instance, Snapshot::new));

        static Snapshot fromPlayer(ServerPlayerEntity player, long now) {
            GemId active = GemPlayerState.getActiveGem(player);
            int energy = GemPlayerState.getEnergy(player);
            List<String> owned = new ArrayList<>();
            for (GemId gem : GemPlayerState.getOwnedGems(player)) {
                owned.add(gem.name());
            }
            return new Snapshot(
                    player.getUuid(),
                    player.getGameProfile().name(),
                    active.name(),
                    energy,
                    owned,
                    now
            );
        }
    }

    private static final class State extends PersistentState {
        private final Map<UUID, Snapshot> players = new HashMap<>();

        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Snapshot.CODEC.listOf().optionalFieldOf(KEY_PLAYERS, List.of()).forGetter(state -> List.copyOf(state.players.values()))
        ).apply(instance, State::fromSnapshots));

        private static State fromSnapshots(List<Snapshot> snapshots) {
            State state = new State();
            if (snapshots != null) {
                for (Snapshot snapshot : snapshots) {
                    if (snapshot == null || snapshot.uuid() == null) {
                        continue;
                    }
                    state.players.put(snapshot.uuid(), snapshot);
                }
            }
            return state;
        }
    }
}
