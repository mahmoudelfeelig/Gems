package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class PillagerRavageAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_RAVAGE;
    }

    @Override
    public String name() {
        return "Ravage";
    }

    @Override
    public String description() {
        return "Bashes a target with heavy knockback.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().ravageCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int range = GemsBalance.v().pillager().ravageRangeBlocks();
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return false;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Can't ravage a trusted player."), true);
            return false;
        }

        float damage = GemsBalance.v().pillager().ravageDamage();
        double knockback = GemsBalance.v().pillager().ravageKnockback();
        Vec3d dir = player.getRotationVec(1.0F);

        DamageSource src = player.getDamageSources().playerAttack(player);
        target.damage(src, damage);
        target.addVelocity(dir.x * knockback, 0.15D, dir.z * knockback);
        target.velocityModified = true;

        AbilityFeedback.burstAt(player.getServerWorld(), target.getPos().add(0.0D, 0.8D, 0.0D), ParticleTypes.SWEEP_ATTACK, 1, 0.0D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_RAVAGER_ATTACK, 0.9F, 1.0F);
        return true;
    }
}

