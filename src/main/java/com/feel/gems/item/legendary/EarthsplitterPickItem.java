package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.text.Text;




public final class EarthsplitterPickItem extends PickaxeItem implements LegendaryItem {
    private static final TagKey<net.minecraft.block.Block> BLACKLIST = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(GemsMod.MOD_ID, "earthsplitter_blacklist")
    );
    private static final String KEY_MINING = "legendaryEarthsplitterMining";
    private static final String KEY_TUNNEL = "legendaryEarthsplitterTunnel";

    public EarthsplitterPickItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "earthsplitter_pick").toString();
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.postMine(stack, world, state, pos, miner);
        if (world.isClient) {
            return result;
        }
        if (!(miner instanceof ServerPlayerEntity player)) {
            return result;
        }
        if (isRecursiveBreak(stack)) {
            return result;
        }
        int radius = GemsBalance.v().legendary().earthsplitterRadiusBlocks();
        if (radius <= 0) {
            return result;
        }
        markRecursiveBreak(stack, true);
        try {
            if (isTunnelMode(stack)) {
                breakTunnel(stack, player, pos, radius);
            } else {
                breakCube(stack, player, pos, radius);
            }
        } finally {
            markRecursiveBreak(stack, false);
        }
        return result;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, net.minecraft.util.Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }
        boolean tunnel = !isTunnelMode(stack);
        setTunnelMode(stack, tunnel);
        if (user instanceof ServerPlayerEntity player) {
            String mode = tunnel ? "Tunnel 9x3x1" : "Cube 3x3x3";
            player.sendMessage(Text.literal("Earthsplitter mode: " + mode), true);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient) {
            return;
        }
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        RegistryEntry<Enchantment> silk = resolveSilkTouch(player);
        if (silk == null) {
            return;
        }
        int current = EnchantmentHelper.getLevel(silk, stack);
        if (current < 1) {
            EnchantmentHelper.apply(stack, builder -> builder.set(silk, 1));
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.gems.earthsplitter_pick.desc"));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    private static RegistryEntry<Enchantment> resolveSilkTouch(ServerPlayerEntity player) {
        var registry = player.getServerWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var entry = registry.getEntry(Enchantments.SILK_TOUCH);
        return entry.orElse(null);
    }

    private static boolean isRecursiveBreak(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        return data.getNbt().getBoolean(KEY_MINING);
    }

    private static void markRecursiveBreak(ItemStack stack, boolean value) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (value) {
                nbt.putBoolean(KEY_MINING, true);
            } else {
                nbt.remove(KEY_MINING);
            }
        });
    }

    private static boolean isTunnelMode(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        return data.getNbt().getBoolean(KEY_TUNNEL);
    }

    private static void setTunnelMode(ItemStack stack, boolean value) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (value) {
                nbt.putBoolean(KEY_TUNNEL, true);
            } else {
                nbt.remove(KEY_TUNNEL);
            }
        });
    }

    private static void breakCube(ItemStack stack, ServerPlayerEntity player, BlockPos pos, int radius) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        boolean stop = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (stack.isEmpty()) {
                        stop = true;
                        break;
                    }
                    mutable.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!canBreak(player, stack, mutable)) {
                        continue;
                    }
                    player.interactionManager.tryBreakBlock(mutable.toImmutable());
                }
                if (stop) {
                    break;
                }
            }
            if (stop) {
                break;
            }
        }
    }

    private static void breakTunnel(ItemStack stack, ServerPlayerEntity player, BlockPos pos, int radius) {
        int length = Math.max(1, GemsBalance.v().legendary().earthsplitterTunnelLengthBlocks());
        Direction facing = player.getHorizontalFacing();
        Direction right = facing.rotateYClockwise();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        boolean stop = false;
        int halfWidth = 0;
        for (int step = 0; step < length; step++) {
            int baseX = pos.getX() + facing.getOffsetX() * step;
            int baseY = pos.getY();
            int baseZ = pos.getZ() + facing.getOffsetZ() * step;
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (stack.isEmpty()) {
                        stop = true;
                        break;
                    }
                    if (step == 0 && dx == 0 && dy == 0) {
                        continue;
                    }
                    mutable.set(
                            baseX + right.getOffsetX() * dx,
                            baseY + dy,
                            baseZ + right.getOffsetZ() * dx
                    );
                    if (!canBreak(player, stack, mutable)) {
                        continue;
                    }
                    player.interactionManager.tryBreakBlock(mutable.toImmutable());
                }
                if (stop) {
                    break;
                }
            }
            if (stop) {
                break;
            }
        }
    }

    private static boolean canBreak(ServerPlayerEntity player, ItemStack stack, BlockPos.Mutable pos) {
        BlockState target = player.getWorld().getBlockState(pos);
        if (target.isAir() || target.isIn(BLACKLIST)) {
            return false;
        }
        if (target.getHardness(player.getWorld(), pos) < 0.0F) {
            return false;
        }
        if (target.isToolRequired() && !stack.isSuitableFor(target)) {
            return false;
        }
        return true;
    }
}
