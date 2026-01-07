package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.air.AirDashAbility;
import com.feel.gems.power.ability.air.AirCrosswindAbility;
import com.feel.gems.power.ability.air.AirWindJumpAbility;
import com.feel.gems.power.ability.beacon.BeaconAuraAbility;
import com.feel.gems.power.ability.pillager.PillagerFangsAbility;
import com.feel.gems.power.ability.pillager.PillagerVolleyAbility;
import com.feel.gems.power.ability.spy.SpyMimicFormAbility;
import com.feel.gems.power.ability.spy.SpyStealAbility;
import com.feel.gems.power.ability.summoner.SummonRecallAbility;
import com.feel.gems.power.ability.summoner.SummonSlotAbility;
import com.feel.gems.power.ability.terror.PanicRingAbility;
import com.feel.gems.power.gem.beacon.BeaconAuraRuntime;
import com.feel.gems.power.gem.pillager.PillagerDiscipline;
import com.feel.gems.power.gem.pillager.PillagerVolleyRuntime;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityDisables;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;




public final class GemsAbilityGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astralCameraReturnsToStart(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        BlockPos startBlock = BlockPos.ofFloored(startPos);
        teleport(player, world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        AbilityRuntime.startAstralCamera(player, 20);

        Vec3d movedPos = context.getAbsolute(new Vec3d(6.5D, 2.0D, 0.5D));
        teleport(player, world, movedPos.x, movedPos.y, movedPos.z, 90.0F, 0.0F);

        context.runAtTick(80L, () -> {
            if (!player.getBlockPos().equals(startBlock)) {
                context.throwGameTestException("Astral Camera did not return player to start position");
            }
            if (player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
                context.throwGameTestException("Astral Camera did not restore original gamemode");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void heartLockDoesNotOscillate(TestContext context) {
        ServerWorld world = context.getWorld();

        ServerPlayerEntity caster = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        caster.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        target.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(caster, world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);
        teleport(target, world, startPos.x + 1.0D, startPos.y, startPos.z, 0.0F, 0.0F);

        target.setHealth(6.0F); // lock to 3 hearts (6 health points)
        AbilityRuntime.startHeartLock(caster, target, 120);

        float lockedMax = target.getHealth();
        long[] checks = new long[]{25L, 45L, 65L, 85L};
        for (long tick : checks) {
            context.runAtTick(tick, () -> {
                double max = target.getAttributeValue(EntityAttributes.MAX_HEALTH);
                if (Math.abs(max - lockedMax) > 0.01D) {
                    context.throwGameTestException("Heart Lock oscillated: expected max=" + lockedMax + " got max=" + max);
                }
            });
        }
        context.runAtTick(110L, context::complete);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void panicRingSpawnsConfiguredTnt(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        int expected = GemsBalance.v().terror().panicRingTntCount();
        context.runAtTick(2L, () -> {
            boolean ok = new PanicRingAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Panic Ring did not activate");
            }
        });

        context.runAtTick(10L, () -> {
            Box box = new Box(player.getBlockPos()).expand(6.0D);
            int found = world.getEntitiesByClass(TntEntity.class, box, e -> true).size();
            if (found < expected) {
                context.throwGameTestException("Expected at least " + expected + " primed TNT, found " + found);
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 400)
    public void summonerSummonsHaveNoDropsAndRecallWorks(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SUMMONER);
        GemPlayerState.setEnergy(player, 10);
        GemPowers.sync(player);

        int expected = GemsBalance.v().summoner().slot1().stream().mapToInt(s -> s.count).sum();
        context.runAtTick(2L, () -> {
            boolean ok = new SummonSlotAbility(1).activate(player);
            if (!ok) {
                context.throwGameTestException("Summon slot 1 did not activate");
            }
        });

        context.runAtTick(20L, () -> {
            Box box = new Box(player.getBlockPos()).expand(12.0D);
            var summons = world.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, box,
                    e -> SummonerSummons.isSummon(e) && player.getUuid().equals(SummonerSummons.ownerUuid(e)));
            if (summons.size() < expected) {
                context.throwGameTestException("Expected at least " + expected + " summons, found " + summons.size());
            }

            // Kill one summon and ensure it drops no items or XP.
            var mob = summons.getFirst();
            mob.damage(world, mob.getDamageSources().outOfWorld(), 10_000.0F);

            context.runAtTick(40L, () -> {
                Box lootBox = new Box(player.getBlockPos()).expand(12.0D);
                int items = world.getEntitiesByClass(ItemEntity.class, lootBox, e -> true).size();
                int xp = world.getEntitiesByClass(ExperienceOrbEntity.class, lootBox, e -> true).size();
                if (items > 0) {
                    context.throwGameTestException("Summons dropped items unexpectedly (" + items + ")");
                }
                if (xp > 0) {
                    context.throwGameTestException("Summons dropped XP unexpectedly (" + xp + ")");
                }

                boolean recalled = new SummonRecallAbility().activate(player);
                if (!recalled) {
                    context.throwGameTestException("Recall did not activate");
                }
                context.runAtTick(60L, () -> {
                    int after = world.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, lootBox,
                            e -> SummonerSummons.isSummon(e) && player.getUuid().equals(SummonerSummons.ownerUuid(e))).size();
                    if (after != 0) {
                        context.throwGameTestException("Expected all summons to be recalled, remaining=" + after);
                    }
                    context.complete();
                });
            });
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerFangsSpawnsFangs(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        Vec3d targetPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 1.5D));
        var target = EntityType.ARMOR_STAND.create(world, armorStand -> { }, BlockPos.ofFloored(targetPos), SpawnReason.TRIGGERED, false, false);
        if (target == null) {
            context.throwGameTestException("Failed to create target entity");
            return;
        }
        target.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F);
        target.setNoGravity(true);
        world.spawnEntity(target);

        // Aim at the target.
        Vec3d aimAt = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        double dx = aimAt.x - player.getX();
        double dz = aimAt.z - player.getZ();
        double dy = aimAt.y - player.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        teleport(player, world, startPos.x, startPos.y, startPos.z, yaw, pitch);

        context.runAtTick(5L, () -> {
            boolean activated = new PillagerFangsAbility().activate(player);
            if (!activated) {
                context.throwGameTestException("Fangs did not activate");
                return;
            }

            Box box = new Box(target.getBlockPos()).expand(24.0D);
            int fangs = world.getEntitiesByClass(EvokerFangsEntity.class, box, e -> true).size();
            if (fangs <= 0) {
                context.throwGameTestException("Expected fangs entities to be spawned");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 220)
    public void spyStillnessCloakAppliesInvisibility(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.SPY_MIMIC);
            GemPlayerState.setEnergy(player, 5);
        });

        // Tick the stillness cloak logic deterministically.
        for (long t = 20L; t <= 140L; t += 20L) {
            long at = t;
            context.runAtTick(at, () -> SpyMimicSystem.tickEverySecond(player));
        }

        context.runAtTick(160L, () -> {
            if (!player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                context.throwGameTestException("Expected Stillness Cloak to apply invisibility");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyStealDisablesVictimAbility(TestContext context) {
        ServerWorld world = context.getWorld();
        var server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }

        ServerPlayerEntity spy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity victim = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        spy.changeGameMode(GameMode.SURVIVAL);
        victim.changeGameMode(GameMode.SURVIVAL);

        Vec3d spyPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        Vec3d victimPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 1.5D));
        // Yaw 0 faces toward +Z (victim is in front).
        teleport(spy, world, spyPos.x, spyPos.y, spyPos.z, 0.0F, 0.0F);
        teleport(victim, world, victimPos.x, victimPos.y, victimPos.z, 180.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(spy);
            GemPlayerState.setActiveGem(spy, GemId.SPY_MIMIC);
            GemPlayerState.setEnergy(spy, 5);

            GemPlayerState.initIfNeeded(victim);
            GemPlayerState.setActiveGem(victim, GemId.ASTRA);
            GemPlayerState.setEnergy(victim, 5);
        });

        Identifier stolen = com.feel.gems.power.registry.PowerIds.ASTRAL_DAGGERS;

        context.runAtTick(20L, () -> {
            // Simulate the victim casting an ability in front of the spy enough times.
            for (int i = 0; i < GemsBalance.v().spyMimic().stealRequiredWitnessCount(); i++) {
                SpyMimicSystem.onAbilityUsed(server, victim, stolen);
            }

            boolean ok = new SpyStealAbility().activate(spy);
            if (!ok) {
                context.throwGameTestException("Steal did not activate");
                return;
            }
            if (!AbilityDisables.isDisabled(victim, stolen)) {
                context.throwGameTestException("Expected victim ability to be disabled after steal");
                return;
            }
            if (!stolen.equals(SpyMimicSystem.selectedStolenAbility(spy))) {
                context.throwGameTestException("Expected stolen ability to be selected after steal");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airDashAppliesVelocityAndIFrames(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(2L, () -> {
            boolean ok = new AirDashAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Air Dash did not activate");
            }
        });

        context.runAtTick(8L, () -> {
            Vec3d vel = player.getVelocity();
            if (vel.lengthSquared() < 0.01D) {
                context.throwGameTestException("Dash did not apply forward velocity");
                return;
            }
            var res = player.getStatusEffect(StatusEffects.RESISTANCE);
            if (res == null || res.getDuration() <= 0) {
                context.throwGameTestException("Dash did not grant temporary i-frames");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airWindJumpResetsFallAndLaunchesUp(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(2L, () -> {
            player.fallDistance = 10.0F;
            boolean ok = new AirWindJumpAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Wind Jump did not activate");
            }
        });

        context.runAtTick(8L, () -> {
            if (player.fallDistance != 0.0F) {
                context.throwGameTestException("Wind Jump did not reset fall distance");
                return;
            }
            if (player.getVelocity().y <= 0.0D) {
                context.throwGameTestException("Wind Jump did not add upward velocity");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airCrosswindSlowsAndKnocksEnemies(TestContext context) {
        ServerWorld world = context.getWorld();

        ServerPlayerEntity caster = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        caster.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ally.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        enemy.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(caster, world, pos.x, pos.y, pos.z, 90.0F, 0.0F);
        teleport(ally, world, pos.x + 1.0D, pos.y, pos.z + 1.0D, 90.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 270.0F, 0.0F);

        GemTrust.trust(caster, ally.getUuid());
        final float enemyHealthBefore = enemy.getHealth();
        final float allyHealthBefore = ally.getHealth();
        AtomicBoolean knockbackSeen = new AtomicBoolean(false);

        context.runAtTick(2L, () -> {
            boolean ok = new AirCrosswindAbility().activate(caster);
            if (!ok) {
                context.throwGameTestException("Crosswind did not activate");
            }
        });

        context.runAtTick(4L, () -> {
            if (enemy.getVelocity().lengthSquared() > 0.0D) {
                knockbackSeen.set(true);
            }
        });

        context.runAtTick(10L, () -> {
            var cfg = GemsBalance.v().air();
            if (ally.hasStatusEffect(StatusEffects.SLOWNESS)) {
                context.throwGameTestException("Trusted ally should not receive slowness");
                return;
            }
            if (ally.getHealth() < allyHealthBefore) {
                context.throwGameTestException("Trusted ally should not take damage from crosswind");
                return;
            }
            boolean damageActive = cfg.crosswindDamage() > 0.0F;
            boolean slowActive = cfg.crosswindSlownessDurationTicks() > 0;
            boolean knockbackActive = cfg.crosswindKnockback() > 0.0D;
            boolean sawDamage = enemy.getHealth() < enemyHealthBefore;
            boolean sawSlow = enemy.hasStatusEffect(StatusEffects.SLOWNESS);
            boolean sawKnockback = knockbackSeen.get();

            if (!damageActive && !slowActive && !knockbackActive) {
                context.complete();
                return;
            }
            if ((damageActive && sawDamage) || (slowActive && sawSlow) || (knockbackActive && sawKnockback)) {
                context.complete();
                return;
            }

            context.throwGameTestException(
                "Crosswind did not affect enemy. "
                    + "damage=" + sawDamage
                    + " slow=" + sawSlow
                    + " knockback=" + sawKnockback
                    + " cfgDamage=" + cfg.crosswindDamage()
                    + " cfgSlow=" + cfg.crosswindSlownessDurationTicks()
                    + " cfgKnockback=" + cfg.crosswindKnockback());
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 220)
    public void beaconAuraAppliesOnlyToTrusted(TestContext context) {
        ServerWorld world = context.getWorld();

        ServerPlayerEntity beacon = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        beacon.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity trusted = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        trusted.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity untrusted = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        untrusted.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(beacon, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(trusted, world, pos.x + 1.5D, pos.y, pos.z, 0.0F, 0.0F);
        teleport(untrusted, world, pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(beacon);
        GemPlayerState.setActiveGem(beacon, GemId.BEACON);
        GemPlayerState.setEnergy(beacon, 10);
        GemPowers.sync(beacon);
        GemTrust.trust(beacon, trusted.getUuid());

        context.runAtTick(4L, () -> {
            boolean ok = new BeaconAuraAbility(BeaconAuraRuntime.AuraType.SPEED).activate(beacon);
            if (!ok) {
                context.throwGameTestException("Beacon aura did not activate");
            }
        });

        for (long tick = 8L; tick <= 32L; tick += 8L) {
            long at = tick;
            context.runAtTick(at, () -> BeaconAuraRuntime.tickEverySecond(beacon));
        }

        context.runAtTick(40L, () -> {
            var eff = trusted.getStatusEffect(StatusEffects.SPEED);
            if (eff == null) {
                context.throwGameTestException("Trusted player did not receive beacon aura");
                return;
            }
            int expectedAmp = GemsBalance.v().beacon().auraSpeedAmplifier();
            if (eff.getAmplifier() != expectedAmp) {
                context.throwGameTestException("Unexpected aura amplifier: " + eff.getAmplifier() + " expected=" + expectedAmp);
                return;
            }
            if (untrusted.hasStatusEffect(StatusEffects.SPEED)) {
                context.throwGameTestException("Untrusted player should not receive beacon aura");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 260)
    public void pillagerVolleyFiresAndStopsWhenEnergyGone(TestContext context) {
        ServerWorld world = context.getWorld();
        var server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }

        final int[] arrowsAtStop = new int[1];

        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 10);
        GemPowers.sync(player);

        context.runAtTick(2L, () -> {
            boolean ok = new PillagerVolleyAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Pillager Volley did not activate");
            }
        });

        for (long tick = 5L; tick <= 80L; tick += 10L) {
            long at = tick;
            context.runAtTick(at, () -> PillagerVolleyRuntime.tick(server));
        }

        context.runAtTick(90L, () -> {
            Box box = new Box(player.getBlockPos()).expand(24.0D);
            int arrows = world.getEntitiesByClass(ArrowEntity.class, box, e -> true).size();
            if (arrows <= 0) {
                context.throwGameTestException("Volley did not spawn arrows");
                return;
            }
            arrowsAtStop[0] = arrows;
        });

        context.runAtTick(100L, () -> GemPlayerState.setEnergy(player, 0));
        for (long tick = 110L; tick <= 200L; tick += 20L) {
            long at = tick;
            context.runAtTick(at, () -> PillagerVolleyRuntime.tick(server));
        }

        context.runAtTick(220L, () -> {
            Box box = new Box(player.getBlockPos()).expand(24.0D);
            int arrowsAfter = world.getEntitiesByClass(ArrowEntity.class, box, e -> true).size();
            // Energy drop should stop spawning new arrows; count should stay stable after the stop tick window.
            if (arrowsAfter != arrowsAtStop[0]) {
                context.throwGameTestException("Volley continued firing after energy hit zero (before=" + arrowsAtStop[0] + " after=" + arrowsAfter + ")");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerDisciplineTriggersBelowThreshold(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 10);

        context.runAtTick(2L, () -> {
            player.setHealth(20.0F);
            PillagerDiscipline.tick(player);
            if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                context.throwGameTestException("Discipline should not trigger at high health");
            }
            player.setHealth(6.0F);
            PillagerDiscipline.tick(player);
        });

        context.runAtTick(40L, () -> {
            var res = player.getStatusEffect(StatusEffects.RESISTANCE);
            if (res == null || res.getDuration() <= 0) {
                context.throwGameTestException("Discipline did not grant resistance when low health");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 340)
    public void spyMimicFormAppliesAndCleansUp(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity spy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        spy.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(spy, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        var pig = EntityType.PIG.create(world, e -> { }, BlockPos.ofFloored(pos), SpawnReason.TRIGGERED, false, false);
        if (pig == null) {
            context.throwGameTestException("Failed to spawn pig to mimic");
            return;
        }
        pig.refreshPositionAndAngles(spy.getX(), spy.getY(), spy.getZ(), 0.0F, 0.0F);
        world.spawnEntity(pig);

        GemPlayerState.initIfNeeded(spy);
        GemPlayerState.setActiveGem(spy, GemId.SPY_MIMIC);
        GemPlayerState.setEnergy(spy, 10);

        context.runAtTick(10L, () -> {
            SpyMimicSystem.recordLastKilledMob(spy, pig);
            pig.discard();
            boolean ok = new SpyMimicFormAbility().activate(spy);
            if (!ok) {
                context.throwGameTestException("Mimic Form did not activate after recording kill");
            }
        });

        context.runAtTick(20L, () -> {
            double baseMax = spy.getAttributeValue(EntityAttributes.MAX_HEALTH);
            spy.damage(world, spy.getDamageSources().outOfWorld(), 0.0F); // force attribute sync
            if (baseMax <= 20.0D) {
                context.throwGameTestException("Mimic Form did not increase max health");
                return;
            }
            if (!spy.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                context.throwGameTestException("Mimic Form should grant invisibility during the form");
                return;
            }
        });

        for (long tick = 40L; tick <= 260L; tick += 20L) {
            long at = tick;
            context.runAtTick(at, () -> SpyMimicSystem.tickEverySecond(spy));
        }

        context.runAtTick(300L, () -> {
            double after = spy.getAttributeValue(EntityAttributes.MAX_HEALTH);
            if (after > 20.1D) {
                context.throwGameTestException("Mimic Form buffs did not clear");
                return;
            }
            if (spy.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                context.throwGameTestException("Mimic Form invisibility should expire after duration");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyStealRespectsWitnessCountEvenOnColdCache(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity spy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity caster = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        spy.changeGameMode(GameMode.SURVIVAL);
        caster.changeGameMode(GameMode.SURVIVAL);

        Vec3d spyPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        Vec3d casterPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 6.5D));
        teleport(spy, world, spyPos.x, spyPos.y, spyPos.z, 0.0F, 0.0F); // yaw 0 faces +Z
        teleport(caster, world, casterPos.x, casterPos.y, casterPos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(spy);
        GemPlayerState.setActiveGem(spy, GemId.SPY_MIMIC);
        GemPlayerState.setEnergy(spy, 10);

        Identifier observed = PowerIds.FIREBALL;
        int required = GemsBalance.v().spyMimic().stealRequiredWitnessCount();
        long now = GemsTime.now(world);
        for (int i = 0; i < required; i++) {
            SpyMimicSystem.onAbilityUsed(world.getServer(), caster, observed);
        }

        int seen = SpyMimicSystem.witnessedCount(spy, observed);
        if (seen < required) {
            context.throwGameTestException("Spy did not record required observations; saw=" + seen + " required=" + required);
            return;
        }
        if (!SpyMimicSystem.canSteal(spy, observed, now)) {
            context.throwGameTestException("Spy cannot steal after required observations (cold cache path)");
            return;
        }

        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void summonerRecallCleansSummons(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity owner = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        owner.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        var zombie = EntityType.ZOMBIE.create(world, e -> { }, BlockPos.ofFloored(pos), SpawnReason.TRIGGERED, false, false);
        if (zombie == null) {
            context.throwGameTestException("Failed to create zombie summon");
            return;
        }
        zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        zombie.setPersistent();
        world.spawnEntity(zombie);

        long until = GemsTime.now(owner) + 200;
        SummonerSummons.mark(zombie, owner.getUuid(), until);
        SummonerSummons.trackSpawn(owner, zombie);

        int tracked = SummonerSummons.pruneAndCount(owner);
        if (tracked != 1) {
            context.throwGameTestException("Expected one tracked summon, got " + tracked);
            return;
        }

        SummonerSummons.discardAll(owner);
        if (SummonerSummons.findEntity(world.getServer(), zombie.getUuid()) != null) {
            context.throwGameTestException("Summon was not discarded on recall");
            return;
        }
        if (!SummonerSummons.ownedSummonUuids(owner).isEmpty()) {
            context.throwGameTestException("Owned summon list not cleared on recall");
            return;
        }

        context.complete();
    }

}

