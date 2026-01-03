package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.legendary.LegendaryPlayerTracker;
import com.feel.gems.net.TrackerCompassScreenPayload;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;




public final class TrackerCompassItem extends CompassItem implements LegendaryItem {
    private static final String KEY_TARGET = "legendaryTrackTarget";
    private static final String KEY_TARGET_NAME = "legendaryTrackTargetName";
    private static final String KEY_TARGET_DIM = "legendaryTrackDim";
    private static final String KEY_TARGET_POS = "legendaryTrackPos";
    private static final String KEY_TARGET_RESPAWN_DIM = "legendaryTrackRespawnDim";
    private static final String KEY_TARGET_RESPAWN_POS = "legendaryTrackRespawnPos";
    private static final String KEY_TARGET_LAST_SEEN = "legendaryTrackLastSeen";
    private static final String KEY_LAST_UPDATE = "legendaryTrackLastUpdate";

    public TrackerCompassItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "tracker_compass").toString();
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            openSelectionScreen(player);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        UUID target = targetUuid(stack);
        if (target == null) {
            return;
        }
        long now = GemsTime.now(player);
        int refresh = Math.max(1, GemsBalance.v().legendary().trackerRefreshTicks());
        long last = lastUpdate(stack);
        if (now - last < refresh) {
            return;
        }
        updateTrackingData(stack, player.getEntityWorld().getServer(), target, now);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("item.gems.tracker_compass.desc"));
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            tooltip.accept(Text.literal("Target: none"));
            return;
        }
        NbtCompound nbt = data.copyNbt();
        if (nbt.getString(KEY_TARGET_NAME).isEmpty()) {
            tooltip.accept(Text.literal("Target: none"));
            return;
        }
        String name = nbt.getString(KEY_TARGET_NAME, "");
        tooltip.accept(Text.literal("Target: " + name));
        BlockPos pos = readBlockPos(nbt, KEY_TARGET_POS);
        if (pos != null) {
            String dim = nbt.getString(KEY_TARGET_DIM, "");
            tooltip.accept(Text.literal("Last seen: " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " (" + dim + ")"));
        }
        BlockPos respawn = readBlockPos(nbt, KEY_TARGET_RESPAWN_POS);
        if (respawn != null) {
            String dim = nbt.getString(KEY_TARGET_RESPAWN_DIM, "");
            tooltip.accept(Text.literal("Respawn: " + respawn.getX() + " " + respawn.getY() + " " + respawn.getZ() + " (" + dim + ")"));
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    public static void setTarget(ServerPlayerEntity player, String name) {
        ItemStack stack = findHeldCompass(player);
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("Hold the Tracker Compass to set a target."), true);
            return;
        }
        if (name == null || name.isBlank()) {
            clearTarget(stack);
            player.sendMessage(Text.literal("Tracker Compass target cleared."), true);
            return;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        LegendaryPlayerTracker.Snapshot snapshot = null;
        for (LegendaryPlayerTracker.Snapshot entry : LegendaryPlayerTracker.knownSnapshots(server)) {
            if (entry.name() != null && entry.name().equalsIgnoreCase(name)) {
                snapshot = entry;
                break;
            }
        }
        if (snapshot == null) {
            player.sendMessage(Text.literal("Unknown player: " + name), true);
            return;
        }
        applyTarget(player, stack, snapshot.uuid(), snapshot.name());
    }

    public static void setTarget(ServerPlayerEntity player, UUID uuid) {
        if (uuid == null) {
            clearTarget(player);
            return;
        }
        ItemStack stack = findHeldCompass(player);
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("Hold the Tracker Compass to set a target."), true);
            return;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        LegendaryPlayerTracker.Snapshot snapshot = LegendaryPlayerTracker.snapshot(server, uuid);
        if (snapshot == null) {
            player.sendMessage(Text.literal("Unknown player to track."), true);
            return;
        }
        applyTarget(player, stack, uuid, snapshot.name());
    }

    public static void clearTarget(ServerPlayerEntity player) {
        ItemStack stack = findHeldCompass(player);
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("Hold the Tracker Compass to clear a target."), true);
            return;
        }
        clearTarget(stack);
        player.sendMessage(Text.literal("Tracker Compass target cleared."), true);
    }

    public static void openSelectionScreen(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        List<LegendaryPlayerTracker.Snapshot> profiles = new ArrayList<>(LegendaryPlayerTracker.knownSnapshots(server));
        profiles.sort(java.util.Comparator.comparing(p -> p.name().toLowerCase(java.util.Locale.ROOT)));
        List<TrackerCompassScreenPayload.Entry> entries = new ArrayList<>(profiles.size());
        for (LegendaryPlayerTracker.Snapshot profile : profiles) {
            boolean online = server.getPlayerManager().getPlayer(profile.uuid()) != null;
            entries.add(new TrackerCompassScreenPayload.Entry(profile.uuid(), profile.name(), online));
        }
        ServerPlayNetworking.send(player, new TrackerCompassScreenPayload(entries));
    }

    private static void applyTarget(ServerPlayerEntity player, ItemStack stack, UUID uuid, String display) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.put(KEY_TARGET, GemsNbt.fromUuid(uuid));
            nbt.putString(KEY_TARGET_NAME, display);
            nbt.putLong(KEY_LAST_UPDATE, 0L);
        });
        updateTrackingData(stack, server, uuid, GemsTime.now(server));
        player.sendMessage(Text.literal("Tracker Compass now tracking " + display + "."), true);
        sendTargetInfo(player, server, uuid);
    }

    private static void sendTargetInfo(ServerPlayerEntity player, MinecraftServer server, UUID target) {
        LegendaryPlayerTracker.Snapshot snapshot = LegendaryPlayerTracker.snapshot(server, target);
        if (snapshot == null) {
            return;
        }
        BlockPos pos = snapshot.pos();
        BlockPos respawn = snapshot.respawnPos();
        player.sendMessage(Text.literal("Current: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + " (" + snapshot.dimension() + ")"), true);
        player.sendMessage(Text.literal("Respawn: " + respawn.getX() + " " + respawn.getY() + " " + respawn.getZ()
                + " (" + snapshot.respawnDimension() + ")"), true);
    }

    private static void updateTrackingData(ItemStack stack, MinecraftServer server, UUID target, long now) {
        LegendaryPlayerTracker.Snapshot snapshot = LegendaryPlayerTracker.snapshot(server, target);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putLong(KEY_LAST_UPDATE, now);
            if (snapshot == null) {
                nbt.putString(KEY_TARGET_DIM, "");
                nbt.remove(KEY_TARGET_POS);
                nbt.putString(KEY_TARGET_RESPAWN_DIM, "");
                nbt.remove(KEY_TARGET_RESPAWN_POS);
                nbt.remove(KEY_TARGET_LAST_SEEN);
                return;
            }
            nbt.putString(KEY_TARGET_NAME, snapshot.name());
            nbt.putString(KEY_TARGET_DIM, snapshot.dimension().toString());
            writeBlockPos(nbt, KEY_TARGET_POS, snapshot.pos());
            nbt.putString(KEY_TARGET_RESPAWN_DIM, snapshot.respawnDimension().toString());
            writeBlockPos(nbt, KEY_TARGET_RESPAWN_POS, snapshot.respawnPos());
            nbt.putLong(KEY_TARGET_LAST_SEEN, snapshot.lastSeenTick());
        });

        if (snapshot != null) {
            GlobalPos global = GlobalPos.create(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, snapshot.dimension()), snapshot.pos());
            stack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.of(global), true));
        } else {
            stack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.empty(), false));
        }
    }

    private static UUID targetUuid(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        NbtCompound nbt = data.copyNbt();
        if (!nbt.contains(KEY_TARGET)) {
            return null;
        }
        return GemsNbt.toUuid(nbt.get(KEY_TARGET));
    }

    private static long lastUpdate(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            return 0L;
        }
        return data.copyNbt().getLong(KEY_LAST_UPDATE, 0L);
    }

    private static void clearTarget(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.remove(KEY_TARGET);
            nbt.remove(KEY_TARGET_NAME);
            nbt.remove(KEY_TARGET_DIM);
            nbt.remove(KEY_TARGET_POS);
            nbt.remove(KEY_TARGET_RESPAWN_DIM);
            nbt.remove(KEY_TARGET_RESPAWN_POS);
            nbt.remove(KEY_TARGET_LAST_SEEN);
            nbt.remove(KEY_LAST_UPDATE);
        });
        stack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.empty(), false));
    }

    private static ItemStack findHeldCompass(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof TrackerCompassItem)) {
            stack = player.getOffHandStack();
        }
        if (!(stack.getItem() instanceof TrackerCompassItem)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    private static void writeBlockPos(NbtCompound nbt, String key, BlockPos pos) {
        nbt.put(key, new net.minecraft.nbt.NbtIntArray(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
    }

    private static BlockPos readBlockPos(NbtCompound nbt, String key) {
        int[] values = nbt.getIntArray(key).orElse(null);
        if (values == null) {
            return null;
        }
        if (values.length != 3) {
            return null;
        }
        return new BlockPos(values[0], values[1], values[2]);
    }
}
