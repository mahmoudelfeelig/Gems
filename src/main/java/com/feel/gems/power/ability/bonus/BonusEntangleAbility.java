package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
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

/**
 * Entangle - vines erupt to root enemies.
 */
public final class BonusEntangleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ENTANGLE;
    }

    @Override
    public String name() {
        return "Entangle";
    }

    @Override
    public String description() {
        return "Vines erupt from the ground to root enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().entangleCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        var cfg = GemsBalance.v().bonusPool();
        int duration = cfg.entangleDurationSeconds * 20;
        Box box = player.getBoundingBox().expand(cfg.entangleRadiusBlocks);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer) {
                if (GemTrust.isTrusted(player, otherPlayer) || VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 4, false, false));
        }
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(), player.getZ(), 20, 1.0, 0.2, 1.0, 0.02);
        return true;
    }
}
