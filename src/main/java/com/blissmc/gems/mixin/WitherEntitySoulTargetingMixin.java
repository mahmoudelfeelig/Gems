package com.feel.gems.mixin;

import com.feel.gems.power.SoulSummons;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

@Mixin(WitherEntity.class)
public abstract class WitherEntitySoulTargetingMixin {
    @ModifyVariable(method = "setTrackedEntityId", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int gems$soulPreventHeadTargetingTrusted(int entityId) {
        if (entityId <= 0) {
            return entityId;
        }

        WitherEntity self = (WitherEntity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld world)) {
            return entityId;
        }
        if (!SoulSummons.isSoul(self)) {
            return entityId;
        }

        Entity candidate = world.getEntityById(entityId);
        if (!(candidate instanceof ServerPlayerEntity player)) {
            return entityId;
        }

        UUID ownerUuid = SoulSummons.ownerUuid(self);
        if (ownerUuid == null) {
            return 0;
        }
        if (player.getUuid().equals(ownerUuid)) {
            return 0;
        }
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner != null && GemTrust.isTrusted(owner, player)) {
            return 0;
        }
        return entityId;
    }
}

