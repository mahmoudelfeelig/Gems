package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Decoy Trap - Create decoy armor stands that distract enemies.
 */
public final class BonusDecoyTrapAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_DECOY_TRAP;
    }

    @Override
    public String name() {
        return "Decoy Trap";
    }

    @Override
    public String description() {
        return "Create decoy armor stands that distract enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        
        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2 / 3) * i;
            double x = player.getX() + Math.cos(angle) * 2;
            double z = player.getZ() + Math.sin(angle) * 2;
            
            ArmorStandEntity stand = EntityType.ARMOR_STAND.create(world, e -> { }, BlockPos.ofFloored(x, player.getY(), z), SpawnReason.TRIGGERED, false, false);
            if (stand != null) {
                stand.refreshPositionAndAngles(x, player.getY(), z, player.getYaw(), 0);
                stand.setInvisible(false);
                stand.setNoGravity(false);
                world.spawnEntity(stand);
                world.spawnParticles(ParticleTypes.CLOUD, x, player.getY() + 1, z, 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
        return true;
    }
}
