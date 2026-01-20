package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.text.Text;




public final class EarthsplitterPickItem extends Item implements LegendaryItem {
    private static final TagKey<net.minecraft.block.Block> BLACKLIST = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(GemsMod.MOD_ID, "earthsplitter_blacklist")
    );
    private static final String KEY_MINING = "legendaryEarthsplitterMining";
    private static final String KEY_TUNNEL = "legendaryEarthsplitterTunnel";

    public EarthsplitterPickItem(ToolMaterial material, Settings settings) {
        super(settings.pickaxe(material, 1.0F, -2.8F).enchantable(15));
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "earthsplitter_pick").toString();
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.postMine(stack, world, state, pos, miner);
        if (world.isClient()) {
            return result;
        }
        if (!(miner instanceof ServerPlayerEntity player)) {
            return result;
        }
        if (isRecursiveBreak(stack)) {
            return result;
        }
        int horizontalRadius = 4;
        int verticalRadius = 1;
        markRecursiveBreak(stack, true);
        try {
            if (isTunnelMode(stack)) {
                breakTunnel(stack, player, pos, verticalRadius);
            } else {
                breakCube(stack, player, pos, horizontalRadius, verticalRadius);
            }
        } finally {
            markRecursiveBreak(stack, false);
        }
        return result;
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, net.minecraft.util.Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        boolean tunnel = !isTunnelMode(stack);
        setTunnelMode(stack, tunnel);
        if (user instanceof ServerPlayerEntity player) {
                int horizontalRadius = 4;
                int verticalRadius = 1;
                int cubeSize = horizontalRadius * 2 + 1;
                int height = verticalRadius * 2 + 1;
                int tunnelLength = 20;
                int tunnelWidth = 2;
            String mode = tunnel
                    ? "Tunnel " + tunnelLength + "x" + tunnelWidth + "x" + height
                    : "Volume " + cubeSize + "x" + cubeSize + "x" + height;
            player.sendMessage(Text.translatable("gems.item.earthsplitter_pick.mode", mode), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
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
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        GemsTooltipFormat.appendDescription(tooltip, Text.translatable("item.gems.earthsplitter_pick.desc"));
    }

    private static RegistryEntry<Enchantment> resolveSilkTouch(ServerPlayerEntity player) {
        return player.getEntityWorld().getRegistryManager().getOptionalEntry(Enchantments.SILK_TOUCH).orElse(null);
    }

    private static boolean isRecursiveBreak(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        return data.copyNbt().getBoolean(KEY_MINING, false);
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
        return data.copyNbt().getBoolean(KEY_TUNNEL, false);
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

    private static void breakCube(ItemStack stack, ServerPlayerEntity player, BlockPos pos, int horizontalRadius, int verticalRadius) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        boolean stop = false;
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
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
                    tryBreakBlockNoDurability(player, stack, mutable.toImmutable());
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

    private static void breakTunnel(ItemStack stack, ServerPlayerEntity player, BlockPos pos, int verticalRadius) {
        int length = 20;
        Direction facing = player.getHorizontalFacing();
        Direction right = facing.rotateYClockwise();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        boolean stop = false;
        int width = 2;
        int widthMin = -(width - 1);
        int widthMax = 0;
        for (int step = 0; step < length; step++) {
            int baseX = pos.getX() + facing.getOffsetX() * step;
            int baseY = pos.getY();
            int baseZ = pos.getZ() + facing.getOffsetZ() * step;
            for (int dx = widthMin; dx <= widthMax; dx++) {
                for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
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
                    tryBreakBlockNoDurability(player, stack, mutable.toImmutable());
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
        BlockState target = player.getEntityWorld().getBlockState(pos);
        if (target.isAir() || target.isIn(BLACKLIST)) {
            return false;
        }
        if (target.getHardness(player.getEntityWorld(), pos) < 0.0F) {
            return false;
        }
        if (target.isToolRequired() && !stack.isSuitableFor(target)) {
            return false;
        }
        return true;
    }

    private static void tryBreakBlockNoDurability(ServerPlayerEntity player, ItemStack stack, BlockPos pos) {
        int desiredDamage = stack.isDamageable() ? stack.getDamage() : 0;
        int maxDamage = stack.getMaxDamage();
        if (stack.isDamageable() && maxDamage > 0) {
            int safeDamage = desiredDamage;
            if (maxDamage > 1 && desiredDamage >= maxDamage - 1) {
                safeDamage = Math.max(0, maxDamage - 2);
            }
            if (safeDamage != desiredDamage) {
                stack.setDamage(safeDamage);
            }
        }

        player.interactionManager.tryBreakBlock(pos);

        if (!stack.isEmpty() && stack.isDamageable()) {
            stack.setDamage(desiredDamage);
        }
    }
}
