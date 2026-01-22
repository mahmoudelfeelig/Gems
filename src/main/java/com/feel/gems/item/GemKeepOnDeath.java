package com.feel.gems.item;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.rule.GameRules;




/**
 * Keeps gem items from dropping on death by stashing them into persistent player NBT and restoring on respawn.
 */
public final class GemKeepOnDeath {
    private static final String KEY_KEPT_GEMS = "keptGems";

    private GemKeepOnDeath() {
    }

    public static void stash(ServerPlayerEntity player) {
        if (player.getEntityWorld().getGameRules().getValue(GameRules.KEEP_INVENTORY)) {
            return;
        }
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        root.remove(KEY_KEPT_GEMS);

        GemPlayerState.initIfNeeded(player);
        GemId active = GemPlayerState.getActiveGem(player);

        List<ItemStack> kept = new ArrayList<>();
        boolean found = false;

        var mainStacks = player.getInventory().getMainStacks();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (!found && stack.getItem() instanceof GemItem gem && gem.gemId() == active) {
                kept.add(stack.copy());
                mainStacks.set(i, ItemStack.EMPTY);
                found = true;
            }
        }
        if (!found) {
            ItemStack offhand = player.getOffHandStack();
            if (offhand.getItem() instanceof GemItem gem && gem.gemId() == active) {
                kept.add(offhand.copy());
                player.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                found = true;
            }
        }
        if (!found) {
            net.minecraft.entity.EquipmentSlot[] armorSlots = {
                    net.minecraft.entity.EquipmentSlot.HEAD,
                    net.minecraft.entity.EquipmentSlot.CHEST,
                    net.minecraft.entity.EquipmentSlot.LEGS,
                    net.minecraft.entity.EquipmentSlot.FEET
            };
            for (var slot : armorSlots) {
                ItemStack stack = player.getEquippedStack(slot);
                if (stack.getItem() instanceof GemItem gem && gem.gemId() == active) {
                    kept.add(stack.copy());
                    player.equipStack(slot, ItemStack.EMPTY);
                    found = true;
                    break;
                }
            }
        }

        if (kept.isEmpty()) {
            return;
        }
        root.put(KEY_KEPT_GEMS, ItemStack.CODEC.listOf(), kept);
    }

    public static void restore(ServerPlayerEntity player) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        List<ItemStack> kept = root.get(KEY_KEPT_GEMS, ItemStack.CODEC.listOf()).orElse(List.of());
        if (kept.isEmpty()) {
            return;
        }
        root.remove(KEY_KEPT_GEMS);
        for (ItemStack stack : kept) {
            if (!stack.isEmpty()) {
                GemOwnership.tagOwned(stack, player);
                player.giveItemStack(stack);
            }
        }
    }
}
