package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class BreezyBashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BREEZY_BASH;
    }

    @Override
    public String name() {
        return "Breezy Bash";
    }

    @Override
    public String description() {
        return "Launches a target into the air.";
    }

    @Override
    public int cooldownTicks() {
        return 20 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, 10.0D);
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return true;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }

        Vec3d away = target.getPos().subtract(player.getPos()).normalize();
        target.addVelocity(away.x * 0.6D, 1.2D, away.z * 0.6D);
        target.velocityModified = true;
        target.damage(player.getDamageSources().playerAttack(player), 4.0F);

        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_BREEZE_WIND_BURST, SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}

