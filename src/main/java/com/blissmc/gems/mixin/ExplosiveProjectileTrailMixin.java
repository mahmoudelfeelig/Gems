package com.blissmc.gems.mixin;

import com.blissmc.gems.power.AbilityFeedback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
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
            Vec3d pos = self.getPos();
            AbilityFeedback.burstAt(world, pos, ParticleTypes.FLAME, 1, 0.05D);
            AbilityFeedback.burstAt(world, pos, ParticleTypes.SMOKE, 1, 0.05D);
        }
    }
}
