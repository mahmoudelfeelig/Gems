package com.feel.gems.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Mixin to prevent spectral blade vexes from attacking their owner.
 * Targets MobEntity since that's where setTarget is defined.
 */
@Mixin(MobEntity.class)
public abstract class VexEntitySpectralBladeMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void gems$preventSpectralBladeOwnerTargeting(LivingEntity target, CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        
        // Only handle vexes
        if (!(self instanceof VexEntity)) {
            return;
        }
        
        if (target == null) {
            return;
        }
        
        Set<String> tags = self.getCommandTags();
        
        // Check if this is a spectral blade
        if (!tags.contains("gems_spectral_blade")) {
            return;
        }
        
        // Find the owner tag
        String ownerUuid = null;
        for (String tag : tags) {
            if (tag.startsWith("owner:")) {
                ownerUuid = tag.substring(6);
                break;
            }
        }
        
        if (ownerUuid == null) {
            return;
        }
        
        // Prevent targeting the owner
        if (target instanceof ServerPlayerEntity player) {
            if (player.getUuidAsString().equals(ownerUuid)) {
                ci.cancel();
            }
        }
    }
}
