package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import com.blissmc.gems.state.GemsPersistentDataHolder;
import com.blissmc.gems.trust.GemTrust;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class StaticBurstAbility implements GemAbility {
    private static final String KEY_STORED_DAMAGE = "fluxStoredDamage";
    private static final String KEY_STORED_AT = "fluxStoredDamageAt";

    @Override
    public Identifier id() {
        return PowerIds.STATIC_BURST;
    }

    @Override
    public String name() {
        return "Static Burst";
    }

    @Override
    public String description() {
        return "Releases damage you've stored recently as an area burst.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().flux().staticBurstCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        float stored = nbt.getFloat(KEY_STORED_DAMAGE);
        if (stored <= 0.0F) {
            player.sendMessage(Text.literal("No stored damage."), true);
            return true;
        }

        ServerWorld world = player.getServerWorld();
        int hits = 0;
        float damage = Math.min(GemsBalance.v().flux().staticBurstMaxDamage(), stored);
        int radius = GemsBalance.v().flux().staticBurstRadiusBlocks();
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= radius * (double) radius)) {
            if (GemTrust.isTrusted(player, other)) {
                continue;
            }
            other.damage(player.getDamageSources().magic(), damage);
            hits++;
        }
        nbt.putFloat(KEY_STORED_DAMAGE, 0.0F);
        nbt.putLong(KEY_STORED_AT, world.getTime());
        player.sendMessage(Text.literal("Static Burst hit " + hits + " players."), true);
        return true;
    }

    public static void onDamaged(ServerPlayerEntity player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.FLUX_CHARGE_STORAGE)) {
            return;
        }
        NbtCompound nbt = persistent(player);
        long now = player.getServerWorld().getTime();
        long storedAt = nbt.getLong(KEY_STORED_AT);
        if (storedAt <= 0 || now - storedAt > GemsBalance.v().flux().staticBurstStoreWindowTicks()) {
            nbt.putFloat(KEY_STORED_DAMAGE, 0.0F);
            nbt.putLong(KEY_STORED_AT, now);
        }
        nbt.putFloat(KEY_STORED_DAMAGE, nbt.getFloat(KEY_STORED_DAMAGE) + amount);
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
