package com.feel.gems.gametest.ability;

import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.bonus.BonusArcaneMissilesAbility;
import com.feel.gems.power.ability.bonus.BonusBanishmentAbility;
import com.feel.gems.power.ability.bonus.BonusBerserkerRageAbility;
import com.feel.gems.power.ability.bonus.BonusBlinkAbility;
import com.feel.gems.power.ability.bonus.BonusBloodlustAbility;
import com.feel.gems.power.ability.bonus.BonusChainLightningAbility;
import com.feel.gems.power.ability.bonus.BonusCorpseExplosionAbility;
import com.feel.gems.power.ability.bonus.BonusCrystalCageAbility;
import com.feel.gems.power.ability.bonus.BonusCurseBoltAbility;
import com.feel.gems.power.ability.bonus.BonusDecoyTrapAbility;
import com.feel.gems.power.ability.bonus.BonusDoomBoltAbility;
import com.feel.gems.power.ability.bonus.BonusEarthshatterAbility;
import com.feel.gems.power.ability.bonus.BonusEtherealStepAbility;
import com.feel.gems.power.ability.bonus.BonusFrostbiteAbility;
import com.feel.gems.power.ability.bonus.BonusGravityCrushAbility;
import com.feel.gems.power.ability.bonus.BonusGravityWellAbility;
import com.feel.gems.power.ability.bonus.BonusIceWallAbility;
import com.feel.gems.power.ability.bonus.BonusIcicleBarrageAbility;
import com.feel.gems.power.ability.bonus.BonusInfernoDashAbility;
import com.feel.gems.power.ability.bonus.BonusLifeTapAbility;
import com.feel.gems.power.ability.bonus.BonusMagmaPoolAbility;
import com.feel.gems.power.ability.bonus.BonusMindSpikeAbility;
import com.feel.gems.power.ability.bonus.BonusMirrorImageAbility;
import com.feel.gems.power.ability.bonus.BonusOverchargeAbility;
import com.feel.gems.power.ability.bonus.BonusPlagueCloudAbility;
import com.feel.gems.power.ability.bonus.BonusPurgeAbility;
import com.feel.gems.power.ability.bonus.BonusQuicksandAbility;
import com.feel.gems.power.ability.bonus.BonusRadiantBurstAbility;
import com.feel.gems.power.ability.bonus.BonusReflectionWardAbility;
import com.feel.gems.power.ability.bonus.BonusSanctuaryAbility;
import com.feel.gems.power.ability.bonus.BonusSearingLightAbility;
import com.feel.gems.power.ability.bonus.BonusShadowstepAbility;
import com.feel.gems.power.ability.bonus.BonusSmokeScreenAbility;
import com.feel.gems.power.ability.bonus.BonusSonicBoomAbility;
import com.feel.gems.power.ability.bonus.BonusSoulLinkAbility;
import com.feel.gems.power.ability.bonus.BonusSoulSwapAbility;
import com.feel.gems.power.ability.bonus.BonusSpectralBladeAbility;
import com.feel.gems.power.ability.bonus.BonusSpectralChainsAbility;
import com.feel.gems.power.ability.bonus.BonusStarfallAbility;
import com.feel.gems.power.ability.bonus.BonusThornsNovaAbility;
import com.feel.gems.power.ability.bonus.BonusThunderstrikeAbility;
import com.feel.gems.power.ability.bonus.BonusTidalWaveAbility;
import com.feel.gems.power.ability.bonus.BonusTimewarpAbility;
import com.feel.gems.power.ability.bonus.BonusTremorAbility;
import com.feel.gems.power.ability.bonus.BonusVampiricTouchAbility;
import com.feel.gems.power.ability.bonus.BonusVenomsprayAbility;
import com.feel.gems.power.ability.bonus.BonusVortexStrikeAbility;
import com.feel.gems.power.ability.bonus.BonusVulnerabilityAbility;
import com.feel.gems.power.ability.bonus.BonusWarpStrikeAbility;
import com.feel.gems.power.ability.bonus.BonusWindSlashAbility;
import com.feel.gems.power.runtime.EtherealState;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.PlayerStateManager;
import java.util.EnumSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Behavior tests for all bonus abilities.
 */
