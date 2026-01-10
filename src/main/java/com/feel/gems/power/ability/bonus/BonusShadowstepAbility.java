package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class BonusShadowstepAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SHADOWSTEP;
    }

    @Override
    public String name() {
        return "Shadowstep";
    }

    @Override
    public String description() {
        return "Teleport a short distance in the direction you're looking.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(15));
        
        HitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        
        Vec3d target = hit.getType() == HitResult.Type.MISS ? end : hit.getPos().subtract(direction.multiply(0.5));
        
        world.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1, 0.5, 0.1);
        GemsTeleport.teleport(player, world, target.x, target.y, target.z, player.getYaw(), player.getPitch());
        world.spawnParticles(ParticleTypes.PORTAL, target.x, target.y + 1, target.z, 30, 0.5, 1, 0.5, 0.1);
        
        return true;
    }
}
