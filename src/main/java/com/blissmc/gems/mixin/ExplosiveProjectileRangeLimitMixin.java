package com.blissmc.gems.mixin;

import com.blissmc.gems.power.RangeLimitedProjectile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExplosiveProjectileEntity.class)
public abstract class ExplosiveProjectileRangeLimitMixin implements RangeLimitedProjectile {
    @Unique
    private Vec3d gems$origin;

    @Unique
    private double gems$maxDistSq;

    @Unique
    private boolean gems$limitSet;

    @Override
    public void gems$setRangeLimit(Vec3d origin, double maxDistanceBlocks) {
        this.gems$origin = origin;
        this.gems$maxDistSq = Math.max(0.0D, maxDistanceBlocks) * Math.max(0.0D, maxDistanceBlocks);
        this.gems$limitSet = true;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void gems$rangeLimitTick(CallbackInfo ci) {
        if (!gems$limitSet || gems$origin == null) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (self.getPos().squaredDistanceTo(gems$origin) > gems$maxDistSq) {
            self.discard();
        }
    }
}
