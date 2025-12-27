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
import net.minecraft.entity.EquipmentSlot;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.text.Text;




public final class EarthsplitterPickItem extends PickaxeItem implements LegendaryItem {
    private static final TagKey<net.minecraft.block.Block> BLACKLIST = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(GemsMod.MOD_ID, "earthsplitter_blacklist")
    );
    private static final String KEY_MINING = "legendaryEarthsplitterMining";

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
        if (!player.isSneaking()) {
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
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        boolean stop = false;
        try {
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
                        BlockState target = world.getBlockState(mutable);
                        if (target.isAir() || target.isIn(BLACKLIST)) {
                            continue;
                        }
                        if (target.getHardness(world, mutable) < 0.0F) {
                            continue;
                        }
                        if (target.isToolRequired() && !stack.isSuitableFor(target)) {
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
        } finally {
            markRecursiveBreak(stack, false);
        }
        return result;
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
}
