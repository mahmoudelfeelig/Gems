package com.feel.gems.mixin;

import com.feel.gems.power.GemPowers;
import com.feel.gems.power.PowerIds;
import net.minecraft.block.entity.SculkSensorBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.Vibrations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkSensorBlockEntity.class)
public abstract class SculkSensorSilentStepMixin {
    @Inject(method = "createCallback", at = @At("RETURN"), cancellable = true)
    private void gems$wrapCallback(CallbackInfoReturnable<Vibrations.Callback> cir) {
        Vibrations.Callback original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        cir.setReturnValue(new SilentStepCallback(original));
    }

    private record SilentStepCallback(Vibrations.Callback delegate) implements Vibrations.Callback {
        @Override
        public int getRange() {
            return delegate.getRange();
        }

        @Override
        public net.minecraft.world.event.PositionSource getPositionSource() {
            return delegate.getPositionSource();
        }

        @Override
        public boolean triggersAvoidCriterion() {
            return delegate.triggersAvoidCriterion();
        }

        @Override
        public boolean accepts(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter) {
            if (emitter != null && emitter.sourceEntity() instanceof ServerPlayerEntity player) {
                if (GemPowers.isPassiveActive(player, PowerIds.SCULK_SILENCE) || GemPowers.isPassiveActive(player, PowerIds.SPY_SILENT_STEP)) {
                    return false;
                }
            }
            return delegate.accepts(world, pos, event, emitter);
        }

        @Override
        public void accept(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, Entity entity, Entity sourceEntity, float distance) {
            delegate.accept(world, pos, event, entity, sourceEntity, distance);
        }

        @Override
        public void onListen() {
            delegate.onListen();
        }

        @Override
        public boolean requiresTickingChunksAround() {
            return delegate.requiresTickingChunksAround();
        }
    }
}
