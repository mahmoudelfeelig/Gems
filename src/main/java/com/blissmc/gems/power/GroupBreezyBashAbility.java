package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class GroupBreezyBashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.GROUP_BREEZY_BASH;
    }

    @Override
    public String name() {
        return "Group Breezy Bash";
    }

    @Override
    public String description() {
        return "Knocks back all untrusted players nearby.";
    }

    @Override
    public int cooldownTicks() {
        return 45 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        int affected = 0;
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= 10.0D * 10.0D)) {
            if (GemTrust.isTrusted(player, other)) {
                continue;
            }
            Vec3d away = other.getPos().subtract(player.getPos()).normalize();
            other.addVelocity(away.x * 1.2D, 0.8D, away.z * 1.2D);
            other.velocityModified = true;
            affected++;
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_BREEZE_WIND_BURST, SoundCategory.PLAYERS, 1.0F, 1.0F);
        player.sendMessage(Text.literal("Bashed " + affected + " players."), true);
        return true;
    }
}
