package com.feel.gems.power.ability.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class SentinelInterventionAbility implements GemAbility {
    public static final String INTERVENTION_TARGET_KEY = "sentinel_intervention_target";
    public static final String INTERVENTION_ACTIVE_KEY = "sentinel_intervention_active";

    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_INTERVENTION;
    }

    @Override
    public String name() {
        return "Intervention";
    }

    @Override
    public String description() {
        return "Instantly teleport to a trusted ally and absorb the next hit they would take.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().interventionCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().sentinel().interventionRangeBlocks();

        // Find nearest trusted ally within range
        Box box = player.getBoundingBox().expand(range);
        ServerPlayerEntity target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof ServerPlayerEntity)) {
            ServerPlayerEntity ally = (ServerPlayerEntity) e;
            if (!GemTrust.isTrusted(player, ally)) continue;

            double dist = ally.squaredDistanceTo(player);
            if (dist < closestDist) {
                closestDist = dist;
                target = ally;
            }
        }

        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Teleport to ally
        GemsTeleport.teleport(player, world, target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch());
        player.velocityDirty = true;

        // Set up damage absorption for ally
        SentinelInterventionRuntime.setProtecting(player, target.getUuid());

        AbilityFeedback.burstAt(world, player.getEntityPos().add(0, 1, 0), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.burstAt(world, target.getEntityPos().add(0, 1, 0), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.2F);
        return true;
    }
}
