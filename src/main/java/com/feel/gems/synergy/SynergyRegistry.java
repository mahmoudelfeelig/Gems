package com.feel.gems.synergy;

import com.feel.gems.core.GemId;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTeleport;
import java.util.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Registry of all synergy combos.
 * Each synergy defines which gems need to combine and what effect triggers.
 */
public final class SynergyRegistry {
    private static final Map<String, SynergyDefinition> SYNERGIES = new LinkedHashMap<>();

    static {
        registerSynergies();
    }

    private SynergyRegistry() {
    }

    /**
     * Get all registered synergies.
     */
    public static Collection<SynergyDefinition> getAll() {
        return Collections.unmodifiableCollection(SYNERGIES.values());
    }

    /**
     * Get a synergy by ID.
     */
    public static SynergyDefinition get(String id) {
        return SYNERGIES.get(id);
    }

    private static void register(SynergyDefinition synergy) {
        SYNERGIES.put(synergy.id(), synergy);
    }

    // Helper to get player position as Vec3d
    private static Vec3d getPlayerPos(ServerPlayerEntity player) {
        return new Vec3d(player.getX(), player.getY(), player.getZ());
    }

    // Helper to get nearby entities
    private static List<LivingEntity> getNearbyLiving(ServerPlayerEntity player, double radius) {
        ServerWorld world = player.getEntityWorld();
        Box box = new Box(player.getBlockPos()).expand(radius);
        return world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive());
    }

    // ========== Synergy Definitions ==========

    private static void registerSynergies() {
        // ==================== GEM-BASED SYNERGIES ====================
        // These trigger when any ability from each gem is cast within the window
        
        // FIRE + FLUX = Thunderstorm: Lightning strike + fire explosion
        register(SynergyDefinition.gemBased(
                "thunderstorm",
                "gems.synergy.thunderstorm",
                Set.of(GemId.FIRE, GemId.FLUX),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        // Create lightning at target location (5 blocks ahead)
                        Vec3d playerPos = getPlayerPos(player);
                        Vec3d forward = player.getRotationVector().multiply(5);
                        Vec3d targetPos = playerPos.add(forward);
                        var lightning = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
                        if (lightning != null) {
                            lightning.refreshPositionAfterTeleport(targetPos.x, targetPos.y, targetPos.z);
                            world.spawnEntity(lightning);
                        }
                    }
                }
        ));

        // LIFE + REAPER = Soul Exchange: Participants swap health percentages
        register(SynergyDefinition.gemBased(
                "soul_exchange",
                "gems.synergy.soul_exchange",
                Set.of(GemId.LIFE, GemId.REAPER),
                60, 600,
                participants -> {
                    if (participants.size() >= 2) {
                        var p1 = participants.get(0).player();
                        var p2 = participants.get(1).player();
                        float pct1 = p1.getHealth() / p1.getMaxHealth();
                        float pct2 = p2.getHealth() / p2.getMaxHealth();
                        p1.setHealth(p1.getMaxHealth() * pct2);
                        p2.setHealth(p2.getMaxHealth() * pct1);
                        p1.sendMessage(Text.translatable("gems.synergy.soul_exchange.swapped").formatted(Formatting.LIGHT_PURPLE), true);
                        p2.sendMessage(Text.translatable("gems.synergy.soul_exchange.swapped").formatted(Formatting.LIGHT_PURPLE), true);
                    }
                }
        ));

        // SPEED + SPACE = Warp Drive: All participants get massive speed boost and short-range teleport
        register(SynergyDefinition.gemBased(
                "warp_drive",
                "gems.synergy.warp_drive",
                Set.of(GemId.SPEED, GemId.SPACE),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 4));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 100, 2));
                        // Teleport forward 10 blocks in look direction
                        Vec3d playerPos = getPlayerPos(player);
                        Vec3d forward = player.getRotationVector().multiply(10);
                        Vec3d targetPos = playerPos.add(forward);
                        GemsTeleport.teleport(player, player.getEntityWorld(), targetPos.x, targetPos.y, targetPos.z, player.getYaw(), player.getPitch());
                    }
                }
        ));

        // STRENGTH + TERROR = Warcry: All participants and nearby allies get strength + enemies get fear
        register(SynergyDefinition.gemBased(
                "warcry",
                "gems.synergy.warcry",
                Set.of(GemId.STRENGTH, GemId.TERROR),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        // Buff self
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 2));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));
                        // Play intimidating sound
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                        // Fear nearby enemies
                        for (LivingEntity other : getNearbyLiving(player, 15)) {
                            if (other != player && other instanceof ServerPlayerEntity otherPlayer) {
                                otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 0));
                                otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
                            }
                        }
                    }
                }
        ));

        // SPY + ASTRA = Phantom Strike: All participants become invisible + deal bonus damage on next hit
        register(SynergyDefinition.gemBased(
                "phantom_strike",
                "gems.synergy.phantom_strike",
                Set.of(GemId.SPY, GemId.ASTRA),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 3));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1));
                    }
                }
        ));

        // BEACON + LIFE = Radiant Restoration: Massive AoE heal for all allies
        register(SynergyDefinition.gemBased(
                "radiant_restoration",
                "gems.synergy.radiant_restoration",
                Set.of(GemId.BEACON, GemId.LIFE),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 3));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 400, 2));
                        // Heal nearby allies
                        for (LivingEntity other : getNearbyLiving(player, 20)) {
                            if (other instanceof ServerPlayerEntity otherPlayer) {
                                if (otherPlayer == player || GemTrust.isTrusted(player, otherPlayer)) {
                                    otherPlayer.heal(10.0f);
                                    otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
                                }
                            }
                        }
                    }
                }
        ));

        // AIR + PUFF = Cyclone: Create a vortex that pulls in and launches enemies
        register(SynergyDefinition.gemBased(
                "cyclone",
                "gems.synergy.cyclone",
                Set.of(GemId.AIR, GemId.PUFF),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        Vec3d playerPos = getPlayerPos(player);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other != player) {
                                boolean ally = other instanceof ServerPlayerEntity otherPlayer && 
                                              GemTrust.isTrusted(player, otherPlayer);
                                if (!ally) {
                                    // Pull towards caster then launch up
                                    Vec3d otherPos = new Vec3d(other.getX(), other.getY(), other.getZ());
                                    Vec3d direction = playerPos.subtract(otherPos).normalize();
                                    other.addVelocity(direction.x * 1.5, 1.5, direction.z * 1.5);
                                    other.velocityDirty = true;
                                }
                            }
                        }
                    }
                }
        ));

        // SUMMONER + HUNTER = Pack Alpha: All summoned creatures get massive damage boost
        register(SynergyDefinition.gemBased(
                "pack_alpha",
                "gems.synergy.pack_alpha",
                Set.of(GemId.SUMMONER, GemId.HUNTER),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 2));
                        // Give the player night vision to help track prey
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0));
                    }
                }
        ));

        // PILLAGER + WEALTH = Plunder: Enemies drop extra loot for a duration
        register(SynergyDefinition.gemBased(
                "plunder",
                "gems.synergy.plunder",
                Set.of(GemId.PILLAGER, GemId.WEALTH),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        // Give luck effect for increased drops
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 600, 3));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 300, 2));
                        player.sendMessage(Text.translatable("gems.synergy.plunder.active").formatted(Formatting.GOLD), true);
                    }
                }
        ));

        // DUELIST + SENTINEL = Guardian's Challenge: Draw all nearby enemies towards you, gain massive defense
        register(SynergyDefinition.gemBased(
                "guardians_challenge",
                "gems.synergy.guardians_challenge",
                Set.of(GemId.DUELIST, GemId.SENTINEL),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        // Gain massive defense
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 3));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0));
                        // Taunt nearby enemies (give them glowing so they're visible)
                        for (LivingEntity other : getNearbyLiving(player, 15)) {
                            if (other != player && other instanceof ServerPlayerEntity otherPlayer) {
                                if (!GemTrust.isTrusted(player, otherPlayer)) {
                                    otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0));
                                }
                            }
                        }
                    }
                }
        ));

        // TRICKSTER + CHAOS = Reality Warp: Random crazy effect on all nearby players
        register(SynergyDefinition.gemBased(
                "reality_warp",
                "gems.synergy.reality_warp",
                Set.of(GemId.TRICKSTER, GemId.CHAOS),
                60, 600,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        java.util.Random rand = new java.util.Random();
                        // Apply random effects to everyone nearby
                        for (LivingEntity other : getNearbyLiving(player, 20)) {
                            if (other instanceof ServerPlayerEntity otherPlayer) {
                                // Random effect pool
                                var effects = List.of(
                                        StatusEffects.SPEED, StatusEffects.SLOWNESS,
                                        StatusEffects.STRENGTH, StatusEffects.WEAKNESS,
                                        StatusEffects.JUMP_BOOST, StatusEffects.LEVITATION,
                                        StatusEffects.INVISIBILITY, StatusEffects.GLOWING,
                                        StatusEffects.REGENERATION, StatusEffects.POISON
                                );
                                var effect = effects.get(rand.nextInt(effects.size()));
                                otherPlayer.addStatusEffect(new StatusEffectInstance(effect, 100, rand.nextInt(3)));
                            }
                        }
                    }
                }
        ));

        // ==================== ABILITY-BASED SYNERGIES ====================
        // These trigger when specific abilities are cast within the window

        // FIREBALL + FLUX BEAM = Plasma Lance: Piercing plasma corridor with explosions and shock
        register(SynergyDefinition.abilityBased(
                "plasma_lance",
                "gems.synergy.plasma_lance",
                Set.of(GemId.FIRE, GemId.FLUX),
                Set.of(PowerIds.FIREBALL, PowerIds.FLUX_BEAM),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        Vec3d start = getPlayerPos(player).add(0, 1.4, 0);
                        Vec3d dir = player.getRotationVector().normalize();

                        AbilityFeedback.sound(player, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.2f, 0.9f);

                        for (int i = 1; i <= 6; i++) {
                            Vec3d point = start.add(dir.multiply(i * 3.0));
                            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 20, 0.6, 0.6, 0.6, 0.1);
                            world.createExplosion(player, point.x, point.y, point.z, 2.4f, net.minecraft.world.World.ExplosionSourceType.MOB);

                            Box box = new Box(point, point).expand(2.5);
                            for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive())) {
                                if (other == player) {
                                    continue;
                                }
                                if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                    continue;
                                }
                                other.setOnFireFor(4);
                                other.damage(world, player.getDamageSources().indirectMagic(player, player), 6.0f);
                            }
                        }
                    }
                }
        ));

        // METEOR SHOWER + ORBITAL LASER = Stellar Cataclysm: Falling fire rain + lightning impact
        register(SynergyDefinition.abilityBased(
                "stellar_cataclysm",
                "gems.synergy.stellar_cataclysm",
                Set.of(GemId.FIRE, GemId.SPACE),
                Set.of(PowerIds.METEOR_SHOWER, PowerIds.SPACE_ORBITAL_LASER),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        Vec3d center = getPlayerPos(player).add(player.getRotationVector().multiply(6));

                        var lightning = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
                        if (lightning != null) {
                            lightning.refreshPositionAfterTeleport(center.x, center.y, center.z);
                            world.spawnEntity(lightning);
                        }

                        world.createExplosion(player, center.x, center.y, center.z, 3.2f, net.minecraft.world.World.ExplosionSourceType.MOB);
                        world.spawnParticles(ParticleTypes.FLAME, center.x, center.y + 1, center.z, 60, 1.5, 1.0, 1.5, 0.08);

                        for (int i = 0; i < 8; i++) {
                            double angle = i * (Math.PI * 2.0 / 8.0);
                            double radius = 6.0;
                            Vec3d spawn = center.add(Math.cos(angle) * radius, 18.0, Math.sin(angle) * radius);
                            SmallFireballEntity fireball = new SmallFireballEntity(world, player, new Vec3d(0.0, -1.0, 0.0));
                            fireball.setPosition(spawn.x, spawn.y, spawn.z);
                            world.spawnEntity(fireball);
                        }
                    }
                }
        ));

        // GRAVITY FIELD + SLIPSTREAM = Gravity Weave: Allies surge forward, enemies are pulled and slowed
        register(SynergyDefinition.abilityBased(
                "gravity_weave",
                "gems.synergy.gravity_weave",
                Set.of(GemId.SPACE, GemId.SPEED),
                Set.of(PowerIds.SPACE_GRAVITY_FIELD, PowerIds.SPEED_SLIPSTREAM),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        Vec3d forward = player.getRotationVector().normalize();

                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other == player) {
                                continue;
                            }
                            boolean ally = other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer);
                            if (ally) {
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 3));
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 120, 2));
                                other.addVelocity(forward.x * 1.8, 0.2, forward.z * 1.8);
                                other.velocityDirty = true;
                            } else {
                                Vec3d pull = getPlayerPos(player).subtract(new Vec3d(other.getX(), other.getY(), other.getZ())).normalize();
                                other.addVelocity(pull.x * 1.4, 0.4, pull.z * 1.4);
                                other.velocityDirty = true;
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 3));
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
                            }
                        }

                        world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 40, 2.0, 1.0, 2.0, 0.03);
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.2f);
                    }
                }
        ));

        // SMOKE BOMB + GLITCH STEP = Shadow Relay: Blink behind a nearby enemy and cripple them
        register(SynergyDefinition.abilityBased(
                "shadow_relay",
                "gems.synergy.shadow_relay",
                Set.of(GemId.SPY, GemId.TRICKSTER),
                Set.of(PowerIds.SPY_SMOKE_BOMB, PowerIds.TRICKSTER_GLITCH_STEP),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();

                        LivingEntity target = null;
                        double bestDist = Double.MAX_VALUE;
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other == player) {
                                continue;
                            }
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                continue;
                            }
                            double d = other.squaredDistanceTo(player);
                            if (d < bestDist) {
                                bestDist = d;
                                target = other;
                            }
                        }

                        Vec3d origin = getPlayerPos(player);
                        if (target != null) {
                            Vec3d behind = new Vec3d(target.getX(), target.getY(), target.getZ())
                                    .subtract(target.getRotationVector().normalize().multiply(2.0));
                            GemsTeleport.teleport(player, world, behind.x, behind.y, behind.z, player.getYaw(), player.getPitch());
                            target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0));
                            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
                        }

                        world.spawnParticles(ParticleTypes.SMOKE, origin.x, origin.y + 1, origin.z, 40, 1.2, 0.6, 1.2, 0.02);
                        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, new Box(origin, origin).expand(6), e -> e.isAlive())) {
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                continue;
                            }
                            if (other != player) {
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
                            }
                        }
                    }
                }
        ));

        // BLOOD CHARGE + LIFE CIRCLE = Blood Convergence: Drain enemies, supercharge allies
        register(SynergyDefinition.abilityBased(
                "blood_convergence",
                "gems.synergy.blood_convergence",
                Set.of(GemId.REAPER, GemId.LIFE),
                Set.of(PowerIds.REAPER_BLOOD_CHARGE, PowerIds.LIFE_CIRCLE),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();

                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) {
                                continue;
                            }
                            boolean ally = other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer);
                            if (ally) {
                                other.heal(6.0f);
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 2));
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 2));
                            } else {
                                other.damage(world, player.getDamageSources().indirectMagic(player, player), 8.0f);
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
                            }
                        }

                        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(), 45, 1.8, 0.8, 1.8, 0.02);
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_WITHER_AMBIENT, 0.9f, 1.0f);
                    }
                }
        ));

        // FIREBALL + BLACK HOLE = Singularity Ignition: Pull and incinerate enemies at impact
        register(SynergyDefinition.abilityBased(
                "singularity_ignition",
                "gems.synergy.singularity_ignition",
                Set.of(GemId.FIRE, GemId.SPACE),
                Set.of(PowerIds.FIREBALL, PowerIds.SPACE_BLACK_HOLE),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        Vec3d center = getPlayerPos(player).add(player.getRotationVector().multiply(6));

                        world.spawnParticles(ParticleTypes.PORTAL, center.x, center.y + 1, center.z, 80, 1.2, 1.2, 1.2, 0.05);
                        world.spawnParticles(ParticleTypes.FLAME, center.x, center.y + 1, center.z, 60, 1.0, 0.6, 1.0, 0.03);
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_WITHER_SPAWN, 0.6f, 1.1f);
                        world.createExplosion(player, center.x, center.y, center.z, 2.8f, net.minecraft.world.World.ExplosionSourceType.MOB);

                        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(9), e -> e.isAlive())) {
                            if (other == player) {
                                continue;
                            }
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                continue;
                            }
                            Vec3d pull = center.subtract(new Vec3d(other.getX(), other.getY(), other.getZ())).normalize();
                            other.addVelocity(pull.x * 1.4, 0.5, pull.z * 1.4);
                            other.velocityDirty = true;
                            other.setOnFireFor(6);
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 1));
                        }
                    }
                }
        ));

        // FLUX DISCHARGE + CROSSWIND = Ion Burst: Thunderous shockwave
        register(SynergyDefinition.abilityBased(
                "ion_burst",
                "gems.synergy.ion_burst",
                Set.of(GemId.FLUX, GemId.AIR),
                Set.of(PowerIds.FLUX_DISCHARGE, PowerIds.AIR_CROSSWIND),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.3f);
                        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 80, 2.0, 1.0, 2.0, 0.1);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                                continue;
                            }
                            Vec3d dir = new Vec3d(other.getX() - player.getX(), 0.0, other.getZ() - player.getZ()).normalize();
                            other.addVelocity(dir.x * 1.4, 0.6, dir.z * 1.4);
                            other.velocityDirty = true;
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 1));
                        }
                    }
                }
        ));

        // VITALITY VORTEX + BEACON REGEN AURA = Sanctuary Pulse: Cleanse and massive heal
        register(SynergyDefinition.abilityBased(
                "sanctuary_pulse",
                "gems.synergy.sanctuary_pulse",
                Set.of(GemId.LIFE, GemId.BEACON),
                Set.of(PowerIds.VITALITY_VORTEX, PowerIds.BEACON_AURA_REGEN),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1, player.getZ(), 40, 1.6, 0.8, 1.6, 0.02);
                        AbilityFeedback.sound(player, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);

                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other instanceof ServerPlayerEntity otherPlayer && (otherPlayer == player || GemTrust.isTrusted(player, otherPlayer))) {
                                otherPlayer.heal(8.0f);
                                otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2));
                                otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 2));
                                otherPlayer.removeStatusEffect(StatusEffects.POISON);
                                otherPlayer.removeStatusEffect(StatusEffects.WITHER);
                                otherPlayer.removeStatusEffect(StatusEffects.WEAKNESS);
                                otherPlayer.removeStatusEffect(StatusEffects.SLOWNESS);
                                otherPlayer.removeStatusEffect(StatusEffects.BLINDNESS);
                            }
                        }
                    }
                }
        ));

        // SCYTHE SWEEP + NET SHOT = Reaping Snare: Root and wither
        register(SynergyDefinition.abilityBased(
                "reaping_snare",
                "gems.synergy.reaping_snare",
                Set.of(GemId.REAPER, GemId.HUNTER),
                Set.of(PowerIds.REAPER_SCYTHE_SWEEP, PowerIds.HUNTER_NET_SHOT),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 40, 1.2, 0.6, 1.2, 0.02);
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_WITHER_HURT, 0.7f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 3));
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
                            other.damage(world, player.getDamageSources().indirectMagic(player, player), 5.0f);
                        }
                    }
                }
        ));

        // AFTERIMAGE + SMOKE BOMB = Ghostline: Cloaked assault
        register(SynergyDefinition.abilityBased(
                "ghostline",
                "gems.synergy.ghostline",
                Set.of(GemId.SPEED, GemId.SPY),
                Set.of(PowerIds.SPEED_AFTERIMAGE, PowerIds.SPY_SMOKE_BOMB),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 120, 0));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 2));
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1, player.getZ(), 50, 1.4, 0.6, 1.4, 0.02);
                        for (LivingEntity other : getNearbyLiving(player, 6)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
                            other.damage(world, player.getDamageSources().indirectMagic(player, player), 3.0f);
                        }
                    }
                }
        ));

        // PARRY + SHIELD WALL = Bulwark Riposte: Shockwave counter
        register(SynergyDefinition.abilityBased(
                "bulwark_riposte",
                "gems.synergy.bulwark_riposte",
                Set.of(GemId.DUELIST, GemId.SENTINEL),
                Set.of(PowerIds.DUELIST_PARRY, PowerIds.SENTINEL_SHIELD_WALL),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 160, 2));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 1));
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1, player.getZ(), 20, 1.2, 0.6, 1.2, 0.0);
                        AbilityFeedback.sound(player, SoundEvents.ITEM_SHIELD_BLOCK, 0.8f, 0.9f);
                        for (LivingEntity other : getNearbyLiving(player, 6)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            Vec3d dir = new Vec3d(other.getX() - player.getX(), 0.0, other.getZ() - player.getZ()).normalize();
                            other.addVelocity(dir.x * 1.1, 0.4, dir.z * 1.1);
                            other.velocityDirty = true;
                            other.damage(world, player.getDamageSources().indirectMagic(player, player), 4.0f);
                        }
                    }
                }
        ));

        // SUMMON SLOT 1 + CALL THE PACK = Alpha Swarm: Pack frenzy
        register(SynergyDefinition.abilityBased(
                "alpha_swarm",
                "gems.synergy.alpha_swarm",
                Set.of(GemId.SUMMONER, GemId.HUNTER),
                Set.of(PowerIds.SUMMON_SLOT_1, PowerIds.HUNTER_CALL_THE_PACK),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other instanceof ServerPlayerEntity otherPlayer) {
                                if (otherPlayer == player || GemTrust.isTrusted(player, otherPlayer)) {
                                    otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 160, 2));
                                    otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 1));
                                } else {
                                    otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 120, 0));
                                    otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 0));
                                }
                            }
                        }
                    }
                }
        ));

        // BREACH CHARGE + WARHORN = Siegebreak: Blast and debuff
        register(SynergyDefinition.abilityBased(
                "siegebreak",
                "gems.synergy.siegebreak",
                Set.of(GemId.TERROR, GemId.PILLAGER),
                Set.of(PowerIds.TERROR_BREACH_CHARGE, PowerIds.PILLAGER_WARHORN),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        Vec3d center = getPlayerPos(player).add(player.getRotationVector().multiply(4));
                        world.createExplosion(player, center.x, center.y, center.z, 2.6f, net.minecraft.world.World.ExplosionSourceType.MOB);
                        world.spawnParticles(ParticleTypes.EXPLOSION, center.x, center.y + 1, center.z, 12, 0.6, 0.6, 0.6, 0.0);
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_RAVAGER_ROAR, 0.9f, 0.9f);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1));
                        }
                    }
                }
        ));

        // FLUX SURGE + TEMPO SHIFT = Overclock: Burst cooldowns for allies
        register(SynergyDefinition.abilityBased(
                "overclock",
                "gems.synergy.overclock",
                Set.of(GemId.FLUX, GemId.SPEED),
                Set.of(PowerIds.FLUX_SURGE, PowerIds.SPEED_TEMPO_SHIFT),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 60, 1.4, 0.7, 1.4, 0.12);
                        AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.7f, 1.4f);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (!(other instanceof ServerPlayerEntity target)) {
                                continue;
                            }
                            boolean ally = target == player || GemTrust.isTrusted(player, target);
                            if (ally) {
                                long now = world.getTime();
                                for (Identifier id : com.feel.gems.power.registry.ModAbilities.all().keySet()) {
                                    long next = com.feel.gems.power.runtime.GemAbilityCooldowns.nextAllowedTick(target, id);
                                    if (next > now) {
                                        com.feel.gems.power.runtime.GemAbilityCooldowns.setNextAllowedTick(target, id, Math.max(now, next - 100));
                                    }
                                }
                            } else {
                                target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 120, 1));
                            }
                        }
                    }
                }
        ));

        // WHITE HOLE + GUST = Vacuum Reversal: Blast enemies away
        register(SynergyDefinition.abilityBased(
                "vacuum_reversal",
                "gems.synergy.vacuum_reversal",
                Set.of(GemId.SPACE, GemId.PUFF),
                Set.of(PowerIds.SPACE_WHITE_HOLE, PowerIds.PUFF_GUST),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 50, 2.2, 1.0, 2.2, 0.04);
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other == player) continue;
                            boolean ally = other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer);
                            if (ally) {
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 120, 0));
                            } else {
                                Vec3d dir = new Vec3d(other.getX() - player.getX(), 0.0, other.getZ() - player.getZ()).normalize();
                                other.addVelocity(dir.x * 1.6, 0.8, dir.z * 1.6);
                                other.velocityDirty = true;
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 40, 0));
                            }
                        }
                    }
                }
        ));

        // BREEZY BASH + TERMINAL VELOCITY = Sonic Uppercut
        register(SynergyDefinition.abilityBased(
                "sonic_uppercut",
                "gems.synergy.sonic_uppercut",
                Set.of(GemId.PUFF, GemId.SPEED),
                Set.of(PowerIds.BREEZY_BASH, PowerIds.TERMINAL_VELOCITY),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.2f);
                        for (LivingEntity other : getNearbyLiving(player, 7)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.addVelocity(0.0, 1.2, 0.0);
                            other.velocityDirty = true;
                            other.damage(world, player.getDamageSources().indirectMagic(player, player), 6.0f);
                        }
                    }
                }
        ));

        // ORBITAL LASER + LOCKDOWN = Judgment Field: Mark and slow enemies
        register(SynergyDefinition.abilityBased(
                "judgment_field",
                "gems.synergy.judgment_field",
                Set.of(GemId.SPACE, GemId.SENTINEL),
                Set.of(PowerIds.SPACE_ORBITAL_LASER, PowerIds.SENTINEL_LOCKDOWN),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        Vec3d center = getPlayerPos(player).add(player.getRotationVector().multiply(5));
                        var lightning = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
                        if (lightning != null) {
                            lightning.refreshPositionAfterTeleport(center.x, center.y, center.z);
                            world.spawnEntity(lightning);
                        }
                        world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y + 1, center.z, 60, 2.0, 1.0, 2.0, 0.02);
                        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(8), e -> e.isAlive())) {
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            if (other != player) {
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 140, 0));
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 140, 2));
                            }
                        }
                    }
                }
        ));

        // HEAT HAZE + REMOTE CHARGE = Ember Chain
        register(SynergyDefinition.abilityBased(
                "ember_chain",
                "gems.synergy.ember_chain",
                Set.of(GemId.FIRE, GemId.TERROR),
                Set.of(PowerIds.HEAT_HAZE_ZONE, PowerIds.TERROR_REMOTE_CHARGE),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_BLAZE_SHOOT, 0.8f, 1.0f);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.setOnFireFor(6);
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
                        }
                        world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 70, 2.0, 0.8, 2.0, 0.05);
                    }
                }
        ));

        // SHADOW CLONE + ASTRAL DAGGERS = Shadow Barrage
        register(SynergyDefinition.abilityBased(
                "shadow_barrage",
                "gems.synergy.shadow_barrage",
                Set.of(GemId.REAPER, GemId.ASTRA),
                Set.of(PowerIds.REAPER_SHADOW_CLONE, PowerIds.ASTRAL_DAGGERS),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 50, 1.4, 0.6, 1.4, 0.1);
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_SCREAM, 0.6f, 1.3f);
                        for (LivingEntity other : getNearbyLiving(player, 8)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.damage(world, player.getDamageSources().indirectMagic(player, player), 5.0f);
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
                        }
                    }
                }
        ));

        // SKINSHIFT + MIRAGE = Mirror Heist: Confuse and vanish
        register(SynergyDefinition.abilityBased(
                "mirror_heist",
                "gems.synergy.mirror_heist",
                Set.of(GemId.SPY, GemId.TRICKSTER),
                Set.of(PowerIds.SPY_SKINSHIFT, PowerIds.TRICKSTER_MIRAGE),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 120, 0));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 1));
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 40, 1.2, 0.6, 1.2, 0.02);
                        for (LivingEntity other : getNearbyLiving(player, 10)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0));
                        }
                    }
                }
        ));

        // HEALTH DRAIN + RETRIBUTION = Harvest Tide
        register(SynergyDefinition.abilityBased(
                "harvest_tide",
                "gems.synergy.harvest_tide",
                Set.of(GemId.LIFE, GemId.REAPER),
                Set.of(PowerIds.HEALTH_DRAIN, PowerIds.REAPER_RETRIBUTION),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_WITHER_SHOOT, 0.7f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 9)) {
                            if (other == player) continue;
                            boolean ally = other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer);
                            if (ally) {
                                other.heal(4.0f);
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 160, 1));
                            } else {
                                other.damage(world, player.getDamageSources().indirectMagic(player, player), 6.0f);
                                other.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 0));
                            }
                        }
                        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(), 40, 1.2, 0.6, 1.2, 0.02);
                    }
                }
        ));

        // FLUX BEAM + VINDICATOR BREAK = Stormbreaker
        register(SynergyDefinition.abilityBased(
                "stormbreaker",
                "gems.synergy.stormbreaker",
                Set.of(GemId.FLUX, GemId.PILLAGER),
                Set.of(PowerIds.FLUX_BEAM, PowerIds.PILLAGER_VINDICATOR_BREAK),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.7f, 1.2f);
                        for (LivingEntity other : getNearbyLiving(player, 8)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.damage(world, player.getDamageSources().indirectMagic(player, player), 7.0f);
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
                        }
                        world.spawnParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 30, 1.0, 0.4, 1.0, 0.2);
                    }
                }
        ));

        // GRAVITY FIELD + AIR DASH = Celestial Stride
        register(SynergyDefinition.abilityBased(
                "celestial_stride",
                "gems.synergy.celestial_stride",
                Set.of(GemId.SPACE, GemId.AIR),
                Set.of(PowerIds.SPACE_GRAVITY_FIELD, PowerIds.AIR_DASH),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        Vec3d playerPos = getPlayerPos(player);
                        Vec3d forward = player.getRotationVector().multiply(8);
                        Vec3d target = playerPos.add(forward);
                        GemsTeleport.teleport(player, player.getEntityWorld(), target.x, target.y, target.z, player.getYaw(), player.getPitch());
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 140, 0));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 140, 1));
                        player.getEntityWorld().spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.8, 0.4, 0.8, 0.02);
                    }
                }
        ));

        // BEACON RESISTANCE AURA + RALLY CRY = Iron Chorus
        register(SynergyDefinition.abilityBased(
                "iron_chorus",
                "gems.synergy.iron_chorus",
                Set.of(GemId.BEACON, GemId.SENTINEL),
                Set.of(PowerIds.BEACON_AURA_RESISTANCE, PowerIds.SENTINEL_RALLY_CRY),
                60, 1200,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.1f);
                        for (LivingEntity other : getNearbyLiving(player, 12)) {
                            if (other instanceof ServerPlayerEntity otherPlayer && (otherPlayer == player || GemTrust.isTrusted(player, otherPlayer))) {
                                otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 2));
                                otherPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 2));
                                otherPlayer.removeStatusEffect(StatusEffects.WITHER);
                                otherPlayer.removeStatusEffect(StatusEffects.POISON);
                            }
                        }
                        world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 50, 1.6, 0.8, 1.6, 0.01);
                    }
                }
        ));

        // POUNCE + LUNGE = Predator Lunge
        register(SynergyDefinition.abilityBased(
                "predator_lunge",
                "gems.synergy.predator_lunge",
                Set.of(GemId.HUNTER, GemId.DUELIST),
                Set.of(PowerIds.HUNTER_POUNCE, PowerIds.DUELIST_LUNGE),
                60, 900,
                participants -> {
                    for (var p : participants) {
                        ServerPlayerEntity player = p.player();
                        ServerWorld world = player.getEntityWorld();
                        world.spawnParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 40, 1.2, 0.6, 1.2, 0.2);
                        AbilityFeedback.sound(player, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.2f);
                        for (LivingEntity other : getNearbyLiving(player, 7)) {
                            if (other == player) continue;
                            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) continue;
                            other.damage(world, player.getDamageSources().indirectMagic(player, player), 6.0f);
                            other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
                        }
                    }
                }
        ));
    }
}
