package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight runtime for Space gem "anomalies" that require per-tick physics (black/white holes and delayed orbital strikes).
 *
 * <p>Important: keep this performant — it should early-exit when no anomalies exist and keep radii clamped by config.</p>
 */
public final class SpaceAnomalies {
    private static final List<Anomaly> ACTIVE = new ArrayList<>();

    private SpaceAnomalies() {
    }

    public static void spawnBlackHole(ServerPlayerEntity caster, Vec3d pos) {
        ServerWorld world = caster.getServerWorld();
        long now = GemsTime.now(world);
        ACTIVE.add(Anomaly.blackHole(caster.getUuid(), world.getRegistryKey(), pos, now));
        AbilityFeedback.sound(caster, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.9F, 0.8F);
        AbilityFeedback.sound(caster, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 0.6F, 0.7F);
    }

    public static void spawnWhiteHole(ServerPlayerEntity caster, Vec3d pos) {
        ServerWorld world = caster.getServerWorld();
        long now = GemsTime.now(world);
        ACTIVE.add(Anomaly.whiteHole(caster.getUuid(), world.getRegistryKey(), pos, now));
        AbilityFeedback.sound(caster, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.9F, 1.4F);
        AbilityFeedback.sound(caster, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 0.6F, 1.6F);
    }

