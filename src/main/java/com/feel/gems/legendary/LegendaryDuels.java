package com.feel.gems.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTeleport;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;

/**
 * Server-wide duel runtime used by Challenger's Gauntlet.
 *
 * <p>Implements an isolated arena in the overworld sky. Duel participants are teleported in, and on duel end
 * they are returned to their original locations. Drops created in the arena are transferred to the winner's
 * return location before the arena is cleaned up.</p>
 */
public final class LegendaryDuels {
    private static final String STATE_KEY = GemsMod.MOD_ID + "_legendary_duels";

    private static final int ARENA_HALF_SIZE = 5;
    private static final int ARENA_HEIGHT = 5;
    private static final int ARENA_SPACING = 64;
    private static final int DUEL_TIMEOUT_TICKS = 5 * 60 * 20;

    private LegendaryDuels() {
    }

    private static final PersistentStateType<State> STATE_TYPE =
            new PersistentStateType<>(STATE_KEY, State::new, State.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    private static State get(MinecraftServer server) {
        PersistentStateManager mgr = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return mgr.getOrCreate(STATE_TYPE);
    }

    public static boolean isInGauntletDuel(MinecraftServer server, UUID player) {
        State state = get(server);
        return state.playerToDuelId.containsKey(player);
    }

    public static boolean startGauntletDuel(ServerPlayerEntity challenger, ServerPlayerEntity target) {
        MinecraftServer server = challenger.getEntityWorld().getServer();
        if (server == null) {
            return false;
        }
        if (challenger == target) {
            return false;
        }
        if (target.isSpectator() || challenger.isSpectator()) {
            return false;
        }
        if (challenger.getEntityWorld() != target.getEntityWorld()) {
            return false;
        }

        State state = get(server);
        if (state.playerToDuelId.containsKey(challenger.getUuid()) || state.playerToDuelId.containsKey(target.getUuid())) {
            return false;
        }

        ServerWorld arenaWorld = server.getOverworld();
        int arenaIndex = state.nextArenaIndex++;
        state.markDirty();

        BlockPos center = arenaCenter(arenaWorld, arenaIndex);
        buildArena(arenaWorld, center);

        Duel duel = new Duel(
                UUID.randomUUID(),
                challenger.getUuid(),
                target.getUuid(),
                ReturnLocation.from(challenger),
                ReturnLocation.from(target),
                center,
                arenaWorld.getRegistryKey().getValue(),
                arenaWorld.getTime() + DUEL_TIMEOUT_TICKS
        );

        state.addDuel(duel);

        teleportIntoArena(challenger, arenaWorld, center, -2, 0, 90.0F);
        teleportIntoArena(target, arenaWorld, center, 2, 0, -90.0F);

        return true;
    }

    public static void tickEverySecond(MinecraftServer server) {
        State state = get(server);
        if (state.duels.isEmpty()) {
            return;
        }

        long now = server.getOverworld().getTime();
        for (Duel duel : java.util.List.copyOf(state.duels.values())) {
            if (now < duel.endsAtTick) {
                continue;
            }
            // Timeout: return anyone still online and clean up.
            ServerPlayerEntity a = server.getPlayerManager().getPlayer(duel.playerA);
            ServerPlayerEntity b = server.getPlayerManager().getPlayer(duel.playerB);
            if (a != null) {
                duel.returnA.teleport(server, a);
            }
            if (b != null) {
                duel.returnB.teleport(server, b);
            }
            cleanupArena(server, duel);
            state.removeDuel(duel.duelId);
        }
    }

    public static void onDisconnect(MinecraftServer server, ServerPlayerEntity player) {
        if (server == null || player == null) {
            return;
        }
        State state = get(server);
        UUID duelId = state.playerToDuelId.get(player.getUuid());
        if (duelId == null) {
            return;
        }
        Duel duel = state.duels.get(duelId);
        if (duel == null) {
            state.playerToDuelId.remove(player.getUuid());
            state.markDirty();
            return;
        }

        // Cancel duel: return both participants (best-effort) and clean up.
        ServerPlayerEntity a = server.getPlayerManager().getPlayer(duel.playerA);
        ServerPlayerEntity b = server.getPlayerManager().getPlayer(duel.playerB);
        if (a != null) {
            duel.returnA.teleport(server, a);
        }
        if (b != null) {
            duel.returnB.teleport(server, b);
        }
        cleanupArena(server, duel);
        state.removeDuel(duel.duelId);
    }

    /**
     * Called from {@code GemsPlayerDeath.onDeathTail} so we can end duels even when the death
     * had no player attacker (environmental death).
     */
    public static void onDuelParticipantDeathTail(ServerPlayerEntity victim, DamageSource source) {
        if (victim == null) {
            return;
        }
        MinecraftServer server = victim.getEntityWorld().getServer();
        if (server == null) {
            return;
        }

        State state = get(server);
        UUID duelId = state.playerToDuelId.get(victim.getUuid());
        if (duelId == null) {
            return;
        }
        Duel duel = state.duels.get(duelId);
        if (duel == null) {
            state.playerToDuelId.remove(victim.getUuid());
            state.markDirty();
            return;
        }

        UUID opponentId = duel.playerA.equals(victim.getUuid()) ? duel.playerB : duel.playerA;
        ServerPlayerEntity opponent = server.getPlayerManager().getPlayer(opponentId);

        // If the duel ended by a non-opponent kill, award the opponent the +1 energy the core death logic won't.
        Entity attacker = source == null ? null : source.getAttacker();
        boolean killedByOpponent = attacker instanceof ServerPlayerEntity attackerPlayer && attackerPlayer.getUuid().equals(opponentId);
        if (!killedByOpponent && opponent != null) {
            GemPlayerState.initIfNeeded(opponent);
            GemPlayerState.addEnergy(opponent, 1);
            GemItemGlint.sync(opponent);
            GemStateSync.send(opponent);
        }

        // Move any drops from inside the arena to the opponent's return location (if we have one).
        ReturnLocation lootDest = opponentId.equals(duel.playerA) ? duel.returnA : duel.returnB;
        moveArenaDrops(server, duel, lootDest);

        // Return opponent immediately; return victim on respawn via COPY_FROM.
        if (opponent != null) {
            if (opponentId.equals(duel.playerA)) {
                duel.returnA.teleport(server, opponent);
            } else {
                duel.returnB.teleport(server, opponent);
            }
        }

        // Store pending return for the victim's next respawned player entity.
        ReturnLocation victimReturn = victim.getUuid().equals(duel.playerA) ? duel.returnA : duel.returnB;
        state.pendingReturn.put(victim.getUuid(), victimReturn);
        state.markDirty();

        cleanupArena(server, duel);
        state.removeDuel(duel.duelId);
    }

    public static void onPlayerCopyFrom(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        if (alive) {
            return;
        }
        MinecraftServer server = newPlayer.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        State state = get(server);
        ReturnLocation dest = state.pendingReturn.remove(oldPlayer.getUuid());
        if (dest == null) {
            return;
        }
        state.markDirty();
        dest.teleport(server, newPlayer);
    }

    private static BlockPos arenaCenter(ServerWorld world, int index) {
        BlockPos spawn = world.getSpawnPoint().getPos();
        int gridWidth = 16;
        int gx = index % gridWidth;
        int gz = index / gridWidth;
        int x = spawn.getX() + (gx * ARENA_SPACING);
        int z = spawn.getZ() + (gz * ARENA_SPACING);
        int maxY = world.getBottomY() + world.getDimension().height() - 1;
        int y = Math.max(world.getBottomY() + 20, maxY - 20);
        return new BlockPos(x, y, z);
    }

    private static void teleportIntoArena(ServerPlayerEntity player, ServerWorld arena, BlockPos center, int dx, int dz, float yaw) {
        double x = center.getX() + 0.5D + dx;
        double y = center.getY() + 1.1D;
        double z = center.getZ() + 0.5D + dz;
        GemsTeleport.teleport(player, arena, x, y, z, yaw, 0.0F);
    }

    private static void buildArena(ServerWorld world, BlockPos center) {
        // Ensure chunk is loaded.
        world.getChunk(center);

        int minX = center.getX() - ARENA_HALF_SIZE;
        int maxX = center.getX() + ARENA_HALF_SIZE;
        int minZ = center.getZ() - ARENA_HALF_SIZE;
        int maxZ = center.getZ() + ARENA_HALF_SIZE;
        int floorY = center.getY();
        int ceilingY = floorY + ARENA_HEIGHT + 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(new BlockPos(x, floorY, z), Blocks.BARRIER.getDefaultState());
                world.setBlockState(new BlockPos(x, ceilingY, z), Blocks.BARRIER.getDefaultState());
            }
        }
        for (int y = floorY + 1; y <= floorY + ARENA_HEIGHT; y++) {
            for (int x = minX; x <= maxX; x++) {
                world.setBlockState(new BlockPos(x, y, minZ), Blocks.BARRIER.getDefaultState());
                world.setBlockState(new BlockPos(x, y, maxZ), Blocks.BARRIER.getDefaultState());
            }
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(new BlockPos(minX, y, z), Blocks.BARRIER.getDefaultState());
                world.setBlockState(new BlockPos(maxX, y, z), Blocks.BARRIER.getDefaultState());
            }
        }

        // Clear interior (best-effort).
        for (int x = minX + 1; x <= maxX - 1; x++) {
            for (int z = minZ + 1; z <= maxZ - 1; z++) {
                for (int y = floorY + 1; y <= ceilingY - 1; y++) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static Box arenaBox(BlockPos center) {
        int minX = center.getX() - ARENA_HALF_SIZE;
        int maxX = center.getX() + ARENA_HALF_SIZE;
        int minZ = center.getZ() - ARENA_HALF_SIZE;
        int maxZ = center.getZ() + ARENA_HALF_SIZE;
        int minY = center.getY();
        int maxY = center.getY() + ARENA_HEIGHT + 2;
        return new Box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    private static void moveArenaDrops(MinecraftServer server, Duel duel, ReturnLocation destination) {
        if (destination == null) {
            return;
        }
        ServerWorld arenaWorld = server.getWorld(World.OVERWORLD);
        if (arenaWorld == null) {
            return;
        }
        Box box = arenaBox(duel.arenaCenter);

        ServerWorld destWorld = destination.world(server);
        if (destWorld == null) {
            return;
        }
        double x = destination.pos.getX() + 0.5D;
        double y = destination.pos.getY() + 1.0D;
        double z = destination.pos.getZ() + 0.5D;

        for (ItemEntity item : arenaWorld.getEntitiesByClass(ItemEntity.class, box, e -> true)) {
            var stack = item.getStack().copy();
            if (stack.isEmpty()) {
                item.discard();
                continue;
            }
            ItemEntity moved = new ItemEntity(destWorld, x, y, z, stack);
            moved.setPickupDelay(10);
            destWorld.spawnEntity(moved);
            item.discard();
        }
    }

    private static void cleanupArena(MinecraftServer server, Duel duel) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) {
            return;
        }
        BlockPos center = duel.arenaCenter;
        int minX = center.getX() - ARENA_HALF_SIZE;
        int maxX = center.getX() + ARENA_HALF_SIZE;
        int minZ = center.getZ() - ARENA_HALF_SIZE;
        int maxZ = center.getZ() + ARENA_HALF_SIZE;
        int minY = center.getY();
        int maxY = center.getY() + ARENA_HEIGHT + 2;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static final class State extends PersistentState {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("nextArenaIndex", 0).forGetter(s -> s.nextArenaIndex),
                Codec.unboundedMap(Uuids.CODEC, Duel.CODEC).optionalFieldOf("duels", Map.of()).forGetter(s -> s.duels),
                Codec.unboundedMap(Uuids.CODEC, ReturnLocation.CODEC).optionalFieldOf("pendingReturn", Map.of()).forGetter(s -> s.pendingReturn)
        ).apply(instance, State::new));

        private int nextArenaIndex;
        private final Map<UUID, Duel> duels;
        private final Map<UUID, ReturnLocation> pendingReturn;

        // Derived index for quick lookup.
        private final Map<UUID, UUID> playerToDuelId;

        State() {
            this(0, Map.of(), Map.of());
        }

        State(int nextArenaIndex, Map<UUID, Duel> duels, Map<UUID, ReturnLocation> pendingReturn) {
            this.nextArenaIndex = Math.max(0, nextArenaIndex);
            this.duels = new HashMap<>(duels == null ? Map.of() : duels);
            this.pendingReturn = new HashMap<>(pendingReturn == null ? Map.of() : pendingReturn);
            this.playerToDuelId = new HashMap<>();
            for (var entry : this.duels.entrySet()) {
                Duel duel = entry.getValue();
                if (duel == null) {
                    continue;
                }
                this.playerToDuelId.put(duel.playerA, duel.duelId);
                this.playerToDuelId.put(duel.playerB, duel.duelId);
            }
        }

        void addDuel(Duel duel) {
            this.duels.put(duel.duelId, duel);
            this.playerToDuelId.put(duel.playerA, duel.duelId);
            this.playerToDuelId.put(duel.playerB, duel.duelId);
            markDirty();
        }

        void removeDuel(UUID duelId) {
            Duel duel = this.duels.remove(duelId);
            if (duel != null) {
                this.playerToDuelId.remove(duel.playerA);
                this.playerToDuelId.remove(duel.playerB);
            }
            markDirty();
        }
    }

    private record ReturnLocation(Identifier dimension, BlockPos pos, float yaw, float pitch) {
        static final Codec<ReturnLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(ReturnLocation::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(ReturnLocation::pos),
                Codec.FLOAT.fieldOf("yaw").forGetter(ReturnLocation::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(ReturnLocation::pitch)
        ).apply(instance, ReturnLocation::new));

        static ReturnLocation from(ServerPlayerEntity player) {
            return new ReturnLocation(
                    player.getEntityWorld().getRegistryKey().getValue(),
                    player.getBlockPos(),
                    player.getYaw(),
                    player.getPitch()
            );
        }

        static ReturnLocation from(ServerPlayerEntity player, ReturnLocation fallback) {
            if (player == null) {
                return fallback;
            }
            return from(player);
        }

        ServerWorld world(MinecraftServer server) {
            if (server == null) {
                return null;
            }
            var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dimension);
            return server.getWorld(key);
        }

        void teleport(MinecraftServer server, ServerPlayerEntity player) {
            ServerWorld world = world(server);
            if (world == null) {
                return;
            }
            GemsTeleport.teleport(player, world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, yaw, pitch);
        }
    }

    private record Duel(
            UUID duelId,
            UUID playerA,
            UUID playerB,
            ReturnLocation returnA,
            ReturnLocation returnB,
            BlockPos arenaCenter,
            Identifier arenaDimension,
            long endsAtTick
    ) {
        static final Codec<Duel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Uuids.CODEC.fieldOf("duelId").forGetter(Duel::duelId),
                Uuids.CODEC.fieldOf("playerA").forGetter(Duel::playerA),
                Uuids.CODEC.fieldOf("playerB").forGetter(Duel::playerB),
                ReturnLocation.CODEC.fieldOf("returnA").forGetter(Duel::returnA),
                ReturnLocation.CODEC.fieldOf("returnB").forGetter(Duel::returnB),
                BlockPos.CODEC.fieldOf("arenaCenter").forGetter(Duel::arenaCenter),
                Identifier.CODEC.fieldOf("arenaDimension").forGetter(Duel::arenaDimension),
                Codec.LONG.fieldOf("endsAtTick").forGetter(Duel::endsAtTick)
        ).apply(instance, Duel::new));
    }
}
