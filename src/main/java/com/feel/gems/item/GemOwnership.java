package com.feel.gems.item;

import com.feel.gems.state.GemPlayerState;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;




/**
 * Ownership tagging and validation for gem items.
 */
public final class GemOwnership {
    private static final String KEY_OWNER = "GemOwner";
    private static final String KEY_EPOCH = "GemOwnerEpoch";
    private static final String KEY_SKIP_HEART_DROP = "gemsSkipHeartDropOnce";

    private static final int PLAYER_PURGE_BUDGET = 8;
    private static final int CHUNK_PURGE_BUDGET = 6;
    private static final Deque<PurgeTask> PURGE_QUEUE = new ArrayDeque<>();
    private static final Set<UUID> PURGE_IN_FLIGHT = new HashSet<>();

    private static final String KEY_OFFLINE_PENALTIES = "gems_offline_gem_penalties";
    private static final PersistentState.Type<OfflinePenaltyState> OFFLINE_PENALTY_STATE_TYPE =
            new PersistentState.Type<>(OfflinePenaltyState::new, OfflinePenaltyState::fromNbt, DataFixTypes.SAVED_DATA_MAP_DATA);

    private GemOwnership() {
    }

    public static void tagOwned(ItemStack stack, UUID owner, int epoch) {
        if (!(stack.getItem() instanceof GemItem)) {
            return;
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putUuid(KEY_OWNER, owner);
            nbt.putInt(KEY_EPOCH, epoch);
        });
    }

    public static void ensureOwner(ItemStack stack, ServerPlayerEntity owner) {
        if (!(stack.getItem() instanceof GemItem)) {
            return;
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (nbt.containsUuid(KEY_OWNER)) {
                return;
            }
            nbt.putUuid(KEY_OWNER, owner.getUuid());
            nbt.putInt(KEY_EPOCH, GemPlayerState.getGemEpoch(owner));
        });
    }

    public static UUID ownerUuid(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || !custom.getNbt().containsUuid(KEY_OWNER)) {
            return null;
        }
        return custom.getNbt().getUuid(KEY_OWNER);
    }

    public static boolean isInvalidForEpoch(MinecraftServer server, ItemStack stack) {
        if (!(stack.getItem() instanceof GemItem)) {
            return false;
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || !custom.getNbt().containsUuid(KEY_OWNER)) {
            return false;
        }
        NbtCompound tag = custom.getNbt();
        UUID owner = tag.getUuid(KEY_OWNER);
        int storedEpoch = tag.getInt(KEY_EPOCH);
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(owner);
        if (player == null) {
            return false; // Owner offline; leave it intact.
        }
        int current = GemPlayerState.getGemEpoch(player);
        return storedEpoch < current;
    }

    public static boolean purgeIfInvalid(MinecraftServer server, ItemStack stack) {
        if (isInvalidForEpoch(server, stack)) {
            stack.setCount(0);
            return true;
        }
        return false;
    }

    public static void purgeInventory(MinecraftServer server, Inventory inv) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (purgeIfInvalid(server, stack)) {
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    public static void purgePlayerInventories(MinecraftServer server, PlayerEntity player) {
        purgeInventory(server, player.getInventory());
        if (player instanceof ServerPlayerEntity sp) {
            purgeInventory(server, sp.getEnderChestInventory());
        }
    }

    public static boolean isOwnedBy(ItemStack stack, UUID owner) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || !custom.getNbt().containsUuid(KEY_OWNER)) {
            return false;
        }
        return custom.getNbt().getUuid(KEY_OWNER).equals(owner);
    }

    public static void applyOwnerPenalty(ServerPlayerEntity owner) {
        GemPlayerState.initIfNeeded(owner);
        GemPlayerState.addMaxHearts(owner, -2);
        int energy = GemPlayerState.getEnergy(owner);
        GemPlayerState.setEnergy(owner, Math.min(energy, 1));
        GemPlayerState.applyMaxHearts(owner);
        ((com.feel.gems.state.GemsPersistentDataHolder) owner).gems$getPersistentData().putBoolean(KEY_SKIP_HEART_DROP, true);
        owner.damage(owner.getDamageSources().magic(), Float.MAX_VALUE);
        owner.sendMessage(net.minecraft.text.Text.literal("Your gem was activated by another player. You paid the cost."), false);
    }

    public static boolean consumeSkipHeartDrop(ServerPlayerEntity player) {
        var data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        boolean skip = data.getBoolean(KEY_SKIP_HEART_DROP);
        if (skip) {
            data.remove(KEY_SKIP_HEART_DROP);
        }
        return skip;
    }

    public static void markSkipHeartDropOnce(ServerPlayerEntity player) {
        var data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean(KEY_SKIP_HEART_DROP, true);
    }

    private static final class OfflinePenaltyState extends PersistentState {
        private final Set<UUID> queued = new HashSet<>();

        OfflinePenaltyState() {
        }

        static OfflinePenaltyState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            OfflinePenaltyState state = new OfflinePenaltyState();
            if (nbt.contains(KEY_OFFLINE_PENALTIES, NbtElement.LIST_TYPE)) {
                NbtList list = nbt.getList(KEY_OFFLINE_PENALTIES, NbtElement.STRING_TYPE);
                for (int i = 0; i < list.size(); i++) {
                    try {
                        state.queued.add(UUID.fromString(list.getString(i)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            NbtList list = new NbtList();
            for (UUID id : queued) {
                list.add(NbtString.of(id.toString()));
            }
            nbt.put(KEY_OFFLINE_PENALTIES, list);
            return nbt;
        }

        void queue(UUID owner) {
            queued.add(owner);
        }

        boolean consume(UUID owner) {
            return queued.remove(owner);
        }
    }

    public static void requestDeferredPurge(MinecraftServer server, UUID owner) {
        if (server == null || PURGE_IN_FLIGHT.contains(owner)) {
            return;
        }
        PURGE_IN_FLIGHT.add(owner);
        PURGE_QUEUE.add(buildTask(server, owner));
    }

    public static void queueOfflinePenalty(MinecraftServer server, UUID owner) {
        if (server == null || owner == null) {
            return;
        }
        OfflinePenaltyState state = penalties(server);
        state.queue(owner);
        state.markDirty();
    }

    public static void consumeOfflinePenalty(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        OfflinePenaltyState state = penalties(server);
        if (state.consume(player.getUuid())) {
            applyOwnerPenalty(player);
            state.markDirty();
        }
    }

    private static OfflinePenaltyState penalties(MinecraftServer server) {
        PersistentStateManager mgr = server.getOverworld().getPersistentStateManager();
        return mgr.getOrCreate(OFFLINE_PENALTY_STATE_TYPE, KEY_OFFLINE_PENALTIES);
    }

    public static void tickPurgeQueue(MinecraftServer server) {
        PurgeTask task = PURGE_QUEUE.peek();
        if (task == null) {
            return;
        }
        if (task.process(server)) {
            PURGE_QUEUE.poll();
            PURGE_IN_FLIGHT.remove(task.owner());
        }
    }

    private static PurgeTask buildTask(MinecraftServer server, UUID owner) {
        Deque<ServerPlayerEntity> players = new ArrayDeque<>(server.getPlayerManager().getPlayerList());
        Deque<ChunkJob> chunks = new ArrayDeque<>();
        int view = server.getPlayerManager().getViewDistance();

        for (ServerWorld world : server.getWorlds()) {
            int minY = world.getBottomY();
            int maxY = world.getTopY();
            Set<Long> seenChunks = new HashSet<>();
            for (ServerPlayerEntity player : world.getPlayers()) {
                ChunkPos center = player.getChunkPos();
                for (int dx = -view; dx <= view; dx++) {
                    for (int dz = -view; dz <= view; dz++) {
                        ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
                        long key = chunk.toLong();
                        if (!seenChunks.add(key)) {
                            continue;
                        }
                        if (!world.isChunkLoaded(chunk.x, chunk.z)) {
                            continue; // skip unloaded chunks to keep the purge queue small
                        }
                        int minX = chunk.getStartX();
                        int minZ = chunk.getStartZ();
                        int maxX = chunk.getEndX() + 1;
                        int maxZ = chunk.getEndZ() + 1;
                        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
                        chunks.add(new ChunkJob(world, box));
                    }
                }
            }
        }

        return new PurgeTask(owner, players, chunks);
    }

    private static final class PurgeTask {
        private final UUID owner;
        private final Deque<ServerPlayerEntity> players;
        private final Deque<ChunkJob> chunks;

        PurgeTask(UUID owner, Deque<ServerPlayerEntity> players, Deque<ChunkJob> chunks) {
            this.owner = owner;
            this.players = players;
            this.chunks = chunks;
        }

        UUID owner() {
            return owner;
        }

        boolean process(MinecraftServer server) {
            int playerBudget = PLAYER_PURGE_BUDGET;
            while (playerBudget-- > 0 && !players.isEmpty()) {
                purgePlayerInventories(server, players.poll());
            }

            int chunkBudget = CHUNK_PURGE_BUDGET;
            while (chunkBudget-- > 0 && !chunks.isEmpty()) {
                ChunkJob job = chunks.poll();
                if (job == null || job.world().getServer() != server) {
                    continue;
                }

                for (ItemEntity itemEntity : job.world().getEntitiesByClass(ItemEntity.class, job.box(), e -> true)) {
                    ItemStack stack = itemEntity.getStack();
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (isOwnedBy(stack, owner) && isInvalidForEpoch(server, stack)) {
                        itemEntity.discard();
                    }
                }
            }

            return players.isEmpty() && chunks.isEmpty();
        }
    }

    private record ChunkJob(ServerWorld world, Box box) {
    }
}
