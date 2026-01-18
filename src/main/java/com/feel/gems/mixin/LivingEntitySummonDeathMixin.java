package com.feel.gems.mixin;

import com.feel.gems.power.gem.summoner.SummonerSummons;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(LivingEntity.class)
public abstract class LivingEntitySummonDeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void gems$applySummonCooldown(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof MobEntity mob)) {
            return;
        }
        if (!SummonerSummons.isSummon(mob)) {
            return;
        }
        if (mob.getCommandTags().contains("gems_spectral_blade")) {
            return;
        }
        UUID ownerUuid = SummonerSummons.ownerUuid(mob);
        if (ownerUuid == null) {
            return;
        }
        if (!(mob.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner == null) {
            return;
        }
        SummonerSummons.applyCooldown(owner);
    }
}
