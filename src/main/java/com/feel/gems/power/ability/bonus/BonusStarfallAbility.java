package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Starfall - Rain down small meteors in an area.
 */
public final class BonusStarfallAbility implements GemAbility {
    private static final int METEOR_COUNT = 8;
    private static final double RANGE = 30.0;
    private static final double SPREAD = 5.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_STARFALL;
    }

    @Override
    public String name() {
        return "Starfall";
    }

    @Override
    public String description() {
        return "Rain 8 meteors at your crosshair location.";
    }

    @Override
    public int cooldownTicks() {
        return 500; // 25 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(RANGE));
        
        HitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        Vec3d targetCenter = hit.getPos();

        for (int i = 0; i < METEOR_COUNT; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * SPREAD * 2;
            double offsetZ = (world.random.nextDouble() - 0.5) * SPREAD * 2;
            Vec3d spawnPos = targetCenter.add(offsetX, 15 + world.random.nextDouble() * 5, offsetZ);
            
            SmallFireballEntity fireball = new SmallFireballEntity(world, player, new Vec3d(0, -1, 0));
            fireball.setPosition(spawnPos);
            world.spawnEntity(fireball);
            
            world.spawnParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y, spawnPos.z, 
                    5, 0.2, 0.2, 0.2, 0.05);
        }

        world.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
