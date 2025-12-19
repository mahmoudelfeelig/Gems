package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class DashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DASH;
    }

    @Override
    public String name() {
        return "Dash";
    }

    @Override
    public String description() {
        return "Dashes forward and damages enemies in your path.";
    }

    @Override
    public int cooldownTicks() {
        return 6 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d dir = player.getRotationVec(1.0F).normalize();
        player.addVelocity(dir.x * 1.8D, 0.1D, dir.z * 1.8D);
        player.velocityModified = true;

        Box box = player.getBoundingBox().stretch(dir.multiply(4.0D)).expand(1.0D);
        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            if (e instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            ((LivingEntity) e).damage(player.getDamageSources().playerAttack(player), 6.0F);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.7F, 1.3F);
        return true;
    }
}

