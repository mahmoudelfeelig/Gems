package com.feel.gems.legendary;

import com.feel.gems.util.GemsTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;




public final class LegendaryPlayerTracker {
    private static final String STATE_KEY = "gems_legendary_player_tracker";
    private static final PersistentState.Type<State> STATE_TYPE =
            new PersistentState.Type<>(State::new, State::fromNbt, DataFixTypes.SAVED_DATA_MAP_DATA);

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
        return mgr.getOrCreate(STATE_TYPE, STATE_KEY);
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
        static Snapshot fromPlayer(MinecraftServer server, ServerPlayerEntity player, long now) {
            Identifier dim = player.getServerWorld().getRegistryKey().getValue();
            BlockPos pos = player.getBlockPos();

            RegistryKey<World> respawnKey = player.getSpawnPointDimension();
            BlockPos respawnPos = player.getSpawnPointPosition();
            if (respawnKey == null || respawnPos == null) {
                ServerWorld overworld = server.getOverworld();
                respawnKey = World.OVERWORLD;
                respawnPos = overworld.getSpawnPos();
            }

            return new Snapshot(
                    player.getUuid(),
                    player.getGameProfile().getName(),
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

        static State fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            State state = new State();
            if (!nbt.contains(KEY_PLAYERS, NbtElement.LIST_TYPE)) {
                return state;
            }
            NbtList list = nbt.getList(KEY_PLAYERS, NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound entry = list.getCompound(i);
                if (!entry.containsUuid(KEY_UUID)) {
                    continue;
                }
                UUID uuid = entry.getUuid(KEY_UUID);
                String name = entry.getString(KEY_NAME);
                Identifier dim = Identifier.tryParse(entry.getString(KEY_DIM));
                BlockPos pos = readBlockPos(entry, KEY_POS);
                Identifier respawnDim = Identifier.tryParse(entry.getString(KEY_RESPAWN_DIM));
                BlockPos respawnPos = readBlockPos(entry, KEY_RESPAWN_POS);
                long lastSeen = entry.getLong(KEY_LAST_SEEN);
                if (dim == null || pos == null || respawnDim == null || respawnPos == null) {
                    continue;
                }
                state.players.put(uuid, new Snapshot(uuid, name, dim, pos, respawnDim, respawnPos, lastSeen));
            }
            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            NbtList list = new NbtList();
            for (Snapshot snapshot : players.values()) {
                NbtCompound entry = new NbtCompound();
                entry.putUuid(KEY_UUID, snapshot.uuid());
                entry.putString(KEY_NAME, snapshot.name());
                entry.putString(KEY_DIM, snapshot.dimension().toString());
                writeBlockPos(entry, KEY_POS, snapshot.pos());
                entry.putString(KEY_RESPAWN_DIM, snapshot.respawnDimension().toString());
                writeBlockPos(entry, KEY_RESPAWN_POS, snapshot.respawnPos());
                entry.putLong(KEY_LAST_SEEN, snapshot.lastSeenTick());
                list.add(entry);
            }
            nbt.put(KEY_PLAYERS, list);
            return nbt;
        }

        private static BlockPos readBlockPos(NbtCompound nbt, String key) {
            if (!nbt.contains(key, NbtElement.INT_ARRAY_TYPE)) {
                return null;
            }
            int[] values = nbt.getIntArray(key);
            if (values.length != 3) {
                return null;
            }
            return new BlockPos(values[0], values[1], values[2]);
        }

        private static void writeBlockPos(NbtCompound nbt, String key, BlockPos pos) {
            nbt.put(key, new net.minecraft.nbt.NbtIntArray(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
    }
}
