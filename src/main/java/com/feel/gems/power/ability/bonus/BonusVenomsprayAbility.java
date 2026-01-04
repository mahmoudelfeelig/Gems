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

public final class BonusVenomsprayAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_VENOMSPRAY;
    }

    @Override
    public String name() {
        return "Venomspray";
    }

    @Override
    public String description() {
        return "Spray poison in a cone, poisoning all enemies hit.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(8);
        
        world.spawnParticles(ParticleTypes.ITEM_SLIME, player.getX(), player.getY() + 1, player.getZ(), 40, 3, 1, 3, 0.2);
        
        world.getOtherEntities(player, area, e -> e instanceof LivingEntity)
                .forEach(e -> {
                    if (e instanceof LivingEntity living && !(e instanceof ServerPlayerEntity)) {
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 300, 2));
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 1));
                    }
                });
        return true;
    }
}
