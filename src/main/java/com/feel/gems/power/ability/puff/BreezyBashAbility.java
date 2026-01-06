package com.feel.gems.power.ability.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.puff.BreezyBashTracker;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;



public final class BreezyBashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BREEZY_BASH;
    }

    @Override
    public String name() {
        return "Breezy Bash";
    }

    @Override
    public String description() {
        return "Uppercut + Impact: launches a target upward; if they land soon, they take bonus impact damage.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().breezyBashCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().puff().breezyBashRangeBlocks());
        if (target == null) {
            player.sendMessage(Text.translatable("gems.message.no_target"), true);
            return false;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.translatable("gems.message.target_trusted"), true);
            return false;
        }

        Vec3d away = target.getEntityPos().subtract(player.getEntityPos()).normalize();
        double knockback = GemsBalance.v().puff().breezyBashKnockback();
        target.addVelocity(away.x * knockback, GemsBalance.v().puff().breezyBashUpVelocityY(), away.z * knockback);
        target.velocityDirty = true;
        ServerWorld world = player.getEntityWorld();
        target.damage(world, player.getDamageSources().playerAttack(player), GemsBalance.v().puff().breezyBashInitialDamage());
        AbilityFeedback.burstAt(world, target.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.GUST, 16, 0.35D);
        BreezyBashTracker.track(player, target, GemsBalance.v().puff().breezyBashImpactWindowTicks());

        AbilityFeedback.sound(player, SoundEvents.ENTITY_BREEZE_WIND_BURST, 1.0F, 1.0F);
        return true;
    }
}
