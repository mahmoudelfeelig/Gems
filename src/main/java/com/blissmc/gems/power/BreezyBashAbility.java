package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;

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
            player.sendMessage(Text.literal("No target."), true);
            return true;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }

        Vec3d away = target.getPos().subtract(player.getPos()).normalize();
        double knockback = GemsBalance.v().puff().breezyBashKnockback();
        target.addVelocity(away.x * knockback, GemsBalance.v().puff().breezyBashUpVelocityY(), away.z * knockback);
        target.velocityModified = true;
        target.damage(player.getDamageSources().playerAttack(player), GemsBalance.v().puff().breezyBashInitialDamage());
        AbilityFeedback.burstAt(player.getServerWorld(), target.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.GUST, 16, 0.35D);
        if (target instanceof ServerPlayerEntity otherPlayer) {
            BreezyBashTracker.track(player, otherPlayer, GemsBalance.v().puff().breezyBashImpactWindowTicks());
        }

        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_BREEZE_WIND_BURST, SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
