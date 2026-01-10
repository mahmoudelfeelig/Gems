package com.feel.gems.legendary;

import com.feel.gems.util.GemsTime;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;




public final class LegendaryPlayerTracker {
    private static final String STATE_KEY = "gems_legendary_player_tracker";
    private static final PersistentStateType<State> STATE_TYPE =
            new PersistentStateType<>(STATE_KEY, State::new, State.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    private static final String KEY_PLAYERS = "players";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_NAME = "name";
    private static final String KEY_DIM = "dim";
    private static final String KEY_POS = "pos";
    private static final String KEY_RESPAWN_DIM = "respawn_dim";
    private static final String KEY_RESPAWN_POS = "respawn_pos";
    private static final String KEY_LAST_SEEN = "last_seen";

    private LegendaryPlayerTracker() {
    }

    public static void tick(MinecraftServer server) {
        State state = state(server);
        long now = GemsTime.now(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            state.players.put(player.getUuid(), Snapshot.fromPlayer(server, player, now));
        }
        state.markDirty();
    }

    public static Snapshot snapshot(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
        if (online != null) {
            return Snapshot.fromPlayer(server, online, GemsTime.now(server));
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
            Identifier dimension,
            BlockPos pos,
            Identifier respawnDimension,
            BlockPos respawnPos,
            long lastSeenTick
    ) {
        static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Uuids.CODEC.fieldOf(KEY_UUID).forGetter(Snapshot::uuid),
                Codec.STRING.fieldOf(KEY_NAME).forGetter(Snapshot::name),
                Identifier.CODEC.fieldOf(KEY_DIM).forGetter(Snapshot::dimension),
                BlockPos.CODEC.fieldOf(KEY_POS).forGetter(Snapshot::pos),
                Identifier.CODEC.fieldOf(KEY_RESPAWN_DIM).forGetter(Snapshot::respawnDimension),
                BlockPos.CODEC.fieldOf(KEY_RESPAWN_POS).forGetter(Snapshot::respawnPos),
                Codec.LONG.fieldOf(KEY_LAST_SEEN).forGetter(Snapshot::lastSeenTick)
        ).apply(instance, Snapshot::new));

        static Snapshot fromPlayer(MinecraftServer server, ServerPlayerEntity player, long now) {
            Identifier dim = player.getEntityWorld().getRegistryKey().getValue();
            BlockPos pos = player.getBlockPos();

            var respawn = player.getRespawn();
            var spawnPoint = respawn == null ? null : respawn.respawnData();
            RegistryKey<World> respawnKey = spawnPoint == null ? null : spawnPoint.getDimension();
            BlockPos respawnPos = spawnPoint == null ? null : spawnPoint.getPos();
            if (respawnKey == null || respawnPos == null) {
                ServerWorld overworld = server.getOverworld();
                respawnKey = World.OVERWORLD;
                respawnPos = overworld.getSpawnPoint().getPos();
            }

            return new Snapshot(
                    player.getUuid(),
                    player.getGameProfile().name(),
                    dim,
                    pos,
                    respawnKey.getValue(),
                    respawnPos,
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
