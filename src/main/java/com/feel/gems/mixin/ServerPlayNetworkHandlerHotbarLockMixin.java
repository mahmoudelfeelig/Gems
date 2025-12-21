package com.feel.gems.mixin;

import com.feel.gems.power.HotbarLock;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerHotbarLockMixin {
    @Shadow
    @Final
    public ServerPlayerEntity player;

    @Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void gems$hotbarLock(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        int locked = HotbarLock.lockedSlot(player);
        if (locked < 0) {
            return;
        }

        int requested = packet.getSelectedSlot();
        if (requested == locked) {
            return;
        }

        player.getInventory().selectedSlot = locked;
        player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(locked));
        ci.cancel();
    }
}

