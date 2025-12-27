package com.feel.gems.state;

import net.minecraft.nbt.NbtCompound;




public interface GemsPersistentDataHolder {
    NbtCompound gems$getPersistentData();

    void gems$setPersistentData(NbtCompound nbt);
}
