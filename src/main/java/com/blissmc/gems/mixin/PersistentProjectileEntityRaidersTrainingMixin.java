package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.GemPowers;
import com.feel.gems.power.PowerIds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityRaidersTrainingMixin {
    @Unique
    private static final String TAG_SCALED = "gems_raiders_training_scaled";

    @Unique
    private double gems$velocityMultiplier = 1.0D;

    @Inject(method = "setVelocity(DDDFF)V", at = @At("HEAD"))
    private void gems$computeMultiplier(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        if (self.getCommandTags().contains(TAG_SCALED)) {
            gems$velocityMultiplier = 1.0D;
            return;
        }
        Entity owner = self.getOwner();
        if (!(owner instanceof ServerPlayerEntity player)) {
            gems$velocityMultiplier = 1.0D;
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.PILLAGER_RAIDERS_TRAINING)) {
            gems$velocityMultiplier = 1.0D;
            return;
        }
        gems$velocityMultiplier = GemsBalance.v().pillager().raidersTrainingProjectileVelocityMultiplier();
    }

    @ModifyVariable(method = "setVelocity(DDDFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double gems$scaleX(double x) {
        return x * gems$velocityMultiplier;
    }

    @ModifyVariable(method = "setVelocity(DDDFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double gems$scaleY(double y) {
        return y * gems$velocityMultiplier;
    }

    @ModifyVariable(method = "setVelocity(DDDFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double gems$scaleZ(double z) {
        return z * gems$velocityMultiplier;
    }

    @Inject(method = "setVelocity(DDDFF)V", at = @At("RETURN"))
    private void gems$markScaled(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
        if (gems$velocityMultiplier == 1.0D) {
            return;
        }
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        self.addCommandTag(TAG_SCALED);
        gems$velocityMultiplier = 1.0D;
    }
}

