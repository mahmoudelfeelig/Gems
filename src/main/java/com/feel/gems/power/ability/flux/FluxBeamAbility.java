package com.feel.gems.power.ability.flux;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.net.GemExtraStateSync;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
            player.sendMessage(Text.translatable("gems.message.no_target"), true);
            return false;
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
            beamFx(player, other.getEntityPos().add(0.0D, 1.0D, 0.0D), true);
            player.sendMessage(Text.translatable("gems.ability.flux.beam.repaired", charge), true);
            consumeCharge(player);
            return true;
        }

        ServerWorld world = player.getEntityWorld();
        DamageSource source = player.getDamageSources().magic();
        target.damage(world, source, damage);
        if (target instanceof ServerPlayerEntity victim) {
            damageArmor(victim, durabilityDamage);
        }
        beamFx(player, target.getEntityPos().add(0.0D, 1.0D, 0.0D), false);
        player.sendMessage(Text.translatable("gems.ability.flux.beam.fired", charge), true);
        consumeCharge(player);
        return true;
    }

    private static void beamFx(ServerPlayerEntity player, Vec3d hitPos, boolean healing) {
        var world = player.getEntityWorld();
        Vec3d from = player.getEyePos();
        var core = healing ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ELECTRIC_SPARK;
        var trail = healing ? ParticleTypes.END_ROD : ParticleTypes.END_ROD;
        AbilityFeedback.beam(world, from, hitPos, core, 28);
        AbilityFeedback.beam(world, from, hitPos, trail, 18);
        AbilityFeedback.burstAt(world, hitPos, core, 18, 0.35D);
        AbilityFeedback.burstAt(world, hitPos, TintedParticleEffect.create(ParticleTypes.FLASH, 0xFFFFFF), 1, 0.0D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_GUARDIAN_ATTACK, 0.9F, healing ? 1.6F : 1.1F);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25F, healing ? 1.8F : 1.4F);
    }

    private static void consumeCharge(ServerPlayerEntity player) {
        // Flux charge is a one-shot resource; after firing, it resets.
        FluxCharge.set(player, 0);
        FluxCharge.clearIfBelow100(player);
        GemExtraStateSync.send(player);
    }

    private static void damageArmor(ServerPlayerEntity player, int amount) {
        if (amount <= 0) {
            return;
        }
        net.minecraft.entity.EquipmentSlot[] slots = {
                net.minecraft.entity.EquipmentSlot.HEAD,
                net.minecraft.entity.EquipmentSlot.CHEST,
                net.minecraft.entity.EquipmentSlot.LEGS,
                net.minecraft.entity.EquipmentSlot.FEET
        };
        for (var slot : slots) {
            ItemStack armor = player.getEquippedStack(slot);
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
        net.minecraft.entity.EquipmentSlot[] slots = {
                net.minecraft.entity.EquipmentSlot.HEAD,
                net.minecraft.entity.EquipmentSlot.CHEST,
                net.minecraft.entity.EquipmentSlot.LEGS,
                net.minecraft.entity.EquipmentSlot.FEET
        };
        for (var slot : slots) {
            ItemStack armor = player.getEquippedStack(slot);
            if (armor.isEmpty() || !armor.isDamageable() || !armor.isDamaged()) {
                continue;
            }
            armor.setDamage(Math.max(0, armor.getDamage() - amount));
        }
    }
}
