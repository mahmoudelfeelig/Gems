package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.registry.tag.EntityTypeTags;

import java.util.List;

/**
 * Searing Light - Beam of holy light that burns undead extra.
 */
public final class BonusSearingLightAbility implements GemAbility {
    private static final double RANGE = 30.0;
    private static final float BASE_DAMAGE = 8.0f;
    private static final float UNDEAD_BONUS = 8.0f;
    private static final double BEAM_WIDTH = 2.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SEARING_LIGHT;
    }

    @Override
    public String name() {
        return "Searing Light";
    }

    @Override
    public String description() {
        return "Fire a beam of holy light. Deals 8 damage, double to undead.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(RANGE));

        // Get all entities along the beam path
        Box beamBox = new Box(start, end).expand(BEAM_WIDTH);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, beamBox,
                e -> e != player && e.isAlive());

        for (LivingEntity entity : entities) {
            // Check if entity is actually in beam path
            Vec3d toEntity = entity.getEntityPos().add(0, entity.getHeight() / 2, 0).subtract(start);
            Vec3d projected = direction.multiply(toEntity.dotProduct(direction));
            double distance = toEntity.subtract(projected).length();
            
            if (distance < BEAM_WIDTH && projected.length() < RANGE && projected.dotProduct(direction) > 0) {
                float damage = BASE_DAMAGE;
                if (entity.getType().isIn(EntityTypeTags.UNDEAD)) {
                    damage += UNDEAD_BONUS;
                    entity.setOnFireFor(5);
                }
                entity.damage(world, player.getDamageSources().indirectMagic(player, player), damage);
            }
        }

        // Beam particle effect
        for (int i = 0; i < 30; i++) {
            Vec3d particlePos = start.add(direction.multiply(i));
            world.spawnParticles(ParticleTypes.END_ROD, particlePos.x, particlePos.y, particlePos.z, 
                    2, 0.1, 0.1, 0.1, 0.01);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.5f);
        return true;
    }
}
