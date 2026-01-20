package com.feel.gems.mixin;

import com.feel.gems.power.gem.summoner.SummonerSummons;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Entity.class)
public abstract class EntitySummonDiscardMixin {
    @Inject(method = "discard", at = @At("HEAD"))
    private void gems$applyCooldownOnDiscard(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof MobEntity mob)) {
            return;
        }
        if (!SummonerSummons.isSummon(mob)) {
            return;
        }
        if (mob.getCommandTags().contains("gems_spectral_blade")) {
            return;
        }
        if (!(mob.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        UUID ownerUuid = SummonerSummons.ownerUuid(mob);
        if (ownerUuid == null) {
            return;
        }
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner == null) {
            return;
        }
        SummonerSummons.applyCooldown(owner);
    }
}
