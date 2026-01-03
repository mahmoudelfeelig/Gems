package com.feel.gems.mixin;

import com.feel.gems.world.FixedLootChest;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public final class ChunkGeneratorFixedLootMixin {
    @Inject(method = "generateFeatures", at = @At("TAIL"))
    private void gems$fixedLootChest(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor, CallbackInfo ci) {
        FixedLootChest.placeIfTargetChunk(world, chunk);
    }
}

