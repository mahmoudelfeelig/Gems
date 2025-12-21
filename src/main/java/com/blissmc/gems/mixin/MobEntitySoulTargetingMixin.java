package com.feel.gems.mixin;

import com.feel.gems.power.SoulSummons;
import com.feel.gems.power.SummonerSummons;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobEntitySoulTargetingMixin {
    @Inject(method = "setTarget", at = @At("TAIL"))
    private void gems$preventTargetingTrusted(LivingEntity target, CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (target == null) {
            return;
        }
        if (!(target instanceof ServerPlayerEntity candidate)) {
            return;
        }
        boolean soul = SoulSummons.isSoul(self);
        boolean summon = SummonerSummons.isSummon(self);
        if (!soul && !summon) {
            return;
        }

        UUID ownerUuid = soul ? SoulSummons.ownerUuid(self) : SummonerSummons.ownerUuid(self);
        if (ownerUuid == null) {
            // no owner tag => safest is "do not target players"
            self.setTarget(null);
            return;
        }
        if (candidate.getUuid().equals(ownerUuid)) {
            self.setTarget(null);
            return;
        }

        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner == null) {
            return;
        }
        if (GemTrust.isTrusted(owner, candidate)) {
            self.setTarget(null);
        }
    }
}
