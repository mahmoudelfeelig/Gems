package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HunterHuntingTrapAbility implements GemAbility {
    public static final Map<BlockPos, TrapData> ACTIVE_TRAPS = new HashMap<>();

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

        int lifetimeTicks = GemsBalance.v().hunter().huntingTrapLifetimeTicks();
        long expireTime = world.getTime() + lifetimeTicks;

        TrapData trap = new TrapData(player.getUuid(), expireTime, world.getRegistryKey().getValue().toString());
        ACTIVE_TRAPS.put(trapPos, trap);

        AbilityFeedback.burstAt(world, Vec3d.ofCenter(trapPos), ParticleTypes.ENCHANT, 15, 0.3D);
        AbilityFeedback.sound(player, SoundEvents.BLOCK_TRIPWIRE_ATTACH, 0.5F, 1.2F);
        return true;
    }

    public static void triggerTrap(ServerPlayerEntity victim, BlockPos pos, ServerWorld world) {
        TrapData trap = ACTIVE_TRAPS.remove(pos);
        if (trap == null) return;

        float damage = GemsBalance.v().hunter().huntingTrapDamage();
        int rootTicks = GemsBalance.v().hunter().huntingTrapRootTicks();

        victim.damage(world, world.getDamageSources().magic(), damage);
        victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, rootTicks, 127, false, false)); // Max slowness = rooted

        AbilityFeedback.burstAt(world, Vec3d.ofCenter(pos).add(0, 1, 0), ParticleTypes.CRIT, 20, 0.5D);
        AbilityFeedback.sound(victim, SoundEvents.ENTITY_IRON_GOLEM_HURT, 1.0F, 0.5F);
    }

    public static void cleanExpiredTraps(ServerWorld world) {
        long currentTime = world.getTime();
        String worldId = world.getRegistryKey().getValue().toString();
        ACTIVE_TRAPS.entrySet().removeIf(entry -> 
            entry.getValue().worldId.equals(worldId) && currentTime > entry.getValue().expireTime
        );
    }

    public record TrapData(UUID ownerId, long expireTime, String worldId) {}
}
