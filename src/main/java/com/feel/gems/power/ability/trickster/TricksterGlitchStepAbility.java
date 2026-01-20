package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Glitch Step - short-range teleport that leaves a damaging afterimage at your origin.
 */
public final class TricksterGlitchStepAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_GLITCH_STEP;
    }

    @Override
    public String name() {
        return "Glitch Step";
    }

    @Override
    public String description() {
        return "Short-range teleport that leaves a damaging afterimage at your origin.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().glitchStepCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int distance = GemsBalance.v().trickster().glitchStepDistanceBlocks();
        float afterimgDamage = GemsBalance.v().trickster().glitchStepAfterimgDamage();
        int afterimgRadius = GemsBalance.v().trickster().glitchStepAfterimgRadiusBlocks();

        Vec3d origin = player.getEntityPos();
        Vec3d dir = player.getRotationVec(1.0F).normalize();
        Vec3d destination = origin.add(dir.multiply(distance));

        // Teleport
        GemsTeleport.teleport(player, world, destination.x, destination.y, destination.z, player.getYaw(), player.getPitch());
        player.velocityDirty = true;

        // Damage enemies near origin (afterimage explosion)
        Box damageBox = new Box(origin.subtract(afterimgRadius, afterimgRadius, afterimgRadius), 
                                 origin.add(afterimgRadius, afterimgRadius, afterimgRadius));
        for (Entity e : world.getOtherEntities(player, damageBox, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            if (e instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (e instanceof ServerPlayerEntity other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }
            ((LivingEntity) e).damage(world, player.getDamageSources().indirectMagic(player, player), afterimgDamage);
        }

        // Visual effects
        AbilityFeedback.burstAt(world, origin.add(0, 1, 0), ParticleTypes.PORTAL, 30, 0.8D);
        AbilityFeedback.burstAt(world, destination.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 20, 0.5D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.5F);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.5F, 1.8F);
        return true;
    }
}
