package com.feel.gems.power.ability.astra;

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



public final class TagAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TAG;
    }

    @Override
    public String name() {
        return "Tag";
    }

    @Override
    public String description() {
        return "Marks a target so they glow through walls for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().tagCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().astra().tagRangeBlocks());
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
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GemsBalance.v().astra().tagDurationTicks(), 0, true, false, false));
        AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.6F);
        AbilityFeedback.burstAt(player.getEntityWorld(), target.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.GLOW, 14, 0.2D);
        player.sendMessage(Text.translatable("gems.ability.astra.tag.tagged", target.getName()), true);
        return true;
    }
}
