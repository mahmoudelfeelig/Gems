package com.feel.gems.power.ability.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;


public final class PanicRingAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_PANIC_RING;
    }

    @Override
    public String name() {
        return "Panic Ring";
    }

    @Override
    public String description() {
        return "Spawns primed TNT around you.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().terror().panicRingCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int count = GemsBalance.v().terror().panicRingTntCount();
        if (count <= 0) {
            player.sendMessage(Text.translatable("gems.ability.terror.panic_ring.no_tnt"), true);
            return false;
        }

        var world = player.getEntityWorld();
        Vec3d center = player.getEntityPos();
        double radius = GemsBalance.v().terror().panicRingRadiusBlocks();
        int fuse = GemsBalance.v().terror().panicRingFuseTicks();

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D) * (i / (double) count);
            Vec3d pos = center.add(Math.cos(angle) * radius, 0.1D, Math.sin(angle) * radius);
            TntEntity tnt = new TntEntity(world, pos.x, pos.y, pos.z, player);
            tnt.setFuse(fuse);
            world.spawnEntity(tnt);
        }

        AbilityFeedback.burst(player, ParticleTypes.SMOKE, 18, 0.35D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_TNT_PRIMED, 1.0F, 1.0F);
        return true;
    }
}

