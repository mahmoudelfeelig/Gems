package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class FluxCharge {
    private static final String KEY_CHARGE = "fluxCharge";
    private static final String KEY_AT_100 = "fluxChargeAt100";
    private static final String KEY_LAST_OVERCHARGE_TICK = "fluxLastOverchargeTick";

    private FluxCharge() {
    }

    public static int get(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        if (!nbt.contains(KEY_CHARGE, NbtElement.INT_TYPE)) {
            return 0;
        }
        return clamp(nbt.getInt(KEY_CHARGE), 0, 200);
    }

    public static void set(ServerPlayerEntity player, int charge) {
        persistent(player).putInt(KEY_CHARGE, clamp(charge, 0, 200));
    }

    public static boolean tryConsumeChargeItem(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.FLUX_CHARGE_STORAGE)) {
            player.sendMessage(Text.literal("Charge Storage is not active."), true);
            return false;
        }

        int charge = get(player);
        if (charge >= 100) {
            player.sendMessage(Text.literal("Flux charge is already at 100%."), true);
            return false;
        }

        ItemStack fuel = player.getOffHandStack();
        int add = chargeValue(fuel);
        if (add <= 0) {
            player.sendMessage(Text.literal("Hold a charge item in your offhand."), true);
            return false;
        }

        fuel.decrement(1);
        int next = Math.min(100, charge + add);
        set(player, next);
        if (charge < 100 && next >= 100) {
            persistent(player).putLong(KEY_AT_100, player.getServerWorld().getTime());
        }
        player.sendMessage(Text.literal("Flux charge: " + next + "%"), true);
        return true;
    }

    public static void tickOvercharge(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.FLUX_OVERCHARGE_RAMP)) {
            return;
        }
        int charge = get(player);
        if (charge < 100 || charge >= 200) {
            return;
        }

        NbtCompound nbt = persistent(player);
        long now = player.getServerWorld().getTime();
        long at100 = nbt.getLong(KEY_AT_100);
        if (at100 <= 0) {
            nbt.putLong(KEY_AT_100, now);
            return;
        }
        if (now - at100 < GemsBalance.v().flux().overchargeDelayTicks()) {
            return;
        }

        long last = nbt.getLong(KEY_LAST_OVERCHARGE_TICK);
        if (now - last < 20L) {
            return;
        }
        nbt.putLong(KEY_LAST_OVERCHARGE_TICK, now);

        int next = Math.min(200, charge + GemsBalance.v().flux().overchargePerSecond());
        set(player, next);
        player.damage(player.getDamageSources().magic(), GemsBalance.v().flux().overchargeSelfDamagePerSecond());
        com.blissmc.gems.net.GemExtraStateSync.send(player);
    }

    public static void clearIfBelow100(ServerPlayerEntity player) {
        int charge = get(player);
        if (charge < 100) {
            NbtCompound nbt = persistent(player);
            if (nbt.contains(KEY_AT_100)) {
                nbt.remove(KEY_AT_100);
            }
            if (nbt.contains(KEY_LAST_OVERCHARGE_TICK)) {
                nbt.remove(KEY_LAST_OVERCHARGE_TICK);
            }
        }
    }

    private static int chargeValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (stack.isOf(Items.DIAMOND_BLOCK)) {
            return GemsBalance.v().flux().chargeDiamondBlock();
        }
        if (stack.isOf(Items.GOLD_BLOCK)) {
            return GemsBalance.v().flux().chargeGoldBlock();
        }
        if (stack.isOf(Items.COPPER_BLOCK)) {
            return GemsBalance.v().flux().chargeCopperBlock();
        }

        if (!EnchantmentHelper.hasEnchantments(stack)) {
            return 0;
        }
        if (stack.getItem() instanceof ToolItem tool && tool.getMaterial() == ToolMaterials.DIAMOND) {
            return GemsBalance.v().flux().chargeEnchantedDiamondItem();
        }
        if (stack.getItem() instanceof ArmorItem armor && armor.getMaterial() == ArmorMaterials.DIAMOND) {
            return GemsBalance.v().flux().chargeEnchantedDiamondItem();
        }
        return 0;
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
