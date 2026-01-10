package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.entity.ShadowCloneEntity;
import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;



public final class ReaperShadowCloneAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_SHADOW_CLONE;
    }

    @Override
    public String name() {
        return "Shadow Clone";
    }

    @Override
    public String description() {
        return "Summons invincible decoys around you that vanish after a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().shadowCloneCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var cfg = GemsBalance.v().reaper();
        int duration = cfg.shadowCloneDurationTicks();
        int count = cfg.shadowCloneCount();
        if (duration <= 0 || count <= 0) {
            player.sendMessage(Text.translatable("gems.ability.reaper.shadow_clone.disabled"), true);
            return false;
        }

        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        
        List<java.util.UUID> spawned = new ArrayList<>();
        float yaw = player.getYaw();
        double baseRadius = 1.5D;
        
        for (int i = 0; i < count; i++) {
            Entity entity = ModEntities.SHADOW_CLONE.create(world, SpawnReason.MOB_SUMMONED);
            if (!(entity instanceof ShadowCloneEntity clone)) {
                continue;
            }
            
            double angle = (Math.PI * 2.0D) * (i / (double) count);
            double offset = baseRadius + (world.random.nextDouble() * 0.6D);
            double dx = Math.cos(angle) * offset;
            double dz = Math.sin(angle) * offset;
            Vec3d pos = new Vec3d(player.getX() + dx, player.getY(), player.getZ() + dz);

            clone.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw + (world.random.nextFloat() - 0.5F) * 30F, 0.0F);
            clone.setOwner(player);
            clone.setMaxLifetime(duration); // Set auto-despawn lifetime

            world.spawnEntity(clone);
            spawned.add(clone.getUuid());
            
            // Send sync payload to all nearby players for skin rendering
            ShadowCloneSyncPayload syncPayload = new ShadowCloneSyncPayload(
                    clone.getId(),
                    player.getUuid(),
                    player.getGameProfile().name()
            );
            for (ServerPlayerEntity tracker : PlayerLookup.tracking(clone)) {
                ServerPlayNetworking.send(tracker, syncPayload);
            }
            // Also send to the caster
            ServerPlayNetworking.send(player, syncPayload);
        }

        if (spawned.isEmpty()) {
            player.sendMessage(Text.translatable("gems.ability.reaper.shadow_clone.failed"), true);
            return false;
        }

        AbilityRuntime.startReaperShadowClone(player, spawned, duration);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.6F, 0.6F);
        player.sendMessage(Text.translatable("gems.ability.reaper.shadow_clone.spawned"), true);
        return true;
    }
}
