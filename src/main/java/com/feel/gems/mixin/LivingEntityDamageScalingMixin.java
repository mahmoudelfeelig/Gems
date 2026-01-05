package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.duelist.DuelistPassiveRuntime;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.hunter.HunterPreyMarkRuntime;
import com.feel.gems.power.gem.reaper.ReaperBloodCharge;
import com.feel.gems.power.gem.sentinel.SentinelPassiveRuntime;
import com.feel.gems.power.gem.space.SpaceLunarScaling;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.trust.GemTrust;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;




@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageScalingMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float gems$scaleDamage(float amount, ServerWorld world, DamageSource source, float originalAmount) {
        if (amount <= 0.0F) {
            return amount;
        }

        LivingEntity self = (LivingEntity) (Object) this;

        float scaled = amount;

        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity playerAttacker) {
            if (GemPowers.isPassiveActive(playerAttacker, PowerIds.SPACE_LUNAR_SCALING)) {
                scaled *= SpaceLunarScaling.multiplier(playerAttacker.getEntityWorld());
            }

            if (!(self instanceof ServerPlayerEntity victim && GemTrust.isTrusted(playerAttacker, victim))) {
                scaled *= ReaperBloodCharge.consumeMultiplierIfActive(playerAttacker);
            }

            if (AbilityRuntime.isReaperDeathOathActive(playerAttacker)) {
                UUID oathTarget = AbilityRuntime.reaperDeathOathTarget(playerAttacker);
                if (oathTarget != null && oathTarget.equals(self.getUuid())) {
                    scaled += GemsBalance.v().reaper().deathOathBonusDamage();
                }
            }

            // Duelist: Riposte - bonus damage after successful block
            if (DuelistPassiveRuntime.consumeRiposte(playerAttacker)) {
                scaled *= DuelistPassiveRuntime.getRiposteDamageMultiplier();
            }

            // Duelist: Focus - bonus damage in 1v1 combat
            if (self instanceof ServerPlayerEntity victim) {
                if (DuelistPassiveRuntime.isIn1v1Combat(playerAttacker, victim)) {
                    scaled *= DuelistPassiveRuntime.getFocusDamageMultiplier();
                }
            }

            // Hunter: Prey Mark - bonus damage to marked targets
            if (self instanceof ServerPlayerEntity victim) {
                if (HunterPreyMarkRuntime.isMarked(playerAttacker, victim)) {
                    scaled *= HunterPreyMarkRuntime.getDamageMultiplier();
                }
            }
        }

            if (self instanceof ServerPlayerEntity victim) {
            if (AbilityRuntime.isReaperRetributionActive(victim)) {
                if (attacker instanceof LivingEntity livingAttacker && livingAttacker != victim) {
                    float reflected = scaled * GemsBalance.v().reaper().retributionDamageMultiplier();
                    livingAttacker.damage(world, victim.getDamageSources().magic(), reflected);
                    return 0.0F;
                }
            }

            if (GemPowers.isPassiveActive(victim, PowerIds.SPACE_STARSHIELD) && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                if (world.getDimension().hasSkyLight() && world.isNight() && world.isSkyVisible(victim.getBlockPos())) {
                    scaled *= GemsBalance.v().space().starshieldProjectileDamageMultiplier();
                }
            }

            if (GemPowers.isPassiveActive(victim, PowerIds.REAPER_UNDEAD_WARD)
                    && attacker instanceof LivingEntity livingAttacker
                    && livingAttacker.getType().isIn(EntityTypeTags.UNDEAD)) {
                scaled *= GemsBalance.v().reaper().undeadWardDamageMultiplier();
            }

            if (GemPowers.isPassiveActive(victim, PowerIds.AIR_AERIAL_GUARD) && AirMacePassive.isHoldingMace(victim)) {
                scaled *= GemsBalance.v().air().aerialGuardDamageMultiplier();
            }

            if (GemPowers.isPassiveActive(victim, PowerIds.FLUX_INSULATION)) {
                int charge = FluxCharge.get(victim);
                if (charge >= GemsBalance.v().flux().fluxInsulationChargeThreshold()) {
                    scaled *= GemsBalance.v().flux().fluxInsulationDamageMultiplier();
                }
            }

            // Sentinel: Guardian Aura - nearby allies take less damage
            if (SentinelPassiveRuntime.isProtectedByGuardianAura(victim)) {
                scaled *= (1.0f - SentinelPassiveRuntime.getGuardianAuraDamageReduction());
            }

            // Sentinel: Retribution Thorns - attackers take reflected damage
            if (SentinelPassiveRuntime.hasRetributionThorns(victim)) {
                if (attacker instanceof LivingEntity livingAttacker && livingAttacker != victim) {
                    float reflected = scaled * SentinelPassiveRuntime.getRetributionThornsDamagePercent();
                    livingAttacker.damage(world, victim.getDamageSources().thorns(victim), reflected);
                }
            }
        }

        return scaled;
    }
}
