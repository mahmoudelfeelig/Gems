package com.feel.gems.mixin;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(PlayerEntity.class)
public abstract class PlayerEntityDataMixin implements GemsPersistentDataHolder {
    @Unique
    private static final String GEMS_PERSISTED_KEY = "gems";

    @Unique
    private NbtCompound gems$persistentData;

    @Override
    public NbtCompound gems$getPersistentData() {
        if (gems$persistentData == null) {
            gems$persistentData = new NbtCompound();
        }
        return gems$persistentData;
    }

    @Override
    public void gems$setPersistentData(NbtCompound nbt) {
        gems$persistentData = (nbt == null) ? new NbtCompound() : nbt;
    }

    @Inject(method = "writeCustomData", at = @At("HEAD"))
    private void gems$writeCustomData(WriteView view, CallbackInfo ci) {
        view.put(GEMS_PERSISTED_KEY, NbtCompound.CODEC, gems$getPersistentData());
    }

    @Inject(method = "readCustomData", at = @At("HEAD"))
    private void gems$readCustomData(ReadView view, CallbackInfo ci) {
        gems$persistentData = view.read(GEMS_PERSISTED_KEY, NbtCompound.CODEC).orElseGet(NbtCompound::new);
    }
}
