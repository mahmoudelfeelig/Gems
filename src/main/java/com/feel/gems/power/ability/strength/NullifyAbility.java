package com.feel.gems.power.ability.strength;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;


public final class NullifyAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.NULLIFY;
    }

    @Override
    public String name() {
        return "Nullify";
    }

    @Override
    public String description() {
        return "Removes status effects from nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().strength().nullifyCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        int radius = GemsBalance.v().strength().nullifyRadiusBlocks();
        int affected = 0;
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayerEntity otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                continue;
            }
            other.clearStatusEffects();
            AbilityFeedback.burstAt(world, other.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 10, 0.25D);
            affected++;
        }
        AbilityFeedback.sound(player, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.8F, 0.8F);
        AbilityFeedback.burst(player, ParticleTypes.ENCHANT, 14, 0.35D);
        player.sendMessage(Text.translatable("gems.ability.strength.nullify.affected", affected), true);
        return true;
    }
}
