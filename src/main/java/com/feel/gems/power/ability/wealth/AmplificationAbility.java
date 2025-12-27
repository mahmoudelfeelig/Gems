package com.feel.gems.power.ability.wealth;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.gem.wealth.EnchantmentAmplification;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public final class AmplificationAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AMPLIFICATION;
    }

    @Override
    public String name() {
        return "Amplification";
    }

    @Override
    public String description() {
        return "Temporarily boosts enchantments on your tools and armor.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().amplificationCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().wealth().amplificationDurationTicks();
        AbilityRuntime.startAmplification(player, duration);
        EnchantmentAmplification.apply(player, duration);
        AbilityFeedback.sound(player, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.9F, 1.2F);
        AbilityFeedback.burst(player, ParticleTypes.ENCHANT, 18, 0.35D);
        player.sendMessage(Text.literal("Amplification active."), true);
        return true;
    }
}
