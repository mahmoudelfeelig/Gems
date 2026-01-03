package com.feel.gems.power.ability.pillager;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;



public final class PillagerFangsAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_FANGS;
    }

    @Override
    public String name() {
        return "Fangs";
    }

    @Override
    public String description() {
        return "Conjures evoker fangs in a line at the target zone.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().fangsCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int range = GemsBalance.v().pillager().fangsRangeBlocks();
        ServerWorld world = player.getEntityWorld();

        LivingEntity living = Targeting.raycastLiving(player, range);
        Vec3d center;
        if (living != null) {
            center = living.getEntityPos();
        } else {
            HitResult hit = player.raycast(range, 1.0F, false);
            if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult bhr)) {
                player.sendMessage(Text.literal("No target."), true);
                return false;
            }
            BlockPos pos = bhr.getBlockPos();
            center = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D);
        }

        int count = GemsBalance.v().pillager().fangsCount();
        float spacing = GemsBalance.v().pillager().fangsSpacingBlocks();
        int warmupStep = GemsBalance.v().pillager().fangsWarmupStepTicks();

        Vec3d toCenter = center.subtract(player.getEntityPos());
        double distSq = toCenter.lengthSquared();

        // Close range: evoker-style ring of fangs around the target.
        if (distSq < 9.0D) {
            double radius = Math.max(0.8D, spacing);
            for (int i = 0; i < Math.max(8, count); i++) {
                double angle = (Math.PI * 2.0D * i) / Math.max(8, count);
                double x = center.x + radius * Math.cos(angle);
                double z = center.z + radius * Math.sin(angle);
                EvokerFangsEntity fangs = new EvokerFangsEntity(world, x, center.y, z, (float) Math.toDegrees(angle), i * warmupStep, player);
                world.spawnEntity(fangs);
            }
        } else {
            // Far: line of fangs marching forward from the impact point.
            Vec3d dir = player.getRotationVec(1.0F).multiply(Math.max(0.1D, spacing));
            float yaw = player.getYaw();
            for (int i = 0; i < count; i++) {
                Vec3d p = center.add(dir.multiply(i));
                EvokerFangsEntity fangs = new EvokerFangsEntity(world, p.x, p.y, p.z, yaw, i * warmupStep, player);
                world.spawnEntity(fangs);
            }
        }

        AbilityFeedback.burstAt(world, center.add(0.0D, 0.1D, 0.0D), ParticleTypes.CRIT, 18, 0.25D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.0F, 1.0F);
        return true;
    }
}

