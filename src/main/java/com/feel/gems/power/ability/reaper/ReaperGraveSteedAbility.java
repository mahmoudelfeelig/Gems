package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class ReaperGraveSteedAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_GRAVE_STEED;
    }

    @Override
    public String name() {
        return "Grave Steed";
    }

    @Override
    public String description() {
        return "Summons a saddled skeleton horse mount that decays over time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().graveSteedCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().reaper().graveSteedDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.literal("Grave Steed is disabled."), true);
            return false;
        }
        if (!(player.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld world)) {
            return false;
        }
        SkeletonHorseEntity horse = EntityType.SKELETON_HORSE.create(world, SpawnReason.MOB_SUMMONED);
        if (horse == null) {
            player.sendMessage(Text.literal("Failed to summon steed."), true);
            return false;
        }
        horse.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
        horse.setTame(true);
        horse.setOwner(player);
        horse.equipStack(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        horse.setTrapped(false);
        world.spawnEntity(horse);
        player.startRiding(horse);

        AbilityRuntime.startReaperGraveSteed(player, horse.getUuid(), duration);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_SKELETON_HORSE_AMBIENT, 0.9F, 1.0F);
        player.sendMessage(Text.literal("Grave Steed summoned."), true);
        return true;
    }
}
