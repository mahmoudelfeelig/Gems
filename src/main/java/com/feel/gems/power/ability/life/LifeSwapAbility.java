package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class LifeSwapAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.LIFE_SWAP;
    }

    @Override
    public String name() {
        return "Life Swap";
    }

    @Override
    public String description() {
        return "Swap health with a target you can see.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().lifeSwapCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        float minHearts = GemsBalance.v().life().lifeSwapMinHearts();
        if (player.getHealth() < minHearts * 2.0F) {
            player.sendMessage(Text.literal("You need at least " + minHearts + " hearts to use Life Swap."), true);
            return false;
        }

        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().life().lifeSwapRangeBlocks());
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return false;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return false;
        }

        float playerHealth = player.getHealth();
        float targetHealth = target.getHealth();
        player.setHealth(Math.min(player.getMaxHealth(), targetHealth));
        target.setHealth(Math.min(target.getMaxHealth(), playerHealth));

        AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.7F, 1.4F);
        AbilityFeedback.burst(player, ParticleTypes.HEART, 10, 0.25D);
        if (player.getServerWorld() != null) {
            AbilityFeedback.burstAt(player.getServerWorld(), target.getPos().add(0.0D, target.getHeight() * 0.6D, 0.0D), ParticleTypes.HEART, 10, 0.25D);
        }
        player.sendMessage(Text.literal("Life Swap activated."), true);
        return true;
    }
}
