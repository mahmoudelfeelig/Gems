package com.blissmc.gems.mixin;

import com.blissmc.gems.power.GemPowers;
import com.blissmc.gems.power.PowerIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandTrampleImmunityMixin {
    @Inject(method = "onLandedUpon", at = @At("HEAD"), cancellable = true)
    private void gems$preventTrample(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        if (entity instanceof ServerPlayerEntity player && GemPowers.isPassiveActive(player, PowerIds.CROP_TRAMPLE_IMMUNITY)) {
            entity.handleFallDamage(fallDistance, 1.0F, entity.getDamageSources().fall());
            ci.cancel();
        }
    }
}

