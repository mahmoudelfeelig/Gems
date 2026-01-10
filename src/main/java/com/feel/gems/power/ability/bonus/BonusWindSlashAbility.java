package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class BonusWindSlashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_WIND_SLASH;
    }

    @Override
    public String name() {
        return "Wind Slash";
    }

    @Override
    public String description() {
        return "Send a cutting wind projectile in the direction you're facing.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        
        for (int i = 1; i <= 20; i++) {
            Vec3d pos = start.add(direction.multiply(i));
            world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 5, 0.2, 0.2, 0.2, 0.02);
            
            Box hitBox = new Box(pos.subtract(1, 1, 1), pos.add(1, 1, 1));
            world.getOtherEntities(player, hitBox, e -> e instanceof LivingEntity)
                    .forEach(e -> {
                        if (e instanceof LivingEntity living) {
                            living.damage(world, player.getDamageSources().playerAttack(player), 6.0f);
                            living.setVelocity(direction.multiply(0.5));
                            living.velocityDirty = true;
                        }
                    });
        }
        return true;
    }
}
