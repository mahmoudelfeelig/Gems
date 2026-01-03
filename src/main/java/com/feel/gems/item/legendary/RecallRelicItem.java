package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.player.PlayerEntity;
import java.util.function.Consumer;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
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
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import com.feel.gems.util.GemsTeleport;




public final class RecallRelicItem extends Item implements LegendaryItem {
    private static final String KEY_MARK_DIM = "legendaryRecallDim";
    private static final String KEY_MARK_POS = "legendaryRecallPos";

    public RecallRelicItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "recall_relic").toString();
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }
        ItemStack held = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(held)) {
            return ActionResult.SUCCESS;
        }
        if (hasMark(player)) {
            teleportToMark(player);
        } else {
            setMark(player);
        }
        int cooldown = GemsBalance.v().legendary().recallCooldownTicks();
        if (cooldown > 0) {
            player.getItemCooldownManager().set(held, cooldown);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("item.gems.recall_relic.desc"));
    }

    public static void ensureForceload(ServerPlayerEntity player) {
        if (!hasMark(player)) {
            return;
        }
        Mark mark = readMark(player);
        if (mark == null) {
            return;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        ServerWorld world = server == null ? null : server.getWorld(mark.dimension());
        if (world == null) {
            return;
        }
        forceChunk(world, mark.pos(), true);
    }

    public static void clearIfMissingItem(ServerPlayerEntity player) {
        if (!hasMark(player)) {
            return;
        }
        if (hasRelic(player)) {
            return;
        }
        clearMark(player);
    }

    private static boolean hasRelic(ServerPlayerEntity player) {
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (stack.getItem() instanceof RecallRelicItem) {
                return true;
            }
        }
        if (player.getOffHandStack().getItem() instanceof RecallRelicItem) {
            return true;
        }
        return player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).getItem() instanceof RecallRelicItem
                || player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).getItem() instanceof RecallRelicItem
                || player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS).getItem() instanceof RecallRelicItem
                || player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET).getItem() instanceof RecallRelicItem;
    }

    private static void setMark(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        ServerWorld world = player.getEntityWorld();
        data.putString(KEY_MARK_DIM, world.getRegistryKey().getValue().toString());
        writeBlockPos(data, KEY_MARK_POS, player.getBlockPos());
        forceChunk(world, player.getBlockPos(), true);
        player.sendMessage(Text.literal("Recall mark set."), true);
    }

    private static void teleportToMark(ServerPlayerEntity player) {
        Mark mark = readMark(player);
        if (mark == null) {
            clearMark(player);
            return;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        ServerWorld targetWorld = server.getWorld(mark.dimension());
        if (targetWorld == null) {
            player.sendMessage(Text.literal("Recall failed: target world missing."), true);
            clearMark(player);
            return;
        }
        BlockPos pos = mark.pos();
        GemsTeleport.teleport(player, targetWorld, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYaw(), player.getPitch());
        clearMark(player);
        player.sendMessage(Text.literal("Recalled to mark."), true);
    }

    private static boolean hasMark(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return data.getString(KEY_MARK_DIM).isPresent() && data.getIntArray(KEY_MARK_POS).isPresent();
    }

    private static Mark readMark(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String dimRaw = data.getString(KEY_MARK_DIM, "");
        if (dimRaw.isEmpty()) {
            return null;
        }
        Identifier dim = Identifier.tryParse(dimRaw);
        BlockPos pos = readBlockPos(data, KEY_MARK_POS);
        if (dim == null || pos == null) {
            return null;
        }
        return new Mark(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dim), pos);
    }

    private static void clearMark(ServerPlayerEntity player) {
        Mark mark = readMark(player);
        MinecraftServer server = player.getEntityWorld().getServer();
        if (mark != null && server != null) {
            ServerWorld world = server.getWorld(mark.dimension());
            if (world != null) {
                forceChunk(world, mark.pos(), false);
            }
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.remove(KEY_MARK_DIM);
        data.remove(KEY_MARK_POS);
    }

    private static void forceChunk(ServerWorld world, BlockPos pos, boolean forced) {
        ChunkPos chunk = new ChunkPos(pos);
        world.setChunkForced(chunk.x, chunk.z, forced);
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

    private record Mark(net.minecraft.registry.RegistryKey<World> dimension, BlockPos pos) {
    }
}
