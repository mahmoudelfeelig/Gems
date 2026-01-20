package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.util.Targeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Vampiric Touch - Drain health from touched enemy over time.
 */
public final class BonusVampiricTouchAbility implements GemAbility {
    private static final double RANGE = 4.0;
    private static final float DAMAGE = 8.0f;
    private static final float HEAL_RATIO = 0.75f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_VAMPIRIC_TOUCH;
    }

    @Override
    public String name() {
        return "Vampiric Touch";
    }

    @Override
    public String description() {
        return "Touch an enemy to drain 8 HP, healing yourself for 75% of damage dealt.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        LivingEntity target = Targeting.raycastLiving(player, RANGE);

        if (target == null) {
            return false;
        }

        float beforeHealth = target.getHealth();
        target.damage(world, player.getDamageSources().indirectMagic(player, player), DAMAGE);
        float dealt = Math.max(0.0F, beforeHealth - target.getHealth());

        float healAmount = dealt * HEAL_RATIO;
        if (healAmount > 0.0F) {
            player.heal(healAmount);
        }

        // Particle trail from target to player
        Vec3d targetPos = target.getEntityPos().add(0, target.getHeight() / 2, 0);
        Vec3d playerPos = player.getEntityPos().add(0, player.getHeight() / 2, 0);
        Vec3d diff = playerPos.subtract(targetPos);
        for (int i = 0; i < 10; i++) {
            Vec3d particlePos = targetPos.add(diff.multiply(i / 10.0));
            world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, 
                    particlePos.x, particlePos.y, particlePos.z, 1, 0.1, 0.1, 0.1, 0);
        }

        world.spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.5, player.getZ(), 
                (int) (healAmount / 2), 0.3, 0.3, 0.3, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 0.7f);
        return true;
    }
}
