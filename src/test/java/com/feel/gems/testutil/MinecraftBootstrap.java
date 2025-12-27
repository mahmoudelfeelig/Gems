package com.feel.gems.testutil;

import java.lang.reflect.Field;
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
        trySetBootstrapFlag();
    }

    private static void trySetBootstrapFlag() {
        try {
            Class<?> bootstrap = Class.forName("net.minecraft.Bootstrap");
            // Different Minecraft versions name the guard differently; set any boolean flag we find.
            String[] candidates = {"initialized", "bootstrapped", "BOOTSTRAPPED"};
            for (String name : candidates) {
                trySetBoolean(bootstrap, name, true);
            }
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private static void trySetBoolean(Class<?> cls, String fieldName, boolean value) {
        try {
            Field f = cls.getDeclaredField(fieldName);
            f.setAccessible(true);
            if (f.getType() == boolean.class) {
                f.setBoolean(null, value);
            }
        } catch (Throwable ignored) {
            // best effort
        }
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

