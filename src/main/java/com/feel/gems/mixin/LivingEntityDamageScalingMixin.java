package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.legendary.DuelistsRapierItem;
import com.feel.gems.item.legendary.ReversalMirrorItem;
import com.feel.gems.power.ability.hunter.HunterPackTacticsRuntime;
import com.feel.gems.power.ability.duelist.DuelistParryAbility;
import com.feel.gems.power.ability.sentinel.SentinelInterventionRuntime;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.gem.duelist.DuelistPassiveRuntime;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.hunter.HunterPreyMarkRuntime;
import com.feel.gems.power.gem.reaper.ReaperBloodCharge;
import com.feel.gems.power.gem.sentinel.SentinelPassiveRuntime;
import com.feel.gems.power.gem.space.SpaceLunarScaling;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.AbilityRestrictions;
import com.feel.gems.power.runtime.EtherealState;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.trust.GemTrust;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
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

        // Ethereal Step: complete damage immunity
        if (self instanceof ServerPlayerEntity victim && EtherealState.isEthereal(victim)) {
            return 0.0F;
        }

        // Duelist: Parry ability - blocks melee damage and stuns attacker
        if (self instanceof ServerPlayerEntity victim && DuelistParryAbility.isParrying(victim)) {
            Entity attackerEntity = source.getAttacker();
            // Only block melee attacks (not projectiles, magic, etc.)
            if (attackerEntity instanceof LivingEntity attackerLiving && !source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                DuelistParryAbility.consumeParry(victim);
                int stunTicks = GemsBalance.v().duelist().parryStunTicks();
                if (attackerLiving instanceof ServerPlayerEntity attackerPlayer) {
                    // Stun the attacker (players only)
                    AbilityRestrictions.stun(attackerPlayer, stunTicks);
                } else {
                    // Briefly punish mobs so the parry window meaningfully matters in PvE too.
                    attackerLiving.takeKnockback(0.6F, victim.getX() - attackerLiving.getX(), victim.getZ() - attackerLiving.getZ());
                    attackerLiving.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, stunTicks, 1, false, true));
                    attackerLiving.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, stunTicks, 0, false, true));
                }
                // Visual/audio feedback
                AbilityFeedback.sound(victim, SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 1.0F);
                if (attackerLiving instanceof ServerPlayerEntity attackerPlayer) {
                    AbilityFeedback.sound(attackerPlayer, SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0F, 0.8F);
                }
                // Trigger Riposte window for the victim
                DuelistPassiveRuntime.triggerRiposte(victim);
                return 0.0F;
            }
        }

        // Duelist's Rapier: item-based parry - blocks melee damage and grants guaranteed crit
        if (self instanceof ServerPlayerEntity victim && DuelistsRapierItem.isInParryWindow(victim)) {
            Entity attackerEntity = source.getAttacker();
            // Only block melee attacks (not projectiles, magic, etc.)
            if (attackerEntity instanceof LivingEntity && !source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                DuelistsRapierItem.onSuccessfulParry(victim);
                // Visual/audio feedback
                AbilityFeedback.sound(victim, SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 1.2F);
                return 0.0F;
            }
        }

        // Sentinel: Intervention - redirect the next hit from the ally to the sentinel.
        if (self instanceof ServerPlayerEntity ally) {
            ServerPlayerEntity protector = SentinelInterventionRuntime.getProtector(ally);
            if (protector != null && protector.isAlive()) {
                SentinelInterventionRuntime.consumeProtection(protector);
                if (protector.getEntityWorld() instanceof ServerWorld protectorWorld) {
                    protector.damage(protectorWorld, source, amount);
                } else {
                    protector.damage(world, source, amount);
                }
                AbilityFeedback.sound(ally, SoundEvents.ITEM_SHIELD_BLOCK, 0.9F, 1.0F);
                AbilityFeedback.sound(protector, SoundEvents.ITEM_SHIELD_BLOCK, 0.9F, 0.9F);
                return 0.0F;
            }
        }

        float scaled = amount;

        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity playerAttacker) {
            // Duelist's Rapier: consume guaranteed crit on the next melee hit (avoid extra hit + invulnerability issues).
            if (playerAttacker.getMainHandStack().isOf(com.feel.gems.item.ModItems.DUELISTS_RAPIER)
                    && !source.isIn(DamageTypeTags.IS_PROJECTILE)
                    && DuelistsRapierItem.hasAndConsumeGuaranteedCrit(playerAttacker)) {
                scaled *= GemsBalance.v().legendary().duelistsRapierCritDamageMultiplier();
                AbilityFeedback.sound(playerAttacker, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
                if (world instanceof ServerWorld serverWorld) {
                    AbilityFeedback.burstAt(serverWorld, self.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.CRIT, 12, 0.25D);
                }
            }

            // Void Immunity: target is immune to gem ability/passive damage bonuses
            boolean targetHasVoidImmunity = self instanceof ServerPlayerEntity victim 
                    && VoidImmunity.shouldBlockEffect(playerAttacker, victim);
            
            if (!targetHasVoidImmunity) {
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

                // Hunter: Pack Tactics - allies deal bonus damage to marked target
                if (self instanceof ServerPlayerEntity victim) {
                    if (HunterPackTacticsRuntime.hasBuffAgainst(playerAttacker, victim.getUuid())) {
                        scaled *= HunterPackTacticsRuntime.getDamageMultiplier();
                    }
                }

                // Bonus Passives: attack damage multipliers
                scaled *= BonusPassiveRuntime.getAttackDamageMultiplier(playerAttacker, self, scaled);

                // Bonus Passives: lifesteal
                BonusPassiveRuntime.applyLifesteal(playerAttacker, scaled);
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

            // Bonus Passives: defense damage modifiers
            scaled = BonusPassiveRuntime.getDefenseDamageMultiplier(victim, scaled, world, 
                    attacker instanceof LivingEntity ? (LivingEntity) attacker : null);

            // Legendary: Reversal Mirror - reflect incoming damage back to attacker.
            ReversalMirrorItem.tryReflectDamage(victim, attacker, scaled, world);
        }

        return scaled;
    }
}
