package com.feel.gems.item;

import com.feel.gems.state.GemPlayerState;
import com.feel.gems.core.GemId;
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
import net.minecraft.util.Uuids;
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
    private static final String KEY_OWNER_FALLBACK = "gemsOwner";

    private static final int PLAYER_PURGE_BUDGET = 8;
    private static final int CHUNK_PURGE_BUDGET = 6;
    private static final long PURGE_WINDOW_TICKS = 20L * 60L * 10L;
    private static final Deque<PurgeTask> PURGE_QUEUE = new ArrayDeque<>();
    private static final Set<UUID> PURGE_IN_FLIGHT = new HashSet<>();

    private static final String KEY_OFFLINE_PENALTIES = "gems_offline_gem_penalties";
    private static final String KEY_OWNER_EPOCHS = "gems_owner_epochs";
    private static final PersistentStateType<OfflinePenaltyState> OFFLINE_PENALTY_STATE_TYPE =
            new PersistentStateType<>(KEY_OFFLINE_PENALTIES, OfflinePenaltyState::new, OfflinePenaltyState.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);
    private static final PersistentStateType<OwnerEpochState> OWNER_EPOCH_STATE_TYPE =
            new PersistentStateType<>(KEY_OWNER_EPOCHS, OwnerEpochState::new, OwnerEpochState.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

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

    public static void tagOwned(ItemStack stack, ServerPlayerEntity owner) {
        if (!(stack.getItem() instanceof GemItem gem)) {
            return;
        }
        int epoch = currentEpochFor(owner, gem.gemId());
        tagOwned(stack, owner.getUuid(), epoch);
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
            if (stack.getItem() instanceof GemItem gem) {
                nbt.putInt(KEY_EPOCH, currentEpochFor(owner, gem.gemId()));
            } else {
                nbt.putInt(KEY_EPOCH, GemPlayerState.getGemEpoch(owner));
            }
        });
    }

    public static UUID ownerUuid(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        NbtCompound nbt = custom.copyNbt();
        if (GemsNbt.containsUuid(nbt, KEY_OWNER)) {
            return GemsNbt.getUuid(nbt, KEY_OWNER);
        }
        if (GemsNbt.containsUuid(nbt, KEY_OWNER_FALLBACK)) {
            return GemsNbt.getUuid(nbt, KEY_OWNER_FALLBACK);
        }
        return null;
    }

    public static void tagUnownedGems(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        GemPlayerState.initIfNeeded(player);
        int epoch = GemPlayerState.getGemEpoch(player);
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            tagIfMissingOwner(stack, player, epoch);
        }
        tagIfMissingOwner(player.getOffHandStack(), player, epoch);
        tagIfMissingOwner(player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD), player, epoch);
        tagIfMissingOwner(player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST), player, epoch);
        tagIfMissingOwner(player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS), player, epoch);
        tagIfMissingOwner(player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET), player, epoch);
    }

    private static void tagIfMissingOwner(ItemStack stack, ServerPlayerEntity player, int epoch) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GemItem)) {
            return;
        }
        if (ownerUuid(stack) != null) {
            return;
        }
        tagOwned(stack, player);
    }

    public static boolean isInvalidForEpoch(MinecraftServer server, ItemStack stack) {
        if (!(stack.getItem() instanceof GemItem gem)) {
            return false;
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return false;
        }
        NbtCompound tag = custom.copyNbt();
        UUID owner = null;
        if (GemsNbt.containsUuid(tag, KEY_OWNER)) {
            owner = GemsNbt.getUuid(tag, KEY_OWNER);
        } else if (GemsNbt.containsUuid(tag, KEY_OWNER_FALLBACK)) {
            owner = GemsNbt.getUuid(tag, KEY_OWNER_FALLBACK);
        }
        if (owner == null) {
            return false;
        }
        int storedEpoch = tag.getInt(KEY_EPOCH, 0);
        int current = resolveEpoch(server, owner, gem.gemId());
        if (current < 0) {
            return false;
        }
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

    public static void purgePendingPlayerInventories(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            purgePlayerInventories(server, player);
        }
    }

    public static void purgeInventoryIfPending(MinecraftServer server, Inventory inv) {
        if (inv == null) {
            return;
        }
        purgeInventory(server, inv);
    }

    public static boolean isOwnedBy(ItemStack stack, UUID owner) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || !GemsNbt.containsUuid(custom.copyNbt(), KEY_OWNER)) {
            return false;
        }
        UUID stored = GemsNbt.getUuid(custom.copyNbt(), KEY_OWNER);
        return stored != null && stored.equals(owner);
    }

    public static int removeOwnedGemFromInventory(ServerPlayerEntity holder, UUID owner, GemId gemId) {
        if (holder == null || owner == null || gemId == null) {
            return 0;
        }
        int removed = 0;
        var mainStacks = holder.getInventory().getMainStacks();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (isOwnedGem(stack, owner, gemId)) {
                mainStacks.set(i, ItemStack.EMPTY);
                removed++;
            }
        }
        ItemStack offhand = holder.getOffHandStack();
        if (isOwnedGem(offhand, owner, gemId)) {
            holder.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            removed++;
        }
        net.minecraft.entity.EquipmentSlot[] armorSlots = {
                net.minecraft.entity.EquipmentSlot.HEAD,
                net.minecraft.entity.EquipmentSlot.CHEST,
                net.minecraft.entity.EquipmentSlot.LEGS,
                net.minecraft.entity.EquipmentSlot.FEET
        };
        for (var slot : armorSlots) {
            ItemStack stack = holder.getEquippedStack(slot);
            if (isOwnedGem(stack, owner, gemId)) {
                holder.equipStack(slot, ItemStack.EMPTY);
                removed++;
            }
        }
        return removed;
    }

    public static int removeOwnedGemFromEnderChest(ServerPlayerEntity holder, UUID owner, GemId gemId) {
        if (holder == null || owner == null || gemId == null) {
            return 0;
        }
        int removed = 0;
        Inventory inv = holder.getEnderChestInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (isOwnedGem(stack, owner, gemId)) {
                inv.setStack(i, ItemStack.EMPTY);
                removed++;
            }
        }
        return removed;
    }

    private static boolean isOwnedGem(ItemStack stack, UUID owner, GemId gemId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof GemItem gem) || gem.gemId() != gemId) {
            return false;
        }
        return isOwnedBy(stack, owner);
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

    private static OwnerEpochState epochs(MinecraftServer server) {
        PersistentStateManager mgr = server.getOverworld().getPersistentStateManager();
        return mgr.getOrCreate(OWNER_EPOCH_STATE_TYPE);
    }

    public static void recordOwnerEpoch(MinecraftServer server, UUID owner, int epoch) {
        if (server == null || owner == null) {
            return;
        }
        OwnerEpochState state = epochs(server);
        state.setBaseEpoch(owner, epoch);
        state.markDirty();
    }

    public static void setGemEpochOverride(MinecraftServer server, UUID owner, GemId gemId, int epoch) {
        if (server == null || owner == null || gemId == null) {
            return;
        }
        OwnerEpochState state = epochs(server);
        state.setGemEpoch(owner, gemId, epoch);
        state.markPending(owner, server.getOverworld().getTime() + PURGE_WINDOW_TICKS);
        state.markDirty();
    }

    public static int getGemEpochOverride(MinecraftServer server, UUID owner, GemId gemId) {
        if (server == null || owner == null || gemId == null) {
            return -1;
        }
        return epochs(server).getGemEpoch(owner, gemId);
    }

    public static int nextGemEpoch(MinecraftServer server, UUID owner, GemId gemId, int baseEpoch) {
        if (server == null || owner == null || gemId == null) {
            return baseEpoch + 1;
        }
        int currentOverride = getGemEpochOverride(server, owner, gemId);
        int max = Math.max(baseEpoch, currentOverride);
        return max + 1;
    }

    public static int currentEpochFor(ServerPlayerEntity owner, GemId gemId) {
        if (owner == null || gemId == null) {
            return 0;
        }
        MinecraftServer server = owner.getEntityWorld().getServer();
        int base = GemPlayerState.getGemEpoch(owner);
        if (server != null) {
            OwnerEpochState state = epochs(server);
            state.setBaseEpoch(owner.getUuid(), base);
            int gemEpoch = state.getGemEpoch(owner.getUuid(), gemId);
            state.markDirty();
            if (gemEpoch >= 0) {
                return Math.max(base, gemEpoch);
            }
        }
        return base;
    }

    private static int resolveEpoch(MinecraftServer server, UUID owner, GemId gemId) {
        if (server == null || owner == null || gemId == null) {
            return -1;
        }
        OwnerEpochState state = epochs(server);
        int base;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(owner);
        if (player != null) {
            base = GemPlayerState.getGemEpoch(player);
            state.setBaseEpoch(owner, base);
        } else {
            base = state.getBaseEpoch(owner);
        }
        int gemEpoch = state.getGemEpoch(owner, gemId);
        state.markDirty();
        if (gemEpoch < 0) {
            return base;
        }
        if (base < 0) {
            return gemEpoch;
        }
        return Math.max(base, gemEpoch);
    }

    private static boolean hasPendingPurge(MinecraftServer server) {
        if (server == null) {
            return false;
        }
        return epochs(server).hasPending(server.getOverworld().getTime());
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

    private static final class OwnerEpochState extends PersistentState {
        static final Codec<OwnerEpochState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Uuids.CODEC, Codec.INT)
                        .optionalFieldOf("epochs", java.util.Map.of())
                        .forGetter(state -> state.baseEpochs),
                Codec.unboundedMap(Uuids.CODEC, Codec.unboundedMap(Codec.STRING, Codec.INT))
                        .optionalFieldOf("gemEpochs", java.util.Map.of())
                        .forGetter(state -> state.gemEpochsRaw),
                Codec.unboundedMap(Uuids.CODEC, Codec.LONG)
                        .optionalFieldOf("purgeUntil", java.util.Map.of())
                        .forGetter(state -> state.purgeUntil)
        ).apply(instance, OwnerEpochState::new));

        private final java.util.Map<UUID, Integer> baseEpochs;
        private final java.util.Map<UUID, java.util.Map<String, Integer>> gemEpochsRaw;
        private final java.util.Map<UUID, Long> purgeUntil;

        OwnerEpochState() {
            this(java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
        }

        OwnerEpochState(java.util.Map<UUID, Integer> baseEpochs, java.util.Map<UUID, java.util.Map<String, Integer>> gemEpochsRaw,
                        java.util.Map<UUID, Long> purgeUntil) {
            this.baseEpochs = new java.util.HashMap<>(baseEpochs == null ? java.util.Map.of() : baseEpochs);
            this.gemEpochsRaw = new java.util.HashMap<>(gemEpochsRaw == null ? java.util.Map.of() : gemEpochsRaw);
            this.purgeUntil = new java.util.HashMap<>(purgeUntil == null ? java.util.Map.of() : purgeUntil);
        }

        int getBaseEpoch(UUID owner) {
            return baseEpochs.getOrDefault(owner, -1);
        }

        void setBaseEpoch(UUID owner, int epoch) {
            baseEpochs.put(owner, epoch);
        }

        int getGemEpoch(UUID owner, GemId gemId) {
            java.util.Map<String, Integer> map = gemEpochsRaw.get(owner);
            if (map == null) {
                return -1;
            }
            return map.getOrDefault(gemId.name(), -1);
        }

        void setGemEpoch(UUID owner, GemId gemId, int epoch) {
            java.util.Map<String, Integer> map = gemEpochsRaw.computeIfAbsent(owner, key -> new java.util.HashMap<>());
            map.put(gemId.name(), epoch);
        }

        void markPending(UUID owner, long untilTick) {
            if (owner == null) {
                return;
            }
            long current = purgeUntil.getOrDefault(owner, 0L);
            if (untilTick > current) {
                purgeUntil.put(owner, untilTick);
            }
        }

        boolean hasPending(long now) {
            if (purgeUntil.isEmpty()) {
                return false;
            }
            boolean any = false;
            java.util.Iterator<java.util.Map.Entry<UUID, Long>> it = purgeUntil.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<UUID, Long> entry = it.next();
                if (entry.getValue() == null || entry.getValue() <= now) {
                    it.remove();
                    continue;
                }
                any = true;
            }
            return any;
        }
    }
}
