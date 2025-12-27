package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.player.PlayerEntity;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;




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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.success(stack);
        }
        if (hasMark(player)) {
            teleportToMark(player);
        } else {
            setMark(player);
        }
        int cooldown = GemsBalance.v().legendary().recallCooldownTicks();
        if (cooldown > 0) {
            player.getItemCooldownManager().set(this, cooldown);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.gems.recall_relic.desc"));
    }

    public static void ensureForceload(ServerPlayerEntity player) {
        if (!hasMark(player)) {
            return;
        }
        Mark mark = readMark(player);
        if (mark == null) {
            return;
        }
        ServerWorld world = player.getServer() == null ? null : player.getServer().getWorld(mark.dimension());
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
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() instanceof RecallRelicItem) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.getItem() instanceof RecallRelicItem) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof RecallRelicItem) {
                return true;
            }
        }
        return false;
    }

    private static void setMark(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putString(KEY_MARK_DIM, player.getServerWorld().getRegistryKey().getValue().toString());
        writeBlockPos(data, KEY_MARK_POS, player.getBlockPos());
        forceChunk(player.getServerWorld(), player.getBlockPos(), true);
        player.sendMessage(Text.literal("Recall mark set."), true);
    }

    private static void teleportToMark(ServerPlayerEntity player) {
        Mark mark = readMark(player);
        if (mark == null) {
            clearMark(player);
            return;
        }
        MinecraftServer server = player.getServer();
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
        player.teleport(targetWorld, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYaw(), player.getPitch());
        clearMark(player);
        player.sendMessage(Text.literal("Recalled to mark."), true);
    }

    private static boolean hasMark(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return data.contains(KEY_MARK_DIM, NbtElement.STRING_TYPE) && data.contains(KEY_MARK_POS, NbtElement.INT_ARRAY_TYPE);
    }

    private static Mark readMark(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!data.contains(KEY_MARK_DIM, NbtElement.STRING_TYPE)) {
            return null;
        }
        if (!data.contains(KEY_MARK_POS, NbtElement.INT_ARRAY_TYPE)) {
            return null;
        }
        Identifier dim = Identifier.tryParse(data.getString(KEY_MARK_DIM));
        BlockPos pos = readBlockPos(data, KEY_MARK_POS);
        if (dim == null || pos == null) {
            return null;
        }
        return new Mark(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dim), pos);
    }

    private static void clearMark(ServerPlayerEntity player) {
        Mark mark = readMark(player);
        if (mark != null && player.getServer() != null) {
            ServerWorld world = player.getServer().getWorld(mark.dimension());
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
        int[] values = nbt.getIntArray(key);
        if (values.length != 3) {
            return null;
        }
        return new BlockPos(values[0], values[1], values[2]);
    }

    private record Mark(net.minecraft.registry.RegistryKey<World> dimension, BlockPos pos) {
    }
}
