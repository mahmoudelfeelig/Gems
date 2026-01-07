package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Mirror Image - Create 3 illusions that confuse enemies.
 */
public final class BonusMirrorImageAbility implements GemAbility {
    private static final int ILLUSION_COUNT = 3;
    private static final int DURATION = 100; // 5 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_MIRROR_IMAGE;
    }

    @Override
    public String name() {
        return "Mirror Image";
    }

    @Override
    public String description() {
        return "Create 3 illusions of yourself that confuse enemies for 5s.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d playerPos = player.getEntityPos();

        for (int i = 0; i < ILLUSION_COUNT; i++) {
            double angle = (2 * Math.PI / ILLUSION_COUNT) * i;
            double offsetX = Math.cos(angle) * 2;
            double offsetZ = Math.sin(angle) * 2;
            Vec3d illusionPos = playerPos.add(offsetX, 0, offsetZ);

            // Create armor stand as illusion marker
            ArmorStandEntity illusion = new ArmorStandEntity(world, illusionPos.x, illusionPos.y, illusionPos.z);
            illusion.setInvisible(true);
            illusion.setNoGravity(true);
            illusion.setInvulnerable(true);
            illusion.setCustomName(player.getName());
            illusion.addCommandTag("gems_mirror_image");
            illusion.addCommandTag("owner:" + player.getUuidAsString());
            world.spawnEntity(illusion);

            // Spawn particles at illusion location
            world.spawnParticles(ParticleTypes.CLOUD, illusionPos.x, illusionPos.y + 1, illusionPos.z, 
                    15, 0.3, 0.8, 0.3, 0.02);
        }

        // Grant brief invisibility to player
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.INVISIBILITY, 40, 0, false, false, false));

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
