package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class BonusRadiantBurstAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_RADIANT_BURST;
    }

    @Override
    public String name() {
        return "Radiant Burst";
    }

    @Override
    public String description() {
        return "Emit a burst of light that blinds enemies and heals allies.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(10);
        
        world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 50, 5, 2, 5, 0.2);
        
        world.getOtherEntities(player, area, e -> e instanceof LivingEntity)
                .forEach(e -> {
                    if (e instanceof ServerPlayerEntity other) {
                        if (VoidImmunity.shouldBlockEffect(player, other)) {
                            return;
                        }
                        // Heal allies, blind enemies.
                        if (GemTrust.isTrusted(player, other)) {
                            other.heal(6.0f);
                        } else {
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0));
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0));
                        }
                    } else if (e instanceof LivingEntity living) {
                        // Blind enemies
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0));
                        living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0));
                    }
                });
        return true;
    }
}
