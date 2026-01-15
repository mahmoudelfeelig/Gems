package com.feel.gems.item;

import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsNbt;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;




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
    private static final PersistentStateType<OfflinePenaltyState> OFFLINE_PENALTY_STATE_TYPE =
            new PersistentStateType<>(KEY_OFFLINE_PENALTIES, OfflinePenaltyState::new, OfflinePenaltyState.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    private GemOwnership() {
    }

    public static void tagOwned(ItemStack stack, UUID owner, int epoch) {
        if (!(stack.getItem() instanceof GemItem)) {
            return;
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            GemsNbt.putUuid(nbt, KEY_OWNER, owner);
            nbt.putInt(KEY_EPOCH, epoch);
        });
    }

    public static void ensureOwner(ItemStack stack, ServerPlayerEntity owner) {
        if (!(stack.getItem() instanceof GemItem)) {
            return;
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (GemsNbt.containsUuid(nbt, KEY_OWNER)) {
                return;
            }
            GemsNbt.putUuid(nbt, KEY_OWNER, owner.getUuid());
            nbt.putInt(KEY_EPOCH, GemPlayerState.getGemEpoch(owner));
        });
    }

    public static UUID ownerUuid(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || !GemsNbt.containsUuid(custom.copyNbt(), KEY_OWNER)) {
            return null;
        }
        return GemsNbt.getUuid(custom.copyNbt(), KEY_OWNER);
    }

    public static boolean isInvalidForEpoch(MinecraftServer server, ItemStack stack) {
        if (!(stack.getItem() instanceof GemItem)) {
            return false;
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || !GemsNbt.containsUuid(custom.copyNbt(), KEY_OWNER)) {
            return false;
        }
        NbtCompound tag = custom.copyNbt();
        UUID owner = GemsNbt.getUuid(tag, KEY_OWNER);
        int storedEpoch = tag.getInt(KEY_EPOCH, 0);
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
        if (custom == null || !GemsNbt.containsUuid(custom.copyNbt(), KEY_OWNER)) {
            return false;
        }
        UUID stored = GemsNbt.getUuid(custom.copyNbt(), KEY_OWNER);
        return stored != null && stored.equals(owner);
    }

    public static void applyOwnerPenalty(ServerPlayerEntity owner) {
        GemPlayerState.initIfNeeded(owner);
        GemPlayerState.addMaxHearts(owner, -2);
        // int energy = GemPlayerState.getEnergy(owner);
        // GemPlayerState.setEnergy(owner, Math.min(energy, 1));
        GemPlayerState.applyMaxHearts(owner);
        ((com.feel.gems.state.GemsPersistentDataHolder) owner).gems$getPersistentData().putBoolean(KEY_SKIP_HEART_DROP, true);
        owner.damage(owner.getEntityWorld(), owner.getDamageSources().magic(), Float.MAX_VALUE);
        owner.sendMessage(net.minecraft.text.Text.translatable("gems.item.gem.penalty_applied"), false);
    }

    public static boolean consumeSkipHeartDrop(ServerPlayerEntity player) {
        var data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        boolean skip = data.getBoolean(KEY_SKIP_HEART_DROP).orElse(false);
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
        private static final Codec<OfflinePenaltyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                net.minecraft.util.Uuids.SET_CODEC.optionalFieldOf(KEY_OFFLINE_PENALTIES, Set.of()).forGetter(state -> state.queued)
        ).apply(instance, OfflinePenaltyState::new));

        private final Set<UUID> queued;

        OfflinePenaltyState() {
            this(Set.of());
        }

        OfflinePenaltyState(Set<UUID> queued) {
            this.queued = new HashSet<>(queued == null ? Set.of() : queued);
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
        MinecraftServer server = player.getEntityWorld().getServer();
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
        return mgr.getOrCreate(OFFLINE_PENALTY_STATE_TYPE);
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
        Deque<ServerPlayerEntity> players = new ArrayDeque<>();
        ServerPlayerEntity ownerPlayer = server.getPlayerManager().getPlayer(owner);
        if (ownerPlayer != null) {
            players.add(ownerPlayer);
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ownerPlayer != null && player == ownerPlayer) {
                continue;
            }
            players.add(player);
        }

        int view = server.getPlayerManager().getViewDistance();
        return new PurgeTask(owner, players, view);
    }

    private static final class PurgeTask {
        private final UUID owner;
        private final Deque<ServerPlayerEntity> players;
        private final int viewDistanceChunks;
        private java.util.Iterator<ItemEntity> scanningItems;
        private int scanBudgetRemaining;

        PurgeTask(UUID owner, Deque<ServerPlayerEntity> players, int viewDistanceChunks) {
            this.owner = owner;
            this.players = players;
            this.viewDistanceChunks = viewDistanceChunks;
        }

        UUID owner() {
            return owner;
        }

        boolean process(MinecraftServer server) {
            int playerBudget = PLAYER_PURGE_BUDGET;
            while (playerBudget-- > 0 && !players.isEmpty()) {
                purgePlayerInventories(server, players.poll());
            }

            if (scanningItems == null) {
                while (!players.isEmpty() && scanningItems == null) {
                    ServerPlayerEntity next = players.poll();
                    if (next == null || next.isRemoved() || next.getEntityWorld().getServer() != server) {
                        continue;
                    }
                    scanBudgetRemaining = CHUNK_PURGE_BUDGET * 64;
                    scanningItems = findNearbyItemEntities(next);
                }
            }

            if (scanningItems != null) {
                while (scanBudgetRemaining-- > 0 && scanningItems.hasNext()) {
                    ItemEntity itemEntity = scanningItems.next();
                    if (itemEntity == null) {
                        continue;
                    }
                    ItemStack stack = itemEntity.getStack();
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (isOwnedBy(stack, owner) && isInvalidForEpoch(server, stack)) {
                        itemEntity.discard();
                    }
                }
                if (!scanningItems.hasNext()) {
                    scanningItems = null;
                }
            }

            return players.isEmpty() && scanningItems == null;
        }

        private java.util.Iterator<ItemEntity> findNearbyItemEntities(ServerPlayerEntity aroundPlayer) {
            ServerWorld world = aroundPlayer.getEntityWorld();
            if (world == null) {
                return java.util.List.<ItemEntity>of().iterator();
            }
            int radiusBlocks = Math.max(0, viewDistanceChunks) * 16;
            if (radiusBlocks <= 0) {
                return java.util.List.<ItemEntity>of().iterator();
            }
            int minY = world.getBottomY();
            int maxY = world.getTopYInclusive();
            Box box = new Box(
                    aroundPlayer.getX() - radiusBlocks, minY, aroundPlayer.getZ() - radiusBlocks,
                    aroundPlayer.getX() + radiusBlocks, maxY, aroundPlayer.getZ() + radiusBlocks
            );
            return world.getEntitiesByClass(ItemEntity.class, box, e -> true).iterator();
        }
    }
}
