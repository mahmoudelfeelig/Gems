package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.hunter.HunterPreyMarkRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class HunterPounceAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HUNTER_POUNCE;
    }

    @Override
    public String name() {
        return "Pounce";
    }

    @Override
    public String description() {
        return "Leap toward a marked target from up to 20 blocks away.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().pounceCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().hunter().pounceRangeBlocks();
        float damage = GemsBalance.v().hunter().pounceDamage();

        // Find marked target
        ServerPlayerEntity target = HunterPreyMarkRuntime.getMarkedTarget(player);
        if (target == null || target.squaredDistanceTo(player) > range * range) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Calculate leap trajectory to land ON the target
        Vec3d playerPos = player.getEntityPos();
        Vec3d targetPos = target.getEntityPos();
        Vec3d toTarget = targetPos.subtract(playerPos);
        double horizontalDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        double verticalDist = toTarget.y;
        
        // Physics-based trajectory calculation
        // We want to arrive at the target in a set time, adjusting for distance
        double flightTime = Math.min(Math.max(horizontalDist / 15.0, 0.4), 1.2); // 0.4-1.2 seconds based on distance
        double gravity = 0.08; // Minecraft gravity per tick (roughly)
        double ticksToArrive = flightTime * 20.0;
        
        // Calculate required horizontal velocity to cover distance in flight time
        double horizontalVel = horizontalDist / ticksToArrive;
        
        // Calculate required initial vertical velocity to land at target height
        // Using: y = v0*t - 0.5*g*t^2, solve for v0: v0 = (y + 0.5*g*t^2) / t
        double verticalVel = (verticalDist + 0.5 * gravity * ticksToArrive * ticksToArrive) / ticksToArrive;
        verticalVel = Math.max(verticalVel, 0.3); // Ensure some upward arc
        verticalVel = Math.min(verticalVel, 2.0); // Cap to prevent crazy launches
        
        // Scale horizontal velocity to match
        horizontalVel = Math.min(horizontalVel * 1.1, 3.0); // Slightly overshoot to ensure arrival
        
        // Apply velocity in direction of target
        Vec3d horizontalDir = new Vec3d(toTarget.x, 0, toTarget.z).normalize();
        player.setVelocity(horizontalDir.x * horizontalVel, verticalVel, horizontalDir.z * horizontalVel);
        player.velocityDirty = true;
        AbilityFeedback.syncVelocity(player);

        // Deal damage on arrival (handled by landing check in runtime)
        HunterPounceRuntime.setPouncing(player, target.getUuid(), damage);

        AbilityFeedback.beam(world, player.getEntityPos().add(0, 1, 0), target.getEntityPos().add(0, 1, 0), ParticleTypes.CRIT, 15);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_CAT_HISS, 0.8F, 1.2F);
        return true;
    }
}
