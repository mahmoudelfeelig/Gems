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
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

/**
 * Blinding Flash - blind nearby enemies.
 */
public final class BonusBlindingFlashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_BLINDING_FLASH;
    }

    @Override
    public String name() {
        return "Blinding Flash";
    }

    @Override
    public String description() {
        return "Blind all nearby enemies with an intense flash.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().blindingFlashCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        var cfg = GemsBalance.v().bonusPool();
        int duration = cfg.blindingFlashBlindSeconds * 20;
        Box box = player.getBoundingBox().expand(cfg.blindingFlashRadiusBlocks);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer) {
                if (GemTrust.isTrusted(player, otherPlayer) || VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, duration, 0, false, false));
        }
        world.spawnParticles(TintedParticleEffect.create(ParticleTypes.FLASH, 0xFFFFFF),
                player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
        return true;
    }
}
