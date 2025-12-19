package com.blissmc.gems.mixin;

import com.blissmc.gems.power.GemPowers;
import com.blissmc.gems.power.PowerIds;
import net.minecraft.block.entity.SculkShriekerBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerSilenceMixin {
    @Inject(method = "shriek", at = @At("HEAD"), cancellable = true)
    private void gems$sculkSilence(ServerWorld world, ServerPlayerEntity player, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        if (GemPowers.isPassiveActive(player, PowerIds.SCULK_SILENCE)) {
            ci.cancel();
        }
    }
}

