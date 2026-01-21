package com.feel.gems.power.ability.strength;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public final class FrailerAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FRAILER;
    }

    @Override
    public String name() {
        return "Frailer";
    }

    @Override
    public String description() {
        return "Applies Weakness to a targeted enemy.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().strength().frailerCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var target = Targeting.raycastLiving(player, GemsBalance.v().strength().frailerRangeBlocks());
        if (target == null) {
            player.sendMessage(Text.translatable("gems.message.no_target"), true);
            return false;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.translatable("gems.message.target_trusted"), true);
            return false;
        }
        if (target instanceof ServerPlayerEntity other && !VoidImmunity.canBeTargeted(player, other)) {
            player.sendMessage(Text.translatable("gems.message.target_immune"), true);
            return false;
        }

        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.STRENGTH, GemsBalance.v().strength().frailerDurationTicks());
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0));
        AbilityFeedback.sound(player, SoundEvents.ENTITY_WITHER_SHOOT, 0.7F, 1.3F);
        if (player.getEntityWorld() instanceof ServerWorld world) {
            AbilityFeedback.burstAt(world, target.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ASH, 14, 0.25D);
        }
        player.sendMessage(Text.translatable("gems.ability.strength.frailer.weakened", target.getName().getString()), true);
        return true;
    }
}
