package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
 * Plague Cloud - Create a lingering cloud that poisons and weakens.
 */
public final class BonusPlagueCloudAbility implements GemAbility {
    private static final double RANGE = 30.0;
    private static final int CLOUD_DURATION = 200; // 10 seconds
    private static final float CLOUD_RADIUS = 4.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_PLAGUE_CLOUD;
    }

    @Override
    public String name() {
        return "Plague Cloud";
    }

    @Override
    public String description() {
        return "Create a lingering cloud that poisons and weakens enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(RANGE));
        
        HitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        Vec3d targetPos = hit.getPos();

        // Create area effect cloud
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, targetPos.x, targetPos.y, targetPos.z);
        cloud.setOwner(player);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setDuration(CLOUD_DURATION);
        cloud.setRadiusGrowth(-0.01f);
        cloud.setWaitTime(0);
        cloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1));
        cloud.addEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
        
        world.spawnEntity(cloud);

        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.PLAYERS, 1.0f, 0.5f);
        return true;
    }
}
