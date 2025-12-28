package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.reaper.ReaperBloodCharge;
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
    private float gems$scaleDamage(float amount, DamageSource source) {
        if (amount <= 0.0F) {
            return amount;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) {
            return amount;
        }

        float scaled = amount;

        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity playerAttacker) {
            if (GemPowers.isPassiveActive(playerAttacker, PowerIds.SPACE_LUNAR_SCALING)) {
                scaled *= SpaceLunarScaling.multiplier(playerAttacker.getServerWorld());
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
        }

        if (self instanceof ServerPlayerEntity victim) {
            if (AbilityRuntime.isReaperRetributionActive(victim)) {
                if (attacker instanceof LivingEntity livingAttacker && livingAttacker != victim) {
                    float reflected = scaled * GemsBalance.v().reaper().retributionDamageMultiplier();
                    livingAttacker.damage(victim.getDamageSources().magic(), reflected);
                    return 0.0F;
                }
            }

            if (GemPowers.isPassiveActive(victim, PowerIds.SPACE_STARSHIELD) && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                ServerWorld world = victim.getServerWorld();
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
        }

        return scaled;
    }
}
