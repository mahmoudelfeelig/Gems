package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class BonusArcaneMissilesAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ARCANE_MISSILES;
    }

    @Override
    public String name() {
        return "Arcane Missiles";
    }

    @Override
    public String description() {
        return "Fire a barrage of magical missiles at your target.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d direction = player.getRotationVector();
        
        for (int i = 0; i < 5; i++) {
            Vec3d offset = new Vec3d(
                    direction.x + (world.random.nextDouble() - 0.5) * 0.2,
                    direction.y + (world.random.nextDouble() - 0.5) * 0.2,
                    direction.z + (world.random.nextDouble() - 0.5) * 0.2);
            SmallFireballEntity fireball = new SmallFireballEntity(world, player, offset);
            fireball.setPosition(player.getEyePos().add(direction.multiply(0.5)));
            world.spawnEntity(fireball);
        }
        return true;
    }
}
