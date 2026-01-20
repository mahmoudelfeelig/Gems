package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class BonusTimewarpAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_TIMEWARP;
    }

    @Override
    public String name() {
        return "Timewarp";
    }

    @Override
    public String description() {
        return "Briefly slow time for enemies in an area.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().timewarpCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        var cfg = GemsBalance.v().bonusPool();
        int duration = cfg.timewarpDurationSeconds * 20;
        int radius = cfg.timewarpRadiusBlocks;
        int amp = cfg.timewarpSlownessAmplifier;
        Box box = player.getBoundingBox().expand(radius);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer) {
                if (GemTrust.isTrusted(player, otherPlayer) || VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, amp, false, false));
        }
        return true;
    }
}
