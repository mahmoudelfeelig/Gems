package com.feel.gems.mixin;

import com.feel.gems.power.SoulSummons;
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
        if (!SoulSummons.isSoul(self)) {
            return;
        }

        UUID ownerUuid = SoulSummons.ownerUuid(self);
        if (ownerUuid == null && SoulSummons.isSoul(self)) {
            // no owner tag => safest is "do not target players"
            self.setTarget(null);
            return;
        }
        if (ownerUuid == null) {
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

