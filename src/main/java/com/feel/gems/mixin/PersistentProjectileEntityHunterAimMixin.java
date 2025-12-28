package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import com.feel.gems.legendary.LegendaryTargeting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityHunterAimMixin {
    @Unique
    private Vec3d gems$aimDir = null;

    @Inject(method = "setVelocity(DDDFF)V", at = @At("HEAD"))
    private void gems$computeAim(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
        gems$aimDir = null;
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        Entity owner = self.getOwner();
        if (!(owner instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!player.getMainHandStack().isOf(ModItems.HUNTERS_SIGHT_BOW) && !player.getOffHandStack().isOf(ModItems.HUNTERS_SIGHT_BOW)) {
            return;
        }
        float strength = GemsBalance.v().legendary().hunterAimAssistStrength();
        if (strength <= 0.0F) {
            return;
        }
        int range = GemsBalance.v().legendary().hunterAimRangeBlocks();
        int timeout = GemsBalance.v().legendary().hunterAimTimeoutTicks();
        LivingEntity target = LegendaryTargeting.findTarget(player, range, timeout);
        if (target == null) {
            return;
        }
        Vec3d desired = target.getEyePos().subtract(player.getEyePos());
        if (desired.lengthSquared() < 0.0001D) {
            return;
        }
        Vec3d original = new Vec3d(x, y, z);
        if (original.lengthSquared() < 0.0001D) {
            return;
        }
        Vec3d blended = original.normalize().lerp(desired.normalize(), strength).normalize();
        gems$aimDir = blended;
    }

    @Inject(method = "setVelocity(DDDFF)V", at = @At("RETURN"))
    private void gems$applyAim(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
        if (gems$aimDir == null) {
            return;
        }
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        Vec3d velocity = self.getVelocity();
        double currentSpeed = velocity.length();
        if (currentSpeed <= 0.0D) {
            return;
        }
        self.setVelocity(gems$aimDir.multiply(currentSpeed));
        gems$aimDir = null;
    }
}
