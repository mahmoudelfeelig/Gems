package com.feel.gems.gametest.passive;

import com.feel.gems.bonus.BonusClaimsState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Behavior tests for bonus passives that are implemented in runtime logic.
 */
public final class GemsBonusPassiveGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    private static ServerPlayerEntity setupBonusPlayer(TestContext context, Vec3d pos) {
        ServerWorld world = context.getWorld();
        GemsGameTestUtil.placeStoneFloor(context, 10);
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.resetPlayerForTest(player);
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.AIR);
        GemPlayerState.setEnergy(player, 10);
        GemPowers.sync(player);
        BonusClaimsState.get(world.getServer()).releaseAllClaims(player.getUuid());
        return player;
    }

    private static void claimBonusPassives(ServerPlayerEntity player, Identifier... passives) {
        ServerWorld world = player.getEntityWorld();
        BonusClaimsState claims = BonusClaimsState.get(world.getServer());
        claims.releaseAllClaims(player.getUuid());
        for (Identifier passiveId : passives) {
            UUID claimant = claims.getPassiveClaimant(passiveId);
            if (claimant != null && !claimant.equals(player.getUuid())) {
                claims.releasePassive(claimant, passiveId);
            }
            if (!claims.claimPassive(player.getUuid(), passiveId)) {
                throw new IllegalStateException("Failed to claim bonus passive " + passiveId);
            }
        }
        GemPowers.sync(player);
    }

    private static void addBonusPassive(ServerPlayerEntity player, Identifier passiveId) {
        ServerWorld world = player.getEntityWorld();
        BonusClaimsState claims = BonusClaimsState.get(world.getServer());
        UUID claimant = claims.getPassiveClaimant(passiveId);
        if (claimant != null && !claimant.equals(player.getUuid())) {
            claims.releasePassive(claimant, passiveId);
        }
        if (!claims.claimPassive(player.getUuid(), passiveId)) {
            throw new IllegalStateException("Failed to claim bonus passive " + passiveId);
        }
        GemPowers.sync(player);
    }

    private static void claimBonusPassive(ServerPlayerEntity player, Identifier passiveId) {
        ServerWorld world = player.getEntityWorld();
        BonusClaimsState claims = BonusClaimsState.get(world.getServer());
        // GameTests share a single server + persistent state. If a prior test failed mid-run, it can leave
        // claims behind and make later tests fail to claim. Prefer deterministic tests by force-releasing.
        UUID claimant = claims.getPassiveClaimant(passiveId);
        if (claimant != null && !claimant.equals(player.getUuid())) {
            claims.releasePassive(claimant, passiveId);
        }
        // Ensure this test player starts clean (max 2 passives).
        claims.releaseAllClaims(player.getUuid());
        if (!claims.claimPassive(player.getUuid(), passiveId)) {
            throw new IllegalStateException("Failed to claim bonus passive " + passiveId);
        }
        GemPowers.sync(player);
    }

    private static void releaseBonusPassive(ServerPlayerEntity player, Identifier passiveId) {
        BonusClaimsState claims = BonusClaimsState.get(player.getEntityWorld().getServer());
        claims.releasePassive(player.getUuid(), passiveId);
        GemPowers.sync(player);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusAdrenalineRushOnKillGrantsSpeed(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity killer = setupBonusPlayer(context, pos);
        ServerPlayerEntity victim = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(victim);
        teleport(victim, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        claimBonusPassive(killer, PowerIds.BONUS_ADRENALINE_RUSH);

        context.runAtTick(5L, () -> {
            BonusPassiveRuntime.onKill(killer, victim);
            if (killer.getStatusEffect(StatusEffects.SPEED) == null) {
                context.throwGameTestException("Adrenaline Rush should grant Speed on kill");
                return;
            }
            releaseBonusPassive(killer, PowerIds.BONUS_ADRENALINE_RUSH);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusAdrenalineSurgeAppliesSpeedAndHaste(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_ADRENALINE_SURGE);

        context.runAtTick(5L, () -> {
            if (player.getStatusEffect(StatusEffects.SPEED) == null || player.getStatusEffect(StatusEffects.HASTE) == null) {
                context.throwGameTestException("Adrenaline Surge should grant Speed and Haste");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_ADRENALINE_SURGE);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusArcaneBarrierAbsorbsFirstHit(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity victim = setupBonusPlayer(context, pos);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        claimBonusPassive(victim, PowerIds.BONUS_ARCANE_BARRIER);

        context.runAtTick(5L, () -> {
            float before = victim.getHealth();
            victim.damage(world, attacker.getDamageSources().playerAttack(attacker), 4.0F);
            if (victim.getHealth() < before) {
                context.throwGameTestException("Arcane Barrier should absorb the first hit");
                return;
            }
            victim.damage(world, attacker.getDamageSources().playerAttack(attacker), 4.0F);
            if (victim.getHealth() >= before) {
                context.throwGameTestException("Arcane Barrier should go on cooldown after the first hit");
                return;
            }
            releaseBonusPassive(victim, PowerIds.BONUS_ARCANE_BARRIER);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusAttackSpeedGrantsHaste(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        double before = player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
        claimBonusPassive(player, PowerIds.BONUS_ATTACK_SPEED);

        context.runAtTick(5L, () -> {
            double after = player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
            if (after <= before) {
                context.throwGameTestException("Attack Speed should increase attack speed attribute");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_ATTACK_SPEED);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBattleMedicHealsAllies(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity healer = setupBonusPlayer(context, pos);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(ally);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        ally.setHealth(10.0F);

        claimBonusPassive(healer, PowerIds.BONUS_BATTLE_MEDIC);
        GemTrust.trust(healer, ally.getUuid());

        context.runAtTick(5L, () -> {
            BonusPassiveRuntime.tickEverySecond(healer, world);
            if (ally.getHealth() <= 10.0F) {
                context.throwGameTestException("Battle Medic should heal nearby allies");
                return;
            }
            releaseBonusPassive(healer, PowerIds.BONUS_BATTLE_MEDIC);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBerserkerBloodAppliesHasteAtLowHealth(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_BERSERKER_BLOOD);
        player.setHealth(4.0F);

        context.runAtTick(5L, () -> {
            BonusPassiveRuntime.tickEverySecond(player, world);
            var haste = player.getStatusEffect(StatusEffects.HASTE);
            if (haste == null || haste.getAmplifier() < 1) {
                context.throwGameTestException("Berserker Blood should grant Haste at low health");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_BERSERKER_BLOOD);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBloodthirstHealsOnKill(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity killer = setupBonusPlayer(context, pos);
        ServerPlayerEntity victim = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(victim);
        teleport(victim, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        killer.setHealth(10.0F);

        claimBonusPassive(killer, PowerIds.BONUS_BLOODTHIRST);

        context.runAtTick(5L, () -> {
            BonusPassiveRuntime.onKill(killer, victim);
            if (killer.getHealth() <= 10.0F) {
                context.throwGameTestException("Bloodthirst should heal on kill");
                return;
            }
            releaseBonusPassive(killer, PowerIds.BONUS_BLOODTHIRST);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBulwarkIncreasesBlockingReduction(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_BULWARK);

        context.runAtTick(5L, () -> {
            float mult = BonusPassiveRuntime.getBlockingDamageMultiplier(player);
            if (mult >= 1.0F) {
                context.throwGameTestException("Bulwark should increase shield blocking reduction");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_BULWARK);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCriticalStrikeCanDoubleDamage(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, context.getWorld(), pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        claimBonusPassive(attacker, PowerIds.BONUS_CRITICAL_STRIKE);

        context.runAtTick(5L, () -> {
            if (!GemPowers.isPassiveActive(attacker, PowerIds.BONUS_CRITICAL_STRIKE)) {
                context.throwGameTestException("Critical Strike passive was not active after claiming");
                return;
            }
            attacker.getRandom().setSeed(123L);
            boolean crit = false;
            for (int i = 0; i < 500; i++) {
                float mult = BonusPassiveRuntime.getAttackDamageMultiplier(attacker, target, 4.0F);
                if (mult > 1.1F) {
                    crit = true;
                    break;
                }
            }
            if (!crit) {
                context.throwGameTestException("Critical Strike should sometimes increase damage");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_CRITICAL_STRIKE);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCullingBladeBoostsDamageToLowHealth(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, context.getWorld(), pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        float threshold = GemsBalance.v().bonusPool().cullingBladeThresholdPercent / 100.0F;
        target.setHealth(Math.max(1.0F, target.getMaxHealth() * threshold * 0.5F));
        claimBonusPassive(attacker, PowerIds.BONUS_CULLING_BLADE);

        context.runAtTick(5L, () -> {
            float baseDamage = 1.0F;
            float mult = BonusPassiveRuntime.getAttackDamageMultiplier(attacker, target, baseDamage);
            if (baseDamage * mult < target.getHealth() + 0.5F) {
                context.throwGameTestException("Culling Blade should provide enough damage to execute low-health targets");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_CULLING_BLADE);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusDamageReductionAppliesResistance(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_DAMAGE_REDUCTION);

        context.runAtTick(5L, () -> {
            if (player.getStatusEffect(StatusEffects.RESISTANCE) == null) {
                context.throwGameTestException("Damage Reduction should grant Resistance");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_DAMAGE_REDUCTION);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusDodgeChanceCanAvoidDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity victim = setupBonusPlayer(context, pos);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);
        claimBonusPassive(victim, PowerIds.BONUS_DODGE_CHANCE);

        context.runAtTick(5L, () -> {
            boolean dodged = false;
            for (int i = 0; i < 200; i++) {
                float before = victim.getHealth();
                victim.damage(world, attacker.getDamageSources().playerAttack(attacker), 2.0F);
                if (victim.getHealth() == before) {
                    dodged = true;
                    break;
                }
                victim.setHealth(Math.min(victim.getMaxHealth(), before));
            }
            if (!dodged) {
                context.throwGameTestException("Dodge Chance should occasionally avoid damage");
                return;
            }
            releaseBonusPassive(victim, PowerIds.BONUS_DODGE_CHANCE);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusEchoStrikeCanTrigger(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, context.getWorld(), pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        claimBonusPassive(attacker, PowerIds.BONUS_ECHO_STRIKE);

        context.runAtTick(5L, () -> {
            boolean triggered = false;
            for (int i = 0; i < 200; i++) {
                if (BonusPassiveRuntime.shouldEchoStrike(attacker)) {
                    triggered = true;
                    break;
                }
            }
            if (!triggered) {
                context.throwGameTestException("Echo Strike should occasionally trigger");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_ECHO_STRIKE);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusElementalHarmonyGrantsFireResistance(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_ELEMENTAL_HARMONY);

        context.runAtTick(5L, () -> {
            if (player.getStatusEffect(StatusEffects.FIRE_RESISTANCE) == null) {
                context.throwGameTestException("Elemental Harmony should grant Fire Resistance");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_ELEMENTAL_HARMONY);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusExecutionerBoostsDamageToLowHealth(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, context.getWorld(), pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        target.setHealth(4.0F);
        claimBonusPassive(attacker, PowerIds.BONUS_EXECUTIONER);

        context.runAtTick(5L, () -> {
            float mult = BonusPassiveRuntime.getAttackDamageMultiplier(attacker, target, 4.0F);
            if (mult <= 1.0F) {
                context.throwGameTestException("Executioner should boost damage to low-health targets");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_EXECUTIONER);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusFocusedMindReducesCooldowns(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_FOCUSED_MIND);

        context.runAtTick(5L, () -> {
            float mult = BonusPassiveRuntime.getCooldownMultiplier(player);
            if (mult >= 1.0F) {
                context.throwGameTestException("Focused Mind should reduce cooldowns");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_FOCUSED_MIND);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusHungerResistGrantsSaturation(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_HUNGER_RESIST);

        context.runAtTick(5L, () -> {
            if (player.getStatusEffect(StatusEffects.SATURATION) == null) {
                context.throwGameTestException("Hunger Resist should grant Saturation");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_HUNGER_RESIST);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusImpactAbsorbAddsAbsorption(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity victim = setupBonusPlayer(context, pos);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);
        claimBonusPassive(victim, PowerIds.BONUS_IMPACT_ABSORB);

        float[] beforeAbsorb = new float[1];
        float[] beforeHealth = new float[1];
        context.runAtTick(5L, () -> {
            if (!GemPowers.isPassiveActive(victim, PowerIds.BONUS_IMPACT_ABSORB)) {
                context.throwGameTestException("Impact Absorb passive was not active after claiming");
                return;
            }
            beforeAbsorb[0] = victim.getAbsorptionAmount();
            beforeHealth[0] = victim.getHealth();
            boolean dealt = victim.damage(world, attacker.getDamageSources().playerAttack(attacker), 4.0F);
            if (!dealt) {
                context.throwGameTestException("Impact Absorb test: damage was not dealt (invulnerable?)");
                return;
            }
            if (victim.getHealth() >= beforeHealth[0]) {
                context.throwGameTestException("Impact Absorb test: health did not decrease");
                return;
            }
            // Check absorption was applied immediately in the same tick
            if (victim.getAbsorptionAmount() <= beforeAbsorb[0]) {
                context.throwGameTestException("Impact Absorb should grant absorption immediately");
                return;
            }
        });

        context.runAtTick(10L, () -> {
            // Recheck on tick 10 to ensure absorption persists
            if (victim.getAbsorptionAmount() <= beforeAbsorb[0]) {
                context.throwGameTestException("Impact Absorb should grant absorption on hit");
                return;
            }
            releaseBonusPassive(victim, PowerIds.BONUS_IMPACT_ABSORB);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusIntimidateWeakensNearbyMobs(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_INTIMIDATE);

        Vec3d mobPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 4.0D));
        MobEntity zombie = EntityType.ZOMBIE.create(world, e -> {}, BlockPos.ofFloored(mobPos), SpawnReason.TRIGGERED, false, false);
        if (zombie == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        zombie.refreshPositionAndAngles(mobPos.x, mobPos.y, mobPos.z, 0.0F, 0.0F);
        world.spawnEntity(zombie);

        context.runAtTick(5L, () -> {
            BonusPassiveRuntime.tickEverySecond(player, world);
            if (zombie.getStatusEffect(StatusEffects.WEAKNESS) == null) {
                context.throwGameTestException("Intimidate should apply Weakness to nearby mobs");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_INTIMIDATE);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusIroncladIncreasesArmorAttribute(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        double[] before = new double[1];

        context.runAtTick(5L, () -> player.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE)));
        context.runAtTick(15L, () -> before[0] = player.getAttributeValue(EntityAttributes.ARMOR));
        context.runAtTick(20L, () -> claimBonusPassive(player, PowerIds.BONUS_IRONCLAD));

        context.runAtTick(35L, () -> {
            if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_IRONCLAD)) {
                context.throwGameTestException("Ironclad passive was not active after claiming");
                return;
            }
            var armor = player.getAttributeInstance(EntityAttributes.ARMOR);
            if (armor == null || armor.getModifier(net.minecraft.util.Identifier.of("gems", "bonus_ironclad")) == null) {
                context.throwGameTestException("Ironclad should apply an armor modifier");
                return;
            }
            double after = player.getAttributeValue(EntityAttributes.ARMOR);
            if (after <= before[0]) {
                context.throwGameTestException("Ironclad should increase armor attribute");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_IRONCLAD);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusLastStandBoostsDamageWhenLowHealth(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, context.getWorld(), pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        attacker.setHealth(4.0F);
        claimBonusPassive(attacker, PowerIds.BONUS_LAST_STAND);

        context.runAtTick(5L, () -> {
            float mult = BonusPassiveRuntime.getAttackDamageMultiplier(attacker, target, 4.0F);
            if (mult <= 1.0F) {
                context.throwGameTestException("Last Stand should boost damage at low health");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_LAST_STAND);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusLifestealHealsOnDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        attacker.setHealth(10.0F);
        claimBonusPassive(attacker, PowerIds.BONUS_LIFESTEAL);

        context.runAtTick(5L, () -> {
            float before = attacker.getHealth();
            target.damage(world, attacker.getDamageSources().playerAttack(attacker), 4.0F);
            if (attacker.getHealth() <= before) {
                context.throwGameTestException("Lifesteal should heal the attacker");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_LIFESTEAL);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusManaShieldConsumesXp(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity victim = setupBonusPlayer(context, pos);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);
        victim.experienceLevel = 10;
        claimBonusPassive(victim, PowerIds.BONUS_MANA_SHIELD);

        context.runAtTick(5L, () -> {
            int before = victim.experienceLevel;
            victim.damage(world, attacker.getDamageSources().playerAttack(attacker), 6.0F);
            if (victim.experienceLevel >= before) {
                context.throwGameTestException("Mana Shield should consume XP to absorb damage");
                return;
            }
            releaseBonusPassive(victim, PowerIds.BONUS_MANA_SHIELD);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusNemesisBoostsDamageToLastKiller(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, context.getWorld(), pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        claimBonusPassive(attacker, PowerIds.BONUS_NEMESIS);

        context.runAtTick(5L, () -> {
            BonusPassiveRuntime.setLastKiller(attacker, target.getUuid());
            float mult = BonusPassiveRuntime.getAttackDamageMultiplier(attacker, target, 4.0F);
            if (mult <= 1.0F) {
                context.throwGameTestException("Nemesis should boost damage against the last killer");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_NEMESIS);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusOpportunistBoostsDamageVsDistractedMob(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(ally);
        teleport(ally, world, pos.x + 6.0D, pos.y, pos.z, 0.0F, 0.0F);
        claimBonusPassive(attacker, PowerIds.BONUS_OPPORTUNIST);

        MobEntity zombie = EntityType.ZOMBIE.create(world, e -> {}, BlockPos.ofFloored(pos.add(3.0D, 0.0D, 0.0D)), SpawnReason.TRIGGERED, false, false);
        if (zombie == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        zombie.refreshPositionAndAngles(pos.x + 3.0D, pos.y, pos.z, -90.0F, 0.0F);
        zombie.setTarget(ally);
        world.spawnEntity(zombie);

        context.runAtTick(5L, () -> {
            float mult = BonusPassiveRuntime.getAttackDamageMultiplier(attacker, zombie, 4.0F);
            if (mult <= 1.0F) {
                context.throwGameTestException("Opportunist should boost damage when target is focused elsewhere");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_OPPORTUNIST);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusOverflowingVitalityIncreasesMaxHealth(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        double before = player.getAttributeValue(EntityAttributes.MAX_HEALTH);
        claimBonusPassive(player, PowerIds.BONUS_OVERFLOWING_VITALITY);

        context.runAtTick(5L, () -> {
            double after = player.getAttributeValue(EntityAttributes.MAX_HEALTH);
            if (after <= before) {
                context.throwGameTestException("Overflowing Vitality should increase max health");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_OVERFLOWING_VITALITY);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusPredatorSenseGlowsLowHealthEnemies(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_PREDATOR_SENSE);

        MobEntity zombie = EntityType.ZOMBIE.create(world, e -> {}, BlockPos.ofFloored(pos.add(3.0D, 0.0D, 0.0D)), SpawnReason.TRIGGERED, false, false);
        if (zombie == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        zombie.refreshPositionAndAngles(pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);
        zombie.setHealth(4.0F);
        world.spawnEntity(zombie);

        context.runAtTick(5L, () -> {
            BonusPassiveRuntime.tickEverySecond(player, world);
            if (zombie.getStatusEffect(StatusEffects.GLOWING) == null) {
                context.throwGameTestException("Predator Sense should apply Glowing to low-health enemies");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_PREDATOR_SENSE);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusRegenerationBoostGrantsRegen(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_REGENERATION_BOOST);

        context.runAtTick(5L, () -> {
            if (player.getStatusEffect(StatusEffects.REGENERATION) == null) {
                context.throwGameTestException("Regeneration Boost should grant Regeneration");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_REGENERATION_BOOST);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSecondWindPreventsLethalDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity victim = setupBonusPlayer(context, pos);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);
        victim.setHealth(2.0F);
        claimBonusPassive(victim, PowerIds.BONUS_SECOND_WIND);

        context.runAtTick(5L, () -> {
            victim.damage(world, attacker.getDamageSources().playerAttack(attacker), 6.0F);
            if (victim.getHealth() <= 0.0F) {
                context.throwGameTestException("Second Wind should prevent lethal damage once");
                return;
            }
            releaseBonusPassive(victim, PowerIds.BONUS_SECOND_WIND);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSpectralFormCanPhaseThroughDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_SPECTRAL_FORM);

        context.runAtTick(5L, () -> {
            player.getRandom().setSeed(123L);
            boolean phased = false;
            for (int i = 0; i < 200; i++) {
                float scaled = BonusPassiveRuntime.getDefenseDamageMultiplier(player, 4.0F, world, null);
                if (scaled <= 0.0F) {
                    phased = true;
                    break;
                }
            }
            if (!phased) {
                context.throwGameTestException("Spectral Form should sometimes phase through attacks");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_SPECTRAL_FORM);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSteelResolveKnockbackImmunityFlag(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupBonusPlayer(context, pos);
        claimBonusPassive(player, PowerIds.BONUS_STEEL_RESOLVE);

        context.runAtTick(5L, () -> {
            if (!BonusPassiveRuntime.isKnockbackImmune(player)) {
                context.throwGameTestException("Steel Resolve should mark player as knockback immune");
                return;
            }
            releaseBonusPassive(player, PowerIds.BONUS_STEEL_RESOLVE);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusStoneSkinReducesDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity victim = setupBonusPlayer(context, pos);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);
        claimBonusPassive(victim, PowerIds.BONUS_STONE_SKIN);

        context.runAtTick(5L, () -> {
            float before = victim.getHealth();
            victim.damage(world, attacker.getDamageSources().playerAttack(attacker), 4.0F);
            float taken = before - victim.getHealth();
            if (taken >= 4.0F) {
                context.throwGameTestException("Stone Skin should reduce incoming damage");
                return;
            }
            releaseBonusPassive(victim, PowerIds.BONUS_STONE_SKIN);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusThickSkinReducesDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity victim = setupBonusPlayer(context, pos);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);
        claimBonusPassive(victim, PowerIds.BONUS_THICK_SKIN);

        context.runAtTick(5L, () -> {
            if (!GemPowers.isPassiveActive(victim, PowerIds.BONUS_THICK_SKIN)) {
                context.throwGameTestException("Thick Skin passive was not active after claiming");
                return;
            }
            net.minecraft.entity.projectile.ArrowEntity arrow = EntityType.ARROW.create(world, SpawnReason.TRIGGERED);
            if (arrow == null) {
                context.throwGameTestException("Failed to create arrow");
                return;
            }
            arrow.setOwner(attacker);
            arrow.refreshPositionAndAngles(attacker.getX(), attacker.getEyeY(), attacker.getZ(), attacker.getYaw(), attacker.getPitch());
            world.spawnEntity(arrow);
            float before = victim.getHealth();
            victim.damage(world, world.getDamageSources().arrow(arrow, attacker), 4.0F);
            float taken = before - victim.getHealth();
            float reduction = GemsBalance.v().bonusPool().thickSkinProjectileReductionPercent() / 100.0F;
            float max = 4.0F * (1.0F - reduction) + 0.1F;
            if (taken > max) {
                context.throwGameTestException("Thick Skin should reduce projectile damage");
                return;
            }
            releaseBonusPassive(victim, PowerIds.BONUS_THICK_SKIN);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusThornsAuraReflectsDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity victim = setupBonusPlayer(context, pos);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);
        claimBonusPassive(victim, PowerIds.BONUS_THORNS_AURA);

        context.runAtTick(5L, () -> {
            float attackerBefore = attacker.getHealth();
            victim.damage(world, attacker.getDamageSources().playerAttack(attacker), 4.0F);
            if (attacker.getHealth() >= attackerBefore) {
                context.throwGameTestException("Thorns Aura should reflect damage to attackers");
                return;
            }
            releaseBonusPassive(victim, PowerIds.BONUS_THORNS_AURA);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusWarCryBuffsAlliesOnHit(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity killer = setupBonusPlayer(context, pos);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(ally);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        GemTrust.trust(killer, ally.getUuid());
        claimBonusPassive(killer, PowerIds.BONUS_WAR_CRY);

        context.runAtTick(5L, () -> {
            BonusPassiveRuntime.onKill(killer, ally);
            if (ally.getStatusEffect(StatusEffects.STRENGTH) == null) {
                context.throwGameTestException("War Cry should grant Strength to nearby allies");
                return;
            }
            releaseBonusPassive(killer, PowerIds.BONUS_WAR_CRY);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusVengeanceBoostsNextAttack(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity hitter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        GemsGameTestUtil.forceSurvival(hitter);
        teleport(target, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        teleport(hitter, world, pos.x + 2.0D, pos.y, pos.z + 2.0D, 180.0F, 0.0F);
        claimBonusPassive(attacker, PowerIds.BONUS_VENGEANCE);

        context.runAtTick(5L, () -> {
            attacker.damage(world, hitter.getDamageSources().playerAttack(hitter), 2.0F);
            float mult = BonusPassiveRuntime.getAttackDamageMultiplier(attacker, target, 4.0F);
            if (mult <= 1.0F) {
                context.throwGameTestException("Vengeance should boost the next attack after taking damage");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_VENGEANCE);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusHuntersInstinctBoostsDamageToFleeingTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, world, pos.x + 2.0D, pos.y, pos.z, -90.0F, 0.0F);
        claimBonusPassives(attacker, PowerIds.BONUS_CRITICAL_STRIKE, PowerIds.BONUS_HUNTERS_INSTINCT);

        GemsGameTestUtil.assertEventually(
                context,
                20L,
                80L,
                5L,
                () -> {
                    teleport(target, world, pos.x + 2.0D, pos.y, pos.z, -90.0F, 0.0F);
                    target.setSprinting(true);
                    target.setVelocity(1.0D, 0.0D, 0.0D);

                    float chance = BonusPassiveRuntime.getCriticalStrikeChance(attacker, target);
                    float baseline = GemsBalance.v().bonusPool().criticalStrikeChanceBonus / 100.0f;
                    float expected = baseline + (GemsBalance.v().bonusPool().huntersInstinctCritBoostPercent / 100.0f);
                    return chance >= expected - 0.001f;
                },
                "Hunter's Instinct should increase crit chance against fleeing targets"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCounterStrikeBoostsNextAttack(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity attacker = setupBonusPlayer(context, pos);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, context.getWorld(), pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        claimBonusPassive(attacker, PowerIds.BONUS_COUNTER_STRIKE);

        context.runAtTick(5L, () -> {
            BonusPassiveRuntime.triggerCounterStrike(attacker);
            float mult = BonusPassiveRuntime.getAttackDamageMultiplier(attacker, target, 4.0F);
            if (mult <= 1.0F) {
                context.throwGameTestException("Counter Strike should boost the next attack after blocking");
                return;
            }
            releaseBonusPassive(attacker, PowerIds.BONUS_COUNTER_STRIKE);
            context.complete();
        });
    }
}
