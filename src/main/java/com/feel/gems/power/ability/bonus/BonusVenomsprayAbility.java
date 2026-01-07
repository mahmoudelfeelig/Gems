package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class BonusVenomsprayAbility implements GemAbility {
    private static final float CONE_ANGLE = 60.0f; // degrees
    private static final float RANGE = 8.0f;
    private static final float DAMAGE = 4.0f; // 2 hearts

    @Override
    public Identifier id() {
        return PowerIds.BONUS_VENOMSPRAY;
    }

    @Override
    public String name() {
        return "Venomspray";
    }

    @Override
    public String description() {
        return "Spray poison in a cone, damaging and poisoning all enemies hit.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d lookDir = player.getRotationVec(1.0f).normalize();
        Box area = player.getBoundingBox().expand(RANGE);
        
        // Spawn cone-shaped particles in the direction player is looking
        for (int i = 0; i < 30; i++) {
            double dist = 1.0 + world.random.nextDouble() * (RANGE - 1);
            double spread = dist * 0.3;
            double px = player.getX() + lookDir.x * dist + (world.random.nextDouble() - 0.5) * spread;
            double py = player.getEyeY() + lookDir.y * dist + (world.random.nextDouble() - 0.5) * spread;
            double pz = player.getZ() + lookDir.z * dist + (world.random.nextDouble() - 0.5) * spread;
            world.spawnParticles(ParticleTypes.ITEM_SLIME, px, py, pz, 2, 0.1, 0.1, 0.1, 0.02);
        }
        
        // Sound effect
        player.playSound(SoundEvents.ENTITY_LLAMA_SPIT, 1.0f, 0.8f);
        
        DamageSource damageSource = world.getDamageSources().magic();
        
        world.getOtherEntities(player, area, e -> e instanceof LivingEntity)
                .forEach(e -> {
                    if (e instanceof LivingEntity living) {
                        // Skip trusted players
                        if (living instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                            return;
                        }
                        // Skip players with Void immunity
                        if (living instanceof ServerPlayerEntity otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                            return;
                        }
                        
                        // Check if target is in the cone
                        Vec3d toTarget = living.getEntityPos().add(0, living.getHeight() / 2, 0)
                                .subtract(player.getEyePos()).normalize();
                        double dot = lookDir.dotProduct(toTarget);
                        double angleRad = Math.acos(Math.max(-1, Math.min(1, dot)));
                        double angleDeg = Math.toDegrees(angleRad);
                        
                        if (angleDeg <= CONE_ANGLE / 2 && player.squaredDistanceTo(living) <= RANGE * RANGE) {
                            // Apply damage and effects
                            living.damage(world, damageSource, DAMAGE);
                            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 1)); // 10s Poison II
                            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0)); // 5s Weakness I
                        }
                    }
                });
        return true;
    }
}
