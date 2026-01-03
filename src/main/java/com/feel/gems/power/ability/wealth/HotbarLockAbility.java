package com.feel.gems.power.ability.wealth;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.power.gem.wealth.HotbarLock;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public final class HotbarLockAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HOTBAR_LOCK;
    }

    @Override
    public String name() {
        return "Hotbar Lock";
    }

    @Override
    public String description() {
        return "Hotbar Lock: locks an enemy to their current hotbar slot for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().hotbarLockCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().wealth().hotbarLockRangeBlocks());
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return false;
        }
        int duration = GemsBalance.v().wealth().hotbarLockDurationTicks();
        if (target instanceof ServerPlayerEntity other) {
            if (GemTrust.isTrusted(player, other)) {
                player.sendMessage(Text.literal("Target is trusted."), true);
                return false;
            }
            HotbarLock.lock(other, other.getInventory().getSelectedSlot(), duration);
        } else {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 0, true, false, false));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
        }
        AbilityFeedback.sound(player, SoundEvents.BLOCK_CHAIN_PLACE, 0.8F, 1.1F);
        if (player.getEntityWorld() instanceof ServerWorld world) {
            AbilityFeedback.burstAt(world, target.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.CRIT, 12, 0.2D);
        }
        player.sendMessage(Text.literal("Hotbar locked."), true);
        return true;
    }
}
