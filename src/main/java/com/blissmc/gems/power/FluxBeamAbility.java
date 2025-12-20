package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class FluxBeamAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FLUX_BEAM;
    }

    @Override
    public String name() {
        return "Flux Beam";
    }

    @Override
    public String description() {
        return "Fires a powerful beam; damage scales with stored charge.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().flux().fluxBeamCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().flux().fluxBeamRangeBlocks());
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return true;
        }

        int charge = FluxCharge.get(player);
        float minDamage = GemsBalance.v().flux().fluxBeamMinDamage();
        float max100 = GemsBalance.v().flux().fluxBeamMaxDamageAt100();
        float max200 = GemsBalance.v().flux().fluxBeamMaxDamageAt200();
        float t100 = Math.min(charge, 100) / 100.0F;
        float tOver = Math.max(0, charge - 100) / 100.0F;
        float damage = minDamage + t100 * (max100 - minDamage) + tOver * (max200 - max100);

        int perPercent = GemsBalance.v().flux().fluxBeamArmorDamagePerPercent();
        int durabilityDamage = charge >= 100
                ? GemsBalance.v().flux().fluxBeamArmorDamageAt100() + (charge - 100) * perPercent
                : charge * perPercent;

        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other) && GemPowers.isPassiveActive(player, PowerIds.FLUX_ALLY_INVERSION)) {
            repairArmor(other, durabilityDamage);
            beamFx(player, other.getPos().add(0.0D, 1.0D, 0.0D), true);
            player.sendMessage(Text.literal("Flux Beam: repaired ally armor (" + charge + "%)"), true);
            return true;
        }

        DamageSource source = player.getDamageSources().magic();
        target.damage(source, damage);
        if (target instanceof ServerPlayerEntity victim) {
            damageArmor(victim, durabilityDamage);
        }
        beamFx(player, target.getPos().add(0.0D, 1.0D, 0.0D), false);
        player.sendMessage(Text.literal("Flux Beam: " + charge + "%"), true);
        return true;
    }

    private static void beamFx(ServerPlayerEntity player, Vec3d hitPos, boolean healing) {
        var world = player.getServerWorld();
        Vec3d from = player.getEyePos();
        var particle = healing ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ELECTRIC_SPARK;
        AbilityFeedback.beam(world, from, hitPos, particle, 16);
        AbilityFeedback.burstAt(world, hitPos, particle, 10, 0.25D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_GUARDIAN_ATTACK, 0.8F, healing ? 1.6F : 1.2F);
    }

    private static void damageArmor(ServerPlayerEntity player, int amount) {
        if (amount <= 0) {
            return;
        }
        for (ItemStack armor : player.getInventory().armor) {
            if (armor.isEmpty() || !armor.isDamageable()) {
                continue;
            }
            armor.setDamage(Math.min(armor.getMaxDamage(), armor.getDamage() + amount));
        }
    }

    private static void repairArmor(ServerPlayerEntity player, int amount) {
        if (amount <= 0) {
            return;
        }
        for (ItemStack armor : player.getInventory().armor) {
            if (armor.isEmpty() || !armor.isDamageable() || !armor.isDamaged()) {
                continue;
            }
            armor.setDamage(Math.max(0, armor.getDamage() - amount));
        }
    }
}
