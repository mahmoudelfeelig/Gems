package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityRotEaterTickMixin {
    private static final String KEY_ROT_EATER_UNTIL = "reaperRotEaterUntil";

    @Inject(method = "tick", at = @At("HEAD"))
    private void gems$clearRotEaterEffects(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = data.getLong(KEY_ROT_EATER_UNTIL);
        if (until <= 0L) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.REAPER_ROT_EATER)) {
            data.remove(KEY_ROT_EATER_UNTIL);
            return;
        }
        long now = GemsTime.now(player);
        if (now > until) {
            data.remove(KEY_ROT_EATER_UNTIL);
            return;
        }
        player.removeStatusEffect(StatusEffects.HUNGER);
        player.removeStatusEffect(StatusEffects.POISON);
    }
}
