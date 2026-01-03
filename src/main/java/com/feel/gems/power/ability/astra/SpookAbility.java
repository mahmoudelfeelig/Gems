package com.feel.gems.power.ability.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
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



public final class SpookAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPOOK;
    }

    @Override
    public String name() {
        return "Spook";
    }

    @Override
    public String description() {
        return "Briefly disorients nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().spookCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int duration = GemsBalance.v().astra().spookDurationTicks();
        int radius = GemsBalance.v().astra().spookRadiusBlocks();
        int affected = 0;
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, duration, 0, true, false, false));
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, duration, 0, true, false, false));
            AbilityFeedback.burstAt(world, other.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.SMOKE, 12, 0.25D);
            affected++;
        }
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_STARE, 0.8F, 0.8F);
        player.sendMessage(Text.literal("Spooked " + affected + " targets."), true);
        return true;
    }
}
