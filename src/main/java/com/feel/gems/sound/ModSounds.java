package com.feel.gems.sound;

import com.feel.gems.GemsMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registry for custom mod sounds.
 */
public final class ModSounds {
    public static final SoundEvent METAL_PIPE = register("metal_pipe");

    private ModSounds() {
    }

    public static void init() {
        // Triggers static initialization.
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.of(GemsMod.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
