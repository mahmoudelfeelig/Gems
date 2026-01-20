package com.feel.gems.power.ability.wealth;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.gem.wealth.WealthFumble;
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
import net.minecraft.util.math.Box;


public final class FumbleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FUMBLE;
    }

    @Override
    public String name() {
        return "Fumble";
    }

    @Override
    public String description() {
        return "Fumble: enemies cannot use their offhand and cannot eat for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().fumbleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        int radius = GemsBalance.v().wealth().fumbleRadiusBlocks();
        int affected = 0;
        int duration = GemsBalance.v().wealth().fumbleDurationTicks();
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer) {
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
                if (!VoidImmunity.canBeTargeted(player, otherPlayer)) {
                    continue;
                }
                WealthFumble.apply(otherPlayer, duration);
                otherPlayer.stopUsingItem();
            } else {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 0, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
            }
            AbilityFeedback.burstAt(world, other.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.SMOKE, 10, 0.25D);
            affected++;
        }
        AbilityFeedback.sound(player, SoundEvents.ENTITY_WITCH_THROW, 0.8F, 1.1F);
        player.sendMessage(Text.translatable("gems.ability.wealth.fumble.affected", affected), true);
        return true;
    }
}
