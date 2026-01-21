package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.entity.ShadowCloneEntity;
import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Mirage - create a single illusory copy that mirrors your movements.
 * The clone despawns on hit or when the duration ends, granting you a brief buff.
 */
public final class TricksterMirageAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_MIRAGE;
    }

    @Override
    public String name() {
        return "Mirage";
    }

    @Override
    public String description() {
        return "Create a mirage clone at your aim point that mirrors your movements and grants a buff when it breaks.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().mirageCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        var cfg = GemsBalance.v().trickster();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.TRICKSTER, cfg.mirageDurationTicks());
        int rangeBlocks = cfg.mirageRangeBlocks();
        int enabled = cfg.mirageCloneCount();
        if (durationTicks <= 0 || enabled <= 0) {
            return false;
        }

        Vec3d center = player.getEntityPos();
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d targetPos = center.add(direction.multiply(rangeBlocks));
        HitResult hit = player.raycast(rangeBlocks, 1.0F, false);
        if (hit.getType() != HitResult.Type.MISS) {
            targetPos = hit.getPos();
        }
        long endTime = world.getTime() + durationTicks;

        Entity entity = ModEntities.SHADOW_CLONE.create(world, SpawnReason.MOB_SUMMONED);
        if (!(entity instanceof ShadowCloneEntity clone)) {
            return false;
        }
        clone.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, player.getYaw(), player.getPitch());
        clone.setOwner(player);
        clone.setMaxLifetime(durationTicks);
        clone.setAiDisabled(true);
        clone.setNoGravity(true);
        clone.setMirageClone(true);
        world.spawnEntity(clone);

        UUID cloneUuid = clone.getUuid();
        Vec3d offset = targetPos.subtract(center);
        TricksterMirageRuntime.createMirage(player, cloneUuid, offset, endTime);

        ShadowCloneSyncPayload syncPayload = new ShadowCloneSyncPayload(
                clone.getId(),
                player.getUuid(),
                player.getGameProfile().name()
        );
        for (ServerPlayerEntity tracker : PlayerLookup.tracking(clone)) {
            ServerPlayNetworking.send(tracker, syncPayload);
        }
        ServerPlayNetworking.send(player, syncPayload);

        AbilityFeedback.burstAt(world, targetPos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 18, 0.5D);
        AbilityFeedback.burstAt(world, targetPos.add(0, 1.2, 0), ParticleTypes.ENCHANT, 10, 0.6D);
        AbilityFeedback.burstAt(world, targetPos.add(0, 0.8, 0), ParticleTypes.END_ROD, 8, 0.4D);
        AbilityFeedback.burstAt(world, center.add(0, 1.0, 0), ParticleTypes.PORTAL, 20, 0.7D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);
        return true;
    }
}
