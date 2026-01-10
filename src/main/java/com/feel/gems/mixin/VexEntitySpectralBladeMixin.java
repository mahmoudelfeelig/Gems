package com.feel.gems.mixin;

import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
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

    @Inject(method = "tick", at = @At("TAIL"))
    private void gems$retargetSpectralBlade(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (!(self instanceof VexEntity vex)) {
            return;
        }
        if (!(self.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        Set<String> tags = self.getCommandTags();
        if (!tags.contains("gems_spectral_blade")) {
            return;
        }
        if (world.getTime() % 10 != 0) {
            return;
        }

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
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(java.util.UUID.fromString(ownerUuid));
        if (owner == null) {
            return;
        }

        LivingEntity currentTarget = vex.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) {
            if (currentTarget != owner && !(currentTarget instanceof ServerPlayerEntity player && GemTrust.isTrusted(owner, player))) {
                return;
            }
        }

        Box searchBox = vex.getBoundingBox().expand(16.0);
        LivingEntity selected = null;
        for (Entity entity : world.getOtherEntities(owner, searchBox)) {
            if (entity instanceof ServerPlayerEntity target) {
                if (!target.isAlive()) {
                    continue;
                }
                if (GemTrust.isTrusted(owner, target)) {
                    continue;
                }
                if (!VoidImmunity.canBeTargeted(owner, target)) {
                    continue;
                }
                selected = target;
                break;
            }
        }
        if (selected == null) {
            for (Entity entity : world.getOtherEntities(owner, searchBox)) {
                if (entity instanceof HostileEntity hostile && hostile.isAlive()) {
                    selected = hostile;
                    break;
                }
            }
        }
        if (selected != null) {
            vex.setTarget(selected);
        }
    }
}
