package com.feel.gems.power.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.List;
import java.util.UUID;

/**
 * Runtime logic for bonus passive effects.
 * These are checked from mixins and event handlers.
 */
public final class BonusPassiveRuntime {
    // State keys for tracking
    private static final String SECOND_WIND_USED_KEY = "bonus_second_wind_used";
    private static final String ARCANE_BARRIER_COOLDOWN_KEY = "bonus_arcane_barrier_cd";
    private static final String COUNTER_STRIKE_READY_KEY = "bonus_counter_strike_ready";
    private static final String VENGEANCE_READY_KEY = "bonus_vengeance_ready";
    private static final String VENGEANCE_END_KEY = "bonus_vengeance_end";
    private static final String LAST_KILLER_KEY = "bonus_last_killer";

    // Cooldowns in ticks
    private static final int ARCANE_BARRIER_COOLDOWN = 600; // 30 seconds
    private static final int VENGEANCE_WINDOW = 100; // 5 seconds

    private BonusPassiveRuntime() {}

    // ========== Second Wind ==========
    // Once per life, survive a killing blow with 1 HP

    public static boolean shouldTriggerSecondWind(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_SECOND_WIND)) {
            return false;
        }
        String used = PlayerStateManager.getPersistent(player, SECOND_WIND_USED_KEY);
        return !"true".equals(used);
    }

    public static void consumeSecondWind(ServerPlayerEntity player) {
        PlayerStateManager.setPersistent(player, SECOND_WIND_USED_KEY, "true");
    }

    public static void resetSecondWind(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, SECOND_WIND_USED_KEY);
    }

    // ========== Arcane Barrier ==========
    // Absorb first hit every 30 seconds

    public static boolean shouldTriggerArcaneBarrier(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_ARCANE_BARRIER)) {
            return false;
        }
        String cdStr = PlayerStateManager.getPersistent(player, ARCANE_BARRIER_COOLDOWN_KEY);
        if (cdStr == null || cdStr.isEmpty()) {
            return true;
        }
        long cooldownEnd = Long.parseLong(cdStr);
        return player.getEntityWorld().getTime() >= cooldownEnd;
    }

    public static void consumeArcaneBarrier(ServerPlayerEntity player) {
        long cooldownEnd = player.getEntityWorld().getTime() + ARCANE_BARRIER_COOLDOWN;
        PlayerStateManager.setPersistent(player, ARCANE_BARRIER_COOLDOWN_KEY, String.valueOf(cooldownEnd));
    }

    // ========== Counter Strike ==========
    // After blocking, next hit deals 2x damage

    public static void triggerCounterStrike(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_COUNTER_STRIKE)) {
            return;
        }
        PlayerStateManager.setPersistent(player, COUNTER_STRIKE_READY_KEY, "true");
    }

    public static boolean consumeCounterStrike(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_COUNTER_STRIKE)) {
            return false;
        }
        String ready = PlayerStateManager.getPersistent(player, COUNTER_STRIKE_READY_KEY);
        if ("true".equals(ready)) {
            PlayerStateManager.clearPersistent(player, COUNTER_STRIKE_READY_KEY);
            return true;
        }
        return false;
    }

    // ========== Vengeance ==========
    // After taking damage, next attack within 5s deals +50% damage

    public static void triggerVengeance(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_VENGEANCE)) {
            return;
        }
        long endTime = player.getEntityWorld().getTime() + VENGEANCE_WINDOW;
        PlayerStateManager.setPersistent(player, VENGEANCE_READY_KEY, "true");
        PlayerStateManager.setPersistent(player, VENGEANCE_END_KEY, String.valueOf(endTime));
    }

    public static boolean consumeVengeance(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_VENGEANCE)) {
            return false;
        }
        String ready = PlayerStateManager.getPersistent(player, VENGEANCE_READY_KEY);
        if (!"true".equals(ready)) {
            return false;
        }
        String endStr = PlayerStateManager.getPersistent(player, VENGEANCE_END_KEY);
        if (endStr == null || endStr.isEmpty()) {
            return false;
        }
        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearVengeance(player);
            return false;
        }
        clearVengeance(player);
        return true;
    }

    public static void clearVengeance(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, VENGEANCE_READY_KEY);
        PlayerStateManager.clearPersistent(player, VENGEANCE_END_KEY);
    }

    // ========== Nemesis ==========
    // +25% damage to last player who killed you

    public static void setLastKiller(ServerPlayerEntity player, UUID killerUuid) {
        PlayerStateManager.setPersistent(player, LAST_KILLER_KEY, killerUuid.toString());
    }

    public static boolean isNemesisTarget(ServerPlayerEntity attacker, LivingEntity target) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.BONUS_NEMESIS)) {
            return false;
        }
        String killerStr = PlayerStateManager.getPersistent(attacker, LAST_KILLER_KEY);
        if (killerStr == null || killerStr.isEmpty()) {
            return false;
        }
        try {
            UUID killerUuid = UUID.fromString(killerStr);
            return killerUuid.equals(target.getUuid());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ========== Attack Damage Multipliers ==========

    public static float getAttackDamageMultiplier(ServerPlayerEntity attacker, LivingEntity target, float baseDamage) {
        float multiplier = 1.0f;

        // Critical Strike - 20% chance for 2x damage
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_CRITICAL_STRIKE)) {
            if (attacker.getRandom().nextFloat() < 0.20f) {
                multiplier *= 2.0f;
            }
        }

        // Culling Blade - bonus damage to <25% HP targets
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_CULLING_BLADE)) {
            if (target.getHealth() < target.getMaxHealth() * 0.25f) {
                multiplier *= 1.5f; // +50% damage
            }
        }

        // Executioner - +30% damage to <25% HP targets
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_EXECUTIONER)) {
            if (target.getHealth() < target.getMaxHealth() * 0.25f) {
                multiplier *= 1.3f;
            }
        }

        // Last Stand - +50% damage when attacker is <25% HP
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_LAST_STAND)) {
            if (attacker.getHealth() < attacker.getMaxHealth() * 0.25f) {
                multiplier *= 1.5f;
            }
        }

        // Nemesis - +25% damage to last killer
        if (isNemesisTarget(attacker, target)) {
            multiplier *= 1.25f;
        }

        // Opportunist - +25% damage to enemies focused on someone else
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_OPPORTUNIST)) {
            if (target instanceof net.minecraft.entity.mob.MobEntity mob) {
                LivingEntity mobTarget = mob.getTarget();
                if (mobTarget != null && mobTarget != attacker) {
                    multiplier *= 1.25f;
                }
            }
        }

        // Hunter's Instinct - +50% crit chance vs fleeing enemies
        if (GemPowers.isPassiveActive(attacker, PowerIds.BONUS_HUNTERS_INSTINCT)) {
            Vec3d targetVel = target.getVelocity();
            Vec3d toAttacker = attacker.getEntityPos().subtract(target.getEntityPos()).normalize();
            // Check if target is moving away from attacker
            if (targetVel.length() > 0.1 && targetVel.normalize().dotProduct(toAttacker) < -0.3) {
                // Fleeing - guaranteed crit
                multiplier *= 1.5f;
            }
        }

        // Counter Strike - after blocking, 2x damage
        if (consumeCounterStrike(attacker)) {
            multiplier *= 2.0f;
        }

        // Vengeance - after taking damage, +50% damage
        if (consumeVengeance(attacker)) {
            multiplier *= 1.5f;
        }

        return multiplier;
    }

    // ========== Defense Damage Modifiers ==========

    public static float getDefenseDamageMultiplier(ServerPlayerEntity victim, float incomingDamage, ServerWorld world, LivingEntity attacker) {
        float scaled = incomingDamage;

        // Stone Skin - flat -1 HP reduction
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_STONE_SKIN)) {
            scaled = Math.max(0, scaled - 1.0f);
        }

        // Thick Skin - 10% damage reduction
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_THICK_SKIN)) {
            scaled *= 0.9f;
        }

        // Dodge Chance - 10% chance to avoid all damage
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_DODGE_CHANCE)) {
            if (victim.getRandom().nextFloat() < 0.10f) {
                return 0.0f;
            }
        }

        // Arcane Barrier - absorb first hit every 30s
        if (shouldTriggerArcaneBarrier(victim)) {
            consumeArcaneBarrier(victim);
            return 0.0f;
        }

        // Second Wind - survive killing blow
        if (scaled >= victim.getHealth() && shouldTriggerSecondWind(victim)) {
            consumeSecondWind(victim);
            victim.setHealth(1.0f);
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 4, false, true));
            return 0.0f;
        }

        // Trigger Vengeance when hit
        triggerVengeance(victim);

        // Thorns Aura - reflect 30% damage back
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_THORNS_AURA)) {
            if (attacker != null && attacker != victim) {
                float reflected = scaled * 0.30f;
                attacker.damage(world, victim.getDamageSources().thorns(victim), reflected);
            }
        }

        // War Cry - when hit, nearby allies get Strength I
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_WAR_CRY)) {
            Box searchBox = victim.getBoundingBox().expand(10);
            List<ServerPlayerEntity> nearbyAllies = world.getEntitiesByClass(
                ServerPlayerEntity.class,
                searchBox,
                p -> p != victim && p.isAlive()
            );
            for (ServerPlayerEntity ally : nearbyAllies) {
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 0, false, true));
            }
        }

        // Impact Absorb - convert 20% damage to absorption
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_IMPACT_ABSORB)) {
            float absorbed = scaled * 0.20f;
            scaled *= 0.80f;
            float currentAbsorption = victim.getAbsorptionAmount();
            victim.setAbsorptionAmount(Math.min(currentAbsorption + absorbed, 20.0f));
        }

        // Mana Shield - absorb 25% damage using XP
        if (GemPowers.isPassiveActive(victim, PowerIds.BONUS_MANA_SHIELD)) {
            int xpLevels = victim.experienceLevel;
            if (xpLevels > 0) {
                float absorbed = Math.min(scaled * 0.25f, xpLevels * 2.0f);
                int levelsUsed = (int) Math.ceil(absorbed / 2.0f);
                victim.addExperienceLevels(-levelsUsed);
                scaled -= absorbed;
            }
        }

        return scaled;
    }

    // ========== Lifesteal ==========
    private static final float LIFESTEAL_PERCENT = 15.0f; // 15% of damage dealt

    public static void applyLifesteal(ServerPlayerEntity attacker, float damageDealt) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.BONUS_LIFESTEAL)) {
            return;
        }
        float healAmount = damageDealt * LIFESTEAL_PERCENT / 100.0f;
        attacker.heal(healAmount);
    }

    // ========== Echo Strike ==========

    public static boolean shouldEchoStrike(ServerPlayerEntity attacker) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.BONUS_ECHO_STRIKE)) {
            return false;
        }
        return attacker.getRandom().nextFloat() < 0.15f;
    }

    // ========== Kill Event Handlers ==========

    public static void onKill(ServerPlayerEntity killer, LivingEntity victim) {
        // Bloodthirst - heal 4 HP on kill
        if (GemPowers.isPassiveActive(killer, PowerIds.BONUS_BLOODTHIRST)) {
            killer.heal(4.0f);
        }

        // Adrenaline Rush - Speed III for 5s on kill
        if (GemPowers.isPassiveActive(killer, PowerIds.BONUS_ADRENALINE_RUSH)) {
            killer.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 2, false, true));
        }
    }

    // ========== Tick Handlers ==========

    public static void tickEverySecond(ServerPlayerEntity player, ServerWorld world) {
        // Predator Sense - apply Glowing to nearby <30% HP enemies
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_PREDATOR_SENSE)) {
            Box searchBox = player.getBoundingBox().expand(20);
            List<LivingEntity> nearbyEntities = world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                e -> e != player && e.isAlive() && e.getHealth() < e.getMaxHealth() * 0.30f
            );
            for (LivingEntity entity : nearbyEntities) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 30, 0, false, false));
            }
        }

        // Battle Medic - heal nearby allies slowly
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_BATTLE_MEDIC)) {
            Box searchBox = player.getBoundingBox().expand(10);
            List<ServerPlayerEntity> nearbyAllies = world.getEntitiesByClass(
                ServerPlayerEntity.class,
                searchBox,
                p -> p != player && p.isAlive() && p.getHealth() < p.getMaxHealth()
            );
            for (ServerPlayerEntity ally : nearbyAllies) {
                ally.heal(0.5f); // 0.25 hearts per second
            }
        }

        // Berserker Blood - attack speed scales with low HP
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_BERSERKER_BLOOD)) {
            float healthPercent = player.getHealth() / player.getMaxHealth();
            int hasteLevel = 0;
            if (healthPercent < 0.75f) hasteLevel = 0;
            if (healthPercent < 0.50f) hasteLevel = 1;
            if (healthPercent < 0.25f) hasteLevel = 2;
            if (hasteLevel >= 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 30, hasteLevel, false, false));
            }
        }

        // Intimidate - debuff nearby enemies
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_INTIMIDATE)) {
            Box searchBox = player.getBoundingBox().expand(8);
            List<LivingEntity> nearbyEnemies = world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                e -> e != player && e.isAlive() && !(e instanceof PlayerEntity)
            );
            for (LivingEntity enemy : nearbyEnemies) {
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 0, false, false));
            }
        }

        // Spectral Form - 20% chance to lose aggro
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_SPECTRAL_FORM)) {
            if (player.getRandom().nextFloat() < 0.20f) {
                Box searchBox = player.getBoundingBox().expand(16);
                List<net.minecraft.entity.mob.MobEntity> nearbyMobs = world.getEntitiesByClass(
                    net.minecraft.entity.mob.MobEntity.class,
                    searchBox,
                    m -> m.getTarget() == player
                );
                for (net.minecraft.entity.mob.MobEntity mob : nearbyMobs) {
                    if (mob.getRandom().nextFloat() < 0.20f) {
                        mob.setTarget(null);
                    }
                }
            }
        }
    }

    // ========== Steel Resolve (Knockback Immunity) ==========

    public static boolean isKnockbackImmune(ServerPlayerEntity player) {
        return GemPowers.isPassiveActive(player, PowerIds.BONUS_STEEL_RESOLVE);
    }

    // ========== Focused Mind (Cooldown Reduction) ==========

    public static float getCooldownMultiplier(ServerPlayerEntity player) {
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_FOCUSED_MIND)) {
            return 0.85f; // 15% reduction
        }
        return 1.0f;
    }

    // ========== Bulwark (Shield Blocking) ==========

    public static float getBlockingDamageMultiplier(ServerPlayerEntity player) {
        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_BULWARK)) {
            return 0.5f; // Block absorbs 50% more
        }
        return 1.0f;
    }
}
