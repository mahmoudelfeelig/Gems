package com.feel.gems.client.modmenu;

import java.lang.reflect.Method;
import net.minecraft.client.MinecraftClient;




final class ClientCommandSender {
    private ClientCommandSender() {
    }

    static void sendCommand(String commandWithoutSlash) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        String cmd = commandWithoutSlash.startsWith("/") ? commandWithoutSlash.substring(1) : commandWithoutSlash;

        Object networkHandler = client.player.networkHandler;
        if (tryInvoke(networkHandler, "sendChatCommand", new Class<?>[]{String.class}, new Object[]{cmd})) {
            return;
        }
        if (tryInvoke(networkHandler, "sendCommand", new Class<?>[]{String.class}, new Object[]{cmd})) {
            return;
        }
        if (tryInvoke(networkHandler, "sendChatMessage", new Class<?>[]{String.class}, new Object[]{"/" + cmd})) {
            return;
        }

        // As a last resort, try on the player instance.
        if (tryInvoke(client.player, "sendChatMessage", new Class<?>[]{String.class}, new Object[]{"/" + cmd})) {
            return;
        }
    }

    private static boolean tryInvoke(Object target, String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            method.invoke(target, args);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}

