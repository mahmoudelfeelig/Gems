package com.blissmc.gems.client;

import com.blissmc.gems.core.GemDefinition;
import com.blissmc.gems.core.GemId;
import com.blissmc.gems.core.GemRegistry;
import com.blissmc.gems.net.ActivateAbilityPayload;
import com.blissmc.gems.net.SoulReleasePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class GemsKeybinds {
    private static final String CATEGORY = "category.gems";

    private static boolean registered = false;
    private static KeyBinding MODIFIER;
    private static final List<KeyBinding> DIRECT_SLOTS = new ArrayList<>();
    private static KeyBinding DIRECT_SOUL_RELEASE;

    // "Modifier + number" uses raw GLFW polling (no keybind conflicts with hotbar).
    // Slots 1..maxAbilityCount, and the slot after the last ability is Soul Release (Astra only).
    private static final int MAX_CHORD_SLOTS = 10; // 1..9,0
    private static final boolean[] CHORD_DOWN = new boolean[MAX_CHORD_SLOTS];

    private GemsKeybinds() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        MODIFIER = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gems.modifier",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                CATEGORY
        ));

        int directCount = Math.min(10, maxAbilityCount());
        for (int i = 1; i <= directCount; i++) {
            DIRECT_SLOTS.add(KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.gems.ability_slot_" + i,
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    CATEGORY
            )));
        }

        DIRECT_SOUL_RELEASE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gems.soul_release",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != null) {
                drainQueuedPresses();
                return;
            }

            for (int i = 0; i < DIRECT_SLOTS.size(); i++) {
                KeyBinding slot = DIRECT_SLOTS.get(i);
                while (slot.wasPressed()) {
                    activateSlot(client, i + 1);
                }
            }
            while (DIRECT_SOUL_RELEASE.wasPressed()) {
                activateSoulRelease(client);
            }

            if (!MODIFIER.isPressed()) {
                clearChordDown();
                return;
            }

            for (int i = 0; i < MAX_CHORD_SLOTS; i++) {
                int keyCode = defaultDigitKey(i + 1);
                if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
                    continue;
                }
                boolean down = isDown(client, keyCode);
                if (down && !CHORD_DOWN[i]) {
                    activateSlot(client, i + 1);
                }
                CHORD_DOWN[i] = down;
            }
        });
    }

    public static String modifierLabel() {
        if (MODIFIER == null) {
            return "";
        }
        return MODIFIER.getBoundKeyLocalizedText().getString();
    }

    public static String chordSlotLabel(int slotNumber) {
        String digit = digitLabel(slotNumber);
        if (digit.isEmpty()) {
            return "";
        }
        String modifier = modifierLabel();
        if (modifier.isEmpty()) {
            return digit;
        }
        return modifier + " + " + digit;
    }

    private static void activateSlot(MinecraftClient client, int slotNumber) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.literal("Not connected."));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.literal("Gem state not synced yet."));
            return;
        }
        ClientAbilitySelection.record(ClientGemState.activeGem(), slotNumber);

        GemDefinition def = GemRegistry.definition(ClientGemState.activeGem());
        int abilityCount = def.abilities().size();

        // Soul release is always the key AFTER the gem's last ability (not "after unlocked abilities").
        if (ClientGemState.activeGem() == GemId.ASTRA && slotNumber == abilityCount + 1) {
            activateSoulRelease(client);
            return;
        }

        int abilityIndex = slotNumber - 1;
        if (abilityIndex < 0 || abilityIndex >= abilityCount) {
            return;
        }

        // Server validates unlocks/cooldowns/suppression.
        ClientPlayNetworking.send(new ActivateAbilityPayload(abilityIndex));
    }

    private static void activateSoulRelease(MinecraftClient client) {
        if (client.getNetworkHandler() == null) {
            sendActionBar(client, Text.literal("Not connected."));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Text.literal("Gem state not synced yet."));
            return;
        }
        ClientAbilitySelection.record(ClientGemState.activeGem(), GemRegistry.definition(ClientGemState.activeGem()).abilities().size() + 1);
        if (ClientGemState.activeGem() != GemId.ASTRA) {
            return;
        }
        ClientPlayNetworking.send(SoulReleasePayload.INSTANCE);
    }

    private static void sendActionBar(MinecraftClient client, Text text) {
        if (client.player == null) {
            return;
        }
        client.player.sendMessage(text, true);
    }

    private static void drainQueuedPresses() {
        for (KeyBinding slot : DIRECT_SLOTS) {
            while (slot.wasPressed()) {
                // drain
            }
        }
        while (DIRECT_SOUL_RELEASE.wasPressed()) {
            // drain
        }
    }

    private static int maxAbilityCount() {
        int max = 0;
        for (GemId id : GemId.values()) {
            max = Math.max(max, GemRegistry.definition(id).abilities().size());
        }
        return max;
    }

    private static String digitLabel(int slotNumber) {
        return switch (slotNumber) {
            case 1 -> "1";
            case 2 -> "2";
            case 3 -> "3";
            case 4 -> "4";
            case 5 -> "5";
            case 6 -> "6";
            case 7 -> "7";
            case 8 -> "8";
            case 9 -> "9";
            case 10 -> "0";
            default -> "";
        };
    }

    private static void clearChordDown() {
        for (int i = 0; i < CHORD_DOWN.length; i++) {
            CHORD_DOWN[i] = false;
        }
    }

    private static boolean isDown(MinecraftClient client, int glfwKeyCode) {
        if (client.getWindow() == null) {
            return false;
        }
        long handle = client.getWindow().getHandle();
        if (handle == 0L) {
            return false;
        }
        return GLFW.glfwGetKey(handle, glfwKeyCode) == GLFW.GLFW_PRESS;
    }

    private static int defaultDigitKey(int slotNumber) {
        return switch (slotNumber) {
            case 1 -> GLFW.GLFW_KEY_1;
            case 2 -> GLFW.GLFW_KEY_2;
            case 3 -> GLFW.GLFW_KEY_3;
            case 4 -> GLFW.GLFW_KEY_4;
            case 5 -> GLFW.GLFW_KEY_5;
            case 6 -> GLFW.GLFW_KEY_6;
            case 7 -> GLFW.GLFW_KEY_7;
            case 8 -> GLFW.GLFW_KEY_8;
            case 9 -> GLFW.GLFW_KEY_9;
            case 10 -> GLFW.GLFW_KEY_0;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }
}
