package com.feel.gems.mixin;

import com.feel.gems.legendary.LegendaryTargeting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityLegendaryHitMixin {
    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void gems$recordHit(EntityHitResult hitResult, CallbackInfo ci) {
        Entity owner = ((PersistentProjectileEntity) (Object) this).getOwner();
        if (!(owner instanceof ServerPlayerEntity attacker)) {
            return;
        }
        if (!(hitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }
        LegendaryTargeting.recordHit(attacker, target);
    }
}
