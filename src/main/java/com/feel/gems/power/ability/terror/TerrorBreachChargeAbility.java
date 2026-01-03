package com.feel.gems.power.ability.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;


public final class TerrorBreachChargeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_BREACH_CHARGE;
    }

    @Override
    public String name() {
        return "Breach Charge";
    }

    @Override
    public String description() {
        return "Plants a primed charge on a targeted block or entity.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().terror().breachChargeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int range = GemsBalance.v().terror().breachChargeRangeBlocks();
        if (range <= 0) {
            player.sendMessage(Text.literal("Breach Charge is disabled."), true);
            return false;
        }
        Vec3d spawnPos = null;
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target != null) {
            spawnPos = target.getEntityPos().add(0.0D, 0.2D, 0.0D);
        } else {
            HitResult hit = player.raycast(range, 1.0F, false);
            if (hit instanceof BlockHitResult blockHit) {
                spawnPos = blockHit.getPos().add(0.0D, 0.1D, 0.0D);
            }
        }
        if (spawnPos == null) {
            player.sendMessage(Text.literal("No target for breach charge."), true);
            return false;
        }

        float power = GemsBalance.v().terror().breachChargeExplosionPower();
        var world = player.getEntityWorld();
        world.createExplosion(player, spawnPos.x, spawnPos.y, spawnPos.z, power, net.minecraft.world.World.ExplosionSourceType.MOB);

        AbilityFeedback.burstAt(world, spawnPos, ParticleTypes.SMOKE, 12, 0.2D);
        AbilityFeedback.burstAt(world, spawnPos, ParticleTypes.SOUL_FIRE_FLAME, 14, 0.15D);
        AbilityFeedback.burstAt(world, spawnPos, ParticleTypes.LARGE_SMOKE, 8, 0.18D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_WITHER_SHOOT, 0.9F, 1.0F);
        return true;
    }
}
