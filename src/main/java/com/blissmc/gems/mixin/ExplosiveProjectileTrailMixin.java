package com.blissmc.gems.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExplosiveProjectileEntity.class)
public abstract class ExplosiveProjectileTrailMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void gems$trail(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (self.getCommandTags().contains("gems_meteor")) {
            world.spawnParticles(ParticleTypes.FLAME, self.getX(), self.getY(), self.getZ(), 1, 0.05D, 0.05D, 0.05D, 0.0D);
            world.spawnParticles(ParticleTypes.SMOKE, self.getX(), self.getY(), self.getZ(), 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }
}

