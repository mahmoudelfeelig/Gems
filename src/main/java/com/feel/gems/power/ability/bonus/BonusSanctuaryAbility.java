package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class BonusSanctuaryAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SANCTUARY;
    }

    @Override
    public String name() {
        return "Sanctuary";
    }

    @Override
    public String description() {
        return "Create a protective zone that heals allies and pushes away enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 1000; // 50 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(6);
        
        world.spawnParticles(ParticleTypes.COMPOSTER, player.getX(), player.getY() + 0.5, player.getZ(), 100, 3, 0.5, 3, 0.1);
        
        world.getOtherEntities(player, area).forEach(e -> {
            if (e instanceof ServerPlayerEntity otherPlayer) {
                // Trusted players = allies, untrusted = enemies
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
                    otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0));
                } else {
                    // Push enemy players away
                    pushAway(player, e);
                }
            } else if (e instanceof LivingEntity) {
                // Push mobs away
                pushAway(player, e);
            }
        });
        
        // Also buff self
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0));
        return true;
    }

    private void pushAway(ServerPlayerEntity player, Entity e) {
        Vec3d direction = e.getEyePos().subtract(player.getEyePos()).normalize().multiply(1.5);
        e.setVelocity(direction.x, 0.5, direction.z);
        e.velocityDirty = true;
    }
}
