package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class BonusDoomBoltAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_DOOM_BOLT;
    }

    @Override
    public String name() {
        return "Doom Bolt";
    }

    @Override
    public String description() {
        return "Mark an enemy for death, dealing massive damage after a delay.";
    }

    @Override
    public int cooldownTicks() {
        return 800; // 40 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(10);
        
        // Target nearest enemy (untrusted player or mob)
        var target = world.getOtherEntities(player, area, e -> {
                    if (!(e instanceof LivingEntity)) return false;
                    if (e instanceof ServerPlayerEntity otherPlayer) {
                        if (GemTrust.isTrusted(player, otherPlayer)) return false;
                        if (!VoidImmunity.canBeTargeted(player, otherPlayer)) return false;
                    }
                    return true; // mobs are always valid targets
                })
                .stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .min((a, b) -> Double.compare(
                        a.squaredDistanceTo(player),
                        b.squaredDistanceTo(player)));
        
        if (target.isEmpty()) {
            player.sendMessage(net.minecraft.text.Text.translatable("gems.ability.no_target"), true);
            return false;
        }
        
        LivingEntity living = target.get();
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0));
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
        // Delayed damage via wither effect
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 3));
        world.spawnParticles(ParticleTypes.WITCH, living.getX(), living.getY() + 1, living.getZ(), 30, 0.5, 1, 0.5, 0.1);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sound.SoundEvents.ENTITY_WITHER_SHOOT, net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 0.6f);
        return true;
    }
}