public final class GemsBonusAbilityGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    private static ServerPlayerEntity setupPlayer(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.resetPlayerForTest(player);
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setEnergy(player, 10);
        GemPowers.sync(player);
        return player;
    }

    private static void aimAt(ServerPlayerEntity player, ServerWorld world, Vec3d target) {
        Vec3d pos = player.getEntityPos();
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        double dy = target.y - player.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        player.teleport(world, pos.x, pos.y, pos.z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    private static void placePlatform(TestContext context, int radius) {
        int safeRadius = Math.max(1, Math.min(radius, 2));
        for (int x = -safeRadius; x <= safeRadius; x++) {
            for (int z = -safeRadius; z <= safeRadius; z++) {
                context.setBlockState(x, 1, z, Blocks.STONE.getDefaultState());
            }
        }
    }

    private static ZombieEntity spawnZombie(ServerWorld world, Vec3d pos) {
        ZombieEntity zombie = EntityType.ZOMBIE.create(world, SpawnReason.TRIGGERED);
        if (zombie == null) {
            return null;
        }
        zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // Clear any armor to ensure consistent damage calculations in tests
        for (net.minecraft.entity.EquipmentSlot slot : net.minecraft.entity.EquipmentSlot.values()) {
            zombie.equipStack(slot, net.minecraft.item.ItemStack.EMPTY);
        }
        world.spawnEntity(zombie);
        return zombie;
    }

    private static Vec3d origin(TestContext context) {
        return context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
    }

    private static Vec3d isolatedOrigin(TestContext context, ServerPlayerEntity player) {
        Vec3d base = origin(context);
        return new Vec3d(base.x, base.y + 40.0D, base.z);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusArcaneMissilesSpawnsFireballs(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Box box = new Box(player.getBlockPos()).expand(6.0);
        int before = world.getEntitiesByClass(SmallFireballEntity.class, box, e -> true).size();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusArcaneMissilesAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Arcane Missiles did not activate");
            }
            int after = world.getEntitiesByClass(SmallFireballEntity.class, box, e -> true).size();
            if (after - before < 5) {
                context.throwGameTestException("Arcane Missiles did not spawn 5 fireballs");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBanishmentTeleportsTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        target.setAiDisabled(true);
        target.setNoGravity(true);
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
        Vec3d before = target.getEntityPos();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusBanishmentAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Banishment did not activate");
            }
            Vec3d after = target.getEntityPos();
            if (after.distanceTo(before) < 40.0D) {
                context.throwGameTestException("Banishment did not teleport target far enough");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBerserkerRageScalesDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        placePlatform(context, 10);
        ServerPlayerEntity player = setupPlayer(context);
        ZombieEntity dummy = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (dummy == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        dummy.setAiDisabled(true);
        dummy.setNoGravity(true);
        dummy.setVelocity(Vec3d.ZERO);

        float boost = BonusBerserkerRageAbility.getDamageBoostMultiplier();
        float taken = BonusBerserkerRageAbility.getDamageTakenMultiplier();
        if (boost <= 1.0F && taken <= 1.0F) {
            context.complete();
            return;
        }

        float[] baseTaken = new float[1];
        float[] baseDealt = new float[1];

        context.runAtTick(40L, () -> {
            player.setHealth(20.0F);
            float before = player.getHealth();
            player.damage(world, player.getDamageSources().generic(), 4.0F);
            baseTaken[0] = before - player.getHealth();

            dummy.setHealth(dummy.getMaxHealth());
            before = dummy.getHealth();
            dummy.damage(world, world.getDamageSources().playerAttack(player), 4.0F);
            baseDealt[0] = before - dummy.getHealth();
        });

        // Avoid invulnerability frame issues by spacing the "rage" hits out.
        context.runAtTick(80L, () -> {
            dummy.setHealth(dummy.getMaxHealth());
            player.setHealth(20.0F);
            // Clear any existing berserker rage state from other tests
            PlayerStateManager.clearPersistent(player, "bonus_berserker_rage_until");
            boolean ok = new BonusBerserkerRageAbility().activate(player);
            if (!ok || !BonusBerserkerRageAbility.isActive(player)) {
                context.throwGameTestException("Berserker Rage did not activate");
                return;
            }
        });

        context.runAtTick(120L, () -> {
            // Verify rage is still active
            if (!BonusBerserkerRageAbility.isActive(player)) {
                context.throwGameTestException("Berserker Rage should still be active at tick 40");
                return;
            }
            
            float before = player.getHealth();
            player.damage(world, player.getDamageSources().generic(), 4.0F);
            float rageTaken = before - player.getHealth();

            before = dummy.getHealth();
            dummy.damage(world, world.getDamageSources().playerAttack(player), 4.0F);
            float rageDealt = before - dummy.getHealth();

            // Check that damage taken increased (if multiplier > 1)
            if (taken > 1.0F && rageTaken < baseTaken[0] + 0.1F) {
                context.throwGameTestException("Berserker Rage should increase damage taken");
                return;
            }
            // Check that damage dealt increased (if multiplier > 1)
            // Use a more lenient check: rage damage should be noticeably higher than baseline
            if (boost > 1.0F && rageDealt < baseDealt[0] + 0.5F) {
                context.throwGameTestException("Berserker Rage should increase damage dealt");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBlinkTeleportsForward(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d before = player.getEntityPos();
        aimAt(player, world, before.add(0.0D, 0.0D, 6.0D));

        context.runAtTick(5L, () -> {
            boolean ok = new BonusBlinkAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Blink did not activate");
            }
            if (player.getEntityPos().distanceTo(before) < 4.0D) {
                context.throwGameTestException("Blink did not move player far enough");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBloodlustScalesWithEnemies(TestContext context) {
        ServerWorld world = context.getWorld();
        placePlatform(context, 12);
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d base = origin(context);
        ZombieEntity z1 = spawnZombie(world, base.add(2.0D, 0.0D, 2.0D));
        ZombieEntity z2 = spawnZombie(world, base.add(-2.0D, 0.0D, 2.0D));
        ZombieEntity z3 = spawnZombie(world, base.add(0.0D, 0.0D, -2.0D));
        if (z1 == null || z2 == null || z3 == null) {
            context.throwGameTestException("Failed to spawn zombies");
            return;
        }
        for (ZombieEntity zombie : java.util.List.of(z1, z2, z3)) {
            zombie.setAiDisabled(true);
            zombie.setNoGravity(true);
            zombie.setVelocity(Vec3d.ZERO);
        }

        context.runAtTick(20L, () -> {
            boolean ok = new BonusBloodlustAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Bloodlust did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                10L,
                160L,
                2L,
                () -> {
                    StatusEffectInstance haste = player.getStatusEffect(StatusEffects.HASTE);
                    StatusEffectInstance strength = player.getStatusEffect(StatusEffects.STRENGTH);
                    return haste != null && haste.getAmplifier() >= 2
                            && strength != null && strength.getAmplifier() >= 1;
                },
                "Bloodlust effects mismatch"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusChainLightningSpawnsBolts(TestContext context) {
        ServerWorld world = context.getWorld();
        placePlatform(context, 12);
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d base = origin(context);
        ZombieEntity z1 = spawnZombie(world, base.add(3.0D, 0.0D, 0.0D));
        ZombieEntity z2 = spawnZombie(world, base.add(-3.0D, 0.0D, 0.0D));
        if (z1 == null || z2 == null) {
            context.throwGameTestException("Failed to spawn zombies");
            return;
        }
        for (ZombieEntity zombie : java.util.List.of(z1, z2)) {
            zombie.setAiDisabled(true);
            zombie.setNoGravity(true);
            zombie.setVelocity(Vec3d.ZERO);
        }
        Box box = new Box(player.getBlockPos()).expand(12.0);
        int before = world.getEntitiesByClass(LightningEntity.class, box, e -> true).size();

        context.runAtTick(20L, () -> {
            boolean ok = new BonusChainLightningAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Chain Lightning did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                21L,
                30L,
                1L,
                () -> {
                    int after = world.getEntitiesByClass(LightningEntity.class, box, e -> true).size();
                    return after - before >= 2;
                },
                "Chain Lightning did not spawn enough lightning"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCorpseExplosionDetonatesLowHealth(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d base = origin(context);
        ZombieEntity corpse = spawnZombie(world, base.add(2.0D, 0.0D, 0.0D));
        ZombieEntity victim = spawnZombie(world, base.add(3.0D, 0.0D, 0.0D));
        if (corpse == null || victim == null) {
            context.throwGameTestException("Failed to spawn zombies");
            return;
        }
        float victimBefore = victim.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusCorpseExplosionAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Corpse Explosion did not activate");
            }
        });

        context.runAtTick(8L, () -> {
            corpse.damage(world, world.getDamageSources().generic(), 1000.0F);
        });

        context.runAtTick(12L, () -> {
            if (victim.getHealth() >= victimBefore) {
                context.throwGameTestException("Corpse Explosion did not damage nearby target");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCrystalCageBuildsCage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        placePlatform(context, 10);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        target.setAiDisabled(true);
        target.setNoGravity(true);
        target.setVelocity(Vec3d.ZERO);
        BlockPos center = target.getBlockPos();

        context.runAtTick(20L, () -> {
            aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
            boolean ok = new BonusCrystalCageAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Crystal Cage did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                22L,
                80L,
                2L,
                () -> {
                    int amethyst = 0;
                    for (int y = 0; y <= 2; y++) {
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                if (Math.abs(x) == 1 || Math.abs(z) == 1 || y == 2) {
                                    if (world.getBlockState(center.add(x, y, z)).isOf(Blocks.AMETHYST_BLOCK)) {
                                        amethyst++;
                                    }
                                }
                            }
                        }
                    }
                    return amethyst >= 8;
                },
                "Crystal Cage did not place enough blocks"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCurseBoltDrainsHealth(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        placePlatform(context, 6);
        Vec3d targetPos = origin(context).add(2.0D, 0.0D, 0.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        target.setAiDisabled(true);
        target.setNoGravity(true);
        target.setVelocity(Vec3d.ZERO);
        target.equipStack(
                net.minecraft.entity.EquipmentSlot.HEAD,
                new net.minecraft.item.ItemStack(net.minecraft.item.Items.LEATHER_HELMET)
        );
        target.setFireTicks(0);
        var maxHealth = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(20.0D);
        }
        player.setHealth(8.0F);
        float[] playerBefore = new float[1];
        float[] targetBefore = new float[1];

        context.runAtTick(40L, () -> {
            aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
            player.setVelocity(Vec3d.ZERO);
            playerBefore[0] = player.getHealth();
            targetBefore[0] = target.getHealth();
            boolean ok = new BonusCurseBoltAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Curse Bolt did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                41L,
                160L,
                1L,
                () -> target.getHealth() < targetBefore[0] && player.getHealth() > playerBefore[0],
                "Curse Bolt did not damage target and heal player"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusDecoyTrapSpawnsTrapItem(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Box box = new Box(player.getBlockPos()).expand(4.0);

        context.runAtTick(5L, () -> {
            boolean ok = new BonusDecoyTrapAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Decoy Trap did not activate");
            }
            List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, e -> true);
            if (items.isEmpty()) {
                context.throwGameTestException("Decoy Trap did not spawn item");
                return;
            }
            ItemEntity item = items.get(0);
            if (!player.getUuid().equals(BonusDecoyTrapAbility.getTrapOwner(item))) {
                context.throwGameTestException("Decoy Trap owner mismatch");
            }
            ZombieEntity picker = spawnZombie(world, item.getEntityPos());
            if (picker == null) {
                context.throwGameTestException("Failed to spawn picker");
                return;
            }
            boolean triggered = BonusDecoyTrapAbility.triggerTrap(item, picker);
            if (!triggered || item.isAlive()) {
                context.throwGameTestException("Decoy Trap did not trigger");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusDoomBoltSpawnsWitherSkull(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Box box = new Box(player.getBlockPos()).expand(12.0);
        int before = world.getEntitiesByClass(WitherSkullEntity.class, box, e -> true).size();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusDoomBoltAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Doom Bolt did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                6L,
                30L,
                1L,
                () -> {
                    int after = world.getEntitiesByClass(WitherSkullEntity.class, box, e -> true).size();
                    return after - before >= 1;
                },
                "Doom Bolt did not spawn Wither Skull"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusEarthshatterDamagesAndLaunches(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        ZombieEntity target = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusEarthshatterAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Earthshatter did not activate");
            }
            if (target.getHealth() >= before) {
                context.throwGameTestException("Earthshatter did not damage target");
            }
            if (target.getVelocity().y <= 0.1D) {
                context.throwGameTestException("Earthshatter did not launch target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusEtherealStepPhasesThroughWall(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d base = origin(context);
        BlockPos wallBase = BlockPos.ofFloored(base.add(0.0D, 0.0D, 3.0D));
        world.setBlockState(wallBase, Blocks.STONE.getDefaultState());
        world.setBlockState(wallBase.up(), Blocks.STONE.getDefaultState());
        Vec3d before = player.getEntityPos();
        aimAt(player, world, base.add(0.0D, 0.0D, 6.0D));

        context.runAtTick(5L, () -> {
            boolean ok = new BonusEtherealStepAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Ethereal Step did not activate");
            }
            if (player.getZ() <= before.z + 2.5D) {
                context.throwGameTestException("Ethereal Step did not pass wall");
            }
            if (!EtherealState.isEthereal(player)) {
                context.throwGameTestException("Ethereal Step did not set ethereal state");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusFrostbiteAppliesSlowness(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        ZombieEntity target = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }

        context.runAtTick(5L, () -> {
            boolean ok = new BonusFrostbiteAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Frostbite did not activate");
            }
            if (!target.hasStatusEffect(StatusEffects.SLOWNESS) || !target.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                context.throwGameTestException("Frostbite did not apply effects");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusGravityCrushRootsTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d base = isolatedOrigin(context, player);
        teleport(player, world, base.x, base.y, base.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        Vec3d targetPos = base.add(0.0D, 0.0D, 2.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        target.setAiDisabled(true);
        target.setNoGravity(true);
        target.setVelocity(Vec3d.ZERO);
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
            boolean ok = new BonusGravityCrushAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Gravity Crush did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                6L,
                40L,
                2L,
                () -> target.getHealth() < before
                        && target.hasStatusEffect(StatusEffects.SLOWNESS)
                        && target.hasStatusEffect(StatusEffects.JUMP_BOOST),
                "Gravity Crush did not slam, damage, or root the target"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusGravityWellPullsTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(5.0D, 0.0D, 0.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }

        context.runAtTick(5L, () -> {
            boolean ok = new BonusGravityWellAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Gravity Well did not activate");
            }
            Vec3d toPlayer = player.getEntityPos().subtract(target.getEntityPos()).normalize();
            if (target.getVelocity().dotProduct(toPlayer) < 0.5D) {
                context.throwGameTestException("Gravity Well did not pull target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusIceWallPlacesPackedIce(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d base = origin(context);
        placePlatform(context, 12);
        BlockPos target = BlockPos.ofFloored(base.add(0.0D, 1.0D, 4.0D));
        world.setBlockState(target, Blocks.STONE.getDefaultState());
        aimAt(player, world, Vec3d.ofCenter(target));
        BlockPos[] center = new BlockPos[] { target };

        context.runAtTick(5L, () -> {
            var hit = world.raycast(new net.minecraft.world.RaycastContext(
                    player.getEyePos(),
                    player.getEyePos().add(player.getRotationVector().multiply(10)),
                    net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE,
                    player
            ));
            center[0] = BlockPos.ofFloored(hit.getPos());

            boolean ok = new BonusIceWallAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Ice Wall did not activate");
            }
            int packed = 0;
            for (int h = 0; h < 4; h++) {
                for (int w = -2; w <= 2; w++) {
                    if (world.getBlockState(center[0].add(w, h, 0)).isOf(Blocks.PACKED_ICE)) {
                        packed++;
                    }
                }
            }
            if (packed < 6) {
                context.throwGameTestException("Ice Wall did not place packed ice");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusIcicleBarrageFreezesTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusIcicleBarrageAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Icicle Barrage did not activate");
            }
            if (target.getHealth() >= before) {
                context.throwGameTestException("Icicle Barrage did not damage target");
            }
            if (target.getFrozenTicks() <= 0) {
                context.throwGameTestException("Icicle Barrage did not freeze target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusInfernoDashLeavesFire(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        placePlatform(context, 12);
        Vec3d base = origin(context);
        int dashDistance = 2;

        context.runAtTick(5L, () -> {
            aimAt(player, world, base.add(0.0D, 0.0D, dashDistance));
            player.setVelocity(Vec3d.ZERO);
            boolean ok = new BonusInfernoDashAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Inferno Dash did not activate");
            }
            boolean fireFound = false;
            for (int i = 1; i <= dashDistance; i++) {
                BlockPos pos = BlockPos.ofFloored(base.add(0.0D, 0.0D, i));
                if (world.getBlockState(pos).isOf(Blocks.FIRE)) {
                    fireFound = true;
                    break;
                }
            }
            if (!fireFound) {
                context.throwGameTestException("Inferno Dash did not place fire");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusLifeTapCostsHealthAndReducesCooldowns(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        player.setHealth(20.0F);
        float before = player.getHealth();

        long now = world.getTime();
        // Give the player a fake cooldown to reduce.
        com.feel.gems.power.runtime.GemAbilityCooldowns.setNextAllowedTick(
                player,
                com.feel.gems.power.registry.PowerIds.BONUS_THUNDERSTRIKE,
                now + 200L
        );

        context.runAtTick(5L, () -> {
            player.setHealth(20.0F);
            boolean ok = new BonusLifeTapAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Life Tap did not activate");
            }
            if (player.getHealth() >= before) {
                context.throwGameTestException("Life Tap did not cost health");
            }
            long after = com.feel.gems.power.runtime.GemAbilityCooldowns.nextAllowedTick(player, com.feel.gems.power.registry.PowerIds.BONUS_THUNDERSTRIKE);
            if (after >= now + 200L) {
                context.throwGameTestException("Life Tap did not reduce cooldowns");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusMagmaPoolCreatesMagma(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        placePlatform(context, 6);
        aimAt(player, world, Vec3d.ofCenter(player.getBlockPos().down()));
        BlockPos[] center = new BlockPos[] { player.getBlockPos().down().up() };

        context.runAtTick(5L, () -> {
            var hit = player.raycast(16.0D, 1.0F, false);
            if (hit instanceof net.minecraft.util.hit.BlockHitResult bhr) {
                center[0] = bhr.getBlockPos().up();
            }
            boolean ok = new BonusMagmaPoolAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Magma Pool did not activate");
            }
            int lava = 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (world.getBlockState(center[0].add(dx, 0, dz)).isOf(Blocks.LAVA)) {
                        lava++;
                    }
                }
            }
            if (lava < 5) {
                context.throwGameTestException("Magma Pool did not place lava");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusMindSpikeDamagesAndGlows(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusMindSpikeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Mind Spike did not activate");
            }
            if (target.getHealth() >= before) {
                context.throwGameTestException("Mind Spike did not damage target");
            }
            if (!target.hasStatusEffect(StatusEffects.GLOWING)) {
                context.throwGameTestException("Mind Spike did not apply glowing");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusMirrorImageSpawnsIllusions(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Box box = new Box(player.getBlockPos()).expand(4.0);

        context.runAtTick(5L, () -> {
            boolean ok = new BonusMirrorImageAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Mirror Image did not activate");
            }
            List<ArmorStandEntity> stands = world.getEntitiesByClass(ArmorStandEntity.class, box,
                    e -> e.getCommandTags().contains("gems_mirror_image"));
            if (stands.size() < 3) {
                context.throwGameTestException("Mirror Image did not spawn 3 illusions");
            }
            if (!player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                context.throwGameTestException("Mirror Image did not cloak player");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusOverchargeMarksPlayer(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        player.setHealth(20.0F);

        context.runAtTick(20L, () -> {
            float before = player.getHealth();
            boolean ok = new BonusOverchargeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Overcharge did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                21L,
                80L,
                2L,
                () -> player.getHealth() < 20.0F && BonusOverchargeAbility.isActive(player),
                "Overcharge did not cost health or apply state"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusPlagueCloudCreatesAreaEffect(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d base = isolatedOrigin(context, player);
        teleport(player, world, base.x, base.y, base.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        Box box = new Box(player.getBlockPos()).expand(16.0);
        Vec3d targetPos = base.add(0.0D, 0.0D, 2.0D);
        BlockPos marker = BlockPos.ofFloored(targetPos.x, targetPos.y - 1.0D, targetPos.z);
        world.setBlockState(marker, Blocks.STONE.getDefaultState());
        CowEntity target = EntityType.COW.create(world, SpawnReason.TRIGGERED);
        if (target == null) {
            context.throwGameTestException("Failed to spawn cow");
            return;
        }
        target.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F);
        target.setAiDisabled(true);
        world.spawnEntity(target);
        target.setNoGravity(true);
        target.setVelocity(Vec3d.ZERO);
        Vec3d[] cloudPos = new Vec3d[1];

        context.runAtTick(40L, () -> {
            aimAt(player, world, Vec3d.ofCenter(marker));
            boolean ok = new BonusPlagueCloudAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Plague Cloud did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                42L,
                160L,
                2L,
                () -> {
                    if (cloudPos[0] == null) {
                        List<AreaEffectCloudEntity> clouds = world.getEntitiesByClass(AreaEffectCloudEntity.class, box, e -> true);
                        if (!clouds.isEmpty()) {
                            cloudPos[0] = clouds.get(0).getEntityPos();
                            Vec3d pos = cloudPos[0];
                            target.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
                            target.setVelocity(Vec3d.ZERO);
                        }
                    }
                    return target.hasStatusEffect(StatusEffects.POISON) && target.hasStatusEffect(StatusEffects.WEAKNESS);
                },
                "Plague Cloud should apply poison and weakness to targets inside it"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusPurgeRemovesBuffs(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));

        context.runAtTick(5L, () -> {
            boolean ok = new BonusPurgeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Purge did not activate");
            }
            if (target.hasStatusEffect(StatusEffects.SPEED)) {
                context.throwGameTestException("Purge did not remove buff");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusQuicksandAppliesSlownessAndCobwebs(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        placePlatform(context, 6);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
        BlockPos center = BlockPos.ofFloored(targetPos);

        context.runAtTick(5L, () -> {
            boolean ok = new BonusQuicksandAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Quicksand did not activate");
            }
            if (!target.hasStatusEffect(StatusEffects.SLOWNESS)) {
                context.throwGameTestException("Quicksand did not apply slowness");
            }
            boolean cobwebFound = false;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (world.getBlockState(center.add(x, 0, z)).isOf(Blocks.COBWEB)) {
                        cobwebFound = true;
                        break;
                    }
                }
            }
            if (!cobwebFound) {
                context.throwGameTestException("Quicksand did not place cobwebs");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusRadiantBurstBlindsEnemies(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        ZombieEntity target = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        target.setAiDisabled(true);
        target.setNoGravity(true);
        target.setVelocity(Vec3d.ZERO);

        context.runAtTick(20L, () -> {
            boolean ok = new BonusRadiantBurstAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Radiant Burst did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                22L,
                80L,
                2L,
                () -> target.hasStatusEffect(StatusEffects.BLINDNESS) && target.hasStatusEffect(StatusEffects.GLOWING),
                "Radiant Burst did not apply effects"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusReflectionWardTagsPlayer(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            boolean ok = new BonusReflectionWardAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Reflection Ward did not activate");
            }
            boolean found = player.getCommandTags().stream().anyMatch(tag -> tag.startsWith("gems_reflection_ward:"));
            if (!found) {
                context.throwGameTestException("Reflection Ward did not set tag");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSanctuaryBuffsSelfAndPushes(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        ZombieEntity target = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }

        context.runAtTick(5L, () -> {
            boolean ok = new BonusSanctuaryAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Sanctuary did not activate");
            }
            if (!player.hasStatusEffect(StatusEffects.REGENERATION) || !player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                context.throwGameTestException("Sanctuary did not buff player");
            }
            Vec3d toTarget = target.getEntityPos().subtract(player.getEntityPos()).normalize();
            if (target.getVelocity().dotProduct(toTarget) < 0.5D) {
                context.throwGameTestException("Sanctuary did not push enemy");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSearingLightBurnsUndead(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 8.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));

        context.runAtTick(5L, () -> {
            boolean ok = new BonusSearingLightAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Searing Light did not activate");
            }
            if (!target.isOnFire()) {
                context.throwGameTestException("Searing Light did not ignite undead");
            }
            if (target.getHealth() > 6.0F) {
                context.throwGameTestException("Searing Light did not deal bonus damage");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusShadowstepTeleportsForward(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d before = player.getEntityPos();
        aimAt(player, world, before.add(0.0D, 0.0D, 12.0D));

        context.runAtTick(5L, () -> {
            boolean ok = new BonusShadowstepAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Shadowstep did not activate");
            }
            if (player.getEntityPos().distanceTo(before) < 3.5D) {
                context.throwGameTestException("Shadowstep did not move player far enough");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSmokeScreenGrantsInvisibility(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            boolean ok = new BonusSmokeScreenAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Smoke Screen did not activate");
            }
            if (!player.hasStatusEffect(StatusEffects.INVISIBILITY) || !player.hasStatusEffect(StatusEffects.SPEED)) {
                context.throwGameTestException("Smoke Screen missing effects");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSonicBoomDamagesAndStaggers(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        ZombieEntity target = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusSonicBoomAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Sonic Boom did not activate");
            }
            if (target.getHealth() >= before) {
                context.throwGameTestException("Sonic Boom did not damage target");
            }
            if (!target.hasStatusEffect(StatusEffects.SLOWNESS) || !target.hasStatusEffect(StatusEffects.NAUSEA)) {
                context.throwGameTestException("Sonic Boom missing effects");
            }
            Vec3d away = target.getEntityPos().subtract(player.getEntityPos()).normalize();
            if (target.getVelocity().dotProduct(away) < 0.2D) {
                context.throwGameTestException("Sonic Boom did not knock back target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSoulLinkAppliesGlowing(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        placePlatform(context, 10);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        target.setAiDisabled(true);
        target.setNoGravity(true);
        target.setVelocity(Vec3d.ZERO);

        context.runAtTick(40L, () -> {
            aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
            boolean ok = new BonusSoulLinkAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Soul Link did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                42L,
                160L,
                2L,
                () -> player.hasStatusEffect(StatusEffects.GLOWING) && target.hasStatusEffect(StatusEffects.GLOWING),
                "Soul Link missing glowing effects"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSoulSwapSwapsPositions(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d base = isolatedOrigin(context, player);
        teleport(player, world, base.x, base.y, base.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        Vec3d playerPos = player.getEntityPos();
        Vec3d targetPos = base.add(0.0D, 3.0D, 0.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        target.setAiDisabled(true);
        target.setNoGravity(true);
        target.setVelocity(Vec3d.ZERO);

        context.runAtTick(5L, () -> {
            Box cleanupBox = new Box(player.getBlockPos()).expand(4.0D);
            for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, cleanupBox, e -> e != player && e != target)) {
                living.discard();
            }
            aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
            player.setVelocity(Vec3d.ZERO);
            boolean ok = new BonusSoulSwapAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Soul Swap did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                10L,
                180L,
                2L,
                () -> player.getEntityPos().distanceTo(targetPos) <= 1.5D
                        && target.getEntityPos().distanceTo(playerPos) <= 1.5D,
                "Soul Swap did not move player and target"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSpectralBladeSpawnsVex(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Box box = new Box(player.getBlockPos()).expand(4.0);

        context.runAtTick(5L, () -> {
            boolean ok = new BonusSpectralBladeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Spectral Blade did not activate");
            }
            List<VexEntity> vexes = world.getEntitiesByClass(VexEntity.class, box,
                    vex -> BonusSpectralBladeAbility.isOwnedBy(vex, player));
            if (vexes.isEmpty()) {
                context.throwGameTestException("Spectral Blade did not spawn owned vex");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSpectralChainsRootsTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        ZombieEntity target = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }

        context.runAtTick(5L, () -> {
            boolean ok = new BonusSpectralChainsAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Spectral Chains did not activate");
            }
            if (!target.hasStatusEffect(StatusEffects.SLOWNESS) || !target.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                context.throwGameTestException("Spectral Chains missing effects");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusStarfallSummonsMeteors(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Box box = new Box(player.getBlockPos()).expand(40.0);
        int before = world.getEntitiesByClass(SmallFireballEntity.class, box, e -> true).size();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusStarfallAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Starfall did not activate");
            }
            int after = world.getEntitiesByClass(SmallFireballEntity.class, box, e -> true).size();
            if (after - before < 8) {
                context.throwGameTestException("Starfall did not spawn enough meteors");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusThornsNovaDamagesTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        ZombieEntity target = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusThornsNovaAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Thorns Nova did not activate");
            }
            if (target.getHealth() >= before) {
                context.throwGameTestException("Thorns Nova did not damage target");
            }
            Vec3d away = target.getEntityPos().subtract(player.getEntityPos()).normalize();
            if (target.getVelocity().dotProduct(away) < 0.1D) {
                context.throwGameTestException("Thorns Nova did not knock back target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusThunderstrikeSummonsLightning(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Box box = new Box(player.getBlockPos()).expand(60.0);
        int before = world.getEntitiesByClass(LightningEntity.class, box, e -> true).size();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusThunderstrikeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Thunderstrike did not activate");
            }
            int after = world.getEntitiesByClass(LightningEntity.class, box, e -> true).size();
            if (after - before < 1) {
                context.throwGameTestException("Thunderstrike did not spawn lightning");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusTidalWaveKnocksBack(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));

        context.runAtTick(5L, () -> {
            boolean ok = new BonusTidalWaveAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Tidal Wave did not activate");
            }
            if (!target.hasStatusEffect(StatusEffects.SLOWNESS)) {
                context.throwGameTestException("Tidal Wave did not slow target");
            }
            Vec3d away = target.getEntityPos().subtract(player.getEntityPos()).normalize();
            if (target.getVelocity().dotProduct(away) < 0.2D) {
                context.throwGameTestException("Tidal Wave did not knock back target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusTimewarpBoostsSpeed(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        placePlatform(context, 10);
        ZombieEntity target = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        target.setAiDisabled(true);
        target.setNoGravity(true);
        target.setVelocity(Vec3d.ZERO);
        context.runAtTick(40L, () -> {
            boolean ok = new BonusTimewarpAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Timewarp did not activate");
                return;
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                42L,
                120L,
                2L,
                () -> target.hasStatusEffect(StatusEffects.SLOWNESS),
                "Timewarp should apply Slowness to nearby enemies"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusTremorSlowsEnemies(TestContext context) {
        ServerWorld world = context.getWorld();
        placePlatform(context, 12);
        ServerPlayerEntity player = setupPlayer(context);
        ZombieEntity target = spawnZombie(world, origin(context).add(2.0D, 0.0D, 0.0D));
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }

        context.runAtTick(5L, () -> {
            boolean ok = new BonusTremorAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Tremor did not activate");
            }
            if (!target.hasStatusEffect(StatusEffects.SLOWNESS) || !target.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                context.throwGameTestException("Tremor missing effects");
            }
            if (target.getVelocity().y <= 0.05D) {
                context.throwGameTestException("Tremor did not stagger target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusVampiricTouchDrainsAndHeals(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 2.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
        player.setHealth(10.0F);

        context.runAtTick(5L, () -> {
            // Capture health right before activation to avoid timing drift
            float playerBefore = player.getHealth();
            float targetBefore = target.getHealth();
            boolean ok = new BonusVampiricTouchAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Vampiric Touch did not activate");
            }
            if (target.getHealth() >= targetBefore) {
                context.throwGameTestException("Vampiric Touch did not damage target");
            }
            if (player.getHealth() <= playerBefore) {
                context.throwGameTestException("Vampiric Touch did not heal player");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusVenomsprayPoisonsTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        placePlatform(context, 4);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 2.0D);
        CowEntity target = EntityType.COW.create(world, SpawnReason.TRIGGERED);
        if (target == null) {
            context.throwGameTestException("Failed to spawn cow");
            return;
        }
        target.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F);
        target.setAiDisabled(true);
        world.spawnEntity(target);
        target.setNoGravity(true);
        target.setVelocity(Vec3d.ZERO);
        float before = target.getHealth();

        context.runAtTick(40L, () -> {
            aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
            boolean ok = new BonusVenomsprayAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Venomspray did not activate");
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                42L,
                160L,
                2L,
                () -> target.getHealth() < before && target.hasStatusEffect(StatusEffects.POISON),
                "Venomspray did not damage target or apply Poison"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusVortexStrikePullsTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(2.0D, 0.0D, 0.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusVortexStrikeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Vortex Strike did not activate");
            }
            if (target.getHealth() >= before) {
                context.throwGameTestException("Vortex Strike did not damage target");
            }
            Vec3d toPlayer = player.getEntityPos().subtract(target.getEntityPos()).normalize();
            if (target.getVelocity().dotProduct(toPlayer) < 0.1D) {
                context.throwGameTestException("Vortex Strike did not pull target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusVulnerabilityMarksTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));

        context.runAtTick(5L, () -> {
            boolean ok = new BonusVulnerabilityAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Vulnerability did not activate");
            }
            if (!target.hasStatusEffect(StatusEffects.WEAKNESS)) {
                context.throwGameTestException("Vulnerability did not apply weakness");
            }
            boolean tagged = target.getCommandTags().stream().anyMatch(tag -> tag.startsWith("gems_vulnerable:"));
            if (!tagged) {
                context.throwGameTestException("Vulnerability did not tag target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusWarpStrikeTeleportsBehindAndDamages(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 6.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusWarpStrikeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Warp Strike did not activate");
            }
            if (target.getHealth() >= before) {
                context.throwGameTestException("Warp Strike did not damage target");
            }
            Vec3d targetFacing = Vec3d.fromPolar(0, target.getYaw()).normalize();
            Vec3d expected = target.getEntityPos().subtract(targetFacing.multiply(2.0));
            if (player.getEntityPos().distanceTo(expected) > 1.5D) {
                context.throwGameTestException("Warp Strike did not place player behind target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusWindSlashDamagesTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = setupPlayer(context);
        Vec3d targetPos = origin(context).add(0.0D, 0.0D, 4.0D);
        ZombieEntity target = spawnZombie(world, targetPos);
        if (target == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        aimAt(player, world, targetPos.add(0.0D, 1.0D, 0.0D));
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new BonusWindSlashAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Wind Slash did not activate");
            }
            if (target.getHealth() >= before) {
                context.throwGameTestException("Wind Slash did not damage target");
            }
            if (target.getVelocity().length() < 0.2D) {
                context.throwGameTestException("Wind Slash did not push target");
            }
            context.complete();
        });
    }
}
