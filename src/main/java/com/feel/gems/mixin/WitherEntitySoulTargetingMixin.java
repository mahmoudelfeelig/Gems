package com.feel.gems.mixin;

import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.trust.GemTrust;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;




@Mixin(WitherEntity.class)
public abstract class WitherEntitySoulTargetingMixin {
    @ModifyVariable(method = "setTrackedEntityId", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int gems$soulPreventHeadTargetingTrusted(int entityId) {
        if (entityId <= 0) {
            return entityId;
        }

        WitherEntity self = (WitherEntity) (Object) this;
        if (!(self.getEntityWorld() instanceof ServerWorld world)) {
            return entityId;
        }
        boolean soul = SoulSummons.isSoul(self);
        boolean summon = SummonerSummons.isSummon(self);
        if (!soul && !summon) {
            return entityId;
        }

        Entity candidate = world.getEntityById(entityId);
        if (!(candidate instanceof ServerPlayerEntity player)) {
            return entityId;
        }

        UUID ownerUuid = soul ? SoulSummons.ownerUuid(self) : SummonerSummons.ownerUuid(self);
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
