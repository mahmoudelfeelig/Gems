package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class BlurAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BLUR;
    }

    @Override
    public String name() {
        return "Blur";
    }

    @Override
    public String description() {
        return "Calls successive lightning strikes to damage and knock back enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 20 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, 40.0D);
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return true;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }

        ServerWorld world = player.getServerWorld();
        for (int i = 0; i < 3; i++) {
            spawnLightning(world, target.getPos());
            DamageSource source = player.getDamageSources().lightningBolt();
            target.damage(source, 5.0F);
        }
        Vec3d away = target.getPos().subtract(player.getPos()).normalize();
        target.addVelocity(away.x * 0.8D, 0.4D, away.z * 0.8D);
        target.velocityModified = true;

        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.6F, 1.2F);
        return true;
    }

    private static void spawnLightning(ServerWorld world, Vec3d pos) {
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt == null) {
            return;
        }
        bolt.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
        bolt.setCosmetic(true);
        world.spawnEntity(bolt);
    }
}

