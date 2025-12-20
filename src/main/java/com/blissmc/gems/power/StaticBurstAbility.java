package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
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
            return false;
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
            AbilityFeedback.burstAt(world, other.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ELECTRIC_SPARK, 14, 0.35D);
            hits++;
        }
        nbt.putFloat(KEY_STORED_DAMAGE, 0.0F);
        nbt.putLong(KEY_STORED_AT, GemsTime.now(world));
        AbilityFeedback.sound(player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5F, 1.4F);
        AbilityFeedback.burst(player, ParticleTypes.ELECTRIC_SPARK, 24, 0.5D);
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
        long now = GemsTime.now(player);
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
