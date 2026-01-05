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

        // Calculate leap trajectory
        Vec3d toTarget = target.getEntityPos().subtract(player.getEntityPos());
        double distance = toTarget.length();
        Vec3d dir = toTarget.normalize();

        // Apply leap velocity
        double horizontalVel = Math.min(distance / 10.0, 2.5);
        double verticalVel = Math.min(distance / 15.0, 1.0);
        player.setVelocity(dir.x * horizontalVel, verticalVel, dir.z * horizontalVel);
        player.velocityDirty = true;
        AbilityFeedback.syncVelocity(player);

        // Deal damage on arrival (handled by landing check in runtime)
        HunterPounceRuntime.setPouncing(player, target.getUuid(), damage);

        AbilityFeedback.beam(world, player.getEntityPos().add(0, 1, 0), target.getEntityPos().add(0, 1, 0), ParticleTypes.CRIT, 15);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_CAT_HISS, 0.8F, 1.2F);
        return true;
    }
}
