package com.feel.gems.mixin;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
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

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void gems$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.put(GEMS_PERSISTED_KEY, gems$getPersistentData());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void gems$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(GEMS_PERSISTED_KEY, NbtElement.COMPOUND_TYPE)) {
            gems$persistentData = nbt.getCompound(GEMS_PERSISTED_KEY);
        } else {
            gems$persistentData = new NbtCompound();
        }
    }
}
