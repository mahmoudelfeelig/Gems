package com.feel.gems.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.util.GemsTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;




public final class LegendaryCrafting {
    private static final String STATE_KEY = "gems_legendary_crafts";
    private static final PersistentState.Type<State> STATE_TYPE =
            new PersistentState.Type<>(State::new, State::fromNbt, DataFixTypes.SAVED_DATA_MAP_DATA);

    private static final String KEY_CRAFTED = "crafted";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_PENDING = "pending";

    private static final String KEY_ID = "id";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_START = "start";
    private static final String KEY_FINISH = "finish";
    private static final String KEY_DIM = "dim";
    private static final String KEY_POS = "pos";
    private static final String KEY_ITEMS = "items";

    private static final String KEY_BLOCKED_NOTIFY = "gemsLegendaryCraftBlockedUntil";

    private static final Map<String, net.minecraft.entity.boss.ServerBossBar> ACTIVE_BARS = new HashMap<>();

    private LegendaryCrafting() {
    }

    public static boolean canStartCraft(ServerPlayerEntity player, ItemStack stack) {
        if (!(stack.getItem() instanceof LegendaryItem legendary)) {
            return true;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        String id = legendary.legendaryId();
        State state = state(server);
        if (!state.isAvailable(id)) {
            notifyBlocked(player, id);
            return false;
        }
        return true;
    }

    public static void onCrafted(ServerPlayerEntity player, ItemStack stack) {
        if (!(stack.getItem() instanceof LegendaryItem legendary)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        String id = legendary.legendaryId();
        State state = state(server);
        if (!state.isAvailable(id)) {
            notifyBlocked(player, id);
            removeCraftResult(player, stack.getItem());
            return;
        }

        long now = GemsTime.now(server);
        int craftTicks = GemsBalance.v().legendary().craftTicks();
        long finish = now + craftTicks;
        CraftLocation location = resolveCraftLocation(player);
        state.start(id, player.getUuid(), now, finish, location);
        state.markDirty();

        removeCraftResult(player, stack.getItem());
        announceCraftStart(server, player, stack.getItem(), craftTicks);
        player.sendMessage(Text.literal("Legendary craft started. Return in " + seconds(craftTicks) + "s."), true);
    }

    public static void tick(MinecraftServer server) {
        State state = state(server);
        long now = GemsTime.now(server);
        updateBossBars(server, state, now);
        if (state.active.isEmpty()) {
            return;
        }
        List<ActiveCraft> finished = new ArrayList<>();
        for (ActiveCraft craft : state.active.values()) {
            if (craft.finishTick <= now) {
                finished.add(craft);
            }
        }
        if (finished.isEmpty()) {
            return;
        }
        for (ActiveCraft craft : finished) {
            state.active.remove(craft.id);
            state.crafted.add(craft.id);
            dropAtLocation(server, craft);
            removeBossBar(craft.id);
        }
        state.markDirty();
    }

    public static void deliverPending(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        State state = state(server);
        List<String> pending = state.pending.remove(player.getUuid());
        if (pending == null || pending.isEmpty()) {
            return;
        }
        for (String id : pending) {
            deliver(player, id);
        }
        state.markDirty();
    }

    private static void deliver(ServerPlayerEntity player, String id) {
        Identifier itemId = Identifier.tryParse(id);
        if (itemId == null) {
            return;
        }
        Item item = Registries.ITEM.get(itemId);
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        applyDefaultEnchants(player.getServer(), stack);
        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }
        player.sendMessage(Text.literal("Legendary craft complete: " + stack.getName().getString()), false);
    }

    private static void dropAtLocation(MinecraftServer server, ActiveCraft craft) {
        Identifier itemId = Identifier.tryParse(craft.id);
        if (itemId == null) {
            return;
        }
        Item item = Registries.ITEM.get(itemId);
        if (item == null) {
            return;
        }
        net.minecraft.registry.RegistryKey<World> worldKey = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, craft.dimension);
        net.minecraft.server.world.ServerWorld world = server.getWorld(worldKey);
        if (world == null) {
            world = server.getOverworld();
        }
        ItemStack stack = new ItemStack(item);
        applyDefaultEnchants(server, stack);
        BlockPos pos = craft.pos;
        world.getChunk(pos);
        net.minecraft.entity.ItemEntity entity = new net.minecraft.entity.ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, stack);
        world.spawnEntity(entity);
        String itemName = stack.getName().getString();
        Text text = Text.literal("Legendary craft complete: " + itemName + " dropped at "
                + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + " (" + craft.dimension + ").");
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(text, false);
        }
    }

    private static void removeCraftResult(ServerPlayerEntity player, Item item) {
        if (player.currentScreenHandler != null) {
            ItemStack cursor = player.currentScreenHandler.getCursorStack();
            if (cursor.isOf(item)) {
                player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                return;
            }
        }
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (stack.isOf(item)) {
                player.getInventory().main.set(i, ItemStack.EMPTY);
                return;
            }
        }
        for (int i = 0; i < player.getInventory().offHand.size(); i++) {
            ItemStack stack = player.getInventory().offHand.get(i);
            if (stack.isOf(item)) {
                player.getInventory().offHand.set(i, ItemStack.EMPTY);
                return;
            }
        }
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (stack.isOf(item)) {
                player.getInventory().armor.set(i, ItemStack.EMPTY);
                return;
            }
        }
    }

    private static void applyDefaultEnchants(MinecraftServer server, ItemStack stack) {
        if (server == null) {
            return;
        }
        if (stack.isOf(com.feel.gems.item.ModItems.EARTHSPLITTER_PICK)) {
            RegistryEntry<Enchantment> silk = resolveEnchantment(server, Enchantments.SILK_TOUCH);
            if (silk != null) {
                EnchantmentHelper.apply(stack, builder -> builder.set(silk, 1));
            }
        }
    }

    private static RegistryEntry<Enchantment> resolveEnchantment(MinecraftServer server, net.minecraft.registry.RegistryKey<Enchantment> key) {
        var registry = server.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var entry = registry.getEntry(key);
        return entry.orElse(null);
    }

    private static void announceCraftStart(MinecraftServer server, ServerPlayerEntity player, Item item, int craftTicks) {
        CraftLocation location = resolveCraftLocation(player);
        BlockPos pos = location.pos;
        String dim = location.dimension.toString();
        String itemName = new ItemStack(item).getName().getString();
        Text text = Text.literal(player.getName().getString()
                + " began crafting " + itemName
                + " at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + " (" + dim + ").");
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            other.sendMessage(text, false);
        }
    }

    private static void notifyBlocked(ServerPlayerEntity player, String id) {
        long now = GemsTime.now(player);
        NbtCompound data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        long next = data.getLong(KEY_BLOCKED_NOTIFY);
        if (next > now) {
            return;
        }
        data.putLong(KEY_BLOCKED_NOTIFY, now + 40);
        player.sendMessage(Text.literal("That legendary item has already been crafted."), true);
    }

    private static int seconds(int ticks) {
        return Math.max(0, ticks / 20);
    }

    private static State state(MinecraftServer server) {
        PersistentStateManager mgr = server.getOverworld().getPersistentStateManager();
        return mgr.getOrCreate(STATE_TYPE, STATE_KEY);
    }

    private record ActiveCraft(String id, UUID owner, long startTick, long finishTick, Identifier dimension, BlockPos pos) {
    }

    private static final class State extends PersistentState {
        private final Set<String> crafted = new HashSet<>();
        private final Map<String, ActiveCraft> active = new HashMap<>();
        private final Map<UUID, List<String>> pending = new HashMap<>();

        static State fromNbt(NbtCompound nbt, WrapperLookup registryLookup) {
            State state = new State();
            if (nbt.contains(KEY_CRAFTED, NbtElement.LIST_TYPE)) {
                NbtList list = nbt.getList(KEY_CRAFTED, NbtElement.STRING_TYPE);
                for (int i = 0; i < list.size(); i++) {
                    state.crafted.add(list.getString(i));
                }
            }
            if (nbt.contains(KEY_ACTIVE, NbtElement.LIST_TYPE)) {
                NbtList list = nbt.getList(KEY_ACTIVE, NbtElement.COMPOUND_TYPE);
                for (int i = 0; i < list.size(); i++) {
                    NbtCompound entry = list.getCompound(i);
                    String id = entry.getString(KEY_ID);
                    if (!entry.containsUuid(KEY_OWNER)) {
                        continue;
                    }
                    UUID owner = entry.getUuid(KEY_OWNER);
                    long start = entry.getLong(KEY_START);
                    long finish = entry.getLong(KEY_FINISH);
                    Identifier dim = Identifier.tryParse(entry.getString(KEY_DIM));
                    BlockPos pos = readBlockPos(entry, KEY_POS);
                    if (dim == null || pos == null) {
                        continue;
                    }
                    if (!id.isEmpty()) {
                        state.active.put(id, new ActiveCraft(id, owner, start, finish, dim, pos));
                    }
                }
            }
            if (nbt.contains(KEY_PENDING, NbtElement.LIST_TYPE)) {
                NbtList list = nbt.getList(KEY_PENDING, NbtElement.COMPOUND_TYPE);
                for (int i = 0; i < list.size(); i++) {
                    NbtCompound entry = list.getCompound(i);
                    if (!entry.containsUuid(KEY_OWNER)) {
                        continue;
                    }
                    UUID owner = entry.getUuid(KEY_OWNER);
                    NbtList items = entry.getList(KEY_ITEMS, NbtElement.STRING_TYPE);
                    List<String> ids = new ArrayList<>(items.size());
                    for (int j = 0; j < items.size(); j++) {
                        ids.add(items.getString(j));
                    }
                    if (!ids.isEmpty()) {
                        state.pending.put(owner, ids);
                    }
                }
            }
            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            NbtList craftedList = new NbtList();
            for (String id : crafted) {
                craftedList.add(NbtString.of(id));
            }
            nbt.put(KEY_CRAFTED, craftedList);

            NbtList activeList = new NbtList();
            for (ActiveCraft craft : active.values()) {
                NbtCompound entry = new NbtCompound();
                entry.putString(KEY_ID, craft.id);
                entry.putUuid(KEY_OWNER, craft.owner);
                entry.putLong(KEY_START, craft.startTick);
                entry.putLong(KEY_FINISH, craft.finishTick);
                entry.putString(KEY_DIM, craft.dimension.toString());
                writeBlockPos(entry, KEY_POS, craft.pos);
                activeList.add(entry);
            }
            nbt.put(KEY_ACTIVE, activeList);

            NbtList pendingList = new NbtList();
            for (Map.Entry<UUID, List<String>> entry : pending.entrySet()) {
                NbtCompound out = new NbtCompound();
                out.putUuid(KEY_OWNER, entry.getKey());
                NbtList items = new NbtList();
                for (String id : entry.getValue()) {
                    items.add(NbtString.of(id));
                }
                out.put(KEY_ITEMS, items);
                pendingList.add(out);
            }
            nbt.put(KEY_PENDING, pendingList);

            return nbt;
        }

        boolean isAvailable(String id) {
            return !crafted.contains(id) && !active.containsKey(id);
        }

        void start(String id, UUID owner, long startTick, long finishTick, CraftLocation location) {
            active.put(id, new ActiveCraft(id, owner, startTick, finishTick, location.dimension, location.pos));
        }

        void addPending(UUID owner, String id) {
            pending.computeIfAbsent(owner, key -> new ArrayList<>()).add(id);
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

    private record CraftLocation(Identifier dimension, BlockPos pos) {
    }

    private static CraftLocation resolveCraftLocation(ServerPlayerEntity player) {
        if (player.currentScreenHandler instanceof net.minecraft.screen.CraftingScreenHandler handler) {
            var context = ((com.feel.gems.mixin.accessor.CraftingScreenHandlerAccessor) handler).gems$getContext();
            if (context != null) {
                final BlockPos[] pos = new BlockPos[1];
                final Identifier[] dim = new Identifier[1];
                context.run((world, blockPos) -> {
                    pos[0] = blockPos.toImmutable();
                    dim[0] = world.getRegistryKey().getValue();
                });
                if (pos[0] != null && dim[0] != null) {
                    return new CraftLocation(dim[0], pos[0]);
                }
            }
        }
        return new CraftLocation(player.getServerWorld().getRegistryKey().getValue(), player.getBlockPos());
    }

    private static void updateBossBars(MinecraftServer server, State state, long now) {
        if (state.active.isEmpty()) {
            clearBossBars();
            return;
        }
        for (ActiveCraft craft : state.active.values()) {
            net.minecraft.entity.boss.ServerBossBar bar = ACTIVE_BARS.computeIfAbsent(craft.id, key -> new net.minecraft.entity.boss.ServerBossBar(
                    Text.literal("Legendary Craft"),
                    net.minecraft.entity.boss.BossBar.Color.PURPLE,
                    net.minecraft.entity.boss.BossBar.Style.PROGRESS
            ));
            long total = Math.max(1L, craft.finishTick - craft.startTick);
            float progress = Math.min(1.0F, Math.max(0.0F, (now - craft.startTick) / (float) total));
            bar.setPercent(progress);

            Item item = Registries.ITEM.get(Identifier.of(craft.id));
            String itemName = item != null ? new ItemStack(item).getName().getString() : craft.id;
            String title = "Crafting " + itemName + " @ " + craft.pos.getX() + " " + craft.pos.getY() + " " + craft.pos.getZ()
                    + " (" + craft.dimension + ")";
            bar.setName(Text.literal(title));

            bar.clearPlayers();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                bar.addPlayer(player);
            }
        }
        List<String> remove = new ArrayList<>();
        for (String id : ACTIVE_BARS.keySet()) {
            if (!state.active.containsKey(id)) {
                remove.add(id);
            }
        }
        for (String id : remove) {
            removeBossBar(id);
        }
    }

    private static void clearBossBars() {
        for (String id : new ArrayList<>(ACTIVE_BARS.keySet())) {
            removeBossBar(id);
        }
    }

    private static void removeBossBar(String id) {
        net.minecraft.entity.boss.ServerBossBar bar = ACTIVE_BARS.remove(id);
        if (bar == null) {
            return;
        }
        bar.clearPlayers();
    }
}
