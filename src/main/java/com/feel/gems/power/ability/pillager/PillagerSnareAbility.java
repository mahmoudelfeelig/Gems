package com.feel.gems.power.ability.pillager;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class PillagerSnareAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_SNARE;
    }

    @Override
    public String name() {
        return "Snare Shot";
    }

    @Override
    public String description() {
        return "Snare a target you can see with a slowing, glowing mark.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().snareCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var cfg = GemsBalance.v().pillager();
        LivingEntity target = Targeting.raycastLiving(player, cfg.snareRangeBlocks());
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

        int duration = cfg.snareDurationTicks();
        int slowAmp = cfg.snareSlownessAmplifier();
        if (duration <= 0) {
            player.sendMessage(Text.translatable("gems.ability.pillager.snare.disabled"), true);
            return false;
        }
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, duration, 0, true, false, false));
        if (slowAmp >= 0) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, slowAmp, true, false, false));
        }

        AbilityFeedback.burst(player, ParticleTypes.CRIT, 8, 0.25D);
        AbilityFeedback.burstAt(player.getEntityWorld(), target.getEntityPos().add(0.0D, target.getHeight() * 0.6D, 0.0D), ParticleTypes.CRIT, 10, 0.25D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ARROW_SHOOT, 0.9F, 1.2F);
        player.sendMessage(Text.translatable("gems.ability.pillager.snare.landed"), true);
        return true;
    }
}
