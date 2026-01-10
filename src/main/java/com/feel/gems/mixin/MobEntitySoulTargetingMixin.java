package com.feel.gems.mixin;

import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.runtime.AbilityRestrictions;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(MobEntity.class)
public abstract class MobEntitySoulTargetingMixin {
    @Inject(method = "setTarget", at = @At("TAIL"))
    private void gems$preventTargetingTrusted(LivingEntity target, CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (!(self.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (target == null) {
            return;
        }
        boolean soul = SoulSummons.isSoul(self);
        boolean summon = SummonerSummons.isSummon(self);
        boolean hypno = HypnoControl.isHypno(self);
        if (!soul && !summon && !hypno) {
            return;
        }

        UUID ownerUuid = soul ? SoulSummons.ownerUuid(self) : (summon ? SummonerSummons.ownerUuid(self) : HypnoControl.ownerUuid(self));
        if (ownerUuid == null) {
            // no owner tag => safest is "do not target players"
            self.setTarget(null);
            return;
        }
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner == null) {
            return;
        }
        GemPlayerState.initIfNeeded(owner);
        if (GemPlayerState.getEnergy(owner) <= 0) {
            return;
        }
        if (!GemPlayerState.arePassivesEnabled(owner)) {
            return;
        }
        if (AbilityRestrictions.isSuppressed(owner)) {
            return;
        }

        if (target instanceof ServerPlayerEntity candidate) {
            if (candidate.getUuid().equals(ownerUuid)) {
                self.setTarget(null);
                return;
            }

            if (GemTrust.isTrusted(owner, candidate)) {
                self.setTarget(null);
            }
            return;
        }

        if (target instanceof MobEntity mobTarget) {
            if (SoulSummons.isSoul(mobTarget) && ownerUuid.equals(SoulSummons.ownerUuid(mobTarget))) {
                self.setTarget(null);
                return;
            }
            if (SummonerSummons.isSummon(mobTarget) && ownerUuid.equals(SummonerSummons.ownerUuid(mobTarget))) {
                self.setTarget(null);
                return;
            }
            if (HypnoControl.isHypno(mobTarget) && ownerUuid.equals(HypnoControl.ownerUuid(mobTarget))) {
                self.setTarget(null);
            }
        }
    }
}