    public static void scheduleOrbitalLaser(ServerPlayerEntity caster, BlockPos target, boolean miningMode) {
        ServerWorld world = caster.getServerWorld();
        long now = GemsTime.now(world);
        ACTIVE.add(Anomaly.laser(caster.getUuid(), world.getRegistryKey(), target, miningMode, now));
        AbilityFeedback.burstAt(world, Vec3d.ofCenter(target), ParticleTypes.END_ROD, 16, 0.35D);
        AbilityFeedback.ring(world, Vec3d.ofCenter(target).add(0.0D, 0.2D, 0.0D), 3.0D, ParticleTypes.END_ROD, 32);
        AbilityFeedback.sound(caster, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8F, miningMode ? 0.7F : 1.2F);
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        long now = GemsTime.now(server);

        Iterator<Anomaly> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Anomaly a = it.next();
            if (a.untilTick <= now) {
                it.remove();
                continue;
            }

            ServerWorld world = server.getWorld(a.dimension);
            if (world == null) {
                it.remove();
                continue;
            }

            ServerPlayerEntity caster = server.getPlayerManager().getPlayer(a.caster);
            if (caster == null) {
                it.remove();
                continue;
            }

            if (a.kind == Kind.ORBITAL_LASER) {
                if (a.strikeAtTick > now) {
                    // Telegraph a small pulse while waiting.
                    if ((now % 5) == 0) {
                        AbilityFeedback.burstAt(world, Vec3d.ofCenter(a.laserTarget), ParticleTypes.END_ROD, 2, 0.15D);
                    }
                    continue;
                }
                strikeOrbitalLaser(caster, world, a);
                it.remove();
                continue;
            }

            tickHole(caster, world, a, now);
        }
    }

    private static void strikeOrbitalLaser(ServerPlayerEntity caster, ServerWorld world, Anomaly a) {
        BlockPos target = a.laserTarget;
        Vec3d center = Vec3d.ofCenter(target);

        double fromY = world.getTopY() + 24.0D;
        Vec3d from = new Vec3d(center.x, fromY, center.z);
        AbilityFeedback.beam(world, from, center, ParticleTypes.END_ROD, 64);
        AbilityFeedback.burstAt(world, center, ParticleTypes.FLASH, 1, 0.0D);
        AbilityFeedback.sound(caster, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5F, a.miningMode ? 0.9F : 1.3F);
        AbilityFeedback.sound(caster, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.35F, a.miningMode ? 0.8F : 1.1F);

        if (a.miningMode) {
            mineAt(caster, world, target);
        } else {
            damageAt(caster, world, center);
        }
    }

    private static void damageAt(ServerPlayerEntity caster, ServerWorld world, Vec3d center) {
        int radius = GemsBalance.v().space().orbitalLaserRadiusBlocks();
        float damage = GemsBalance.v().space().orbitalLaserDamage();
        if (radius <= 0 || damage <= 0.0F) {
            return;
        }

        Box box = new Box(center, center).expand(radius);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != caster);
        for (LivingEntity living : targets) {
            if (living instanceof ServerPlayerEntity other && GemTrust.isTrusted(caster, other)) {
                continue;
            }
            living.damage(caster.getDamageSources().magic(), damage);
        }
        AbilityFeedback.burstAt(world, center, ParticleTypes.ELECTRIC_SPARK, 18, 0.4D);
    }

    private static void mineAt(ServerPlayerEntity caster, ServerWorld world, BlockPos target) {
        int radius = GemsBalance.v().space().orbitalLaserMiningRadiusBlocks();
        int maxBlocks = GemsBalance.v().space().orbitalLaserMiningMaxBlocks();
        float hardnessCap = GemsBalance.v().space().orbitalLaserMiningHardnessCap();
        if (radius <= 0 || maxBlocks <= 0) {
            return;
        }

        int mined = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    pos.set(target.getX() + dx, target.getY() + dy, target.getZ() + dz);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (!isMiningAllowed(state)) {
                        continue;
                    }
                    float hardness = state.getHardness(world, pos);
                    if (hardness < 0.0F || hardness > hardnessCap) {
                        continue;
                    }
                    if (world.breakBlock(pos, true, caster)) {
                        mined++;
                        AbilityFeedback.burstAt(world, Vec3d.ofCenter(pos), ParticleTypes.CLOUD, 2, 0.15D);
                        if (mined >= maxBlocks) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static boolean isMiningAllowed(BlockState state) {
        return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.CRYING_OBSIDIAN) || state.isOf(Blocks.ANCIENT_DEBRIS);
    }

    private static void tickHole(ServerPlayerEntity caster, ServerWorld world, Anomaly a, long now) {
        int radius = a.radiusBlocks;
        if (radius <= 0) {
            return;
        }

        Vec3d center = a.center;
        Box box = new Box(center, center).expand(radius);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive());

        float strength = a.strength;
        for (LivingEntity living : targets) {
            if (living == caster) {
                continue;
            }
            if (living instanceof ServerPlayerEntity other && GemTrust.isTrusted(caster, other)) {
                continue;
            }
            Vec3d delta = (a.kind == Kind.BLACK_HOLE) ? center.subtract(living.getPos()) : living.getPos().subtract(center);
            double dist = Math.max(0.5D, delta.length());
            Vec3d push = delta.normalize().multiply(strength / dist);
            living.setVelocity(living.getVelocity().add(push));
            living.velocityModified = true;
        }

        if (now >= a.nextDamageTick) {
            a.nextDamageTick = now + 20;
            if (a.damagePerSecond > 0.0F) {
                float dmg = a.damagePerSecond;
                for (LivingEntity living : targets) {
                    if (living == caster) {
                        continue;
                    }
                    if (living instanceof ServerPlayerEntity other && GemTrust.isTrusted(caster, other)) {
                        continue;
                    }
                    living.damage(caster.getDamageSources().magic(), dmg);
                }
            }
        }

        if ((now % 5) == 0) {
            var core = (a.kind == Kind.BLACK_HOLE) ? ParticleTypes.PORTAL : ParticleTypes.END_ROD;
            AbilityFeedback.burstAt(world, center, core, 6, 0.25D);
        }
    }

    private enum Kind {
        BLACK_HOLE,
        WHITE_HOLE,
        ORBITAL_LASER
    }

    private static final class Anomaly {
        final Kind kind;
        final UUID caster;
        final net.minecraft.registry.RegistryKey<World> dimension;
        final Vec3d center;
        final long untilTick;

        final int radiusBlocks;
        final float strength;
        final float damagePerSecond;
        long nextDamageTick;

        final BlockPos laserTarget;
        final boolean miningMode;
        final long strikeAtTick;

        private Anomaly(Kind kind, UUID caster, net.minecraft.registry.RegistryKey<World> dimension, Vec3d center, long untilTick,
                        int radiusBlocks, float strength, float damagePerSecond, long nextDamageTick,
                        BlockPos laserTarget, boolean miningMode, long strikeAtTick) {
            this.kind = kind;
            this.caster = caster;
            this.dimension = dimension;
            this.center = center;
            this.untilTick = untilTick;
            this.radiusBlocks = radiusBlocks;
            this.strength = strength;
            this.damagePerSecond = damagePerSecond;
            this.nextDamageTick = nextDamageTick;
            this.laserTarget = laserTarget;
            this.miningMode = miningMode;
            this.strikeAtTick = strikeAtTick;
        }

        static Anomaly blackHole(UUID caster, net.minecraft.registry.RegistryKey<World> dim, Vec3d center, long now) {
            int duration = GemsBalance.v().space().blackHoleDurationTicks();
            int radius = GemsBalance.v().space().blackHoleRadiusBlocks();
            float strength = GemsBalance.v().space().blackHolePullStrength();
            float dmg = GemsBalance.v().space().blackHoleDamagePerSecond();
            return new Anomaly(Kind.BLACK_HOLE, caster, dim, center, now + duration, radius, strength, dmg, now + 20, null, false, 0);
        }

        static Anomaly whiteHole(UUID caster, net.minecraft.registry.RegistryKey<World> dim, Vec3d center, long now) {
            int duration = GemsBalance.v().space().whiteHoleDurationTicks();
            int radius = GemsBalance.v().space().whiteHoleRadiusBlocks();
            float strength = GemsBalance.v().space().whiteHolePushStrength();
            float dmg = GemsBalance.v().space().whiteHoleDamagePerSecond();
            return new Anomaly(Kind.WHITE_HOLE, caster, dim, center, now + duration, radius, strength, dmg, now + 20, null, false, 0);
        }

        static Anomaly laser(UUID caster, net.minecraft.registry.RegistryKey<World> dim, BlockPos target, boolean miningMode, long now) {
            int delay = GemsBalance.v().space().orbitalLaserDelayTicks();
            return new Anomaly(Kind.ORBITAL_LASER, caster, dim, Vec3d.ofCenter(target), now + delay + 200, 0, 0.0F, 0.0F, 0, target, miningMode, now + delay);
        }
    }
}
