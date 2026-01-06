package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.util.GemsTime;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;




public final class LegendaryCrafting {
    private static final String STATE_KEY = "gems_legendary_crafts";
    private static final PersistentStateType<State> STATE_TYPE =
            new PersistentStateType<>(STATE_KEY, State::new, State.CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

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
        MinecraftServer server = player.getEntityWorld().getServer();
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
        MinecraftServer server = player.getEntityWorld().getServer();
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
        player.sendMessage(Text.translatable("gems.legendary.craft_started", seconds(craftTicks)), true);
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
            state.active.remove(craft.key);
            state.addCrafted(craft.id);
            dropAtLocation(server, craft);
            removeBossBar(craft.key);
        }
        state.markDirty();
    }

    public static void deliverPending(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
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
        applyDefaultEnchants(player.getEntityWorld().getServer(), stack);
        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }
        player.sendMessage(Text.translatable("gems.legendary.craft_complete", stack.getName().getString()), false);
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
        Text text = Text.translatable("gems.legendary.craft_dropped", itemName,
                pos.getX(), pos.getY(), pos.getZ(), craft.dimension.toString());
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
        var mainStacks = player.getInventory().getMainStacks();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (stack.isOf(item)) {
                mainStacks.set(i, ItemStack.EMPTY);
                return;
            }
        }
        if (player.getOffHandStack().isOf(item)) {
            player.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            return;
        }
        net.minecraft.entity.EquipmentSlot[] armorSlots = {
                net.minecraft.entity.EquipmentSlot.HEAD,
                net.minecraft.entity.EquipmentSlot.CHEST,
                net.minecraft.entity.EquipmentSlot.LEGS,
                net.minecraft.entity.EquipmentSlot.FEET
        };
        for (var slot : armorSlots) {
            if (player.getEquippedStack(slot).isOf(item)) {
                player.equipStack(slot, ItemStack.EMPTY);
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
        return server.getRegistryManager().getOptionalEntry(key).orElse(null);
    }

    private static void announceCraftStart(MinecraftServer server, ServerPlayerEntity player, Item item, int craftTicks) {
        CraftLocation location = resolveCraftLocation(player);
        BlockPos pos = location.pos;
        String dim = location.dimension.toString();
        String itemName = new ItemStack(item).getName().getString();
        Text text = Text.translatable("gems.legendary.craft_announce",
                player.getName().getString(), itemName,
                pos.getX(), pos.getY(), pos.getZ(), dim);
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            other.sendMessage(text, false);
        }
    }

    private static void notifyBlocked(ServerPlayerEntity player, String id) {
        long now = GemsTime.now(player);
        NbtCompound data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        long next = data.getLong(KEY_BLOCKED_NOTIFY, 0L);
        if (next > now) {
            return;
        }
        data.putLong(KEY_BLOCKED_NOTIFY, now + 40);
        player.sendMessage(Text.translatable("gems.legendary.crafting_limit"), true);
    }

    private static int seconds(int ticks) {
        return Math.max(0, ticks / 20);
    }

    private static State state(MinecraftServer server) {
        PersistentStateManager mgr = server.getOverworld().getPersistentStateManager();
        return mgr.getOrCreate(STATE_TYPE);
    }

    private record ActiveCraft(String key, String id, UUID owner, long startTick, long finishTick, Identifier dimension, BlockPos pos) {
        static final Codec<ActiveCraft> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf(KEY_ID).forGetter(ActiveCraft::id),
                Uuids.CODEC.fieldOf(KEY_OWNER).forGetter(ActiveCraft::owner),
                Codec.LONG.fieldOf(KEY_START).forGetter(ActiveCraft::startTick),
                Codec.LONG.fieldOf(KEY_FINISH).forGetter(ActiveCraft::finishTick),
                Identifier.CODEC.fieldOf(KEY_DIM).forGetter(ActiveCraft::dimension),
                BlockPos.CODEC.fieldOf(KEY_POS).forGetter(ActiveCraft::pos)
        ).apply(instance, (id, owner, start, finish, dim, pos) -> new ActiveCraft(craftKey(id, owner, start), id, owner, start, finish, dim, pos)));
    }

    private static final class State extends PersistentState {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf(KEY_CRAFTED, Map.of()).forGetter(state -> state.crafted),
                Codec.unboundedMap(Codec.STRING, ActiveCraft.CODEC).optionalFieldOf(KEY_ACTIVE, Map.of()).forGetter(state -> state.active),
                Codec.unboundedMap(Uuids.CODEC, Codec.STRING.listOf()).optionalFieldOf(KEY_PENDING, Map.of()).forGetter(state -> state.pending)
        ).apply(instance, State::new));

        private final Map<String, Integer> crafted;
        private final Map<String, ActiveCraft> active;
        private final Map<UUID, List<String>> pending;

        State() {
            this(Map.of(), Map.of(), Map.of());
        }

        State(Map<String, Integer> crafted, Map<String, ActiveCraft> active, Map<UUID, List<String>> pending) {
            this.crafted = new HashMap<>(crafted == null ? Map.of() : crafted);
            this.active = new HashMap<>(active == null ? Map.of() : active);
            this.pending = new HashMap<>();
            if (pending != null) {
                for (var entry : pending.entrySet()) {
                    List<String> items = entry.getValue() == null ? List.of() : entry.getValue();
                    this.pending.put(entry.getKey(), new ArrayList<>(items));
                }
            }
        }

        boolean isAvailable(String id) {
            int maxActive = GemsBalance.v().legendary().craftMaxActivePerItem();
            if (maxActive > 0 && activeCount(id) >= maxActive) {
                return false;
            }
            int maxPerItem = GemsBalance.v().legendary().craftMaxPerItem();
            if (maxPerItem <= 0) {
                return true;
            }
            return craftedCount(id) < maxPerItem;
        }

        void start(String id, UUID owner, long startTick, long finishTick, CraftLocation location) {
            String key = craftKey(id, owner, startTick);
            active.put(key, new ActiveCraft(key, id, owner, startTick, finishTick, location.dimension, location.pos));
        }

        void addCrafted(String id) {
            if (id == null || id.isEmpty()) {
                return;
            }
            crafted.put(id, crafted.getOrDefault(id, 0) + 1);
        }

        int craftedCount(String id) {
            if (id == null || id.isEmpty()) {
                return 0;
            }
            return crafted.getOrDefault(id, 0);
        }

        int activeCount(String id) {
            if (id == null || id.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (ActiveCraft craft : active.values()) {
                if (id.equals(craft.id)) {
                    count++;
                }
            }
            return count;
        }

        void addPending(UUID owner, String id) {
            pending.computeIfAbsent(owner, key -> new ArrayList<>()).add(id);
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
        return new CraftLocation(player.getEntityWorld().getRegistryKey().getValue(), player.getBlockPos());
    }

    private static void updateBossBars(MinecraftServer server, State state, long now) {
        if (state.active.isEmpty()) {
            clearBossBars();
            return;
        }
        for (ActiveCraft craft : state.active.values()) {
            net.minecraft.entity.boss.ServerBossBar bar = ACTIVE_BARS.computeIfAbsent(craft.key, key -> new net.minecraft.entity.boss.ServerBossBar(
                    Text.translatable("gems.legendary.craft_bossbar_title"),
                    net.minecraft.entity.boss.BossBar.Color.PURPLE,
                    net.minecraft.entity.boss.BossBar.Style.PROGRESS
            ));
            long total = Math.max(1L, craft.finishTick - craft.startTick);
            float progress = Math.min(1.0F, Math.max(0.0F, (now - craft.startTick) / (float) total));
            bar.setPercent(progress);

            Item item = Registries.ITEM.get(Identifier.of(craft.id));
            String itemName = item != null ? new ItemStack(item).getName().getString() : craft.id;
            bar.setName(Text.translatable("gems.legendary.craft_bossbar", itemName,
                    craft.pos.getX(), craft.pos.getY(), craft.pos.getZ(), craft.dimension.toString()));

            bar.clearPlayers();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                bar.addPlayer(player);
            }
        }
        List<String> remove = new ArrayList<>();
        for (String key : ACTIVE_BARS.keySet()) {
            if (!state.active.containsKey(key)) {
                remove.add(key);
            }
        }
        for (String key : remove) {
            removeBossBar(key);
        }
    }

    private static void clearBossBars() {
        for (String key : new ArrayList<>(ACTIVE_BARS.keySet())) {
            removeBossBar(key);
        }
    }

    private static void removeBossBar(String key) {
        net.minecraft.entity.boss.ServerBossBar bar = ACTIVE_BARS.remove(key);
        if (bar == null) {
            return;
        }
        bar.clearPlayers();
    }

    private static String craftKey(String id, UUID owner, long startTick) {
        return id + ":" + owner + ":" + startTick;
    }
}
