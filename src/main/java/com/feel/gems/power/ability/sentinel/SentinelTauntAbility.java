package com.feel.gems.power.ability.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class SentinelTauntAbility implements GemAbility {
    public static final String TAUNT_ACTIVE_KEY = "sentinel_taunt_active";
    public static final String TAUNT_END_KEY = "sentinel_taunt_end";

    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_TAUNT;
    }

    @Override
    public String name() {
        return "Taunt";
    }

    @Override
    public String description() {
        return "Force nearby enemies to target you for 5s; gain damage reduction during taunt.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().tauntCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int radius = GemsBalance.v().sentinel().tauntRadiusBlocks();
        int durationTicks = GemsBalance.v().sentinel().tauntDurationTicks();

        long endTime = world.getTime() + durationTicks;
        PlayerStateManager.setPersistent(player, TAUNT_ACTIVE_KEY, "true");
        PlayerStateManager.setPersistent(player, TAUNT_END_KEY, String.valueOf(endTime));

        // Apply taunt effect to nearby enemies
        Box box = player.getBoundingBox().expand(radius);
        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof ServerPlayerEntity)) {
            ServerPlayerEntity target = (ServerPlayerEntity) e;
            if (GemTrust.isTrusted(player, target)) continue;
            if (!VoidImmunity.canBeTargeted(player, target)) continue;

            // Mark them as taunted by this player
            SentinelTauntRuntime.applyTaunt(target, player.getUuid(), durationTicks);
        }

        // Grant resistance to self
        int resistanceAmplifier = (int) ((1.0F - GemsBalance.v().sentinel().tauntDamageReduction()) * 4); // Rough conversion
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, durationTicks, resistanceAmplifier, false, true));

        AbilityFeedback.ring(world, player.getEntityPos().add(0, 1, 0), radius, ParticleTypes.ANGRY_VILLAGER, 24);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_RAVAGER_ROAR, 1.0F, 1.2F);
        return true;
    }

    public static boolean isTaunting(ServerPlayerEntity player) {
        String active = PlayerStateManager.getPersistent(player, TAUNT_ACTIVE_KEY);
        if (!"true".equals(active)) return false;

        String endStr = PlayerStateManager.getPersistent(player, TAUNT_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        return player.getEntityWorld().getTime() < endTime;
    }
}
