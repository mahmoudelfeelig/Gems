package com.feel.gems.power.ability.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class SentinelLockdownAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_LOCKDOWN;
    }

    @Override
    public String name() {
        return "Lockdown";
    }

    @Override
    public String description() {
        return "Create a zone where enemies cannot use movement abilities for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().lockdownCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int radius = GemsBalance.v().sentinel().lockdownRadiusBlocks();
        int durationTicks = GemsBalance.v().sentinel().lockdownDurationTicks();

        Vec3d center = player.getEntityPos();
        long endTime = world.getTime() + durationTicks;

        // Create the lockdown zone
        SentinelLockdownRuntime.createZone(player.getUuid(), center, radius, endTime, world.getRegistryKey().getValue().toString());

        // Apply effect to enemies currently in the zone
        Box box = player.getBoundingBox().expand(radius);
        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof ServerPlayerEntity)) {
            ServerPlayerEntity target = (ServerPlayerEntity) e;
            if (GemTrust.isTrusted(player, target)) continue;
            if (!VoidImmunity.canBeTargeted(player, target)) continue;

            AbilityFeedback.burstAt(world, target.getEntityPos().add(0, 1, 0), ParticleTypes.ENCHANTED_HIT, 15, 0.3D);
        }

        AbilityFeedback.ring(world, center.add(0, 0.1, 0), radius, ParticleTypes.ENCHANTED_HIT, 32);
        AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0F, 0.5F);
        return true;
    }
}
