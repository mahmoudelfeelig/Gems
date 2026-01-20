package com.feel.gems.mixin;

import com.feel.gems.power.ability.bonus.BonusDecoyTrapAbility;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle Decoy Trap explosions when items are picked up.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityDecoyTrapMixin {

    @Shadow
    private int pickupDelay;

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void gems$checkDecoyTrap(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Only trigger if the item can actually be picked up (pickup delay expired)
        if (this.pickupDelay > 0) {
            return;
        }
        
        ItemEntity self = (ItemEntity) (Object) this;
        if (BonusDecoyTrapAbility.triggerTrap(self, serverPlayer)) {
            // The trap exploded - cancel the normal pickup
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void gems$checkMobPickupTrap(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!(self.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        BonusDecoyTrapAbility.tickTrapItem(self);
        if (this.pickupDelay > 0) {
            return;
        }
        if (BonusDecoyTrapAbility.getTrapOwner(self) == null) {
            return;
        }
        Box box = self.getBoundingBox().expand(0.35);
        for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, e -> !(e instanceof PlayerEntity))) {
            if (BonusDecoyTrapAbility.triggerTrap(self, living)) {
                break;
            }
        }
    }
}
