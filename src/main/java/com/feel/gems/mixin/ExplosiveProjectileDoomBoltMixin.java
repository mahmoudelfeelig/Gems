package com.feel.gems.mixin;

import com.feel.gems.power.ability.bonus.BonusDoomBoltAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(ProjectileEntity.class)
public abstract class ExplosiveProjectileDoomBoltMixin {
    @Inject(method = "onCollision(Lnet/minecraft/util/hit/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void gems$doomBoltCollision(HitResult hitResult, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        if (!self.getCommandTags().contains("gems_doom_bolt")) {
            return;
        }
        if (!(self.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        Entity owner = self.getOwner();
        ServerPlayerEntity ownerPlayer = owner instanceof ServerPlayerEntity player ? player : null;

        if (hitResult instanceof EntityHitResult entityHit) {
            Entity hitEntity = entityHit.getEntity();
            if (hitEntity instanceof LivingEntity living) {
                if (ownerPlayer != null) {
                    if (living == ownerPlayer) {
                        self.discard();
                        ci.cancel();
                        return;
                    }
                    if (living instanceof ServerPlayerEntity targetPlayer) {
                        if (GemTrust.isTrusted(ownerPlayer, targetPlayer)) {
                            self.discard();
                            ci.cancel();
                            return;
                        }
                        if (!VoidImmunity.canBeTargeted(ownerPlayer, targetPlayer)) {
                            self.discard();
                            ci.cancel();
                            return;
                        }
                    }
                }
                float damage = BonusDoomBoltAbility.doomBoltDamage();
                if (owner != null) {
                    living.damage(world, world.getDamageSources().indirectMagic(self, owner), damage);
                } else {
                    living.damage(world, world.getDamageSources().magic(), damage);
                }
            }
        }

        AbilityFeedback.burstAt(world, self.getEntityPos(), ParticleTypes.SOUL_FIRE_FLAME, 14, 0.2D);
        AbilityFeedback.soundAt(world, self.getEntityPos(), SoundEvents.ENTITY_WITHER_SKELETON_HURT, 0.8F, 0.7F);
        self.discard();
        ci.cancel();
    }
}
