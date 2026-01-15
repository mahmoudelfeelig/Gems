package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.power.util.Targeting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public final class HunterNetShotAbility implements GemAbility {
    private static final String KEY_NET_UNTIL = "hunterNetShotUntil";

    @Override
    public Identifier id() {
        return PowerIds.HUNTER_NET_SHOT;
    }

    @Override
    public String name() {
        return "Net Shot";
    }

    @Override
    public String description() {
        return "Fire a net that slows and grounds enemies (disables flight/elytra).";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().netShotCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().hunter().netShotRangeBlocks();
        int slowTicks = GemsBalance.v().hunter().netShotSlowTicks();

        // Raycast to find target
        ServerPlayerEntity target = Targeting.raycastPlayer(player, range);
        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        if (!VoidImmunity.canBeTargeted(player, target)) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Apply slowness effect
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, 2, false, true));

        // Ground the target (cancel any upward velocity)
        target.setVelocity(target.getVelocity().x * 0.5, Math.min(target.getVelocity().y, -0.5), target.getVelocity().z * 0.5);
        target.velocityDirty = true;

        // Mark target as netted (disables elytra activation for duration)
        // The LivingEntityElytraNetMixin will block canGlide() to prevent elytra activation
        long endTime = world.getTime() + slowTicks;
        NbtCompound nbt = ((GemsPersistentDataHolder) target).gems$getPersistentData();
        nbt.putLong(KEY_NET_UNTIL, endTime);

        // Visual effect - net particles and radius ring showing net area
        double effectRadius = 2.0;
        AbilityFeedback.ring(world, target.getEntityPos().add(0.0D, 0.15D, 0.0D), effectRadius, ParticleTypes.CLOUD, 16);
        AbilityFeedback.burstAt(world, target.getEntityPos().add(0, 1, 0), ParticleTypes.CLOUD, 30, 0.8D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_FISHING_BOBBER_THROW, 1.0F, 0.6F);
        AbilityFeedback.sound(target, SoundEvents.BLOCK_COBWEB_PLACE, 1.0F, 1.0F);
        return true;
    }

    /**
     * Check if a player is currently netted (elytra disabled).
     */
    public static boolean isNetted(ServerPlayerEntity player) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = nbt.getLong(KEY_NET_UNTIL).orElse(0L);
        return until > 0 && player.getEntityWorld().getTime() < until;
    }
}
