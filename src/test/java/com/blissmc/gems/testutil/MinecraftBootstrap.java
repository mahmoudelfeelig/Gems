package com.feel.gems.testutil;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MinecraftBootstrap {
    private static final AtomicBoolean DONE = new AtomicBoolean(false);

    private MinecraftBootstrap() {
    }

    public static void ensure() {
        if (!DONE.compareAndSet(false, true)) {
            return;
        }

        tryInvokeStatic("net.minecraft.SharedConstants", "createGameVersion");
        tryInvokeStatic("net.minecraft.Bootstrap", "initialize");
        tryInvokeStatic("net.minecraft.Bootstrap", "bootstrap");
    }

    private static void tryInvokeStatic(String className, String methodName) {
        try {
            Class<?> cls = Class.forName(className);
            Method m = cls.getDeclaredMethod(methodName);
            m.setAccessible(true);
            m.invoke(null);
        } catch (Throwable ignored) {
            // best effort (mappings differ across versions)
        }
    }
}

