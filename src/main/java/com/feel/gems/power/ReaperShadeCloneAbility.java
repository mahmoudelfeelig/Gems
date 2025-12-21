package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ReaperShadeCloneAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_SHADE_CLONE;
    }

    @Override
    public String name() {
        return "Shade Clone";
    }

    @Override
    public String description() {
        return "Spawns a decoy that looks like you and vanishes after a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().shadeCloneCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().reaper().shadeCloneDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.literal("Shade Clone is disabled."), true);
            return false;
        }
        var world = player.getServerWorld();
        ArmorStandEntity stand = EntityType.ARMOR_STAND.create(world);
        if (stand == null) {
            player.sendMessage(Text.literal("Failed to spawn clone."), true);
            return false;
        }
        stand.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
        stand.setNoGravity(false);
        stand.setInvisible(false);
        stand.setCustomName(player.getDisplayName());
        stand.setCustomNameVisible(false);
        stand.setSilent(true);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmorSlot() && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
                continue;
            }
            stand.equipStack(slot, player.getEquippedStack(slot).copy());
        }

        world.spawnEntity(stand);
        AbilityRuntime.startReaperShadeClone(player, stand.getUuid(), duration);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.6F, 0.6F);
        player.sendMessage(Text.literal("Shade Clone spawned."), true);
        return true;
    }
}
