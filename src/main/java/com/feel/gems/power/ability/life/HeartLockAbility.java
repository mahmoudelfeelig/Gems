package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.power.gem.life.HeartLockRuntime;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class HeartLockAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HEART_LOCK;
    }

    @Override
    public String name() {
        return "Heart Lock";
    }

    @Override
    public String description() {
        return "Temporarily locks an enemy's max health to their current health.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().heartLockCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().life().heartLockRangeBlocks());
        if (target == null) {
            player.sendMessage(Text.translatable("gems.message.no_target"), true);
            return false;
        }
        int duration = GemsBalance.v().life().heartLockDurationTicks();
        if (target instanceof ServerPlayerEntity other) {
            if (GemTrust.isTrusted(player, other)) {
                player.sendMessage(Text.translatable("gems.message.target_trusted"), true);
                return false;
            }
            AbilityRuntime.startHeartLock(player, other, duration);
        } else {
            if (target instanceof net.minecraft.entity.mob.MobEntity mob) {
                HeartLockRuntime.apply(player, mob, duration);
            }
        }
        AbilityFeedback.sound(player, SoundEvents.BLOCK_CHAIN_PLACE, 0.8F, 0.9F);
        AbilityFeedback.burstAt(player.getEntityWorld(), target.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 18, 0.25D);
        player.sendMessage(Text.translatable("gems.ability.life.heart_lock.applied"), true);
        return true;
    }
}
