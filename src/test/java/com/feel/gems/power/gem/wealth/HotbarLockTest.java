package com.feel.gems.power.gem.wealth;

import com.feel.gems.power.gem.wealth.HotbarLock;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class HotbarLockTest {
    @Test
    void locksAndExpires() {
        NbtCompound nbt = new NbtCompound();
        long now = 1000L;

        HotbarLock.lock(nbt, now, 2, 60);
        assertEquals(2, HotbarLock.lockedSlot(nbt, now));
        assertEquals(2, HotbarLock.lockedSlot(nbt, now + 59));
        assertEquals(-1, HotbarLock.lockedSlot(nbt, now + 60));
        assertEquals(-1, HotbarLock.lockedSlot(nbt, now + 999));
    }

    @Test
    void clampsSlot() {
        NbtCompound nbt = new NbtCompound();
        HotbarLock.lock(nbt, 0L, 999, 20);
        assertEquals(8, HotbarLock.lockedSlot(nbt, 0L));
    }
}

