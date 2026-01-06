package com.feel.gems.power.ability.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
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
import net.minecraft.util.math.Vec3d;



public final class GroupBreezyBashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.GROUP_BREEZY_BASH;
    }

    @Override
    public String name() {
        return "Group Breezy Bash";
    }

    @Override
    public String description() {
        return "Knocks back all untrusted players nearby.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().groupBashCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        int radius = GemsBalance.v().puff().groupBashRadiusBlocks();
        double kb = GemsBalance.v().puff().groupBashKnockback();
        double up = GemsBalance.v().puff().groupBashUpVelocityY();
        int affected = 0;
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            Vec3d away = other.getEntityPos().subtract(player.getEntityPos()).normalize();
            other.addVelocity(away.x * kb, up, away.z * kb);
            other.velocityDirty = true;
            AbilityFeedback.burstAt(world, other.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.GUST, 12, 0.35D);
            affected++;
        }
        AbilityFeedback.ring(world, player.getEntityPos().add(0.0D, 0.2D, 0.0D), Math.min(6.0D, radius), ParticleTypes.GUST, 28);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_BREEZE_WIND_BURST, 1.0F, 1.0F);
        player.sendMessage(Text.translatable("gems.ability.puff.group_bash.affected", affected), true);
        return true;
    }
}
