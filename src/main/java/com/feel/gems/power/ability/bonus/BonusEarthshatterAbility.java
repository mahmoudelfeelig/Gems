package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class BonusEarthshatterAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_EARTHSHATTER;
    }

    @Override
    public String name() {
        return "Earthshatter";
    }

    @Override
    public String description() {
        return "Slam the ground, damaging and launching nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 500; // 25 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(6);
        
        world.spawnParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 20, 3, 0.5, 3, 0.1);
        
        world.getOtherEntities(player, area, e -> e instanceof LivingEntity)
                .forEach(e -> {
                    if (e instanceof LivingEntity living) {
                        living.damage(world, player.getDamageSources().playerAttack(player), 8.0f);
                        living.setVelocity(living.getVelocity().add(0, 0.8, 0));
                        living.velocityDirty = true;
                    }
                });
        return true;
    }
}
