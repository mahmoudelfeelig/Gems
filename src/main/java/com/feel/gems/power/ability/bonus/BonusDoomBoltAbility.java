package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class BonusDoomBoltAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_DOOM_BOLT;
    }

    @Override
    public String name() {
        return "Doom Bolt";
    }

    @Override
    public String description() {
        return "Mark an enemy for death, dealing massive damage after a delay.";
    }

    @Override
    public int cooldownTicks() {
        return 800; // 40 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(10);
        
        world.getOtherEntities(player, area, e -> e instanceof LivingEntity && !(e instanceof ServerPlayerEntity))
                .stream()
                .findFirst()
                .ifPresent(e -> {
                    if (e instanceof LivingEntity living) {
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0));
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
                        // Delayed damage via wither effect
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 3));
                        world.spawnParticles(ParticleTypes.WITCH, living.getX(), living.getY() + 1, living.getZ(), 30, 0.5, 1, 0.5, 0.1);
                    }
                });
        return true;
    }
}
