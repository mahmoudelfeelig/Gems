package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registries;



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

        Identifier entityId = Identifier.tryParse(cfg.shadowCloneEntityId());
        if (entityId == null) {
            player.sendMessage(Text.translatable("gems.ability.reaper.shadow_clone.invalid_id"), true);
            return false;
        }

        EntityType<?> type = Registries.ENTITY_TYPE.get(entityId);
        if (type == null) {
            player.sendMessage(Text.translatable("gems.ability.reaper.shadow_clone.missing_id"), true);
            return false;
        }

        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        List<java.util.UUID> spawned = new ArrayList<>();
        float yaw = player.getYaw();
        double baseRadius = 1.2D;
        for (int i = 0; i < count; i++) {
            Entity entity = type.create(world, SpawnReason.MOB_SUMMONED);
            if (!(entity instanceof MobEntity mob)) {
                continue;
            }
            double angle = (Math.PI * 2.0D) * (i / (double) count);
            double offset = baseRadius + (world.random.nextDouble() * 0.6D);
            double dx = Math.cos(angle) * offset;
            double dz = Math.sin(angle) * offset;
            Vec3d pos = new Vec3d(player.getX() + dx, player.getY(), player.getZ() + dz);

            mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0F);
            mob.initialize(world, world.getLocalDifficulty(BlockPos.ofFloored(pos)), SpawnReason.MOB_SUMMONED, null);
            mob.setAiDisabled(true);
            mob.setSilent(true);
            mob.setInvulnerable(true);
            mob.setPersistent();
            mob.setCustomName(player.getDisplayName());
            mob.setCustomNameVisible(false);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!slot.isArmorSlot() && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
                    continue;
                }
                mob.equipStack(slot, player.getEquippedStack(slot).copy());
                mob.setEquipmentDropChance(slot, 0.0F);
            }

            float maxHealth = mob.getMaxHealth();
            float targetHealth = Math.min(maxHealth, cfg.shadowCloneMaxHealth());
            if (targetHealth > 0.0F) {
                mob.setHealth(targetHealth);
            }

            world.spawnEntity(mob);
            spawned.add(mob.getUuid());
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
