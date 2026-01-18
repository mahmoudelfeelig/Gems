package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HunterHuntingTrapAbility implements GemAbility {
    public static final Map<TrapKey, TrapData> ACTIVE_TRAPS = new HashMap<>();

    public record TrapKey(String worldId, BlockPos pos) {}

    @Override
    public Identifier id() {
        return PowerIds.HUNTER_HUNTING_TRAP;
    }

    @Override
    public String name() {
        return "Hunting Trap";
    }

    @Override
    public String description() {
        return "Place an invisible trap that roots and damages the first enemy who walks over it.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().huntingTrapCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        BlockPos trapPos = player.getBlockPos();
        String worldId = world.getRegistryKey().getValue().toString();

        int lifetimeTicks = GemsBalance.v().hunter().huntingTrapLifetimeTicks();
        long expireTime = world.getTime() + lifetimeTicks;

        TrapData trap = new TrapData(player.getUuid(), expireTime, worldId);
        ACTIVE_TRAPS.put(new TrapKey(worldId, trapPos), trap);

        AbilityFeedback.burstAt(world, Vec3d.ofCenter(trapPos), ParticleTypes.ENCHANT, 15, 0.3D);
        AbilityFeedback.sound(player, SoundEvents.BLOCK_TRIPWIRE_ATTACH, 0.5F, 1.2F);
        return true;
    }

    public static void checkStep(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        ServerWorld world = player.getEntityWorld();
        String worldId = world.getRegistryKey().getValue().toString();
        BlockPos pos = player.getBlockPos();
        TrapKey key = new TrapKey(worldId, pos);
        TrapData trap = ACTIVE_TRAPS.get(key);
        if (trap == null) {
            return;
        }
        if (player.getUuid().equals(trap.ownerId)) {
            return;
        }

        ServerPlayerEntity owner = null;
        MinecraftServer server = world.getServer();
        if (server != null) {
            owner = server.getPlayerManager().getPlayer(trap.ownerId);
            if (owner != null) {
                if (com.feel.gems.trust.GemTrust.isTrusted(owner, player)) {
                    return;
                }
                if (VoidImmunity.shouldBlockEffect(owner, player)) {
                    ACTIVE_TRAPS.remove(key);
                    return;
                }
            }
        }

        TrapData consumed = ACTIVE_TRAPS.remove(key);
        if (consumed == null) {
            return;
        }
        triggerTrap(consumed, player, pos, world, owner);
    }

    public static void tickWorld(ServerWorld world) {
        if (world == null || ACTIVE_TRAPS.isEmpty()) {
            return;
        }

        String worldId = world.getRegistryKey().getValue().toString();
        MinecraftServer server = world.getServer();
        long now = world.getTime();

        Iterator<Map.Entry<TrapKey, TrapData>> it = ACTIVE_TRAPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<TrapKey, TrapData> entry = it.next();
            TrapKey trapKey = entry.getKey();
            if (!worldId.equals(trapKey.worldId)) {
                continue;
            }
            BlockPos pos = trapKey.pos;
            TrapData trap = entry.getValue();
            if (trap.expireTime <= now) {
                it.remove();
                continue;
            }

            // Trigger on mobs that walk over the trap too, but keep the per-player fast-path in checkStep().
            Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            var mobs = world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class, box,
                    e -> e.isAlive() && !(e instanceof ServerPlayerEntity));
            if (mobs.isEmpty()) {
                continue;
            }

            ServerPlayerEntity owner = server != null ? server.getPlayerManager().getPlayer(trap.ownerId) : null;
            net.minecraft.entity.LivingEntity victim = mobs.getFirst();

            // Consume trap.
            it.remove();

            triggerTrap(trap, victim, pos, world, owner);
        }
    }

    private static void triggerTrap(TrapData trap, net.minecraft.entity.LivingEntity victim, BlockPos pos, ServerWorld world, ServerPlayerEntity owner) {
        float damage = GemsBalance.v().hunter().huntingTrapDamage();
        int rootTicks = GemsBalance.v().hunter().huntingTrapRootTicks();

        if (owner != null) {
            victim.damage(world, owner.getDamageSources().indirectMagic(owner, owner), damage);
        } else {
            victim.damage(world, world.getDamageSources().magic(), damage);
        }
        // Max slowness = rooted
        if (owner != null && victim instanceof ServerPlayerEntity playerVictim) {
            // Respect Void immunity / trust for players; mobs are always valid victims.
            if (com.feel.gems.trust.GemTrust.isTrusted(owner, playerVictim) || VoidImmunity.shouldBlockEffect(owner, playerVictim)) {
                return;
            }
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, rootTicks, 127, true, false, false), owner);
        } else {
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, rootTicks, 127, true, false, false));
        }

        AbilityFeedback.burstAt(world, Vec3d.ofCenter(pos).add(0, 1, 0), ParticleTypes.CRIT, 20, 0.5D);
        if (victim instanceof ServerPlayerEntity playerVictim) {
            AbilityFeedback.sound(playerVictim, SoundEvents.ENTITY_IRON_GOLEM_HURT, 1.0F, 0.5F);
        } else {
            world.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.ENTITY_IRON_GOLEM_HURT,
                    net.minecraft.sound.SoundCategory.PLAYERS,
                    1.0F, 0.5F);
        }
    }

    public static void cleanExpiredTraps(ServerWorld world) {
        long currentTime = world.getTime();
        String worldId = world.getRegistryKey().getValue().toString();
        ACTIVE_TRAPS.entrySet().removeIf(entry ->
                entry.getKey().worldId.equals(worldId) && currentTime > entry.getValue().expireTime
        );
    }

    public record TrapData(UUID ownerId, long expireTime, String worldId) {}
}
