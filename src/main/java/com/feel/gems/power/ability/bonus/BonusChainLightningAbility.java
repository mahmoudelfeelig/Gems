package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.List;

public final class BonusChainLightningAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_CHAIN_LIGHTNING;
    }

    @Override
    public String name() {
        return "Chain Lightning";
    }

    @Override
    public String description() {
        return "Strike multiple nearby enemies with chained lightning.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(15);
        
        List<LivingEntity> targets = world.getOtherEntities(player, area, e -> e instanceof LivingEntity && !(e instanceof ServerPlayerEntity))
                .stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .limit(5)
                .toList();
        
        for (LivingEntity target : targets) {
            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
            if (lightning != null) {
                Vec3d pos = target.getEntityPos();
                lightning.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
                world.spawnEntity(lightning);
            }
        }
        return true;
    }
}
