package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class AirUpdraftZoneAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AIR_UPDRAFT_ZONE;
    }

    @Override
    public String name() {
        return "Updraft Zone";
    }

    @Override
    public String description() {
        return "Creates an updraft that lifts allies and disrupts enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().air().updraftZoneCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        GemTrust.clearRuntimeCache(player.getUuid());
        int radius = GemsBalance.v().air().updraftZoneRadiusBlocks();
        double up = GemsBalance.v().air().updraftZoneUpVelocity();
        float damage = GemsBalance.v().air().updraftZoneEnemyDamage();
        double kb = GemsBalance.v().air().updraftZoneEnemyKnockback();
        if (radius <= 0 || up <= 0.0D) {
            return false;
        }

        var world = player.getServerWorld();
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (Entity entity : world.getOtherEntities(player, box, e -> e instanceof LivingEntity living && living.isAlive())) {
            if (entity instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                other.addVelocity(0.0D, up, 0.0D);
                other.velocityModified = true;
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, true, false, false));
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            // Ensure enemies don't keep any ally-only buff that might have been present.
            living.removeStatusEffect(StatusEffects.SLOW_FALLING);
            living.addVelocity(0.0D, up, 0.0D);
            living.velocityModified = true;
            if (damage > 0.0F) {
                float before = living.getHealth();
                boolean applied = living.damage(world.getDamageSources().generic(), damage);
                if (!applied && living.getHealth() >= before) {
                    // Spawn invulnerability or PvP rules can block the damage; enforce a minimal health drop so the zone still punishes enemies.
                    living.setHealth(Math.max(0.1F, before - damage));
                }
            }
            if (kb > 0.0D) {
                var away = living.getPos().subtract(player.getPos()).normalize();
                living.addVelocity(away.x * kb, 0.0D, away.z * kb);
            }
        }

        AbilityFeedback.ring(world, player.getPos().add(0.0D, 0.1D, 0.0D), radius, ParticleTypes.CLOUD, 36);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_BREEZE_WIND_BURST, 0.9F, 1.1F);
        return true;
    }
}
