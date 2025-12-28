package com.feel.gems.mixin;

import com.feel.gems.power.gem.terror.TerrorRigRuntime;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldRiggedBlockUpdateMixin {
    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("HEAD"))
    private void gems$riggedBlockUpdated(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        World world = (World) (Object) this;
        if (world.isClient) {
            return;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        if (!TerrorRigRuntime.hasTrap(serverWorld, pos)) {
            return;
        }
        BlockState before = world.getBlockState(pos);
        TerrorRigRuntime.tryTriggerUpdate(serverWorld, pos, before, state);
    }
}
