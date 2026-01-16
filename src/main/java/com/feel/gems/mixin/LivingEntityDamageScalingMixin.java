package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.legendary.DuelistsRapierItem;
import com.feel.gems.item.legendary.HunterSightBowItem;
import com.feel.gems.item.legendary.GladiatorsMarkItem;
import com.feel.gems.item.legendary.ReversalMirrorItem;
import com.feel.gems.item.legendary.SoulShackleItem;
import com.feel.gems.power.ability.bonus.BonusBerserkerRageAbility;
import com.feel.gems.power.ability.bonus.BonusIronMaidenAbility;
import com.feel.gems.power.ability.bonus.BonusMarkOfDeathAbility;
import com.feel.gems.power.ability.bonus.BonusOverchargeAbility;
import com.feel.gems.power.ability.hunter.HunterPackTacticsRuntime;
import com.feel.gems.power.ability.duelist.DuelistBladeDanceAbility;
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
import com.feel.gems.rivalry.RivalryManager;
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
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;



@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageScalingMixin {
    private static final ThreadLocal<Boolean> SHACKLE_TRANSFER = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> IRON_MAIDEN_REFLECT = ThreadLocal.withInitial(() -> Boolean.FALSE);
    @ModifyVariable(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), ordinal = 0)
    private float gems$scaleDamage(float amount, ServerWorld world, DamageSource source) {
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

        if (BonusMarkOfDeathAbility.isMarked(self)) {
            scaled *= BonusMarkOfDeathAbility.getDamageMultiplier();
        }

        Entity attacker = source.getAttacker();
        if (attacker == null) {
            attacker = source.getSource();
        }
        if (attacker instanceof ServerPlayerEntity playerAttacker) {
            HunterSightBowItem.recordHit(playerAttacker, self);
        }
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
                            // Rivalry: bonus damage to assigned target
                if (self instanceof ServerPlayerEntity victim) {
                    if (GemsBalance.v().rivalry().enabled() && RivalryManager.isRivalryTarget(playerAttacker, victim)) {
                        scaled *= RivalryManager.getDamageMultiplier();
                    }
                }

            // Void Immunity: target is immune to gem ability/passive damage bonuses
            boolean targetHasVoidImmunity = self instanceof ServerPlayerEntity victim 
                    && VoidImmunity.shouldBlockEffect(playerAttacker, victim);
            
            if (!targetHasVoidImmunity) {
                if (BonusBerserkerRageAbility.isActive(playerAttacker)) {
                    scaled *= BonusBerserkerRageAbility.getDamageBoostMultiplier();
                }

                float overcharge = BonusOverchargeAbility.consumeDamageMultiplier(playerAttacker);
                if (overcharge > 1.0f) {
                    scaled *= overcharge;
                }

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

                // Duelist: Blade Dance - consecutive melee hits ramp damage.
                if (source.getSource() == playerAttacker && DuelistBladeDanceAbility.isActive(playerAttacker)) {
                    scaled *= DuelistBladeDanceAbility.getDamageMultiplier(playerAttacker);
                    DuelistBladeDanceAbility.onHit(playerAttacker);
                }

                // Duelist: Focus - bonus damage in 1v1 combat
                if (self instanceof ServerPlayerEntity victim) {
                    if (DuelistPassiveRuntime.isIn1v1Combat(playerAttacker, victim)) {
                        scaled *= DuelistPassiveRuntime.getFocusDamageMultiplier();
                    }
                }

                // Hunter: Prey Mark - bonus damage to marked targets
                if (self instanceof LivingEntity victim) {
                    if (HunterPreyMarkRuntime.isMarked(playerAttacker, victim)) {
                        scaled *= HunterPreyMarkRuntime.getDamageMultiplier();
                    }
                }

                // Hunter: Pack Tactics - allies deal bonus damage to marked target
                if (self instanceof LivingEntity victim) {
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
            if (BonusBerserkerRageAbility.isActive(victim)) {
                scaled *= BonusBerserkerRageAbility.getDamageTakenMultiplier();
            }

            if (AbilityRuntime.isReaperRetributionActive(victim)) {
                if (attacker instanceof LivingEntity livingAttacker && livingAttacker != victim) {
                    float reflected = scaled * GemsBalance.v().reaper().retributionDamageMultiplier();
                    livingAttacker.damage(world, victim.getDamageSources().magic(), reflected);
                    return 0.0F;
                }
            }

            if (GemPowers.isPassiveActive(victim, PowerIds.SPACE_STARSHIELD) && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                // GameTest worlds and some dimensions can have odd sky visibility, so treat this as a "night-time outdoors"
                // check that keys primarily off being in a skylit dimension at night.
                long dayTime = world.getTimeOfDay() % 24000L;
                boolean night = dayTime >= 13000L && dayTime <= 23000L;
                if (night) {
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

            if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_ELEMENTAL_HARMONY)) {
                if (source.isIn(DamageTypeTags.IS_FIRE) || source.isIn(DamageTypeTags.IS_FREEZING)
                        || source.isIn(DamageTypeTags.IS_LIGHTNING)) {
                    float reduction = GemsBalance.v().bonusPool().elementalHarmonyReductionPercent / 100.0f;
                    scaled *= (1.0f - reduction);
                }
            }

            // Bonus Passives: Thick Skin - reduced projectile damage
            if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_THICK_SKIN)
                    && (source.isIn(DamageTypeTags.IS_PROJECTILE) || source.getSource() instanceof ProjectileEntity)) {
                float reduction = GemsBalance.v().bonusPool().thickSkinProjectileReductionPercent() / 100.0f;
                scaled *= (1.0f - reduction);
            }

            if (victim.isBlocking() && !source.isIn(DamageTypeTags.BYPASSES_SHIELD)) {
                scaled *= BonusPassiveRuntime.getBlockingDamageMultiplier(victim);
                BonusPassiveRuntime.triggerCounterStrike(victim);
            }

            // Gladiator's Mark: marked players deal extra damage to each other.
            if (attacker instanceof ServerPlayerEntity attackerPlayer) {
                if (GladiatorsMarkItem.isMarkedAgainst(attackerPlayer, victim)) {
                    scaled *= GladiatorsMarkItem.getDamageMultiplier();
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

            // Bonus Ability: Iron Maiden - reflect incoming damage back to attacker.
            if (!IRON_MAIDEN_REFLECT.get() && BonusIronMaidenAbility.isActive(victim)) {
                if (attacker instanceof LivingEntity livingAttacker && livingAttacker != victim) {
                    float reflectPercent = GemsBalance.v().bonusPool().ironMaidenReflectPercent / 100.0f;
                    float reflected = scaled * reflectPercent;
                    if (reflected > 0.0f) {
                        IRON_MAIDEN_REFLECT.set(Boolean.TRUE);
                        try {
                            livingAttacker.damage(world, victim.getDamageSources().magic(), reflected);
                        } finally {
                            IRON_MAIDEN_REFLECT.set(Boolean.FALSE);
                        }
                    }
                }
            }

            // Legendary: Reversal Mirror - reflect incoming damage back to attacker.
            if (ReversalMirrorItem.tryReflectDamage(victim, attacker, scaled, world)) {
                return 0.0F;
            }

            // Legendary: Soul Shackle - split damage with the linked target.
            if (!SHACKLE_TRANSFER.get()) {
                ServerPlayerEntity linked = SoulShackleItem.getShackledTarget(victim);
                if (linked != null) {
                    float transfer = SoulShackleItem.getDamageToTransfer(victim, scaled);
                    if (transfer > 0.0F) {
                        SHACKLE_TRANSFER.set(Boolean.TRUE);
                        try {
                            linked.damage(world, world.getDamageSources().magic(), transfer);
                        } finally {
                            SHACKLE_TRANSFER.set(Boolean.FALSE);
                        }
                        scaled = Math.max(0.0F, scaled - transfer);
                    }
                }
            }
        }

        return scaled;
    }
}
