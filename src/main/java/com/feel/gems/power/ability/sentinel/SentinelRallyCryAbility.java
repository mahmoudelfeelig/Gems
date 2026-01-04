package com.feel.gems.power.ability.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class SentinelRallyCryAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_RALLY_CRY;
    }

    @Override
    public String name() {
        return "Rally Cry";
    }

    @Override
    public String description() {
        return "Heal all nearby trusted allies and grant Resistance I for 8s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().rallyCryCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int radius = GemsBalance.v().sentinel().rallyCryRadiusBlocks();
        float healAmount = GemsBalance.v().sentinel().rallyCryHealHearts() * 2.0F; // Convert to half-hearts
        int resistanceTicks = GemsBalance.v().sentinel().rallyCryResistanceDurationTicks();

        Box box = player.getBoundingBox().expand(radius);
        int alliesHealed = 0;

        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof ServerPlayerEntity)) {
            ServerPlayerEntity ally = (ServerPlayerEntity) e;
            if (!GemTrust.isTrusted(player, ally)) continue;

            ally.heal(healAmount);
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistanceTicks, 0, false, true));
            AbilityFeedback.burstAt(world, ally.getEntityPos().add(0, 1, 0), ParticleTypes.HEART, 10, 0.5D);
            alliesHealed++;
        }

        // Also affect self
        player.heal(healAmount);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistanceTicks, 0, false, true));

        AbilityFeedback.ring(world, player.getEntityPos().add(0, 1, 0), radius, ParticleTypes.HEART, 20);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_GOAT_HORN_BREAK, 1.0F, 1.0F);
        return true;
    }
}
