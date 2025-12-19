package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ItemLockAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.ITEM_LOCK;
    }

    @Override
    public String name() {
        return "Item Lock";
    }

    @Override
    public String description() {
        return "Locks an enemy from using their held item for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return 30 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, 20.0D);
        if (!(target instanceof ServerPlayerEntity other)) {
            player.sendMessage(Text.literal("No player target."), true);
            return true;
        }
        if (GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }

        ItemLock.lock(other, other.getMainHandStack(), 6 * 20);
        player.sendMessage(Text.literal("Item Lock applied."), true);
        return true;
    }
}

