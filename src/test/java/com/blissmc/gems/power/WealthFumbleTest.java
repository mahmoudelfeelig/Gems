package com.blissmc.gems.power;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WealthFumbleTest {
    @Test
    void appliesAndExpires() {
        NbtCompound nbt = new NbtCompound();
        long now = 500L;
        WealthFumble.apply(nbt, now, 40);

        assertTrue(WealthFumble.until(nbt, now) > now);
        assertTrue(WealthFumble.until(nbt, now + 39) > now + 39);
        assertEquals(0L, WealthFumble.until(nbt, now + 40));
        assertEquals(0L, WealthFumble.until(nbt, now + 999));
    }
}

